package kouch

import kotlin.reflect.KClass

actual fun <T : KouchEntity> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity = TODO()
