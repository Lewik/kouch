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

    suspend inline fun <reified T : KouchDocument> get(
        id: KouchDocument.Id,
        db: KouchDatabase.Name = context.getMetadata(T::class).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null,
    ) = get(id, T::class, db, parameters)

    suspend fun <T : KouchDocument> get(
        id: KouchDocument.Id,
        kClass: KClass<T>,
        db: KouchDatabase.Name = context.getMetadata(kClass).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null,
    ): T? {
        val queryString = context.systemQueryParametersJson.encodeNullableToUrl(parameters, KouchDocument.GetQueryParameters::class)

        val response = context.request(
            method = Get,
            path = "$db/$pathPart${id.value}$queryString"
        )

        val text = response.bodyAsText()
        return when (response.status) {
            //TODO : error if <T> removed
            OK -> context.decodeKouchDocumentFromJsonElement(
                context.responseJson.parseToJsonElement(text).filterNonUnderscoredFieldsWithIdRev(),
                kClass
            )
            NotFound -> null
            NotModified,
            BadRequest,
            Unauthorized,
            Forbidden,
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend inline fun <reified T : KouchDocument> getWithResponse(
        id: KouchDocument.Id,
        db: KouchDatabase.Name = context.getMetadata(T::class).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null,
    ) = getWithResponse(id, T::class, db, parameters)

    suspend fun <T : KouchDocument> getWithResponse(
        id: KouchDocument.Id,
        kClass: KClass<T>,
        db: KouchDatabase.Name = context.getMetadata(kClass).databaseName,
        parameters: KouchDocument.GetQueryParameters? = null,
    ): Pair<KouchDocument.GetResponse, T?> {
        val queryString = context.systemQueryParametersJson.encodeNullableToUrl(parameters, KouchDocument.GetQueryParameters::class)

        val response = context.request(
            method = Get,
            path = "$db/$pathPart${id.value}$queryString"
        )

        val text = response.bodyAsText()
        return when (response.status) {
            OK -> {
                context
                    .responseJson
                    .parseToJsonElement(text)
                    .splitUnderscoredAndNonUnderscoredFields()
                    .let { (responseJson, entityJson) ->
                        Pair(
                            context.systemJson.decodeFromJsonElement(responseJson),
                            context.decodeKouchDocumentFromJsonElement(entityJson, kClass),
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
            Forbidden,
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend inline fun <reified T : KouchDocument> insert(
        entity: T,
        metadata: KouchMetadata.Entity = context.getMetadata(T::class),
        parameters: KouchDocument.PutQueryParameters? = null,
    ) = insert(entity, T::class, metadata, parameters)

    suspend fun <T : KouchDocument> insert(
        entity: T,
        kClass: KClass<T>,
        metadata: KouchMetadata.Entity = context.getMetadata(kClass),
        parameters: KouchDocument.PutQueryParameters? = null,
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

    suspend inline fun <reified T : KouchDocument> update(
        entity: T,
        metadata: KouchMetadata.Entity = context.getMetadata(T::class),
        parameters: KouchDocument.PutQueryParameters? = null,
    ) = update(entity, T::class, metadata, parameters)

    suspend fun <T : KouchDocument> update(
        entity: T,
        kClass: KClass<T>,
        metadata: KouchMetadata.Entity = context.getMetadata(kClass),
        parameters: KouchDocument.PutQueryParameters? = null,
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
        private val getUpdatedEntityCallback: () -> T,
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

    suspend inline fun <reified T : KouchDocument> upsert(
        entity: T,
        metadata: KouchMetadata = context.getMetadata(T::class),
        parameters: KouchDocument.PutQueryParameters? = null,
    ) = upsert(entity, T::class, metadata, parameters)

    suspend fun <T : KouchDocument> upsert(
        entity: T,
        kClass: KClass<T>,
        metadata: KouchMetadata = context.getMetadata(kClass),
        parameters: KouchDocument.PutQueryParameters? = null,
    ): PutResult<T> {
        val queryString = context.systemQueryParametersJson.encodeNullableToUrl(parameters, KouchDocument.PutQueryParameters::class)

        val response = when (metadata) {
            is KouchMetadata.Entity -> context.request(
                method = Put,
                path = "${metadata.databaseName}/$pathPart${entity.id.value}$queryString",
                body = context.encodeToKouchDocument(entity, kClass, metadata.className)
            )
            is KouchMetadata.Design -> context.request(
                method = Put,
                path = "${metadata.databaseName}/$pathPart${entity.id.value}$queryString",
                body = context.encodeToKouchDesign(entity, kClass)
            )
        }


        val text = response.bodyAsText()
        return when (response.status) {
            Created,
            Accepted,
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
            Conflict,
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend inline fun <reified T : KouchDocument> delete(
        entity: T,
        batch: Boolean = false,
        db: KouchDatabase.Name = context.getMetadata(T::class).databaseName,
    ) = delete(entity, T::class, batch, db)

    suspend fun <T : KouchDocument> delete(
        entity: T,
        kClass: KClass<T>,
        batch: Boolean = false,
        db: KouchDatabase.Name = context.getMetadata(kClass).databaseName,
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

    suspend fun <T : KouchDocument> delete(
        id: KouchDocument.Id,
        kClass: KClass<T>,
        revision: KouchDocument.Rev,
        batch: Boolean = false,
    ) =
        delete(
            id = id,
            revision = revision,
            batch = batch,
            db = context.getMetadata(kClass).databaseName
        )

    suspend fun delete(
        id: KouchDocument.Id,
        revision: KouchDocument.Rev,
        batch: Boolean = false,
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
    ): () -> KouchDocument.DeleteResponse {
        val batchStr = if (batch) "ok" else null
        return delete(
            id = id,
            db = db,
            parameters = KouchDocument.DeleteQueryParameters(rev = revision, batch = batchStr)
        )
    }


    suspend fun delete(
        id: KouchDocument.Id,
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
        parameters: KouchDocument.DeleteQueryParameters,
    ): () -> KouchDocument.DeleteResponse {
        val queryString = context.systemQueryParametersJson.encodeToUrl(parameters, KouchDocument.DeleteQueryParameters::class)
        val response = context.request(
            method = Delete,
            path = "$db/${id.value}$queryString",
        )

        val text = response.bodyAsText()
        return when (response.status) {
            OK,
            Accepted,
            NotFound,
            -> {
                { context.systemJson.decodeFromString(text) }
            }
            BadRequest,
            Unauthorized,

            Conflict,
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
}
