package kouch.kouch

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kouch.*
import kouch.client.KouchClientImpl
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.seconds

internal class KouchDocumentSpeedFunTest {
    @KouchEntityMetadata("test_entity", "test_entity")
    @Serializable
    data class TestEntity(
        override val id: String,
        override val revision: String? = null,
        val string: String,
        val label: String,
    ) : KouchEntity


    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)

    private fun getEntity() = TestEntity(
        id = "some-id",
        revision = null,
        string = "some-string",
        label = "some label"
    )


    private val items = 1_000_00

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }

    @Ignore
    @ExperimentalTime
    @Test
    fun oneByOneTest() = runTest {
        kouch.db.createForEntity(kClass = TestEntity::class)
        val entity = getEntity()
        var id = 0

        val result = measureTime {
            (1..items)
                .map { id ->
                    kouch.doc.insert(
                        entity = entity.copy(id = id.toString())
                    )
                }
        }
        val getResponse = kouch.db.get(DatabaseName("test_entity"))
        assertEquals(items, getResponse?.doc_count)
        println(result)
    }

    @Ignore
    @ExperimentalTime
    @Test
    fun parallelTest() = runTest {
        kouch.db.createForEntity(kClass = TestEntity::class)
        val entity = getEntity()

        val result = measureTime {
            (1..items)
                .map { id ->
                    GlobalScope.launch {
                        kouch.doc.insert(
                            entity = entity.copy(id = id.toString())
                        )
                    }
                }
                .forEach { it.join() }
        }
        val getResponse = kouch.db.get(DatabaseName("test_entity"))
        assertEquals(items, getResponse?.doc_count)
        println(result)
    }


    private val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private fun randomString(length: Long) = (0..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    private fun randomEntity() = TestEntity(
        id = randomString(16),
        string = randomString(16),
        label = randomString(24),
    )

    @Ignore
    @Test
    fun testManyUpsertsJs() = runTest {
        testManyUpserts {
            getJsDesignDoc(it)
        }
    }

    @Ignore
    @Test
    fun testManyUpsertsErlang() = runTest {
        testManyUpserts {
            getErlangDesignDoc(it)
        }
    }

    private suspend fun testManyUpserts(designCallback: (Int) -> KouchDesign) {
        kouch.db.createForEntity(randomEntity())

        (0..10).forEach {
            val design = designCallback(it)
            val updateResult = kouch.design
                .upsert(
                    ddoc = design,
                    db = DatabaseName("test_entity")
                )
                .getResponse()
            assertTrue(updateResult.ok ?: false)
            assertNotNull(updateResult.rev)
        }


        val time = measureTime {
            val bulkSize = 10000
            (0..100).forEach { step ->
                while (kouch.server.activeTasks().isNotEmpty()) {
                    println("Waiting...")
                    delay(5.seconds)
                }
                val entities = (0..bulkSize).map { randomEntity() }
                val time = measureTime { kouch.db.bulkUpsert(entities) }
                println("step $step, $time")
            }
        }
        println("Time: ${time.inSeconds}")
    }

    private fun getJsDesignDoc(i: Int): KouchDesign {
        val prefix = if (i == 0) "" else i.toString()
        val design = KouchDesign(
            id = "testdes$i",
            views = mapOf(
                "all${prefix}" to KouchDesign.View(
                    /*language=js*/
                    map = """doc => { emit(doc.label${prefix}, doc) }""",
                ),
                "allnull${prefix}" to KouchDesign.View(
                    /*language=js*/
                    map = """doc => { emit(doc.label${prefix}, null) }""",
                ),
                "by_label${prefix}" to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label${prefix} != null) emit(doc.label${prefix}, doc) }"""
                ),
                "asd_only${prefix}" to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label${prefix} === "ASD") { emit(doc.label${prefix}, doc); } }"""
                ),
                "by_label_and_string${prefix}" to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit([doc.label${prefix}, doc.string], doc) }"""
                ),
                "reduce${prefix}" to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit(doc.label${prefix}, doc) }""",
                    /*language=js*/ reduce = """(key, value) => { return true }"""
                )
            )
        )
        return design
    }

    private fun getErlangDesignDoc(i: Int): KouchDesign {
        val prefix = if (i == 0) "" else i.toString()
        val design = KouchDesign(
            id = "testdes$i",
            language = "Erlang",
            views = mapOf(
                "all${prefix}" to KouchDesign.View(
                    /*language=Erlang*/
                    map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label${prefix}">>, Doc, null),
                                    Emit(K, {Doc})
                                end.
                            """.trimIndent(),
                ),
                "allnull${prefix}" to KouchDesign.View(
                    /*language=Erlang*/
                    map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label${prefix}">>, Doc, null),
                                    Emit(K, null)
                                end.
                            """.trimIndent(),
                ),
                "by_label${prefix}" to KouchDesign.View(
                    /*language=Erlang*/
                    map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label${prefix}">>, Doc, null),
                                    if
                                      K == null -> null;
                                      true -> Emit(null, null)
                                    end
                                end.
                            """.trimIndent(),
                ),
                "asd_only${prefix}" to KouchDesign.View(
                    /*language=Erlang*/ map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label${prefix}">>, Doc, null),
                                    if
                                      K == <<"ASD">> -> Emit(K, {Doc});
                                      true -> null
                                    end
                                end.
                            """.trimIndent()
                ),
                "by_label_and_string${prefix}" to KouchDesign.View(
                    /*language=Erlang*/ map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label${prefix}">>, Doc, null),
                                    S = proplists:get_value(<<"string">>, Doc, null),
                                    Emit([K, S], {Doc})
                                end.
                            """.trimIndent()
                ),
                "reduce${prefix}" to KouchDesign.View(
                    /*language=Erlang*/ map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label${prefix}">>, Doc, null),
                                    Emit(K, {Doc})
                                end.
                            """.trimIndent(),
                    /*language=Erlang*/ reduce = """
                                fun(Keys, Values, ReReduce) -> 
                                  true
                                end.
                            """.trimIndent()
                )
            )
        )
        return design
    }
}
