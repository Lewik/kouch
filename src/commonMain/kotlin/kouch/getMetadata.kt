package kouch

import kotlin.reflect.KClass

class NoMetadataAnnotationException(message: String) : Exception(message)

expect fun <T : KouchEntity> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity
