//package kouch.client.blocking
//
//package kouch.clients.KouchDocumentService
//package kouch.clients.KouchUser
//import com.tavrida.electroscada.stomp.Stompable
//import kotlinx.coroutines.runBlocking
//
//
//class KouchDocumentBlocking(
//    @Suppress("MemberVisibilityCanBePrivate")
//    val kouchDocumentService: KouchDocumentService
//) {
//    inline fun <reified T : Stompable> insert(entity: T, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.insert(entity, user) }
//
//    inline fun <reified T : Stompable> insertBatch(entity: T, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.insertBatch(entity, user) }
//
//    inline fun <reified T : Stompable> update(entity: T, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.update(entity, user) }
//
////    inline fun <reified T : Stompable> updateBatch(entity: T, user: KouchUser.User) =
////        runBlocking { kouchDocumentService.updateBatch(entity, user) }
//
//    inline fun <reified T : Stompable> upsert(entity: T, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.upsert(entity, user) }
//
//    inline fun <reified T : Stompable> get(id: String, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.get<T>(id, user) }
//
//    inline fun <reified T : Stompable> delete(entity: T, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.delete(entity, user) }
//
//    inline fun <reified T : Stompable> getRevision(id: String, user: KouchUser.User, revision: String) =
//        runBlocking { kouchDocumentService.getRevision<T>(id, user, revision) }
//
//    inline fun getRevisions(id: String, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.getRevisions(id, user) }
//
//    inline fun <reified T : Stompable> getRevsInfo(id: String, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.getRevsInfo<T>(id, user) }
//
//    inline fun <reified T : Stompable> getAttachments(id: String, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.getAttachments<T>(id, user) }
//
//    inline fun <reified T : Stompable> getStubAttachments(id: String, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.getStubAttachments<T>(id, user) }
//
//    inline fun <reified T : Stompable> getAttachmentsSinceRevision(id: String, user: KouchUser.User, since: List<String>) =
//        runBlocking { kouchDocumentService.getAttachmentsSinceRevision<T>(id, user, since) }
//
//    inline fun <reified T : Stompable> getAttachmentsEncoding(id: String, user: KouchUser.User) =
//        runBlocking { kouchDocumentService.getAttachmentsEncoding<T>(id, user) }
//
//    inline fun <reified T : Stompable> copy(entity: T) =
//        runBlocking { kouchDocumentService.copy(entity) }
//}
