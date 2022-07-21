package kouch

import kouch.client.KouchDocument
import kouch.client.KouchMetadata
import kotlin.reflect.KClass

class NoMetadataAnnotationException(message: String) : Exception(message)

expect fun <T : KouchDocument> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KouchDocumentMetadata(
    val databaseName: String = "",
    val className: String = "",
    val generateDatabaseName: Boolean = false,
)
