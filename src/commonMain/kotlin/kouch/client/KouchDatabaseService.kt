package kouch.client


import io.ktor.client.statement.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.PreconditionFailed
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kouch.*
import kotlin.reflect.KClass

class KouchDatabaseService(
    val context: Context
) {
    companion object {
        val systemDbs = listOf(
            "_users",
            "_replicator",
            "_global_changes"
        )
            .map { DatabaseName(it) }
    }


    suspend fun isExist(db: DatabaseName): Boolean {
        val response = context.request(method = Head, path = db.value)
        val text = response.readText()
        return when (response.status) {
            OK -> true
            NotFound -> false
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun get(db: DatabaseName): KouchDatabase.GetResponse? {
        val response = context.request(method = Get, path = db.value)
        val text = response.readText()
        return when (response.status) {
            OK -> context.systemJson.decodeFromString(text)
            NotFound -> null
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun getAll(request: KouchServer.AllDbsRequest = KouchServer.AllDbsRequest()): List<DatabaseName> {
        val response = context.request(
            method = Get,
            path = "_all_dbs",
            body = Json.encodeToString(request)
        )
        val text = response.readText()
        return when (response.status) {
            OK -> context.systemJson
                .decodeFromJsonElement<List<String>>(context.responseJson.parseToJsonElement(text))
                .map { DatabaseName(it) }
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun create(db: DatabaseName, partitions: Int? = null, replicas: Int? = null, partitioned: Boolean = false) {

        val response = context.request(
            method = Put,
            path = db.value,
            parameters = mapOf(
                "q" to partitions,
                "n" to replicas,
                "partitioned" to partitioned
            )
        )
        val text = response.readText()
        when (response.status) {
            Created,
            Accepted
            -> Unit
            BadRequest,
            Unauthorized,
            PreconditionFailed
            -> throw KouchDatabaseException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend inline fun <reified T : KouchEntity> createForEntity(
        entity: T,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false
    ) = createForEntity(
        kClass = entity::class,
        partitions = partitions,
        replicas = replicas,
        partitioned = partitioned
    )

    suspend inline fun <reified T : KouchEntity> createForEntity(
        kClass: KClass<out T>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false
    ) = if (context.settings.databaseNaming is Settings.DatabaseNaming.DatabaseNameAnnotation) {
        create(
            db = context.getMetadata(kClass).databaseName,
            partitions = partitions,
            replicas = replicas,
            partitioned = partitioned
        )
    } else {
        println("No need to call createForEntities: Settings.DatabaseNaming.DatabaseNameAnnotation used")
    }


    //TODO : speedup with parallel coroutines
    suspend inline fun <reified T : KouchEntity> createForEntities(
        kClasses: List<KClass<out T>>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false
    ) = kClasses
        .forEach {
            createForEntity(
                kClass = it,
                partitions = partitions,
                replicas = replicas,
                partitioned = partitioned
            )
        }

    suspend inline fun <reified T : KouchEntity> createForEntitiesIfNotExists(
        kClasses: List<KClass<out T>>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false
    ) = if (context.settings.databaseNaming is Settings.DatabaseNaming.DatabaseNameAnnotation) {
        val existedDatabase = getAll()
        kClasses
            .filter { context.getMetadata(it).databaseName !in existedDatabase }
            .forEach { createForEntity(it, partitions, replicas, partitioned) }
    } else {
        println("No need to call createForEntitiesIfNotExists: Settings.DatabaseNaming.DatabaseNameAnnotation used")
    }

    suspend fun delete(db: DatabaseName) {
        val response = context.request(method = Delete, path = db.value)
        val text = response.readText()
        when (response.status) {
            OK,
            Accepted
            -> Unit
            BadRequest, // To avoid deleting a database, CouchDB will respond with the HTTP status code 400 when the request URL includes a ?rev= parameter. This suggests that one wants to delete a document but forgot to add the document id to the URL.
            Unauthorized,
            PreconditionFailed
            -> throw KouchDatabaseException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun createSystemDbs() = systemDbs.forEach { create(db = it) }


    //
//    suspend fun find(db: DatabaseName, request: KouchDatabase.SearchRequest): KouchDatabase.SearchResponse {
//        val json = settings.standardJson
//        val bodyJson = json.encodeToString(KouchDatabase.SearchRequest.serializer(), request)
//
//        val response = settings.client.post<HttpResponse>(
//            scheme = settings.scheme,
//            host = settings.host,
//            port = settings.port,
//            path = "$db/_find",
//            body = TextContent(bodyJson, contentType = ContentType.Application.Json)
//        ) {
//            headers[HttpHeaders.Authorization] = settings.getAdminBasic();
//        }
//
//        return when (response.status) {
//            HttpStatusCode.OK,
//            HttpStatusCode.BadRequest,
//            HttpStatusCode.Unauthorized,
//            HttpStatusCode.NotFound,
//            HttpStatusCode.InternalServerError,
//            HttpStatusCode.Forbidden
//            -> {
//                val body = settings.standardJson
//                    .decodeFromString<KouchDatabase.SearchResponse>(response.readText())
//                when {
//                    body.error != null -> throw IllegalStateException(body.reason)
//                    else -> body
//                }
//            }
//            else -> throw UnsupportedStatusCodeException("$response: $text")
//        }
//    }
//
//    suspend fun getAllDocs(db: DatabaseName): JsonObject? {
//        val response = settings.client.get<HttpResponse>(
//            scheme = settings.scheme,
//            host = settings.host,
//            port = settings.port,
//            path = "$db/_all_docs"
//        ) {
//            headers[HttpHeaders.Authorization] = settings.getAdminBasic();
//        }
//
//        return when (response.status) {
//            HttpStatusCode.OK
//            -> {
//                settings
//                    .standardJson
//                    .decodeFromString<JsonObject>(response.readText())
//            }
//            HttpStatusCode.NotFound -> null
//            else -> throw UnsupportedStatusCodeException("$response: $text")
//        }
//    }
//
//    suspend fun setRoles(db: DatabaseName, request: KouchDatabase.RolesRequest, user: KouchUser.User? = null): KouchDatabase.StandardResponse {
//        val json = settings.standardJson
//        val bodyJson = json.encodeToString(KouchDatabase.RolesRequest.serializer(), request)
//        val response = settings.client.put<HttpResponse>(
//            scheme = settings.scheme,
//            host = settings.host,
//            port = settings.port,
//            path = "$db/_security",
//            body = TextContent(bodyJson, contentType = ContentType.Application.Json)
//        ) {
//            headers[HttpHeaders.Authorization] = settings.getAdminBasic()
//        }
//
//        return when (response.status) {
//            HttpStatusCode.OK,
//            HttpStatusCode.Unauthorized,
//            HttpStatusCode.Forbidden
//            -> {
//                val body = settings.standardJson
//                    .decodeFromString<KouchDatabase.StandardResponse>(response.readText())
//                when {
//                    body.error != null -> throw IllegalStateException(body.reason)
//                    else -> body
//                }
//            }
//            else -> throw UnsupportedStatusCodeException("$response: $text")
//        }
//    }
//
}
