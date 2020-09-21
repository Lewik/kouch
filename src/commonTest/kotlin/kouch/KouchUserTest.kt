//package kouch
//
//import kouch.KouchTestHelper.getUserService
//import kouch.client.KouchUser
//import kotlin.test.*
//
//internal class KouchUserTest {
//
//
//    @BeforeTest
//    fun beforeTest() = runTest {
//        KouchTestHelper.ensureEmptyServer()
//        KouchTestHelper.removeAllUsers()
//    }
//
//    @AfterTest
//    fun afterTest() = runTest {
//        KouchTestHelper.removeAllDbs()
//        KouchTestHelper.removeAllUsers()
//    }
//
//    @Ignore
//    @Test
//    fun createUserTest() = runTest {
//        val userService = getUserService()
//        val result =
//            userService.create(KouchUser.Request(name = "test1", password = "test1", roles = emptyList(), id = "org.couchdb.user:test1", type = "user"))
//
//        assertNotNull(result.ok)
//        assertNotNull(result.rev)
//    }
//
//    @Ignore
//    @Test
//    fun deleteUserTest() = runTest {
//        val userService = getUserService()
//        val result1 =
//            userService.create(KouchUser.Request(name = "test2", password = "test2", roles = emptyList(), id = "org.couchdb.user:test2", type = "user"))
//
//        assertNotNull(result1.ok)
//        assertNotNull(result1.rev)
//
//        val result2 = userService.delete(KouchUser.User(name = "test2", password = "test2", revision = result1.rev))
//
//        assertNotNull(result2.ok)
//    }
//
//}
