package kouch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kouch.client.KouchClientImpl
import kouch.client.KouchDesignService.ViewRequest
import kotlin.test.*

internal class KouchDesignTest {
    @KouchEntityMetadata("test_entity", "test_entity")
    @Serializable
    data class TestEntity(
        override val id: String,
        override val revision: String? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }

    private fun getEntity() = TestEntity(
        id = "some-id",
        revision = "some-revision",
        string = "some-string",
        label = "some label"
    )

    @Test
    fun createDesignDocument() = runTest {
        prepareData()

        val (_, getResult) = kouch.design.getWithResponse(
            id = "testdes",
            db = DatabaseName("test_entity")
        )
        assertNotNull(getResult)
        assertEquals(getResult.language, "javascript")
        assertNotNull(getResult.views)
        assertEquals(getResult.views!!.count(), 3)
    }

    @Test
    fun getNonexistentDesign() = runTest {
        kouch.db.create(DatabaseName("test_entity"))
        val (_, nullResult) = kouch.design.getWithResponse(
            id = "devices2",
            db = DatabaseName("test_entity")
        )
        assertNull(nullResult)
    }

    @Test
    fun deleteDesignDocument() = runTest {
        prepareData()

        val (_, getResult) = kouch.design.getWithResponse(
            id = "testdes",
            db = DatabaseName("test_entity")
        )
        assertNotNull(getResult)
        assertEquals(getResult.language, "javascript")
        assertEquals(getResult.views!!.count(), 3)

        val deleteResult = kouch.design
            .delete(entity = getResult, databaseName = DatabaseName("test_entity"))()

        assertEquals(true, deleteResult.ok)

        val (_, getResult2) = kouch.design.getWithResponse(
            id = "testdes",
            db = DatabaseName("test_entity")
        )
        assertNull(getResult2)
    }

    @Test
    fun getViewShouldReturnView() = runTest {
        prepareData()

        kouch.design.getView<TestEntity>(
            db = DatabaseName("test_entity"),
            id = "testdes",
            viewName = "all"
        ).result.also {
            assertEquals(8, it.size)
            assertEquals("ASD", it.firstOrNull()?.label)
            assertEquals("label35", it.lastOrNull()?.label)
        }

        kouch.design.getView<TestEntity, TestEntity>(
            id = "testdes",
            viewName = "all"
        ).result.also {
            assertEquals(8, it.size)
            assertEquals("ASD", it.firstOrNull()?.label)
            assertEquals("label35", it.lastOrNull()?.label)
        }

        kouch.design.getView<TestEntity>(
            db = DatabaseName("test_entity"),
            id = "testdes",
            viewName = "asd_only"
        ).result.also {
            assertEquals(1, it.size)
            assertEquals("ASD", it.singleOrNull()?.label)
        }

        kouch.design.getView<TestEntity>(
            db = DatabaseName("test_entity"),
            id = "testdes",
            viewName = "all",
            request = ViewRequest(descending = true)
        ).result.also {
            assertEquals(8, it.size)
            assertEquals("label35", it.firstOrNull()?.label)
            assertEquals("ASD", it.lastOrNull()?.label)
        }



        kouch.design.getView<TestEntity>(
            db = DatabaseName("test_entity"),
            id = "testdes",
            viewName = "all",
            request = ViewRequest(
                key = JsonPrimitive("\"label35\"")
            )
        ).result.also {
            assertEquals(1, it.size)
            assertEquals("label35", it.singleOrNull()?.label)
        }

        kouch.design.getView<TestEntity>(
            db = DatabaseName("test_entity"),
            id = "testdes",
            viewName = "all",
            request = ViewRequest(
                limit = 2,
                skip = 1
            )
        ).result.also {
            assertEquals(2, it.size)
            assertEquals("ASD1", it.firstOrNull()?.label)
            assertEquals("label1", it.lastOrNull()?.label)
        }
    }

