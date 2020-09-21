//package kouch.client.blocking
//
//import kouch.client.KouchServer
//
//class KouchServerBlocking(
//    @Suppress("MemberVisibilityCanBePrivate")
//    val kouchServer: KouchServer
//) {
//    fun root() =
//        runBlocking { kouchServer.root() }
//
//    fun allDbs(request: KouchServer.AllDbsRequest = KouchServer.AllDbsRequest()) =
//        runBlocking { kouchServer.allDbs(request) }
//}
