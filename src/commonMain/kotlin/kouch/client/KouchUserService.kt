package kouch.client


import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.content.*
import kotlinx.serialization.decodeFromString
import kouch.*


class KouchUserService(val context: Context) {


    suspend fun create(
        request: KouchUser.Request
    ): KouchUser.StandardResponse {
        val bodyJson = context.systemJson.encodeToString(KouchUser.Request.serializer(), request)

        val response = context.request(
            method = Put,
            path = "_users/org.couchdb.user:${request.name}",
            body = TextContent(bodyJson, contentType = ContentType.Application.Json)
        )

        val text = response.bodyAsText()
        when (response.status) {
            Created,
            Accepted,
            BadRequest,
            Unauthorized,
            NotFound,
            Forbidden
            -> {
                val responseBody = context.systemJson.decodeFromString<KouchUser.StandardResponse>(text)
                when {

                    responseBody.error != null -> throw KouchUserException(responseBody.toString())
                    responseBody.rev == null -> throw ResponseRevisionIsNullException(responseBody.toString())
                    else -> return responseBody
                }
            }
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun delete(
        user: KouchUser.User
    ): KouchUser.StandardResponse {
        when {
            user.name.isBlank() -> throw BlankUserNameException(user.toString())
            user.revision == null -> throw RevisionIsNullException(user.toString())
        }

        val response = context.request(
            method = Delete,
            path = "_users/org.couchdb.user:${user.name}",
            parameters = mapOf("rev" to user.revision)
        )

        val text = response.bodyAsText()
        when (response.status) {
            OK,
            Accepted,
            BadRequest,
            Unauthorized,
            NotFound,
            Conflict
            -> {
                val responseBody = context.systemJson.decodeFromString<KouchUser.StandardResponse>(text)
                when {
                    responseBody.error != null -> throw KouchUserException(responseBody.toString())
                    responseBody.rev == null -> throw ResponseRevisionIsNullException(responseBody.toString())
                    else -> return responseBody
                }
            }
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }

    suspend fun delete(
        id: String,
        revision: String
    ): KouchUser.StandardResponse {
        val response = context.request(
            method = Delete,
            path = "_users/${id}",
            parameters = mapOf("rev" to revision)
        )

        val text = response.bodyAsText()
        when (response.status) {
            OK,
            Accepted,
            BadRequest,
            Unauthorized,
            NotFound,
            Conflict
            -> {
                val responseBody = context.systemJson.decodeFromString<KouchUser.StandardResponse>(text)
                when {
                    responseBody.error != null -> throw KouchUserException(responseBody.toString())
                    responseBody.rev == null -> throw ResponseRevisionIsNullException(responseBody.toString())
                    else -> return responseBody
                }
            }
            else -> throw UnsupportedStatusCodeException("$response: $text")
        }
    }
}
