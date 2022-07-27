package kouch.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmInline

@Serializable
data class KouchDesign(
    override val id: Id,
    override val revision: KouchDocument.Rev? = null,
    val autoupdate: Boolean? = null,
    val language: String = "javascript",
    val options: JsonObject? = null,
    val filters: JsonObject? = null,
    val updates: JsonObject? = null,
    val validate_doc_update: String? = null,
    val views: Map<ViewName, View>? = null,
) : KouchDocument {
    @Serializable
    @JvmInline
    value class Id(override val value: String) : KouchDocument.Id

    @Serializable
    data class View(
        val map: String? = null,
        val reduce: String? = null,
    )

    @Serializable
    @JvmInline
    value class ViewName(val value: String) {
        override fun toString() = value
    }

}
