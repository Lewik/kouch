package kouch.client

import kotlinx.serialization.Serializable

class KouchServer {
    @Serializable
    data class RootResponse(
        val couchdb: String,
        val uuid: String,
        val vendor: Vendor,
        val version: String,
        val git_sha: String,
        val features: List<String>
    ) {
        @Serializable
        data class Vendor(
            val name: String
        )
    }


    @Serializable
    data class AllDbsRequest(
        val descending: Boolean = false,
        val endKey: String? = null,
        val limit: Int? = null,
        val skip: Int? = null,
        val startKey: String? = null
    )
}
