package kouch.client


import io.ktor.client.call.*
import io.ktor.client.plugins.*
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
import io.ktor.http.HttpStatusCode.Companion.UnsupportedMediaType
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kouch.*
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class KouchDatabaseService(
    val context: Context,
) {
    companion object {
        val systemDbs = listOf(
            "_users",
            "_replicator",
            "_global_changes"
        )
            .map { KouchDatabase.Name(it) }
    }


    suspend fun isExist(
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
    ): Boolean {
        val response = context.request(method = Head, path = db.value)
        val text = response.bodyAsText()
        return when (response.status) {
            OK -> true
            NotFound -> false
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun <T : KouchDocument> isExistFor(
        kClass: KClass<out T>,
    ) = isExist(context.getMetadata(kClass).databaseName)

    suspend fun get(
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
    ): KouchDatabase.GetResponse? {
        val response = context.request(method = Get, path = db.value)
        val text = response.bodyAsText()
        return when (response.status) {
            OK -> context.systemJson.decodeFromString(text)
            NotFound -> null
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun getAll(
        request: KouchServer.AllDbsRequest = KouchServer.AllDbsRequest(),
    ): List<KouchDatabase.Name> {
        val response = context.request(
            method = Get,
            path = "_all_dbs",
            body = Json.encodeToString(request)
        )
        val text = response.bodyAsText()
        return when (response.status) {
            OK -> context.systemJson
                .decodeFromJsonElement<List<String>>(context.responseJson.parseToJsonElement(text))
                .map { KouchDatabase.Name(it) }
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun create(
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false,
    ) {

        val response = context.request(
            method = Put,
            path = db.value,
            parameters = mapOf(
                "q" to partitions,
                "n" to replicas,
                "partitioned" to partitioned
            )
        )
        val text = response.bodyAsText()
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

    suspend fun <T : KouchDocument> createForEntity(
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

    suspend fun <T : KouchDocument> createForEntity(
        kClass: KClass<out T>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false,
    ) = create(
        db = context.getMetadata(kClass).databaseName,
        partitions = partitions,
        replicas = replicas,
        partitioned = partitioned
    )


    //TODO : speedup with parallel coroutines
    suspend fun <T : KouchDocument> createForEntities(
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

    suspend fun <T : KouchDocument> createForEntitiesIfNotExists(
        kClasses: List<KClass<out T>>,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false,
    ) {
        val existedDatabase = getAll()
        kClasses
            .map { context.getMetadata(it).databaseName }
            .toSet()
            .filter { it !in existedDatabase }
            .forEach { create(it, partitions, replicas, partitioned) }
    }

    suspend fun delete(
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
    ) {
        val response = context.request(method = Delete, path = db.value)
        val text = response.bodyAsText()
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
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
        request: KouchDatabase.ChangesRequest,
        reconnectionDelay: Duration = 2.seconds,
        entities: List<KClass<out KouchDocument>>,
        listener: suspend (entry: KouchDatabase.ChangesResponse.Result) -> Unit,
    ) = scope.launch {
        val classNameToKClass = entities.associateBy { context.getMetadata(it).className.value }
        var since = request.since
        while (true) {
            try {
                val queryString = context.systemJson
                    .encodeToJsonElement(request.copy(since = since))
                    .jsonObject
                    .plus("feed" to JsonPrimitive("continuous"))
                    .minus("doc_ids")
                    .let { context.systemJson.encodeToUrl(JsonObject(it), JsonObject::class) }

                val bodyMap = if (request.doc_ids.isEmpty()) {
                    emptyMap()
                } else {
                    mapOf("doc_ids" to JsonArray(request.doc_ids.map { JsonPrimitive(it.value) }))
                }
                val body = context.systemJson.encodeToString(JsonObject(bodyMap))

                val requestStatement = context.requestStatement(
                    method = Post,
                    path = "$db/_changes$queryString",
                    body = TextContent(
                        text = body,
                        contentType = ContentType.Application.Json
                    ),
                    timeout = HttpTimeout.INFINITE_TIMEOUT_MS
                )
                requestStatement.execute { response ->
                    val channel = response.body<ByteReadChannel>()
                    channel
                        .readByLineAsFlow()
                        .collect { line ->
//                            println(line)
                            val responseJson = context.systemJson.parseToJsonElement(line).jsonObject
                            if (responseJson["error"] != null) {
                                val error = context.systemJson.decodeFromJsonElement<ErrorResponse>(responseJson)
                                IllegalStateException("$error").printStackTrace()
                            } else {
                                if (responseJson.containsKey("last_seq")) {
//                                    val result = context.systemJson.decodeFromJsonElement<KouchDatabase.ChangesResponse.RawLastResult>(responseJson)
                                    //println(result)
                                } else {
                                    val result = context.systemJson.decodeFromJsonElement<KouchDatabase.ChangesResponse.RawResult>(responseJson)
                                    val doc = if (request.include_docs && !result.deleted) {
                                        val jsonDoc = result.doc!!

                                        val className = jsonDoc[context.classField]?.jsonPrimitive?.content ?: return@collect
                                        val kClass = classNameToKClass[className]
                                            ?: throw IllegalArgumentException("changesContinuous: Unknown classField value: $className")
                                        context.decodeKouchDocumentFromJsonElement(jsonDoc, kClass)
                                    } else {
                                        null
                                    }
                                    since = result.seq
                                    listener(
                                        KouchDatabase.ChangesResponse.Result(
                                            changes = result.changes,
                                            id = result.id,
                                            seq = result.seq,
                                            deleted = result.deleted,
                                            doc = doc
                                        )
                                    )
                                }
                            }
                        }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                delay(reconnectionDelay)
            }
        }
    }

    private suspend fun ByteReadChannel.readByLineAsFlow() = channelFlow {
        while (isActive) {
            val line = readUTF8Line() ?: break
            if (line.isNotEmpty()) {
                send(line)
            }
        }
        awaitClose {
            this@readByLineAsFlow.cancel()
        }
    }

    suspend inline fun <reified T : KouchDocument> bulkGet(
        ids: Iterable<KouchDocument.Id>,
        db: KouchDatabase.Name = context.getMetadata(T::class).databaseName,
    ): KouchDatabase.BulkGetResult<T> {
        val ret = bulkGet(
            db = db,
            ids = ids,
            entityClasses = listOf(T::class)
        )
        val entities = ret.entities.filterIsInstance<T>()
        if (entities.size != ret.entities.size) {
            throw IllegalArgumentException("Not all entities is ${T::class}, $ret")
        }
        return KouchDatabase.BulkGetResult(entities, ret.errors)
    }

    suspend fun bulkGet(
        ids: Iterable<KouchDocument.Id>,
        entityClasses: List<KClass<out KouchDocument>>,
        db: KouchDatabase.Name = context.settings.getPredefinedDatabaseName()!!,
    ): KouchDatabase.BulkGetResult<KouchDocument> {
        val classNameToKClass = entityClasses.associateBy { context.getMetadata(it).className.value }
        val body = buildJsonObject {
            put(
                key = "docs",
                element = buildJsonArray {
                    ids.forEach { add(buildJsonObject { put("id", it.value) }) }
                }
            )
        }

        val response = context.request(
            method = Post,
            path = "$db/_bulk_get",
            body = TextContent(
                text = context.systemJson.encodeToString(body),
                contentType = ContentType.Application.Json
            ),
        )


        val text = response.bodyAsText()
        return when (response.status) {
            OK,
            -> {
                val responseJson = context.systemJson.parseToJsonElement(text).jsonObject
                val result = context.systemJson.decodeFromJsonElement<KouchDatabase.BulkGetRawResult>(responseJson)

                val results = result.results
                    .mapNotNull {
                        val doc = it.docs.single()
                        when {
                            doc.ok != null -> {
                                val jsonDoc = doc.ok
                                if (jsonDoc["_deleted"]?.jsonPrimitive?.boolean == true) {
                                    null
                                } else {
                                    val className = jsonDoc[context.classField]?.jsonPrimitive?.content
                                        ?: throw IllegalArgumentException("Can't find classField value $jsonDoc")
                                    val kClass = classNameToKClass[className]
                                        ?: throw IllegalArgumentException("bulkGet: Unknown classField value: $className")
                                    context.decodeKouchDocumentFromJsonElement(jsonDoc, kClass)
                                }
                            }
                            doc.error != null -> doc.error
                            else -> throw IllegalArgumentException("ok or error should be not null")
                        }
                    }

                val entities = results.filterIsInstance<KouchDocument>()
                val errors = results.filterIsInstance<KouchDatabase.BulkGetRawResult.Result.Doc.Error>()
                if (results.any { it !is KouchDocument && it !is KouchDatabase.BulkGetRawResult.Result.Doc.Error }) {
                    throw IllegalArgumentException("Unsupported results: $results")
                }
                KouchDatabase.BulkGetResult(entities, errors)
            }
            BadRequest,
            Unauthorized,
            NotFound,
            UnsupportedMediaType,
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    class BulkUpsertResult<T>(
        private val getResponseCallback: () -> List<KouchDatabase.BulkUpsertResponse>,
        private val getUpdatedEntitiesCallback: () -> List<T>,
    ) {
        fun getResponseAndUpdatedEntities() = getResponseCallback() to getUpdatedEntitiesCallback()
        fun getResponse() = getResponseCallback()
        fun getUpdatedEntities(failOnError: Boolean = true): List<T> {
            if (failOnError) {
                val responses = getResponseCallback()
                if (responses.any { it.ok != true }) {
                    throw BulkUpsertFailed("${responses.filter { it.ok != true }}")
                }
            }
            return getUpdatedEntitiesCallback()
        }
    }

    suspend inline fun <reified T : KouchDocument> bulkUpsert(
        entities: Iterable<T> = emptyList(),
        entitiesToDelete: Iterable<KouchDocument> = emptyList(),
    ) = bulkUpsert(
        entities = entities,
        entitiesToDelete = entitiesToDelete,
        kClass = T::class,
        db = context.getMetadata(T::class).databaseName
    )


    suspend fun <T : KouchDocument> bulkUpsert(
        entities: Iterable<T> = emptyList(),
        entitiesToDelete: Iterable<KouchDocument> = emptyList(),
        kClass: KClass<T>,
        db: KouchDatabase.Name,
    ): BulkUpsertResult<T> {

        val className = context.getMetadata(kClass).className
        val jsonArray = buildJsonArray {
            entities.forEach { add(element = context.encodeToKouchDocumentJson(kClass, it, className)) }
            entitiesToDelete.forEach {
                add(element = buildJsonObject {
                    put("_id", it.id.value)
                    put("_rev", it.revision?.value)
                    put("_deleted", true)
                })
            }
        }

        val request = KouchDatabase.BulkUpsertRequest(
            docs = jsonArray
        )

        val response = context.request(
            method = Post,
            path = "$db/_bulk_docs",
            body = TextContent(
                text = context.systemJson.encodeToString(request),
                contentType = ContentType.Application.Json
            ),
        )
        val text = response.bodyAsText()
        return when (response.status) {
            Created,
            -> {
                val getResponseCallback = {
                    context.systemJson.decodeFromString<List<KouchDatabase.BulkUpsertResponse>>(text)
                }
                val getUpdatedEntitiesCallback = {
                    val idToRev = getResponseCallback()
                        .filter { it.ok == true }
                        .associateBy({ it.id.value }, { it.rev ?: throw ResponseRevisionIsNullException(text) })
                    entities
                        .mapNotNull { it.copyWithRevision(idToRev[it.id.value] ?: return@mapNotNull null) }
                }
                BulkUpsertResult(
                    getUpdatedEntitiesCallback = getUpdatedEntitiesCallback,
                    getResponseCallback = getResponseCallback
                )
            }
            BadRequest,
            NotFound,
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
}
