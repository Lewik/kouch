package kouch

import kouch.client.KouchDatabaseService
import kouch.client.KouchDesignService
import kouch.client.KouchDocumentService
import kouch.client.KouchServerService

abstract class KouchClient {
    abstract val server: KouchServerService
    abstract val db: KouchDatabaseService
    abstract val doc: KouchDocumentService
    abstract val design: KouchDesignService
}
