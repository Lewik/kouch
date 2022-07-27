package kouch



private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
//private val snakeRegex = "_[a-zA-Z]".toRegex()

// String extensions
fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this) {
        "_${it.value}"
    }.lowercase()
}
