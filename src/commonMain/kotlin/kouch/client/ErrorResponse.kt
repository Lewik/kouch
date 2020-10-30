package kouch.client

import kotlinx.serialization.Serializable

@Serializable
class ErrorResponse(
    val error: String,
    val reason: String?
)
