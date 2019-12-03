package kouch

import kotlinx.serialization.Serializable

class KouchTyped(
    @Suppress("MemberVisibilityCanBePrivate")
    val kouch: Kouch
) {
    @Serializable
    data class Root(
        val couchdb: String,
        val version: String,
        val git_sha: String,
        val uuid: String,
        val features: List<String>,
        val vendor: Vendor
    ) {
        @Serializable
        data class Vendor(
            val name: String
        )
    }


    suspend fun root() = kouch.root<Root>()

}
