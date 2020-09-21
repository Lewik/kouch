//package kouch
//
//import kotlinx.serialization.json.buildJsonObject
//import kotlinx.serialization.json.put
//import kouch.client.KouchDatabase
//import kouch.client.KouchDesign
//import kouch.client.KouchUser
//import kotlin.test.*
//
//internal class KouchRolesTest {
//
//    private val dbAdminUser = KouchUser.User(
//        name = "dbadmin",
//        password = "dbadmin"
//    )
//    private val memberUser = KouchUser.User(
//        name = "user",
//        password = "user"
//    )
//    private val anotherMemberUser = KouchUser.User(
//        name = "user2",
//        password = "user2"
//    )
//    private val DEFAULT_DB_COUNT = 2
//
//    @BeforeTest
//    fun beforeTest() = runTest {
//        KouchTestHelper.removeAllUsers()
//        KouchTestHelper.ensureEmptyServer()
//        KouchTestHelper.createAdmin(dbAdminUser)
//        KouchTestHelper.createUser(memberUser)
//        KouchTestHelper.createUser(anotherMemberUser)
//    }
//
//    @AfterTest
//    fun afterTest() = runTest {
//        KouchTestHelper.removeAllUsers()
//        KouchTestHelper.removeAllDbs()
//    }
//
//    private fun createDb(name: String) = runTestWithResult {
//        val database = KouchTestHelper.getDatabaseService(Settings(name))
//        database.put(name)
//        database
//    }
//
//    /***
//     * Having no members, any user can write regular documents (any non-design document) and read documents from the database.
//     */
//    @Ignore
//    @Test
//    fun `no members test`() = runTest {
//        val database = createDb("nomembers")
//        database.setRoles(KouchDatabase.RolesRequest(admins = KouchDatabase.Role(roles = listOf("admin"))))
//
//        val entity = KouchTestHelper.getEntity().copy(
//            _rev = null,
//            label = "label1"
//        )
//        val document = KouchTestHelper.getDocumentService(Settings("nomembers"))
//        val result = document.insert(entity, memberUser, { e, r -> e.copy(_rev = r) })
//
//        assertTrue(result.second.ok ?: false)
//        assertNotNull(result.first._id)
//        assertEquals("label1", result.first.label)
//
//
//        val result2 = document.get<TestEntity>(result.first._id, memberUser)
//        assertNotNull(result2)
//        assertNotNull(result2._id)
//        assertEquals(result.first.label, result2.label)
//
//
//        val result3 = document.get<TestEntity>(result.first._id, dbAdminUser)
//        assertNotNull(result3)
//        assertNotNull(result3._id)
//        assertEquals(result.first.label, result3.label)
//
//        val design = KouchTestHelper.getDesignService(Settings("noadmins"))
//        val body = KouchDesign.DesignRequest(
//            language = "javascript",
//            views = buildJsonObject {
//                put("all", buildJsonObject {
//                    put("map", "function(doc) { emit(doc.label, doc) }")
//                })
//                put("by_label", buildJsonObject {
//                    put("map", "function(doc) { if (doc.label != null) emit(doc.label, doc) }")
//                })
//                put("asd_only", buildJsonObject {
//                    put("map", "function(doc) { if (doc.label == \"ASD\") { emit(doc.label, doc); } }")
//                })
//            }
//        )
//
//        assertFails { design.update("devices", body) }
//        TODO("exact exception")
//    }
//
//    /***
//     * Having no admins, only server admins (with the reserved _admin role) are able to update design document and make other admin level changes.
//     */
//    @Ignore
//    @Test
//    fun `no admins test`() = runTest {
////        val database = createDb("noadmins")
////        database.setRoles(KouchDatabase.RolesRequest())
////
////        val entity = KouchTestHelper.getEntity().copy(
////            revision = null,
////            label = "label1"
////        )
////        val document = KouchTestHelper.document(Settings("noadmins"))
////        entity.deviceList.take(3).forEach {
////            val result = document.insert(it, dbAdminUser)
////            assertTrue(result.second.ok ?: false)
////        }
//
//        val design = KouchTestHelper.getDesignService(Settings("noadmins"))
//        val body = KouchDesign.DesignRequest(
//            language = "javascript",
//            views = buildJsonObject {
//                put("all", buildJsonObject {
//                    put("map", "function(doc) { emit(doc.dispatcherName, doc) }")
//                })
//                put("by_label", buildJsonObject {
//                    put("map", "function(doc) { if (doc.dispatcherName != null) emit(doc.dispatcherName, doc) }")
//                })
//                put("asd_only", buildJsonObject {
//                    put("map", "function(doc) { if (doc.type == \"ASD\") { emit(doc.type, doc); } }")
//                })
//            }
//        )
//        val updateResult = design.update("devices", body)
//        assertTrue(updateResult!!.ok ?: false)
//
//        val getResult = design.get("devices")
//        assertEquals(getResult!!.views!!.count(), 3)
//    }
//
//    /***
//     * admins have all the privileges of members plus the privileges: write (and edit) design documents, add/remove database admins and members and set the database revisions limit.
//     */
//    @Ignore
//    @Test
//    fun `admin remove role test`() = runTest {
//        val database = createDb("adminrole")
////        database.setRoles(KouchDatabase.RolesRequest(admins = KouchDatabase.Role(roles = listOf("admin"))))
////
////        val entity = KouchTestHelper.getEntity().copy(
////            revision = null,
////            label = "label1"
////        )
////        val document = KouchTestHelper.document(Settings("adminrole"))
////        entity.deviceList.take(3).forEach {
////            val result = document.insert(it, dbAdminUser)
////            assertTrue(result.second.ok ?: false)
////        }
//
//        val design = KouchTestHelper.getDesignService(Settings("adminrole"))
//        val body = KouchDesign.DesignRequest(
//            language = "javascript",
//            views = buildJsonObject {
//                put("all", buildJsonObject {
//                    put("map", "function(doc) { emit(doc.dispatcherName, doc) }")
//                })
//                put("by_label", buildJsonObject {
//                    put("map", "function(doc) { if (doc.dispatcherName != null) emit(doc.dispatcherName, doc) }")
//                })
//                put("asd_only", buildJsonObject {
//                    put("map", "function(doc) { if (doc.type == \"ASD\") { emit(doc.type, doc); } }")
//                })
//            }
//        )
//        val updateResult = design.update("devices", body)
//        assertTrue(updateResult!!.ok ?: false)
//
//        val getResult = design.get("devices")
//        assertEquals(getResult!!.views!!.count(), 3)
//
//        database.setRoles(KouchDatabase.RolesRequest(), user = dbAdminUser)
//
//        assertFails { design.update("devices", body) }
//        TODO("exact exception")
//    }
//
//    @Ignore
//    @Test
//    fun `member update design test`() = runTest {
////        val database = createDb("memberrole")
////        database.setRoles(
////            KouchDatabase.RolesRequest(
////                admins = KouchDatabase.Role(roles = listOf("admin")),
////                members = KouchDatabase.Role(roles = listOf("member"))
////            )
////        )
////
////        val entity = KouchTestHelper.getEntity().copy(
////            revision = null,
////            label = "label1"
////        )
////        val document = KouchTestHelper.document(Settings("memberrole"))
////        entity.deviceList.take(3).forEach {
////            val result = document.insert(it, memberUser)
////            assertTrue(result.second.ok ?: false)
////        }
//
//        val design = KouchTestHelper.getDesignService(Settings("memberrole"))
//        val body = KouchDesign.DesignRequest(
//            language = "javascript",
//            views = buildJsonObject {
//                put("all", buildJsonObject {
//                    put("map", "function(doc) { emit(doc.dispatcherName, doc) }")
//                })
//                put("by_label", buildJsonObject {
//                    put("map", "function(doc) { if (doc.dispatcherName != null) emit(doc.dispatcherName, doc) }")
//                })
//                put("asd_only", buildJsonObject {
//                    put("map", "function(doc) { if (doc.type == \"ASD\") { emit(doc.type, doc); } }")
//                })
//            }
//        )
//
//        assertFails { design.update("devices", body, memberUser) }
//        TODO("exact exception")
//    }
//}
