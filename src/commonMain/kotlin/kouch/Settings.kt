package kouch

import kotlin.reflect.KClass

data class Settings(
    val scheme: String = "http",
    val host: String = "localhost",
    val port: Int = 5984,

    val adminName: String,
    val adminPassword: String,

    val databaseNaming: DatabaseNaming,

    val autoGenerate: AutoGenerate = object : AutoGenerate {
        override fun <T : KouchEntity> generateDatabaseName(kClass: KClass<T>) = kClass.simpleName!!.camelToSnakeCase()
        override fun <T : KouchEntity> generateClassName(kClass: KClass<T>) = kClass.simpleName!!.camelToSnakeCase()
    }
) {
    interface AutoGenerate {
        fun <T : KouchEntity> generateDatabaseName(kClass: KClass<T>): String
        fun <T : KouchEntity> generateClassName(kClass: KClass<T>): String
    }

    sealed class DatabaseNaming {
        object AutoGenerate : DatabaseNaming()
        class Predefined(val databaseName: DatabaseName) : DatabaseNaming()
    }
}
