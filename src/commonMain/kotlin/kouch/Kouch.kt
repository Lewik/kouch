package kouch

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class Kouch(
    val scheme: String = "http",
    val host: String = "localhost",
    val port: Int = 5984,
    /**
     * @See https://docs.couchdb.org/en/stable/intro/api.html#documents
     *
     * Sending an application/json Content-Type header will make a browser offer you the returned JSON for download
     * instead of just displaying it. Sending a text/plain content type, and browsers will display the JSON as text.
     */
    val defaultAcceptHeader: String = "text/plain",
    @Suppress("MemberVisibilityCanBePrivate")
    val client: HttpClient = HttpClient()
) {

    suspend inline fun <reified T> root() = Kouch().client.get<T>(
        scheme = scheme,
        host = host,
        port = port
    )
}
