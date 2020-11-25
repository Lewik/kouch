package kouch.kouch

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kouch.*
import kouch.client.KouchClientImpl
import kouch.client.KouchDatabase
import kouch.client.KouchDatabaseService
import kotlin.test.*

internal class KouchDatabaseTest {

    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)
    private val perEntityDbKouch = KouchClientImpl(KouchTestHelper.perEntityDbContext)

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }

    @Test
    fun dbIsExistForTest1() = runTest {
        val db = DatabaseName("defaultdb")
        kouch.db.createForEntity(TestEntity1::class)
        assertTrue(kouch.db.isExist(db))
        assertTrue(kouch.db.isExistFor(TestEntity1::class))
        kouch.db.delete(db)
        assertFalse(kouch.db.isExist(db))
        assertFalse(kouch.db.isExistFor(TestEntity1::class))
    }

    @Test
    fun dbIsExistForTest2() = runTest {
        val db = DatabaseName("test_entity1")
        perEntityDbKouch.db.createForEntity(TestEntity1::class)
        assertTrue(perEntityDbKouch.db.isExist(db))
        assertTrue(perEntityDbKouch.db.isExistFor(TestEntity1::class))
        perEntityDbKouch.db.delete(db)
        assertFalse(perEntityDbKouch.db.isExist(db))
        assertFalse(perEntityDbKouch.db.isExistFor(TestEntity1::class))
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

    @KouchEntityMetadata
    @Serializable
    data class TestEntity1(
        override val id: String,
        override val revision: String? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    @KouchEntityMetadata
    @Serializable
    data class TestEntity2(
        override val id: String,
        override val revision: String? = null,
        val string: String,
        val label: String,
        val boolean: Boolean
    ) : KouchEntity

    @KouchEntityMetadata
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
            assertTrue(kouch.context.getMetadata(TestEntity1::class).databaseName in it)
        }
    }

    @Test
    fun createForEntity1() = runTest {
        perEntityDbKouch.db.createForEntity(
            TestEntity1(
                id = "id",
                revision = null,
                string = "string",
                label = "label"
            )
        )
        perEntityDbKouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
        }
    }

    @Test
    fun createForEntity11() = runTest {
        kouch.db.createForEntity(
            TestEntity1(
                id = "id",
                revision = null,
                string = "string",
                label = "label"
            )
        )
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(kouch.context.getMetadata(TestEntity1::class).databaseName in it)
        }
    }

    @Test
    fun createForEntities() = runTest {
        perEntityDbKouch.db.createForEntities(kClasses = listOf(TestEntity1::class, TestEntity2::class))
        perEntityDbKouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 2, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
            assertTrue(DatabaseName("test_entity2") in it)
        }
    }

    @Test
    fun createForEntitiesIfNotExists() = runTest {
        perEntityDbKouch.db.createForEntities(kClasses = listOf(TestEntity1::class, TestEntity2::class))
        perEntityDbKouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 2, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
            assertTrue(DatabaseName("test_entity2") in it)
            assertTrue(DatabaseName("test_entity3") !in it)
        }

        perEntityDbKouch.db.createForEntitiesIfNotExists(kClasses = listOf(TestEntity2::class, TestEntity3::class))
        perEntityDbKouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 3, it.size)
            assertTrue(DatabaseName("test_entity1") in it)
            assertTrue(DatabaseName("test_entity2") in it)
            assertTrue(DatabaseName("test_entity3") in it)
        }
    }

    @Test
    fun createForEntitiesIfNotExists2() = runTest {
        kouch.db.createForEntitiesIfNotExists(kClasses = listOf(TestEntity2::class, TestEntity3::class))
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(kouch.context.settings.getPredefinedDatabaseName() in it)
        }
    }

    @Test
    fun changesTest() = runTest {
        kouch.db.createForEntity(TestEntity1::class)

        insertDocs1(100)

        val results = mutableListOf<KouchDatabase.ChangesResponse.Result>()

        val job = kouch.db.changesContinuous(
            scope = GlobalScope,
            db = kouch.context.getMetadata(TestEntity1::class).databaseName,
            request = KouchDatabase.ChangesRequest(
                include_docs = true,
                heartbeat = 20000
            ),
            entities = listOf(
                TestEntity1::class,
                TestEntity2::class,
                TestEntity3::class
            )
        ) { result ->
            results.add(result)
        }
        delay(80000)

        insertDocs1(200)
        delay(80000)

        //TODO how to deal with this exception?
        job.cancelAndJoin()

        println(results.map { it.id }.sorted())
        assertEquals(24, results.size)
        assertEquals(8, results.count { it.deleted })
    }


    @Ignore
    @Test
    fun changesReconnectionTest() = runTest {
        kouch.db.createForEntity(TestEntity1::class)

        insertDocs1(100)

        val results = mutableListOf<KouchDatabase.ChangesResponse.Result>()

        val job = kouch.db.changesContinuous(
            scope = GlobalScope,
            db = DatabaseName("test_entity1"),
            request = KouchDatabase.ChangesRequest(
                include_docs = true,
                heartbeat = 20000
            ),
            entities = listOf(
                TestEntity1::class,
                TestEntity2::class,
                TestEntity3::class
            )
        ) { result ->
            results.add(result)
        }
        println("make reconnect")
        delay(20000)

        insertDocs1(200)
        delay(1000)

        job.cancelAndJoin()

        println(results.map { it.id }.sorted())
        assertEquals(24, results.size)
        assertEquals(8, results.count { it.deleted })
    }

    @Test
    fun bulkGet() = runTest {
        kouch.db.createForEntity(TestEntity1::class)
        insertDocs1(100)
        val result = kouch.db.bulkGet<TestEntity1>(ids = (100..110).map { "some-id$it" })
        assertEquals(listOf("some-id101", "some-id102", "some-id104", "some-id105", "some-id107", "some-id108"), result.entities.map { it.id }.sorted())
        val error = result.errors.singleOrNull()
        assertEquals("some-id110", error?.id)
        assertEquals("not_found", error?.error)
    }

    @Test
    fun bulkGet2() = runTest {
        kouch.db.createForEntitiesIfNotExists(
            listOf(
                TestEntity1::class,
                TestEntity2::class
            )
        )

        insertDocs1(100)
        insertDocs2(200)
        val result = kouch.db.bulkGet(
            ids = ((100..110) + (200..210)).map { "some-id$it" },
            entityClasses = listOf(TestEntity1::class, TestEntity2::class)
        )
        val expected1 = listOf("some-id101", "some-id102", "some-id104", "some-id105", "some-id107", "some-id108")
        val expected2 = listOf("some-id201", "some-id202", "some-id204", "some-id205", "some-id207", "some-id208")
        assertEquals(expected1 + expected2, result.entities.map { it.id }.sorted())
        val error = result.errors
        assertEquals(listOf("some-id110", "some-id210"), error.map { it.id })
        assertEquals(listOf("not_found", "not_found"), error.map { it.error })
    }


    @Test
    fun bulkUpsert() = runTest {
        kouch.db.createForEntity(TestEntity1::class)
        val insertedDocs = insertDocs1(100).associateBy { it.id }
        val newDocs = listOf(
            getEntity1().copy(id = "some-id104", revision = null, label = "conflicted", string = "newstring0"),
            getEntity1().copy(id = "some-id200", revision = null, label = "newlabel0", string = "newstring0"),
            getEntity1().copy(id = "some-id201", revision = null, label = "newlabel1", string = "newstring1"),
            getEntity1().copy(id = "some-id202", revision = null, label = "newlabel2", string = "newstring2"),
        ).associateBy { it.id }
        val updatedDocs = listOf(
            insertedDocs.getValue("some-id100").copy(label = "conflicted"),
            insertedDocs.getValue("some-id101").copy(label = "updated1"),
            insertedDocs.getValue("some-id102").copy(label = "updated2")
        ).associateBy { it.id }

        val docsToUpsert = newDocs + updatedDocs
        val docsToDelete = listOf(insertedDocs.getValue("some-id104"), insertedDocs.getValue("some-id105"))

        val result = kouch.db.bulkUpsert(docsToUpsert.values, docsToDelete)

        assertEquals(9, result.size)
        assertEquals(
            expected = 7,
            actual = result.count { it.ok == true },
            message = Json { prettyPrint = true }.encodeToString(result)
        )
        assertEquals(
            expected = 2,
            actual = result.count { it.error == "conflict" && it.reason == "Document update conflict." },
            message = Json { prettyPrint = true }.encodeToString(result)
        )


        assertNull(kouch.doc.get<TestEntity1>("some-id100"))
        assertEquals("updated1", kouch.doc.get<TestEntity1>("some-id101")?.label)
        assertEquals("updated2", kouch.doc.get<TestEntity1>("some-id102")?.label)
        assertNull(kouch.doc.get<TestEntity1>("some-id103"))
        assertNull(kouch.doc.get<TestEntity1>("some-id104"))
        assertNull(kouch.doc.get<TestEntity1>("some-id105"))
        assertNull(kouch.doc.get<TestEntity1>("some-id106"))
        assertEquals("label1", kouch.doc.get<TestEntity1>("some-id107")?.label)
        assertEquals("label1", kouch.doc.get<TestEntity1>("some-id108")?.label)
        assertNull(kouch.doc.get<TestEntity1>("some-id109"))
        assertNull(kouch.doc.get<TestEntity1>("some-id110"))
        assertNull(kouch.doc.get<TestEntity1>("some-id111"))

        assertEquals("newlabel0", kouch.doc.get<TestEntity1>("some-id200")?.label)
        assertEquals("newlabel1", kouch.doc.get<TestEntity1>("some-id201")?.label)
        assertEquals("newlabel2", kouch.doc.get<TestEntity1>("some-id202")?.label)


    }


    private fun getEntity1() = TestEntity1(
        id = "some-id",
        revision = "some-revision",
        string = "some-string",
        label = "some label"
    )


    private fun getEntity2() = TestEntity2(
        id = "some-id",
        revision = "some-revision",
        string = "some-string",
        label = "some label",
        boolean = true
    )

    private suspend fun insertDocs1(startI: Int): List<TestEntity1> {
        var i = startI
        return listOf(
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "label3", string = "string1"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "label2", string = "string1"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "label35", string = "string2"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string3"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "ASD", string = "string2"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "ASD1", string = "string3"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity1().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity1().copy(id = "some-id${i}", revision = null, label = "label1", string = "string1"),
        )
            .mapIndexed { index, doc ->
                val putResult = kouch.doc.insert(doc)
                assertTrue(putResult.getResponse().ok ?: false)

                if (index % 3 == 0) {
                    val deleteResult = kouch.doc.delete(putResult.getUpdatedEntity())()
                    assertTrue(deleteResult.ok ?: false)
                }
                putResult.getUpdatedEntity()
            }


    }

    private suspend fun insertDocs2(startI: Int): List<Unit> {
        var i = startI
        return listOf(
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "label3", string = "string1"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "label2", string = "string1"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "label35", string = "string2"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string3"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "ASD", string = "string2"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "ASD1", string = "string3"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity2().copy(id = "some-id${i++}", revision = null, label = "label1", string = "string4"),
            getEntity2().copy(id = "some-id${i}", revision = null, label = "label1", string = "string1"),
        )
            .mapIndexed { index, doc ->
                val putResult = kouch.doc.insert(doc)
                assertTrue(putResult.getResponse().ok ?: false)

                if (index % 3 == 0) {
                    val deleteResult = kouch.doc.delete(putResult.getUpdatedEntity())()
                    assertTrue(deleteResult.ok ?: false)
                }
            }
    }
}
