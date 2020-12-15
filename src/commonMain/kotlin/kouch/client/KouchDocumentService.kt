package kouch.client

import io.ktor.client.statement.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.NotModified
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement
import kouch.*
import kotlin.reflect.KClass


class KouchDocumentService(
    val context: Context,
    type: Type,
) {
    enum class Type {
        DOC,
        DESIGN
    }

    val pathPart = when (type) {
        Type.DOC -> ""
        Type.DESIGN -> "_design/"
    }

    suspend inline fun <reified T : KouchEntity> get(
        id: String,
        db: DatabaseName = context.getMetadata(T::class).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null
    ) = get(id, T::class, db, parameters)

    suspend fun <T : KouchEntity> get(
        id: String,
        kClass: KClass<T>,
        db: DatabaseName = context.getMetadata(kClass).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null
    ): T? {
        val queryString = context.systemQueryParametersJson.encodeNullableToUrl(parameters, KouchDocument.GetQueryParameters::class)

        val response = context.request(
            method = Get,
            path = "${db.value}/$pathPart$id$queryString"
        )

        val text = response.readText()
        return when (response.status) {
            //TODO : error if <T> removed
            OK -> context.decodeKouchEntityFromJsonElement(
                context.responseJson.parseToJsonElement(text).filterNonUnderscoredFieldsWithIdRev(),
                kClass
            )
            NotFound -> null
            NotModified,
            BadRequest,
            Unauthorized,
            Forbidden
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend inline fun <reified T : KouchEntity> getWithResponse(
        id: String,
        db: DatabaseName = context.getMetadata(T::class).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null
    ) = getWithResponse(id, T::class, db, parameters)

    suspend fun <T : KouchEntity> getWithResponse(
        id: String,
        kClass: KClass<T>,
        db: DatabaseName = context.getMetadata(kClass).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null
    ): Pair<KouchDocument.GetResponse, T?> {
        val queryString = context.systemQueryParametersJson.encodeNullableToUrl(parameters, KouchDocument.GetQueryParameters::class)

        val response = context.request(
            method = Get,
            path = "${db.value}/$pathPart$id$queryString"
        )

        val text = response.readText()
        return when (response.status) {
            OK -> {
                context
                    .responseJson
                    .parseToJsonElement(text)
                    .splitUnderscoredAndNonUnderscoredFields()
                    .let { (responseJson, entityJson) ->
                        Pair(
                            context.systemJson.decodeFromJsonElement(responseJson),
                            context.decodeKouchEntityFromJsonElement(entityJson, kClass),
                        )
                    }
            }
            NotFound -> Pair(
                context.systemJson.decodeFromJsonElement(context.systemJson.parseToJsonElement(text).filterUnderscoredFields()),
                null
            )
            NotModified,
            BadRequest,
            Unauthorized,
            Forbidden
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend inline fun <reified T : KouchEntity> insert(
        entity: T,
        metadata: KouchMetadata.Entity = context.getMetadata(T::class),
        parameters: KouchDocument.PutQueryParameters? = null
    ) = insert(entity, T::class, metadata, parameters)

    suspend fun <T : KouchEntity> insert(
        entity: T,
        kClass: KClass<T>,
        metadata: KouchMetadata.Entity = context.getMetadata(kClass),
        parameters: KouchDocument.PutQueryParameters? = null
    ): PutResult<T> = when {
        entity.id.isBlank() -> throw IdIsBlankException(entity.toString())
        entity.revision != null -> throw RevisionIsNotNullException(entity.toString())
        else -> upsert(
            entity = entity,
            kClass = kClass,
            metadata = metadata,
            parameters = parameters
        )
    }

    suspend inline fun <reified T : KouchEntity> update(
        entity: T,
        metadata: KouchMetadata.Entity = context.getMetadata(T::class),
        parameters: KouchDocument.PutQueryParameters? = null
    ) = update(entity, T::class, metadata, parameters)

    suspend fun <T : KouchEntity> update(
        entity: T,
        kClass: KClass<T>,
        metadata: KouchMetadata.Entity = context.getMetadata(kClass),
        parameters: KouchDocument.PutQueryParameters? = null
    ): PutResult<T> = when {
        entity.id.isBlank() -> throw IdIsBlankException(entity.toString())
        entity.revision == null -> throw RevisionIsNullException(entity.toString())
        else -> upsert(
            entity = entity,
            kClass = kClass,
            metadata = metadata,
            parameters = parameters
        )
    }


    class PutResult<T>(
        private val getResponseCallback: () -> KouchDocument.PutResponse,
        private val getUpdatedEntityCallback: () -> T
    ) {
        fun getResponseAndUpdatedEntity() = getResponseCallback() to getUpdatedEntityCallback()
        fun getResponse() = getResponseCallback()
        fun getUpdatedEntity(failOnError: Boolean = true): T {
            if (failOnError) {
                val response = getResponseCallback()
                if (response.ok != true) {
                    throw PutFailed("$response")
                }
            }
            return getUpdatedEntityCallback()
        }
    }

    suspend inline fun <reified T : KouchEntity> upsert(
        entity: T,
        metadata: KouchMetadata = context.getMetadata(T::class),
        parameters: KouchDocument.PutQueryParameters? = null
    ) = upsert(entity, T::class, metadata, parameters)

    suspend fun <T : KouchEntity> upsert(
        entity: T,
        kClass: KClass<T>,
        metadata: KouchMetadata = context.getMetadata(kClass),
        parameters: KouchDocument.PutQueryParameters? = null
    ): PutResult<T> {
        val queryString = context.systemQueryParametersJson.encodeNullableToUrl(parameters, KouchDocument.PutQueryParameters::class)

        val response = when (metadata) {
            is KouchMetadata.Entity -> context.request(
                method = Put,
                path = "${metadata.databaseName.value}/$pathPart${entity.id}$queryString",
                body = context.encodeToKouchEntity(entity, kClass, metadata.className)
            )
            is KouchMetadata.Design -> context.request(
                method = Put,
                path = "${metadata.databaseName.value}/$pathPart${entity.id}$queryString",
                body = context.encodeToKouchDesign(entity, kClass)
            )
        }


        val text = response.readText()
        return when (response.status) {
            Created,
            Accepted
            -> {
                val getResponseCallback = { context.responseJson.decodeFromString<KouchDocument.PutResponse>(text) }
                val getUpdatedEntityCallback = { entity.copyWithRevision(getResponseCallback().rev ?: throw ResponseRevisionIsNullException(text)) }
                PutResult(getResponseCallback, getUpdatedEntityCallback)

//                val responseBody = context.responseJson.decodeFromString<KouchDocument.PutResponse>(text)
//                val updatedEntity = entity.copyWithRevision(responseBody.rev ?: throw ResponseRevisionIsNullException(text))
//                updatedEntity to responseBody
            }
            BadRequest,
            Unauthorized,
            NotFound,
            Conflict
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend inline fun <reified T : KouchEntity> delete(
        entity: T,
        batch: Boolean = false,
        db: DatabaseName = context.getMetadata(T::class).databaseName
    ) = delete(entity, T::class, batch, db)

    suspend fun <T : KouchEntity> delete(
        entity: T,
        kClass: KClass<T>,
        batch: Boolean = false,
        db: DatabaseName = context.getMetadata(kClass).databaseName
    ): () -> KouchDocument.DeleteResponse {
        val id = entity.id
        val revision = entity.revision
        return when {
            id.isBlank() -> throw IdIsBlankException(entity.toString())
            revision == null -> throw RevisionIsNullException(entity.toString())
            else -> delete(
                id = id,
                revision = revision,
                batch = batch,
                db = db
            )
        }
    }

    suspend fun <T : KouchEntity> delete(
        id: String,
        kClass: KClass<T>,
        revision: String,
        batch: Boolean = false
    ) =
        delete(
            id = id,
            revision = revision,
            batch = batch,
            db = context.getMetadata(kClass).databaseName
        )

    suspend fun delete(
        id: String,
        revision: String,
        batch: Boolean = false,
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!
    ): () -> KouchDocument.DeleteResponse {
        val batchStr = if (batch) "ok" else null
        return delete(
            id = id,
            db = db,
            parameters = KouchDocument.DeleteQueryParameters(rev = revision, batch = batchStr)
        )
    }


    suspend fun delete(
        id: String,
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
        parameters: KouchDocument.DeleteQueryParameters
    ): () -> KouchDocument.DeleteResponse {
        val queryString = context.systemQueryParametersJson.encodeToUrl(parameters, KouchDocument.DeleteQueryParameters::class)
        val response = context.request(
            method = Delete,
            path = "${db.value}/$id$queryString",
        )

        val text = response.readText()
        return when (response.status) {
            OK,
            Accepted,
            NotFound
            -> {
                { context.systemJson.decodeFromString(text) }
            }
            BadRequest,
            Unauthorized,

            Conflict
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
}
