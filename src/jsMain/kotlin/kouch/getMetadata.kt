package kouch

import kotlin.reflect.KClass

actual inline fun <reified T : KouchEntity> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity = TODO()
