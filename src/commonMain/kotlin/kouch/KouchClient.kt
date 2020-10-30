package kouch

import kouch.client.KouchDatabaseService
import kouch.client.KouchDesignService
import kouch.client.KouchDocumentService
import kouch.client.KouchServerService
import kotlin.reflect.KClass

abstract class KouchClient {
    abstract val context: Context
    abstract val server: KouchServerService
    abstract val db: KouchDatabaseService
    abstract val doc: KouchDocumentService
    abstract val design: KouchDesignService
    abstract fun <T : KouchEntity> getMetadataFor(kClass: KClass<T>): KouchMetadata
}