    //
//    @Ignore
//    @Test
//    fun `limit view`() = runTest {
//
//        val design = KouchTestHelper.getDesignService()
//        val body = KouchDesign.DesignRequest(
//            language = "javascript",
//            views = buildJsonObject {
//                put("all", buildJsonObject {
//                    put("map", "function(doc) { emit(doc.label, doc) }")
//                })
//                put("asd_only", buildJsonObject {
//                    put("map", "function(doc) { if (doc.label == \"ASD\") { emit(doc.label, doc); } }")
//                })
//            }
//        )
//        val updateResult = design.update("devices", body, adminUser)
//        assertTrue(updateResult!!.ok ?: false)
//
//        val getAsdResult = design.getView("devices", "asd_only")
//        val getLimitedResult = design.getView("devices", "asd_only", mapOf("limit" to 2))
//
//        assertNotEquals(getAsdResult!!.total_rows, 0)
//        assertEquals(getLimitedResult!!.rows!!.count(), 2)
//        assertNotNull(getAsdResult)
//        assertNotNull(getLimitedResult)
//        assertTrue(compareValues(getAsdResult.total_rows, getLimitedResult.total_rows) == 0)
//    }
//
//    @Ignore
//    @Test
//    fun `update view should return view`() = runTest {
//        val design = KouchTestHelper.getDesignService()
//        val body = KouchDesign.DesignRequest(
//            language = "javascript",
//            views = buildJsonObject {
//                put("by_type", buildJsonObject {
//                    put("map", "function(doc) { emit(doc.label, doc) }")
//                })
//                put("asd_only", buildJsonObject {
//                    put("map", "function(doc) { if (doc.label == \"ASD\") { emit(doc.label, doc); } }")
//                })
//            }
//        )
//        val updateResult = design.update("devices", body, adminUser)
//        assertTrue(updateResult!!.ok ?: false)
//
//        val getAsdResult = design.getView("devices", "asd_only")
//        val getAllResult = design.updateView(
//            id = "devices",
//            viewName = "by_type",
//            request = KouchDesign.ViewRequest(
//                keys = buildJsonArray { arrayOf("ASD") }
//            )
//        )
//
//        val getLimitedAsdResult = design.getView("devices", "asd_only", mapOf("limit" to 1))
//
//        assertNotEquals(getAsdResult!!.total_rows, 0)
//        assertNotNull(getAsdResult)
//        assertNotNull(getAllResult)
//        assertTrue(compareValues(getAsdResult.total_rows, getAllResult.total_rows) < 0)
//        assertEquals(getLimitedAsdResult!!.rows!!.count(), 1)
//    }
//
//    @Ignore
//    @Test
//    fun `send query view should return view`() = runTest {
//
//
//        val design = KouchTestHelper.getDesignService()
//        val body = KouchDesign.DesignRequest(
//            language = "javascript",
//            views = buildJsonObject {
//                put("by_type", buildJsonObject {
//                    put("map", "function(doc) { emit(doc.label, doc) }")
//                })
//                put("asd_only", buildJsonObject {
//                    put("map", "function(doc) { if (doc.label == \"ASD\") { emit(doc.label, doc); } }")
//                })
//            }
//        )
//        val updateResult = design.update("devices", body, adminUser)
//        assertTrue(updateResult!!.ok ?: false)
//
//        val getAsdResult = design.getView("devices", "asd_only")
//        val getAllResult = design.sendQueries(
//            id = "devices",
//            viewName = "by_type",
//            queries = buildJsonArray { buildJsonObject { arrayOf(put("keys", "ASD")) } }
//        )
//
//        assertNotNull(getAsdResult)
//        assertNotNull(getAllResult)
//        assertNotEquals(getAsdResult.total_rows, 0)
//        assertTrue(getAllResult.contains("results"))
//    }
    private suspend fun prepareData() {
        var i = 1
        kouch.db.create(DatabaseName("test_entity"))
        listOf(
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label3"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label2"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label35"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label1"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "ASD"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "ASD1"),
            getEntity().copy(id = "some-id${i++}", revision = null, label = "label1"),
            getEntity().copy(id = "some-id${i}", revision = null, label = "label1")
        )
            .forEach {
                val result = kouch.doc.insert(it)
                assertTrue(result.getResponse().ok ?: false)
            }

        val design = KouchDesign(
            id = "testdes",
            views = mapOf(
                "all" to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit(doc.label, doc) }"""
                ),
                "by_label" to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label != null) emit(doc.label, doc) }"""
                ),
                "asd_only" to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label === "ASD") { emit(doc.label, doc); } }"""
                ),
            )
        )
        val updateResult = kouch.design
            .upsert(
                designDocument = design,
                databaseName = DatabaseName("test_entity")
            )
            .getResponse()
        assertTrue(updateResult.ok ?: false)
        assertNotNull(updateResult.rev)
    }

}
