package kouch

data class Settings(
    val scheme: String = "http",
    val host: String = "localhost",
    val port: Int = 5984,

    val adminName: String,
    val adminPassword: String,

    val databaseNaming: DatabaseNaming
) {
    sealed class DatabaseNaming {
        object DatabaseNameAnnotation : DatabaseNaming()
        class Predefined(val databaseName: String) : DatabaseNaming()
    }

}
