package kouch

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*


inline fun <reified T : KouchEntity> Context.encodeToKouchEntity(
    entity: T,
    className: ClassName,
) = entityJson.encodeToString(entityJson.encodeToJsonElement(entity)
    .jsonObject
    .mapNotNull { (key, value) ->
        when (key) {
            "id" -> "_id" to value
            "revision" -> if (value.jsonPrimitive.contentOrNull == null) {
                null
            } else {
                "_rev" to value
            }
            else -> key to value
        }
    }
    .plus("class__" to JsonPrimitive(className.value))
    .toMap()
    .let { JsonObject(it) })

inline fun <reified T : KouchEntity> Context.encodeToKouchDesign(
    entity: T
) = designJson.encodeToString(designJson.encodeToJsonElement(entity)
    .jsonObject
    .mapNotNull { (key, value) ->
        when (key) {
            "id" -> "_id" to value
            "revision" -> if (value.jsonPrimitive.contentOrNull == null) {
                null
            } else {
                "_rev" to value
            }
            else -> key to value
        }
    }
    .toMap()
    .let { JsonObject(it) })

inline fun <reified T> Context.decodeKouchEntityFromJsonElement(jsonElement: JsonElement): T {
    val decodeFromJsonElement = entityJson.decodeFromJsonElement<T>(
        jsonElement.jsonObject
            .mapNotNull { (key, value) ->
                when (key) {
                    "_id" -> "id" to value
                    "_rev" -> "revision" to value
                    "class__" -> null
                    else -> key to value
                }
            }
            .toMap()
            .let { JsonObject(it) }
            .also { println(it) }
    )
    return decodeFromJsonElement
}


inline fun <reified T : Any> Json.encodeNullableToUrl(data: T?) = if (data == null) "" else encodeToUrl(data)

inline fun <reified T : Any> Json.encodeToUrl(data: T): String {
    val string = encodeToJsonElement(data)
        .jsonObject
        .entries
        .joinToString(separator = "&") { "${it.key}=${it.value.jsonPrimitive.content}" }

    return if (string.isBlank()) {
        ""
    } else {
        "?$string"
    }
}


fun JsonElement.filterUnderscoredFields() = JsonObject(jsonObject.filterKeys { it.startsWith('_') })

fun JsonElement.filterNonUnderscoredFieldsWithIdRev() = JsonObject(jsonObject.filterKeys { it == "_id" || it == "_rev" || !it.startsWith('_') })

fun JsonElement.splitUnderscoredAndNonUnderscoredFields() = jsonObject
    .entries
    .map { it.toPair() }
    .partition { it.first.startsWith('_') && it.first != "_id" && it.first != "_rev" }
    .let { (first, second) ->
        JsonObject(first.toMap()) to JsonObject(second.toMap())
    }



