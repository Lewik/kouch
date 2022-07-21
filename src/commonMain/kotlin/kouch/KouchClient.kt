package kouch

import kouch.client.*
import kotlin.reflect.KClass

abstract class KouchClient {
    abstract val context: Context
    abstract val server: KouchServerService
    abstract val db: KouchDatabaseService
    abstract val doc: KouchDocumentService
    abstract val design: KouchDesignService
    abstract fun <T : KouchDocument> getMetadataFor(kClass: KClass<T>): KouchMetadata
}
