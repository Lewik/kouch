package kouch.client

import kotlinx.serialization.Serializable
import kouch.KouchEntity

class KouchUser {

    @Serializable
    data class Request(
        val id: KouchEntity.Id? = null,
        val derived_key: String? = null,
        val name: String,
        val roles: List<String>? = null,
        val password: String,
        val password_sha: String? = null,
        val password_scheme: String? = null,
        val salt: String? = null,
        val type: String? = null
    )

    @Serializable
    data class StandardResponse(
        val ok: Boolean? = null,
        val id: KouchEntity.Id? = null,
        val rev: KouchEntity.Rev? = null,
        val error: String? = null,
        val reason: String? = null
    )

    @Serializable
    data class User(
        val name: String,
        val password: String,
        val revision: KouchEntity.Rev? = null
    )
}
