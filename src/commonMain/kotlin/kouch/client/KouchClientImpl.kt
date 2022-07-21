package kouch.client

import kouch.Context
import kouch.KouchClient
import kouch.getMetadata
import kotlin.reflect.KClass

class KouchClientImpl(
    override val context: Context,
) : KouchClient() {
    override val server = KouchServerService(context)
    override val db = KouchDatabaseService(context)
    override val doc = KouchDocumentService(context, KouchDocumentService.Type.DOC)
    override val design = KouchDesignService(context, KouchDocumentService(context, KouchDocumentService.Type.DESIGN))

    override fun <T : KouchDocument> getMetadataFor(kClass: KClass<T>) = context.getMetadata(kClass)
}
