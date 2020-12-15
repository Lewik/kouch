package kouch.client


import io.ktor.client.call.*
import io.ktor.client.features.*
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kouch.*
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.seconds

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


    suspend fun isExist(
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!
    ): Boolean {
        val response = context.request(method = Head, path = db.value)
        val text = response.readText()
        return when (response.status) {
            OK -> true
            NotFound -> false
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun <T : KouchEntity> isExistFor(
        kClass: KClass<out T>,
    ) = isExist(context.getMetadata(kClass).databaseName)

    suspend fun get(
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!
    ): KouchDatabase.GetResponse? {
        val response = context.request(method = Get, path = db.value)
        val text = response.readText()
        return when (response.status) {
            OK -> context.systemJson.decodeFromString(text)
            NotFound -> null
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun getAll(
        request: KouchServer.AllDbsRequest = KouchServer.AllDbsRequest()
    ): List<DatabaseName> {
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

    suspend fun create(
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
        partitions: Int? = null,
        replicas: Int? = null,
        partitioned: Boolean = false
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
    ) = create(
        db = context.getMetadata(kClass).databaseName,
        partitions = partitions,
        replicas = replicas,
        partitioned = partitioned
    )


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
    ) {
        val existedDatabase = getAll()
        kClasses
            .map { context.getMetadata(it).databaseName }
            .toSet()
            .filter { it !in existedDatabase }
            .forEach { create(it, partitions, replicas, partitioned) }
    }

    suspend fun delete(
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!
    ) {
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
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
        request: KouchDatabase.ChangesRequest,
        reconnectionDelay: Duration = 2.seconds,
        entities: List<KClass<out KouchEntity>>,
        listener: suspend (entry: KouchDatabase.ChangesResponse.Result) -> Unit
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
                    mapOf("doc_ids" to JsonArray(request.doc_ids.map { JsonPrimitive(it) }))
                }
                val body = context.systemJson.encodeToString(JsonObject(bodyMap))

                val requestStatement = context.requestStatement(
                    method = Post,
                    path = "${db.value}/_changes$queryString",
                    body = TextContent(
                        text = body,
                        contentType = ContentType.Application.Json
                    ),
                    timeout = HttpTimeout.INFINITE_TIMEOUT_MS
                )
                requestStatement.execute { response ->
                    val channel = response.receive<ByteReadChannel>()
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
                                        context.decodeKouchEntityFromJsonElement(jsonDoc, kClass)
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

    suspend inline fun <reified T : KouchEntity> bulkGet(
        ids: Iterable<String>,
        db: DatabaseName = context.getMetadata(T::class).databaseName
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
        ids: Iterable<String>,
        entityClasses: List<KClass<out KouchEntity>>,
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!
    ): KouchDatabase.BulkGetResult<KouchEntity> {
        val classNameToKClass = entityClasses.associateBy { context.getMetadata(it).className.value }
        val body = buildJsonObject {
            put(
                key = "docs",
                element = buildJsonArray {
                    ids.forEach { add(buildJsonObject { put("id", it) }) }
                }
            )
        }

        val response = context.request(
            method = Post,
            path = "${db.value}/_bulk_get",
            body = TextContent(
                text = context.systemJson.encodeToString(body),
                contentType = ContentType.Application.Json
            ),
        )


        val text = response.readText()
        return when (response.status) {
            OK
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
                                    context.decodeKouchEntityFromJsonElement(jsonDoc, kClass)
                                }
                            }
                            doc.error != null -> doc.error
                            else -> throw IllegalArgumentException("ok or error should be not null")
                        }
                    }

                val entities = results.filterIsInstance<KouchEntity>()
                val errors = results.filterIsInstance<KouchDatabase.BulkGetRawResult.Result.Doc.Error>()
                if (results.any { it !is KouchEntity && it !is KouchDatabase.BulkGetRawResult.Result.Doc.Error }) {
                    throw IllegalArgumentException("Unsupported results: $results")
                }
                KouchDatabase.BulkGetResult(entities, errors)
            }
            BadRequest,
            Unauthorized,
            NotFound,
            UnsupportedMediaType
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    class BulkUpsertResult<T>(
        private val getResponseCallback: () -> List<KouchDatabase.BulkUpsertResponse>,
        private val getUpdatedEntitiesCallback: () -> List<T>
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

    suspend inline fun <reified T : KouchEntity> bulkUpsert(
        entities: Iterable<T> = emptyList(),
        entitiesToDelete: Iterable<KouchEntity> = emptyList()
    ) = bulkUpsert(
        entities = entities,
        entitiesToDelete = entitiesToDelete,
        kClass = T::class,
        db = context.getMetadata(T::class).databaseName
    )


    suspend fun <T : KouchEntity> bulkUpsert(
        entities: Iterable<T> = emptyList(),
        entitiesToDelete: Iterable<KouchEntity> = emptyList(),
        kClass: KClass<T>,
        db: DatabaseName
    ): BulkUpsertResult<T> {

        val className = context.getMetadata(kClass).className
        val jsonArray = buildJsonArray {
            entities.forEach { add(element = context.encodeToKouchEntityJson(kClass, it, className)) }
            entitiesToDelete.forEach {
                add(element = buildJsonObject {
                    put("_id", it.id)
                    put("_rev", it.revision)
                    put("_deleted", true)
                })
            }
        }

        val request = KouchDatabase.BulkUpsertRequest(
            docs = jsonArray
        )

        val response = context.request(
            method = Post,
            path = "${db.value}/_bulk_docs",
            body = TextContent(
                text = context.systemJson.encodeToString(request),
                contentType = ContentType.Application.Json
            ),
        )
        val text = response.readText()
        return when (response.status) {
            Created
            -> {
                val getResponseCallback = {
                    context.systemJson.decodeFromString<List<KouchDatabase.BulkUpsertResponse>>(text)
                }
                val getUpdatedEntitiesCallback = {
                    val idToRev = getResponseCallback()
                        .filter { it.ok == true }
                        .associateBy({ it.id }, { it.rev ?: throw ResponseRevisionIsNullException(text) })
                    entities
                        .mapNotNull { it.copyWithRevision(idToRev[it.id] ?: return@mapNotNull null) }
                }
                BulkUpsertResult(
                    getUpdatedEntitiesCallback = getUpdatedEntitiesCallback,
                    getResponseCallback = getResponseCallback
                )
            }
            BadRequest,
            NotFound
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
}
