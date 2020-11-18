package kouch

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass


fun <T : KouchEntity> Context.encodeToKouchEntity(
    entity: T,
    kClass: KClass<T>,
    className: ClassName,
) = entityJson.encodeToString(encodeToKouchEntityJson(kClass, entity, className))

fun <T : KouchEntity> Context.encodeToKouchEntityJson(kClass: KClass<T>, entity: T, className: ClassName) =
    entityJson.encodeToJsonElement(kClass.serializer(), entity)
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
           .plus(classField to JsonPrimitive(className.value))
           .toMap()
           .let { JsonObject(it) }

fun <T : KouchEntity> Context.encodeToKouchDesign(
    entity: T,
    kClass: KClass<T>,
) = designJson.encodeToString(designJson.encodeToJsonElement(kClass.serializer(), entity)
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

fun <T : Any> Context.decodeKouchEntityFromJsonElement(jsonElement: JsonElement, resultKClass: KClass<T>) = entityJson.decodeFromJsonElement(
    resultKClass.serializer(),
    jsonElement.jsonObject
        .mapNotNull { (key, value) ->
            when (key) {
                "_id" -> "id" to value
                "_rev" -> "revision" to value
                classField -> null
                else -> key to value
            }
        }
        .toMap()
        .let { JsonObject(it) }
)


fun <T : Any> Json.encodeNullableToUrl(
    data: T?,
    kClass: KClass<T>,
) = if (data == null) "" else encodeToUrl(data, kClass)

fun <T : Any> Json.encodeToUrl(
    data: T,
    kClass: KClass<T>,
): String {
    val string = encodeToJsonElement(kClass.serializer(), data)
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



