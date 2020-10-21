package kouch.kouch

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kouch.*
import kouch.client.KouchClientImpl
import kouch.client.KouchDatabase
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

    @Test
    fun changesTest() = runTest {
        kouch.db.createForEntity(TestEntity1::class)

        insertDocs(100)

        val results = mutableListOf<KouchDatabase.ChangesResponse.Result>()

        val job = kouch.db.changesContinuous(
            scope = GlobalScope,
            db = DatabaseName("test_entity1"),
            request = KouchDatabase.ChangesRequest(),
        ) { result ->
            results.add(result)
        }
        delay(1000)

        insertDocs(200)
        delay(1000)

        job.cancelAndJoin()

        assertEquals(24, results.size)
        assertEquals(8, results.count { it.deleted })
    }

    private fun getEntity() = TestEntity1(
        id = "some-id",
        revision = "some-revision",
        string = "some-string",
        label = "some label"
    )

    private suspend fun insertDocs(startI: Int) {
        var i = startI
        listOf(
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label3", string = "string1"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label2", string = "string1"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label35", string = "string2"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string3"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "ASD", string = "string2"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "ASD1", string = "string3"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity().copy(id = "some-id${i}", revision = null, label = "label1", string = "string1"),
        )
            .forEachIndexed { index, doc ->
                val putResult = kouch.doc.insert(doc)
                assertTrue(putResult.getResponse().ok ?: false)

                if (index % 3 == 0) {
                    val deleteResult = kouch.doc.delete(putResult.getUpdatedEntity())()
                    assertTrue(deleteResult.ok ?: false)
                }
            }
    }
}
