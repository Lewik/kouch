package kouch.client


import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
        val update_seq: JsonObject? = null,
    )

    @Serializable
    class IntermediateViewResponse(
        val offset: Int,
        val rows: List<ViewRow>,
        val total_rows: Int,
        val update_seq: JsonObject? = null,
    ) {
        @Serializable
        class ViewRow(
            val id: String,
            val key: JsonElement,
            val value: JsonElement?,
            val doc: JsonElement? = null,
        )
    }

    suspend fun getWithResponse(
        id: String,
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
        parameters: KouchDocument.GetQueryParameters? = null,
    ) = kouchDocumentService.getWithResponse<KouchDesign>(id, db, parameters)

    suspend fun upsert(
        ddoc: KouchDesign,
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
        parameters: KouchDocument.PutQueryParameters? = null,
    ) = kouchDocumentService.upsert(
        entity = ddoc,
        metadata = KouchMetadata.Design(
            databaseName = db
        ),
        parameters = parameters
    )

    suspend inline fun delete(
        entity: KouchDesign,
        batch: Boolean = false,
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
    ) = kouchDocumentService.delete(entity, batch, db)


    suspend inline fun <reified RESULT : Any> getView(
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
        id: String,
        view: String,
        request: ViewRequest = ViewRequest(),
    ) = getView(
        db = db,
        id = id,
        view = view,
        request = request,
        resultKClass = RESULT::class
    )

    suspend inline fun <reified RESULT : Any, reified SOURCE_ENTITY : KouchEntity> getView(
        id: String,
        view: String,
        request: ViewRequest = ViewRequest(),
    ) = getView(
        db = context.getMetadata(SOURCE_ENTITY::class).databaseName,
        id = id,
        view = view,
        request = request,
        resultKClass = RESULT::class
    )

    class Result<T>(
        val result: List<T>,
        val response: ViewResponse,
    )

    suspend fun <RESULT : Any> getView(
        db: DatabaseName = context.settings.getPredefinedDatabaseName()!!,
        id: String,
        view: String,
        request: ViewRequest = ViewRequest(),
        resultKClass: KClass<out RESULT>,
    ): Result<RESULT?> {

        val response = context.request(
            method = HttpMethod.Post,
            path = "${db.value}/_design/$id/_view/$view",
            body = TextContent(
                text = context.systemJson.encodeToString(request),
                contentType = ContentType.Application.Json
            )
        )

        val text = response.readText()
        return when (response.status) {
            HttpStatusCode.OK -> {
                val intermediateResponse = context.systemJson.decodeFromString<IntermediateViewResponse>(text)
                val entities = intermediateResponse.rows.map { viewRow ->
                    val resultJson = if (request.include_docs) {
                        viewRow.doc ?: throw DocIsNullException(text)
                    } else {
                        viewRow.value
                    }
                    resultJson?.let { context.decodeKouchEntityFromJsonElement(it, resultKClass) }
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
            HttpStatusCode.Forbidden,
            -> throw KouchDocumentException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
}
