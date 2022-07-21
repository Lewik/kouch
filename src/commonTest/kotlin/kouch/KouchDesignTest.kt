package kouch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kouch.KouchDesign.ViewName
import kouch.KouchEntity.Rev
import kouch.client.KouchClientImpl
import kouch.client.KouchDesignService.ViewRequest
import kotlin.test.*

internal class KouchDesignTest {
    @KouchEntityMetadata("test_entity", "test_entity")
    @Serializable
    data class TestEntity(
        override val id: Id,
        override val revision: Rev? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }

    private fun getEntity() = TestEntity(
        id = Id("some-id"),
        revision = Rev("some-revision"),
        string = "some-string",
        label = "some label"
    )

    @Test
    fun createDesignDocument() = runTest {
        prepareData()

        val (_, getResult) = kouch.design.getWithResponse(
            id = KouchDesign.Id("testdes"),
            db = DatabaseName("test_entity")
        )
        assertNotNull(getResult)
        assertEquals("javascript", getResult.language)
        assertNotNull(getResult.views)
        assertEquals(6, getResult.views!!.size)
    }

    @Test
    fun getNonexistentDesign() = runTest {
        kouch.db.create(DatabaseName("test_entity"))
        val (_, nullResult) = kouch.design.getWithResponse(
            id = KouchDesign.Id("devices2"),
            db = DatabaseName("test_entity")
        )
        assertNull(nullResult)
    }

    @Test
    fun deleteDesignDocument() = runTest {
        prepareData()

        val (_, getResult) = kouch.design.getWithResponse(
            id = KouchDesign.Id("testdes"),
            db = DatabaseName("test_entity")
        )
        assertNotNull(getResult)
        assertEquals("javascript", getResult.language)
        assertEquals(6, getResult.views!!.size)

        val deleteResult = kouch.design
            .delete(entity = getResult, db = DatabaseName("test_entity"))()

        assertEquals(true, deleteResult.ok)

        val (_, getResult2) = kouch.design.getWithResponse(
            id = KouchDesign.Id("testdes"),
            db = DatabaseName("test_entity")
        )
        assertNull(getResult2)
    }


    @Test
    fun viewSimple() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("all"),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(8, it.size)
            assertEquals("ASD", it.firstOrNull()?.label)
            assertEquals("label35", it.lastOrNull()?.label)
        }
    }

    @Test
    fun viewSimple2() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("asd_only"),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(1, it.size)
            assertEquals("ASD", it.singleOrNull()?.label)
        }
    }

    @Test
    fun viewDatabaseFromEntity() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("all"),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(8, it.size)
            assertEquals("ASD", it.firstOrNull()?.label)
            assertEquals("label35", it.lastOrNull()?.label)
        }
    }

    @Test
    fun viewIncludeDocs() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("allnull"),
            request = ViewRequest(
                include_docs = true
            ),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(8, it.size)
            assertEquals("ASD", it.firstOrNull()?.label)
            assertEquals("label35", it.lastOrNull()?.label)
        }
    }

    @Test
    fun viewNullValues() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("allnull"),
            request = ViewRequest(),
            db = DatabaseName("test_entity")
        ).result.also { list ->
            assertEquals(8, list.size)
            assertTrue(list.all { it == null })
        }
    }

    @Test
    fun viewDescending() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("all"),
            request = ViewRequest(descending = true),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(8, it.size)
            assertEquals("label35", it.firstOrNull()?.label)
            assertEquals("ASD", it.lastOrNull()?.label)
        }


    }

    @Test
    fun viewWithKey() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("all"),
            request = ViewRequest(
                key = JsonPrimitive("label35")
            ),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(1, it.size)
            assertEquals("label35", it.singleOrNull()?.label)
        }
    }

    @Test
    fun viewWithComplexKey() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("by_label_and_string"),
            request = ViewRequest(
                key = JsonArray(listOf(JsonPrimitive("label1"), JsonPrimitive("string3")))
            ),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(1, it.size)
            assertEquals("label1", it.singleOrNull()?.label)
            assertEquals("string3", it.singleOrNull()?.string)
        }
    }

    @Test
    fun viewWithNotExistingComplexKey() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("by_label_and_string"),
            request = ViewRequest(
                key = JsonArray(listOf(JsonPrimitive("notExistingKey"), JsonPrimitive("string3")))
            ),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun viewWithNotExistingComplexKey2() = runTest {
        prepareData()

        kouch.design.getView<JsonElement>(
            db = DatabaseName("test_entity"),
            id = Id("testdes"),
            view = ViewName("by_label_and_string"),
            request = ViewRequest(
                key = JsonArray(listOf(JsonPrimitive("notExistingKey"), JsonPrimitive("notExistingKey")))
            )
        ).result.also {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun viewGroup() = runTest {
        prepareData()

        kouch.design.getRawView(
            db = DatabaseName("test_entity"),
            id = Id("testdes"),
            view = ViewName("reduce"),
            request = ViewRequest(
                group = true
            )
        ).result.also {
            assertEquals(6, it.size)
        }
    }

    @Test
    fun viewWithSkipAndLimit() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            id = Id("testdes"),
            view = ViewName("all"),
            request = ViewRequest(
                limit = 2,
                skip = 1
            ),
            db = DatabaseName("test_entity")
        ).result.also {
            assertEquals(2, it.size)
            assertEquals("ASD1", it.firstOrNull()?.label)
            assertEquals("label1", it.lastOrNull()?.label)
        }
    }

    private suspend fun prepareData() {
        var i = 1
        kouch.db.create(DatabaseName("test_entity"))
        listOf(
            getEntity().copy(id = Id("some-id${i++}"), revision = null, label = "label3", string = "string1"),
            getEntity().copy(id = Id("some-id${i++}"), revision = null, label = "label2", string = "string1"),
            getEntity().copy(id = Id("some-id${i++}"), revision = null, label = "label35", string = "string2"),
            getEntity().copy(id = Id("some-id${i++}"), revision = null, label = "label1", string = "string3"),
            getEntity().copy(id = Id("some-id${i++}"), revision = null, label = "ASD", string = "string2"),
            getEntity().copy(id = Id("some-id${i++}"), revision = null, label = "ASD1", string = "string3"),
            getEntity().copy(id = Id("some-id${i++}"), revision = null, label = "label1", string = "string4"),
            getEntity().copy(id = Id("some-id${i}"), revision = null, label = "label1", string = "string1"),
        )
            .forEach {
                val result = kouch.doc.insert(it)
                assertTrue(result.getResponse().ok ?: false)
            }

        val design = KouchDesign(
            id = KouchDesign.Id("testdes"),
            views = mapOf(
                ViewName("all") to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit(doc.label, doc) }"""
                ),
                ViewName("allnull") to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit(doc.label, null) }"""
                ),
                ViewName("by_label") to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label != null) emit(doc.label, doc) }"""
                ),
                ViewName("asd_only") to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label === "ASD") { emit(doc.label, doc); } }"""
                ),
                ViewName("by_label_and_string") to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit([doc.label, doc.string], doc) }"""
                ),
                ViewName("reduce") to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit(doc.label, doc) }""",
                    /*language=js*/ reduce = """(key, value) => { return true }"""
                )
            )
        )
        val updateResult = kouch.design
            .upsert(
                ddoc = design,
                db = DatabaseName("test_entity")
            )
            .getResponse()
        assertTrue(updateResult.ok ?: false)
        assertNotNull(updateResult.rev)
    }

}
