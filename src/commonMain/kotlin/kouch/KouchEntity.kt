package kouch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
@JvmInline
value class DatabaseName(val value: String) {
    override fun toString() = value
}

@Serializable
@JvmInline
value class ClassName(val value: String) {
    override fun toString() = value
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KouchEntityMetadata(
    val databaseName: String = "",
    val className: String = "",
    val generateDatabaseName: Boolean = false,
)


interface KouchEntity {
    val id: Id
    val revision: Rev?

    interface Id {
        val value: String
        fun isBlank() = value.isBlank()
    }

    @Serializable
    @JvmInline
    value class Rev(val value: String)

    @Serializable
    @JvmInline
    value class CommonId(override val value: String) : Id


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
    override val id: Id,
    override val revision: KouchEntity.Rev? = null,
    val autoupdate: Boolean? = null,
    val language: String = "javascript",
    val options: JsonObject? = null,
    val filters: JsonObject? = null,
    val updates: JsonObject? = null,
    val validate_doc_update: String? = null,
    val views: Map<ViewName, View>? = null,
) : KouchEntity {
    @Serializable
    @JvmInline
    value class Id(override val value: String) : KouchEntity.Id

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
