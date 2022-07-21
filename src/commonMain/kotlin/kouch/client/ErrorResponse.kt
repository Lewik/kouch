package kouch.client

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val reason: String?,
)
