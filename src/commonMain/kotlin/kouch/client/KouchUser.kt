package kouch.client

import kotlinx.serialization.Serializable

class KouchUser {
    @Serializable
    @JvmInline
    value class Id(override val value: String) : KouchDocument.Id

    @Serializable
    data class Request(
        val id: Id? = null,
        val derived_key: String? = null,
        val name: String,
        val roles: List<String>? = null,
        val password: String,
        val password_sha: String? = null,
        val password_scheme: String? = null,
        val salt: String? = null,
        val type: String? = null,
    )

    @Serializable
    data class StandardResponse(
        val ok: Boolean? = null,
        val id: Id? = null,
        val rev: KouchDocument.Rev? = null,
        val error: String? = null,
        val reason: String? = null,
    )

    @Serializable
    data class User(
        val name: String,
        val password: String,
        val revision: KouchDocument.Rev? = null,
    )
}
