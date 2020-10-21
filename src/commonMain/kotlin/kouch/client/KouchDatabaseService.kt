package kouch.client


import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.PreconditionFailed
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kouch.*
import kotlin.reflect.KClass

class KouchDatabaseService(
    val context: Context,
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
            Accepted,
            -> Unit
            BadRequest,
            Unauthorized,
            PreconditionFailed,
            -> throw KouchDatabaseException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun <T : KouchEntity> createForEntity(
        entity: T,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false,
    ) = createForEntity(
        kClass = entity::class,
        partitions = partitions,
        replicas = replicas,
        partitioned = partitioned
    )

    suspend fun <T : KouchEntity> createForEntity(
        kClass: KClass<out T>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false,
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
    suspend fun <T : KouchEntity> createForEntities(
        kClasses: List<KClass<out T>>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false,
    ) = kClasses
        .forEach {
            createForEntity(
                kClass = it,
                partitions = partitions,
                replicas = replicas,
                partitioned = partitioned
            )
        }

    suspend fun <T : KouchEntity> createForEntitiesIfNotExists(
        kClasses: List<KClass<out T>>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false,
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
            Accepted,
            -> Unit
            BadRequest, // To avoid deleting a database, CouchDB will respond with the HTTP status code 400 when the request URL includes a ?rev= parameter. This suggests that one wants to delete a document but forgot to add the document id to the URL.
            Unauthorized,
            PreconditionFailed,
            -> throw KouchDatabaseException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun createSystemDbs() = systemDbs.forEach { create(db = it) }


    suspend fun changesContinuous(
        scope: CoroutineScope,
        db: DatabaseName,
        request: KouchDatabase.ChangesRequest,
        listener: suspend (entry: KouchDatabase.ChangesResponse.Result) -> Unit,
    ) = scope.launch {
        val queryString = context.systemJson
            .encodeToJsonElement(request)
            .jsonObject
            .plus("feed" to JsonPrimitive("continuous"))
            .minus("doc_ids")
            .let { context.systemJson.encodeToUrl(JsonObject(it)) }

        val bodyMap = if (request.doc_ids.isEmpty()) {
            emptyMap()
        } else {
            mapOf("doc_ids" to JsonArray(request.doc_ids.map { JsonPrimitive(it) }))
        }
        val body = context.systemJson.encodeToString(JsonObject(bodyMap))

        context.requestStatement(
            method = Post,
            path = "${db.value}/_changes$queryString",
            body = TextContent(
                text = body,
                contentType = ContentType.Application.Json
            )
        ).execute { response ->
            val channel = response.receive<ByteReadChannel>()
            val flow = channel.readAsFlow()
            flow.collect(listener)
        }
    }

    private suspend fun ByteReadChannel.readAsFlow() = channelFlow {

        while (isActive) {
            val line = readUTF8Line()
            if (line?.isNotEmpty() == true) {
                val result = context.systemJson.decodeFromString<KouchDatabase.ChangesResponse.Result>(line)
                send(result)
            }
        }

        awaitClose {
            this@readAsFlow.cancel()
        }
    }
}
