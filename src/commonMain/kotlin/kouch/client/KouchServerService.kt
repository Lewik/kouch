package kouch.client

import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Get
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kouch.Context
import kouch.KouchDatabaseException
import kouch.KouchServerException
import kouch.UnsupportedStatusCodeException


class KouchServerService(val context: Context) {

    suspend fun root(): KouchServer.RootResponse {
        val response = context.request(
            method = Get,
            path = "/"
        )

        val text = response.readText()
        return when (response.status) {
            HttpStatusCode.OK -> context.systemJson.decodeFromString(text)
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    /**
     * The structure of the request must be identical to the original for the cancellation request to be honoured
     */
    suspend fun cancelPullReplication(request: KouchDatabase.PullReplicationRequestInput): KouchDatabase.ReplicationResponse {
        val cancelRequest = request.copy(cancel = true)
        return pullReplicate(cancelRequest)
    }

    /**
     * The create_target parameter is not destructive. If the database already exists, the replication proceeds as normal.
     */
    suspend fun pullReplicate(
        input: KouchDatabase.PullReplicationRequestInput
    ): KouchDatabase.ReplicationResponse {
        val request = input.toPullReplicationRequest()
//        println("pullReplicate: $request")
        val bodyJson = context.systemJson.encodeToString(request)
        val response = context.request(
            method = HttpMethod.Post,
            path = "_replicate",
            body = TextContent(bodyJson, contentType = ContentType.Application.Json)
        )

        val text = response.readText()
        return when (response.status) {
            HttpStatusCode.OK,
            HttpStatusCode.Accepted
            -> context.systemJson.decodeFromString(text)
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized,
            HttpStatusCode.NotFound,
            HttpStatusCode.InternalServerError
            -> throw KouchDatabaseException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    private fun KouchDatabase.PullReplicationRequestInput.toPullReplicationRequest() = KouchDatabase.ReplicationRequest(
        source = KouchDatabase.UrlWithHeader(
            url = "${sourceSettings.scheme}://${sourceSettings.host}:${sourceSettings.port}/${this.sourceDb.value}",
            headers = KouchDatabase.DbHeader(
                Authorization = context.getAdminBasic()
            )
        ),
        target = KouchDatabase.UrlWithHeader(
            url = this.targetDb.value,
            headers = KouchDatabase.DbHeader(
                Authorization = context.getAdminBasic()
            )
        ),
        create_target = createTargetDb,
        continuous = continuous,
        cancel = cancel
    )

    suspend fun activeTasks(): List<KouchServer.ActiveTaskResponse> {
        val response = context.request(
            method = Get,
            path = "/_active_tasks"
        )

        val text = response.readText()
        return when (response.status) {
            HttpStatusCode.OK -> {
                val json = Json(context.systemJson) {
                    ignoreUnknownKeys = true
                }
                json.decodeFromString(text)
            }
            HttpStatusCode.Unauthorized -> throw KouchServerException("$response: $text")
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
}
