package kouch.kouch

import kotlinx.serialization.Serializable
import kouch.*
import kouch.client.KouchClientImpl
import kouch.client.KouchDatabaseService
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

    @KouchEntityMetadata(autoGenerate = true)
    @Serializable
    data class TestEntity1(
        override val id: String,
        override val revision: String? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    @KouchEntityMetadata(autoGenerate = true)
    @Serializable
    data class TestEntity2(
        override val id: String,
        override val revision: String? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    @KouchEntityMetadata(autoGenerate = true)
    @Serializable
    data class TestEntity3(
        override val id: String,
        override val revision: String? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    @Test
    fun createForEntity() = runTest {
        kouch.db.createForEntity(kClass = TestEntity1::class)
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
        }
    }

    @Test
    fun createForEntity1() = runTest {
        kouch.db.createForEntity(TestEntity1(
            id = "id",
            revision = null,
            string = "string",
            label = "label"
        ))
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
        }
    }

    @Test
    fun createForEntities() = runTest {
        kouch.db.createForEntities(kClasses = listOf(TestEntity1::class, TestEntity2::class))
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 2, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
            assertTrue(DatabaseName("test_entity2") in it)
        }
    }

    @Test
    fun createForEntitiesIfNotExists() = runTest {
        kouch.db.createForEntities(kClasses = listOf(TestEntity1::class, TestEntity2::class))
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 2, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
            assertTrue(DatabaseName("test_entity2") in it)
            assertTrue(DatabaseName("test_entity3") !in it)
        }

        kouch.db.createForEntitiesIfNotExists(kClasses = listOf(TestEntity2::class, TestEntity3::class))
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 3, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
            assertTrue(DatabaseName("test_entity2") in it)
            assertTrue(DatabaseName("test_entity3") in it)
        }
    }
}
