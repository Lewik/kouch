package kouch

import kotlin.reflect.KClass

class NoMetadataAnnotationException(message: String) : Exception(message)

expect inline fun <reified T : KouchEntity> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity
