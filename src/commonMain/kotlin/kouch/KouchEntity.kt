package kouch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

inline class DatabaseName(val value: String)
inline class ClassName(val value: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KouchEntityMetadata(
    val databaseName: String = "",
    val className: String = "",
    val generateDatabaseName: Boolean = false
)

interface KouchEntity {
    val id: String
    val revision: String?
}

sealed class KouchMetadata {
    data class Entity(
        val databaseName: DatabaseName,
        val className: ClassName,
    ) : KouchMetadata()

    data class Design(
        val databaseName: DatabaseName,
    ) : KouchMetadata()
}

@Serializable
data class KouchDesign(
    override val id: String,
    override val revision: String? = null,
    val autoupdate: Boolean? = null,
    val language: String = "javascript",
    val options: JsonObject? = null,
    val filters: JsonObject? = null,
    val updates: JsonObject? = null,
    val validate_doc_update: String? = null,
    val views: Map<String, View>? = null,
) : KouchEntity {
    @Serializable
    data class View(
        val map: String? = null,
        val reduce: String? = null,
    )
}
