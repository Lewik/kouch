package kouch.client


import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kouch.*
import kotlin.reflect.KClass


class KouchDesignService(val context: Context, val kouchDocumentService: KouchDocumentService) {

    @Serializable
    data class ViewRequest(
        val conflicts: Boolean = false,
        val descending: Boolean = false,
        val endkey: JsonElement? = null,
        val endkey_docid: String? = null,
        val group: Boolean = false,
        val group_level: Int? = null,
        val include_docs: Boolean = false,
        val attachments: Boolean = false,
        val att_encoding_info: Boolean = false,
        val inclusive_end: Boolean = true,
        val key: JsonElement? = null,
        val keys: JsonArray? = null,
        val limit: Int? = null,
        val reduce: Boolean? = null,
        val skip: Int = 0,
        val sorted: Boolean = true,
        val stable: Boolean = false,
        val startkey: JsonElement? = null,
        val startkey_docid: String? = null,
        val update: Update = Update.TRUE,
        val update_seq: Boolean = false,
    ) {
        @Suppress("EnumEntryName")
        @Serializable
        enum class Update {
            @SerialName("true")
            TRUE,

            @SerialName("false")
            FALSE,

            @SerialName("lazy")
            LAZY
        }
    }

    data class ViewResponse(
        val offset: Int,
        val total_rows: Int,
        val update_seq: JsonObject? = null
    )

    @Serializable
    class IntermediateViewResponse(
//        val id: String? = null,
//        val rev: String? = null,
//        val error: String? = null,
//        val reason: String? = null,
        val offset: Int,
        val rows: List<ViewRow>,
        val total_rows: Int,
        val update_seq: JsonObject? = null
    ) {
        @Serializable
        class ViewRow(
            val id: String,
            val key: JsonElement,
            val value: JsonElement
        )
    }

    suspend fun getWithResponse(
        id: String,
        db: DatabaseName,
        getQueryParameters: KouchDocument.GetQueryParameters? = null
    ) = kouchDocumentService.getWithResponse<KouchDesign>(id, db, getQueryParameters)

    suspend fun upsert(
        designDocument: KouchDesign,
        databaseName: DatabaseName,
        putQueryParameters: KouchDocument.PutQueryParameters? = null
    ) = kouchDocumentService.upsert(
        entity = designDocument,
        metadata = KouchMetadata.Design(
            databaseName = databaseName
        ),
        putQueryParameters = putQueryParameters
    )

    suspend inline fun delete(
        entity: KouchDesign,
        batch: Boolean = false,
        databaseName: DatabaseName,
    ) = kouchDocumentService.delete(entity, batch, databaseName)


    suspend inline fun <reified T : Any> getView(
        db: DatabaseName,
        id: String,
        viewName: String,
        request: ViewRequest = ViewRequest()
    ) = getView(
        db = db,
        id = id,
        viewName = viewName,
        request = request,
        resultKClass = T::class
    )

    suspend inline fun <reified T : Any, reified KE : KouchEntity> getView(
        id: String,
        viewName: String,
        request: ViewRequest = ViewRequest()
    ) = getView(
        db = context.getMetadata(KE::class).databaseName,
        id = id,
        viewName = viewName,
        request = request,
        resultKClass = T::class
    )

    class Result<T>(
        val result: List<T>,
        val response: ViewResponse
    )

    suspend fun <T : Any> getView(
        db: DatabaseName,
        id: String,
        viewName: String,
        request: ViewRequest = ViewRequest(),
        resultKClass: KClass<out T>
    ): Result<T> {
        val queryString = context.systemQueryParametersJson.encodeNullableToUrl(request)

        val response = context.request(
            method = HttpMethod.Get,
            path = "${db.value}/_design/$id/_view/$viewName$queryString"
        )

        val text = response.readText()
        return when (response.status) {
            HttpStatusCode.OK -> {
                val intermediateResponse = context.systemJson.decodeFromString<IntermediateViewResponse>(text)
                val entities = intermediateResponse.rows.map {
                    context.decodeKouchEntityFromJsonElement(it.value, resultKClass)
                }
                Result(
                    entities, ViewResponse(
                        offset = intermediateResponse.offset,
                        total_rows = intermediateResponse.total_rows,
                        update_seq = intermediateResponse.update_seq
                    )
                )
            }
            HttpStatusCode.NotFound,
            HttpStatusCode.NotModified,
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
//
//    //TODO RENAME
//    suspend fun updateView(
//        db: DatabaseName,
//        id: String,
//        viewName: String,
//        request: KouchDesign.ViewRequest
//    ): KouchDesign.ViewResponse? {
//        val body = context.systemJson.encodeToString(KouchDesign.ViewRequest.serializer(), request)
//
//        val response = context.request(
//            method = Post,
//            path = "$db/_design/$id/_view/$viewName",
//            body = TextContent(body, contentType = ContentType.Application.Json)
//        )
//
//        return when (response.status) {
//            OK,
//            Accepted,
//            BadRequest,
//            Unauthorized,
//            NotFound,
//            Conflict
//            -> {
//                val responseBody = context.systemJson.decodeFromString<KouchDesign.ViewResponse>(response.readText())
//                when {
//                    responseBody.error != null -> throw KouchDesignViewResponseException(responseBody.error + " " + responseBody.reason, responseBody)
//                    else -> responseBody
//                }
//            }
//            else -> throw UnsupportedStatusCodeException("$response: $text")
//        }
//    }
//
//    suspend fun sendQueries(
//        db: DatabaseName,
//        id: String,
//        viewName: String,
//        queries: JsonArray
//    ): JsonObject {
//        val response = context.request(
//            method = Post,
//            path = "$db/_design/$id/_view/$viewName/queries",
//            body = TextContent(Json.encodeToString(buildJsonObject { put("queries", queries) }), contentType = ContentType.Application.Json)
//        )
//
//        return when (response.status) {
//            OK -> context.systemJson.decodeFromString(response.readText())
//            BadRequest,
//            Unauthorized,
//            NotFound,
//            InternalServerError
//            -> throw IllegalStateException(context.systemJson.decodeFromString<JsonObject>(response.readText()).toString())
//            else -> throw UnsupportedStatusCodeException("$response: $text")
//        }
//    }
}
