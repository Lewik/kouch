package kouch

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.Json

class Context(
    val settings: Settings,
    val client: HttpClient = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 60000
            requestTimeoutMillis = 60000
        }
    },
    val strictSystemJson: Boolean = false,
    val classField: String = "class__",
    val responseJson: Json = Json {},
    val entityJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
    val systemJson: Json = Json {
        ignoreUnknownKeys = !strictSystemJson
    },
    val designJson: Json = Json {
        ignoreUnknownKeys = !strictSystemJson
        encodeDefaults = false
    },
    val systemQueryParametersJson: Json = Json {
        encodeDefaults = false
    }
) {
    suspend fun request(
        method: HttpMethod,
        path: String,
        body: Any = EmptyContent,
        parameters: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        timeout: Long? = null
    ) = client.request<HttpResponse> { buildRequest(path, method, body, headers, parameters, timeout) }

    suspend fun requestStatement(
        method: HttpMethod,
        path: String,
        body: Any = EmptyContent,
        parameters: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        timeout: Long? = null
    ) = client.request<HttpStatement> { buildRequest(path, method, body, headers, parameters, timeout) }

    private fun HttpRequestBuilder.buildRequest(
        path: String,
        method: HttpMethod,
        body: Any,
        headers: Map<String, String>,
        parameters: Map<String, Any?>,
        timeout: Long? = null
    ) {
        url(settings.scheme, settings.host, settings.port, path)
        this.method = method
        this.body = body
        this.headers[HttpHeaders.Authorization] = getAdminBasic()
        headers.forEach { (key, value) ->
            this.headers[key] = value
        }
        parameters.forEach { parameter(it.key, it.value) }
        if (timeout != null) {
            timeout {
                requestTimeoutMillis = timeout
            }
        }
    }

    fun getAdminBasic() = "Basic ${"${settings.adminName}:${settings.adminPassword}".toByteArray().toBase64()}"
}
