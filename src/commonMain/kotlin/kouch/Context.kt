package kouch

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.Json

class Context(
    val settings: Settings,
    val client: HttpClient = HttpClient { expectSuccess = false },
    val strictSystemJson: Boolean = false,
) {
    val responseJson: Json = Json {}
    val entityJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val systemJson: Json = Json {
        ignoreUnknownKeys = !strictSystemJson
    }
    val designJson: Json = Json {
        ignoreUnknownKeys = !strictSystemJson
        encodeDefaults = false
    }
    val systemQueryParametersJson: Json = Json {
        encodeDefaults = false
    }
//    val standardJson: Json = Json {
//        isLenient = true
//        ignoreUnknownKeys = true
//        allowSpecialFloatingPointValues = true
//        useArrayPolymorphism = true
//    },

//    val jsonSkipNull: Json = Json { encodeDefaults = false },
//
//    val jsonUnquoted: Json = Json {
//        prettyPrint = true
////        unquotedPrint = true
//        useArrayPolymorphism = true
//        isLenient = true
//    },

    //    /**
//     * @See https://docs.couchdb.org/en/stable/intro/api.html#documents
//     *
//     * Sending an application/json Content-Type header will make a browser offer you the returned JSON for download
//     * instead of just displaying it. Sending a text/plain content type, and browsers will display the JSON as text.
//     */
//    val defaultAcceptHeader: String = "text/plain",


    suspend fun request(
        method: HttpMethod,
        path: String,
        body: Any = EmptyContent,
        parameters: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ) = client.request<HttpResponse> {
        url(settings.scheme, settings.host, settings.port, path)
        this.method = method
        this.body = body
        this.headers[HttpHeaders.Authorization] = getAdminBasic()
        headers.forEach { (key, value) ->
            this.headers[key] = value
        }
        parameters.forEach { parameter(it.key, it.value) }
    }

    fun getAdminBasic() = "Basic ${"${settings.adminName}:${settings.adminPassword}".toByteArray().toBase64()}"
}
