//package kouch.client.blocking
//
//package kouch.clients.KouchDesign
//package kouch.clients.KouchDesignService
//package kouch.clients.KouchUser
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.json.JsonArray
//
//
//class KouchDesignBlocking(
//    @Suppress("MemberVisibilityCanBePrivate")
//    val kouchDesignService: KouchDesignService
//) {
//    fun update(id: String, request: KouchDesign.DesignRequest, user: KouchUser.User? = null) = runBlocking { kouchDesignService.update(id, request, user) }
//
//    fun get(id: String) = runBlocking { kouchDesignService.get(id) }
//
//    fun delete(id: String, rev: String) = runBlocking { kouchDesignService.delete(id, rev) }
//
//    fun getView(
//        id: String,
//        viewName: String,
//        params: Map<String, Any>? = null
//    ) =
//        runBlocking { kouchDesignService.getView(id, viewName, params) }
//
//    fun updateView(
//        id: String,
//        viewName: String,
//        request: KouchDesign.ViewRequest
//    ) =
//        runBlocking { kouchDesignService.updateView(id, viewName, request) }
//
//    fun sendQueries(
//        id: String,
//        viewName: String,
//        queries: JsonArray
//    ) =
//        runBlocking { kouchDesignService.sendQueries(id, viewName, queries) }
//}
