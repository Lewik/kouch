//package kouch.client.blocking
//
//package kouch.clients.KouchDatabase
//package kouch.clients.KouchDatabaseService
//package kouch.clients.KouchUser
//import kotlinx.coroutines.runBlocking
//
//
//class KouchDatabaseBlocking(
//    @Suppress("MemberVisibilityCanBePrivate")
//    val kouchDatabaseService: KouchDatabaseService
//) {
//    fun isExist(db: DatabaseName) =
//        runBlocking { kouchDatabaseService.isExist(db) }
//
//    fun get(db: DatabaseName) =
//        runBlocking { kouchDatabaseService.get(db) }
//
//    fun put(db: DatabaseName? = null, partitions: Int? = null, replicas: Int? = null, partitioned: Boolean = false) =
//        runBlocking { kouchDatabaseService.put(db, partitions, replicas, partitioned) }
//
//    fun delete(db: DatabaseName) =
//        runBlocking { kouchDatabaseService.delete(db) }
//
//    fun replicate(request: KouchDatabase.ReplicationRequest) =
//        runBlocking {
//            kouchDatabaseService.replicate(request)
//        }
//
//    fun cancelReplication(request: KouchDatabase.ReplicationRequest) =
//        runBlocking { kouchDatabaseService.cancelReplication(request) }
//
//    fun find(request: KouchDatabase.SearchRequest) =
//        runBlocking { kouchDatabaseService.find(request) }
//
//    fun getAllDocs() =
//        runBlocking { kouchDatabaseService.getAllDocs() }
//
//    fun setRoles(request: KouchDatabase.RolesRequest, user: KouchUser.User? = null) =
//        runBlocking { kouchDatabaseService.setRoles(request, user) }
//
//}
