package kouch.client

import kouch.Context
import kouch.KouchClient

class KouchClientImpl(
    val context: Context
) : KouchClient() {
    override val server = KouchServerService(context)
    override val db = KouchDatabaseService(context)
    override val doc = KouchDocumentService(context, KouchDocumentService.Type.DOC)
    override val design = KouchDesignService(context, KouchDocumentService(context, KouchDocumentService.Type.DESIGN))
}
