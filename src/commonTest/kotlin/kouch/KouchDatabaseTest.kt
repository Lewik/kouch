package kouch.kouch

import kouch.DatabaseName
import kouch.KouchDatabaseException
import kouch.KouchTestHelper
import kouch.client.KouchClientImpl
import kouch.client.KouchDatabaseService
import kouch.runTest
import kotlin.test.*

internal class KouchDatabaseTest {

    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }


    @Test
    fun dbIsExistTest() = runTest {
        kouch.db.create(DatabaseName("test1"))
        assertTrue(kouch.db.isExist(DatabaseName("test1")))
        kouch.db.delete(DatabaseName("test1"))
        assertFalse(kouch.db.isExist(DatabaseName("test1")))
    }


    @Test
    fun dbPutTest1() = runTest {

        kouch.db.create(DatabaseName("test1"), 8, 3, true)
        assertTrue(kouch.db.isExist(DatabaseName("test1")))
        assertFails { kouch.db.create(DatabaseName("test1")) }
        kouch.db.delete(DatabaseName("test1"))
        assertNull(kouch.db.get(DatabaseName("test1")))
        assertFailsWith<KouchDatabaseException> { kouch.db.create(DatabaseName("1test1")) }
    }

    @Test
    fun dbPutTest2() = runTest {
        kouch.db.create(DatabaseName("test1"))
        assertTrue(kouch.db.isExist(DatabaseName("test1")))
        assertFails { kouch.db.create(DatabaseName("test1")) }
        kouch.db.delete(DatabaseName("test1"))
        assertNull(kouch.db.get(DatabaseName("test1")))
        assertFailsWith<KouchDatabaseException> { kouch.db.create(DatabaseName("1test1")) }
    }

    @Test
    fun dbGetTest() = runTest {
        kouch.db.create(DatabaseName("test1"))
        kouch.db.get(DatabaseName("test1")).also {
            assertNotNull(it)
            assertEquals("test1", it.db_name)
        }
        kouch.db.delete(DatabaseName("test1"))
        assertNull(kouch.db.get(DatabaseName("test1")))
    }

    @Test
    fun dbDeleteTest() = runTest {
        kouch.db.create(DatabaseName("test1"))
        assertFails { kouch.db.delete(DatabaseName("test2")) }
        kouch.db.delete(DatabaseName("test1"))
        assertNull(kouch.db.get(DatabaseName("test1")))
    }

    @Test
    fun dbsTest() = runTest {
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size, it.size)
        }

        kouch.db.create(DatabaseName("test1"))
        kouch.db.create(DatabaseName("test2"))

        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 2, it.size)
            assertTrue(DatabaseName("test1") in it)
            assertTrue(DatabaseName("test2") in it)
        }

        kouch.db.delete(DatabaseName("test1"))

        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(DatabaseName("test2") in it)
        }

        kouch.db.delete(DatabaseName("test2"))

        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size, it.size)
        }
    }


//
//    @Ignore
//    @Test
//    fun findDevicesTest() = runTest {
//        val settings = KouchTestHelper.defaultSettings
//        val database = KouchTestHelper.getDatabaseService(settings)
//        client.db.put("test")
//        val document = KouchTestHelper.getDocumentService(settings)
//        listOf(
//            KouchTestHelper.getEntity().copy(_rev = null, label = "label3"),
//            KouchTestHelper.getEntity().copy(_rev = null, label = "label2"),
//            KouchTestHelper.getEntity().copy(_rev = null, label = "label35"),
//            KouchTestHelper.getEntity().copy(_rev = null, label = "label1"),
//            KouchTestHelper.getEntity().copy(_rev = null, label = "asd_only"),
//            KouchTestHelper.getEntity().copy(_rev = null, label = "ASD1"),
//            KouchTestHelper.getEntity().copy(_rev = null, label = "label1"),
//            KouchTestHelper.getEntity().copy(_rev = null, label = "label1")
//        )
//            .forEach {
//                val result = document.insert(it, adminUser, { e, r -> e.copy(_rev = r) })
//                assertTrue(result.second.ok ?: false)
//            }
//
//        val selector = buildJsonObject { put("label", "label1") }
//
//        val result = database.find(
//            KouchDatabase.SearchRequest(
//                selector = selector
//            )
//        )
//
//        assertNotNull(result)
//        assertNotNull(result.docs)
//        assertEquals(2, result.docs!!.size)
//    }
}
