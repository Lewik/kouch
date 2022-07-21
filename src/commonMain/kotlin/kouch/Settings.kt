package kouch

import kouch.client.KouchDatabase
import kouch.client.KouchDocument
import kotlin.reflect.KClass

data class Settings(
    val scheme: String = "http",
    val host: String = "localhost",
    val port: Int = 5984,

    val adminName: String,
    val adminPassword: String,

    val databaseNaming: DatabaseNaming,

    val autoGenerate: AutoGenerate = object : AutoGenerate {
        override fun <T : KouchDocument> generateDatabaseName(kClass: KClass<T>) = kClass.simpleName!!.camelToSnakeCase()
        override fun <T : KouchDocument> generateClassName(kClass: KClass<T>) = kClass.simpleName!!.camelToSnakeCase()
    },
) {
    interface AutoGenerate {
        fun <T : KouchDocument> generateDatabaseName(kClass: KClass<T>): String
        fun <T : KouchDocument> generateClassName(kClass: KClass<T>): String
    }

    sealed class DatabaseNaming {
        object AutoGenerate : DatabaseNaming()
        class Predefined(val databaseName: KouchDatabase.Name) : DatabaseNaming()
    }

    fun getPredefinedDatabaseName() = (databaseNaming as? DatabaseNaming.Predefined)?.databaseName
}
