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
import kouch.client.KouchDocument
import kouch.client.KouchDocument.CommonId
import kouch.client.KouchDocument.Rev
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
        val db = KouchDatabase.Name("defaultdb")
        kouch.db.createForEntity(TestEntity1::class)
        assertTrue(kouch.db.isExist(db))
        assertTrue(kouch.db.isExistFor(TestEntity1::class))
        kouch.db.delete(db)
        assertFalse(kouch.db.isExist(db))
        assertFalse(kouch.db.isExistFor(TestEntity1::class))
    }

    @Test
    fun dbIsExistForTest2() = runTest {
        val db = KouchDatabase.Name("test_entity1")
        perEntityDbKouch.db.createForEntity(TestEntity1::class)
        assertTrue(perEntityDbKouch.db.isExist(db))
        assertTrue(perEntityDbKouch.db.isExistFor(TestEntity1::class))
        perEntityDbKouch.db.delete(db)
        assertFalse(perEntityDbKouch.db.isExist(db))
        assertFalse(perEntityDbKouch.db.isExistFor(TestEntity1::class))
    }


    @Test
    fun dbPutTest1() = runTest {

        kouch.db.create(KouchDatabase.Name("test1"), 8, 3, true)
        assertTrue(kouch.db.isExist(KouchDatabase.Name("test1")))
        assertFails { kouch.db.create(KouchDatabase.Name("test1")) }
        kouch.db.delete(KouchDatabase.Name("test1"))
        assertNull(kouch.db.get(KouchDatabase.Name("test1")))
        assertFailsWith<KouchDatabaseException> { kouch.db.create(KouchDatabase.Name("1test1")) }
    }

    @Test
    fun dbPutTest2() = runTest {
        kouch.db.create(KouchDatabase.Name("test1"))
        assertTrue(kouch.db.isExist(KouchDatabase.Name("test1")))
        assertFails { kouch.db.create(KouchDatabase.Name("test1")) }
        kouch.db.delete(KouchDatabase.Name("test1"))
        assertNull(kouch.db.get(KouchDatabase.Name("test1")))
        assertFailsWith<KouchDatabaseException> { kouch.db.create(KouchDatabase.Name("1test1")) }
    }

    @Test
    fun dbGetTest() = runTest {
        kouch.db.create(KouchDatabase.Name("test1"))
        kouch.db.get(KouchDatabase.Name("test1")).also {
            assertNotNull(it)
            assertEquals("test1", it.db_name.value)
        }
        kouch.db.delete(KouchDatabase.Name("test1"))
        assertNull(kouch.db.get(KouchDatabase.Name("test1")))
    }

    @Test
    fun dbDeleteTest() = runTest {
        kouch.db.create(KouchDatabase.Name("test1"))
        assertFails { kouch.db.delete(KouchDatabase.Name("test2")) }
        kouch.db.delete(KouchDatabase.Name("test1"))
        assertNull(kouch.db.get(KouchDatabase.Name("test1")))
    }

    @Test
    fun dbsTest() = runTest {
        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size, it.size)
        }

        kouch.db.create(KouchDatabase.Name("test1"))
        kouch.db.create(KouchDatabase.Name("test2"))

        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 2, it.size)
            assertTrue(KouchDatabase.Name("test1") in it)
            assertTrue(KouchDatabase.Name("test2") in it)
        }

        kouch.db.delete(KouchDatabase.Name("test1"))

        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(KouchDatabase.Name("test2") in it)
        }

        kouch.db.delete(KouchDatabase.Name("test2"))

        kouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size, it.size)
        }
    }

    @KouchDocumentMetadata
    @Serializable
    data class TestEntity1(
        override val id: TestId,
        override val revision: Rev? = null,
        val string: String,
        val label: String,
    ) : KouchDocument

    @KouchDocumentMetadata
    @Serializable
    data class TestEntity2(
        override val id: TestId,
        override val revision: Rev? = null,
        val string: String,
        val label: String,
        val boolean: Boolean,
    ) : KouchDocument

    @KouchDocumentMetadata
    @Serializable
    data class TestEntity3(
        override val id: TestId,
        override val revision: Rev? = null,
        val string: String,
        val label: String,
    ) : KouchDocument

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
                id = TestId("id"),
                revision = null,
                string = "string",
                label = "label"
            )
        )
        perEntityDbKouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 1, it.size)
            assertTrue(KouchDatabase.Name("test_entity1") in it)
        }
    }

    @Test
    fun createForEntity11() = runTest {
        kouch.db.createForEntity(
            TestEntity1(
                id = TestId("id"),
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
            assertTrue(KouchDatabase.Name("test_entity1") in it)
            assertTrue(KouchDatabase.Name("test_entity2") in it)
        }
    }

    @Test
    fun createForEntitiesIfNotExists() = runTest {
        perEntityDbKouch.db.createForEntities(kClasses = listOf(TestEntity1::class, TestEntity2::class))
        perEntityDbKouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 2, it.size)
            assertTrue(KouchDatabase.Name("test_entity1") in it)
            assertTrue(KouchDatabase.Name("test_entity2") in it)
            assertTrue(KouchDatabase.Name("test_entity3") !in it)
        }

        perEntityDbKouch.db.createForEntitiesIfNotExists(kClasses = listOf(TestEntity2::class, TestEntity3::class))
        perEntityDbKouch.db.getAll().also {
            assertEquals(KouchDatabaseService.systemDbs.size + 3, it.size)
            assertTrue(KouchDatabase.Name("test_entity1") in it)
            assertTrue(KouchDatabase.Name("test_entity2") in it)
            assertTrue(KouchDatabase.Name("test_entity3") in it)
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
        delay(5000)

        insertDocs1(200)
        delay(5000)

        //TODO how to deal with this exception?
        job.cancelAndJoin()

        println(results.map { it.id.value }.sorted())
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
            db = KouchDatabase.Name("test_entity1"),
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

        println(results.map { it.id.value }.sorted())
        assertEquals(24, results.size)
        assertEquals(8, results.count { it.deleted })
    }

    @Test
    fun bulkGet() = runTest {
        kouch.db.createForEntity(TestEntity1::class)
        insertDocs1(100)
        val result = kouch.db.bulkGet<TestEntity1>(ids = (100..110).map { TestId("some-id$it") })
        assertEquals(listOf("some-id101", "some-id102", "some-id104", "some-id105", "some-id107", "some-id108"), result.entities.map { it.id.value }.sorted())
        val error = result.errors.singleOrNull()
        assertEquals(CommonId("some-id110"), error?.id)
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
            ids = ((100..110) + (200..210)).map { TestId("some-id$it") },
            entityClasses = listOf(TestEntity1::class, TestEntity2::class)
        )
        val expected1 = listOf("some-id101", "some-id102", "some-id104", "some-id105", "some-id107", "some-id108")
        val expected2 = listOf("some-id201", "some-id202", "some-id204", "some-id205", "some-id207", "some-id208")
        assertEquals(expected1 + expected2, result.entities.map { it.id.value }.sorted())
        val error = result.errors
        assertEquals(listOf(CommonId("some-id110"), CommonId("some-id210")), error.map { it.id })
        assertEquals(listOf("not_found", "not_found"), error.map { it.error })
    }


    @Test
    fun bulkUpsert() = runTest {
        kouch.db.createForEntity(TestEntity1::class)
        val insertedDocs = insertDocs1(100).associateBy { it.id }
        val newDocs = listOf(
            getEntity1().copy(id = TestId("some-id104"), revision = null, label = "conflicted", string = "newstring0"),
            getEntity1().copy(id = TestId("some-id200"), revision = null, label = "newlabel0", string = "newstring0"),
            getEntity1().copy(id = TestId("some-id201"), revision = null, label = "newlabel1", string = "newstring1"),
            getEntity1().copy(id = TestId("some-id202"), revision = null, label = "newlabel2", string = "newstring2"),
        ).associateBy { it.id }
        val updatedDocs = listOf(
            insertedDocs.getValue(TestId("some-id100")).copy(label = "conflicted"),
            insertedDocs.getValue(TestId("some-id101")).copy(label = "updated1"),
            insertedDocs.getValue(TestId("some-id102")).copy(label = "updated2")
        ).associateBy { it.id }

        val docsToUpsert = newDocs + updatedDocs
        val docsToDelete = listOf(insertedDocs.getValue(TestId("some-id104")), insertedDocs.getValue(TestId("some-id105")))

        val (response, updatedEntities) = kouch.db.bulkUpsert(docsToUpsert.values, docsToDelete)
            .getResponseAndUpdatedEntities()

        assertEquals(6, updatedEntities.size)
        assertEquals(9, response.size)
        assertEquals(
            expected = 7,
            actual = response.count { it.ok == true },
            message = Json { prettyPrint = true }.encodeToString(response)
        )
        assertEquals(
            expected = 2,
            actual = response.count { it.error == "conflict" && it.reason == "Document update conflict." },
            message = Json { prettyPrint = true }.encodeToString(response)
        )


        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id100")))
        assertEquals("updated1", kouch.doc.get<TestEntity1>(TestId("some-id101"))?.label)
        assertEquals("updated2", kouch.doc.get<TestEntity1>(TestId("some-id102"))?.label)
        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id103")))
        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id104")))
        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id105")))
        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id106")))
        assertEquals("label1", kouch.doc.get<TestEntity1>(TestId("some-id107"))?.label)
        assertEquals("label1", kouch.doc.get<TestEntity1>(TestId("some-id108"))?.label)
        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id109")))
        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id110")))
        assertNull(kouch.doc.get<TestEntity1>(TestId("some-id111")))

        assertEquals("newlabel0", kouch.doc.get<TestEntity1>(TestId("some-id200"))?.label)
        assertEquals("newlabel1", kouch.doc.get<TestEntity1>(TestId("some-id201"))?.label)
        assertEquals("newlabel2", kouch.doc.get<TestEntity1>(TestId("some-id202"))?.label)


    }


    private fun getEntity1() = TestEntity1(
        id = TestId("some-id"),
        revision = Rev("some-revision"),
        string = "some-string",
        label = "some label"
    )


    private fun getEntity2() = TestEntity2(
        id = TestId("some-id"),
        revision = Rev("some-revision"),
        string = "some-string",
        label = "some label",
        boolean = true
    )

    private suspend fun insertDocs1(startI: Int): List<TestEntity1> {
        var i = startI
        return listOf(
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "label3", string = "string1"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "label2", string = "string1"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "label35", string = "string2"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string3"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "ASD", string = "string2"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "ASD1", string = "string3"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string4"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string4"),
            getEntity1().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string4"),
            getEntity1().copy(id = TestId("some-id${i}"), revision = null, label = "label1", string = "string1"),
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
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "label3", string = "string1"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "label2", string = "string1"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "label35", string = "string2"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string3"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "ASD", string = "string2"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "ASD1", string = "string3"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string4"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string4"),
            getEntity2().copy(id = TestId("some-id${i++}"), revision = null, label = "label1", string = "string4"),
            getEntity2().copy(id = TestId("some-id${i}"), revision = null, label = "label1", string = "string1"),
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
