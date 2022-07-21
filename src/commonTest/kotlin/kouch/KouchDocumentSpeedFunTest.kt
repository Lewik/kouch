package kouch.kouch

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kouch.*
import kouch.KouchDesign.ViewName
import kouch.KouchEntity.Id
import kouch.client.KouchClientImpl
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class KouchDocumentSpeedFunTest {
    @KouchEntityMetadata("test_entity", "test_entity")
    @Serializable
    data class TestEntity(
        override val id: Id,
        override val revision: KouchEntity.Rev? = null,
        val string: String,
        val label: String,
    ) : KouchEntity


    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)

    private fun getEntity() = TestEntity(
        id = Id("some-id"),
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

        val result = measureTime {
            (1..items)
                .map { id ->
                    kouch.doc.insert(
                        entity = entity.copy(id = Id(id.toString()))
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
                            entity = entity.copy(id = Id(id.toString()))
                        )
                    }
                }
                .forEach { it.join() }
        }
        val getResponse = kouch.db.get(DatabaseName("test_entity"))
        assertEquals(items, getResponse?.doc_count)
        println(result)
    }

    @Ignore
    @Test
    fun bulkSpeed() = runTest {
        kouch.db.createForEntity(kClass = TestEntity::class)
        val entities = (1..10000).map { randomEntity() }

        println("Write")
        val result = measureTime {
            kouch.db.bulkUpsert(entities)
        }
        println("Read")
        val result2 = measureTime {
            kouch.db.bulkGet<TestEntity>(entities.map { it.id })
        }
        println("Write: $result; Read: $result2")
    }


    private val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private fun randomString(length: Long) = (0..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    private fun randomEntity() = TestEntity(
        id = Id(randomString(16)),
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

        var waitTime = Duration.ZERO
        var insertTime = Duration.ZERO
        val time = measureTime {
            val bulkSize = 10000
            (0..100).forEach { step ->
                val wt = measureTime {
                    while (kouch.server.activeTasks().filter { it.type == "indexer" }.isNotEmpty()) {
                        delay(5.seconds)
                    }
                }
                println("wait $step, $wt")
                waitTime += wt
                val entities = (0..bulkSize).map { randomEntity() }
                val time = measureTime { kouch.db.bulkUpsert(entities) }
                insertTime += time
                println("step $step, $time")
            }
        }
        println("Time: $time, Wait: $waitTime, Insert: $insertTime")
        println("Insertions per second: ${1000000 / time.toDouble(DurationUnit.SECONDS)}")
        println("Insertions per second w/o waiting: ${1000000 / insertTime.toDouble(DurationUnit.SECONDS)}")
    }

    private fun getJsDesignDoc(i: Int): KouchDesign {
        val prefix = if (i == 0) "" else i.toString()
        val design = KouchDesign(
            id = Id("testdes$i"),
            views = mapOf(
                ViewName("all$prefix") to KouchDesign.View(
                    /*language=js*/
                    map = """doc => { emit(doc.label$prefix, doc) }""",
                ),
                ViewName("allnull$prefix") to KouchDesign.View(
                    /*language=js*/
                    map = """doc => { emit(doc.label$prefix, null) }""",
                ),
                ViewName("by_label$prefix") to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label$prefix != null) emit(doc.label$prefix, doc) }"""
                ),
                ViewName("asd_only$prefix") to KouchDesign.View(
                    /*language=js*/ map = """doc => { if (doc.label$prefix === "ASD") { emit(doc.label$prefix, doc); } }"""
                ),
                ViewName("by_label_and_string$prefix") to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit([doc.label$prefix, doc.string], doc) }"""
                ),
                ViewName("reduce$prefix") to KouchDesign.View(
                    /*language=js*/ map = """doc => { emit(doc.label$prefix, doc) }""",
                    /*language=js*/ reduce = """(key, value) => { return true }"""
                )
            )
        )
        return design
    }

    private fun getErlangDesignDoc(i: Int): KouchDesign {
        val prefix = if (i == 0) "" else i.toString()
        val design = KouchDesign(
            id = Id("testdes$i"),
            language = "Erlang",
            views = mapOf(
                ViewName("all$prefix") to KouchDesign.View(
                    /*language=Erlang*/
                    map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label$prefix">>, Doc, null),
                                    Emit(K, {Doc})
                                end.
                            """.trimIndent(),
                ),
                ViewName("allnull$prefix") to KouchDesign.View(
                    /*language=Erlang*/
                    map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label$prefix">>, Doc, null),
                                    Emit(K, null)
                                end.
                            """.trimIndent(),
                ),
                ViewName("by_label$prefix") to KouchDesign.View(
                    /*language=Erlang*/
                    map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label$prefix">>, Doc, null),
                                    if
                                      K == null -> null;
                                      true -> Emit(null, null)
                                    end
                                end.
                            """.trimIndent(),
                ),
                ViewName("asd_only$prefix") to KouchDesign.View(
                    /*language=Erlang*/ map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label$prefix">>, Doc, null),
                                    if
                                      K == <<"ASD">> -> Emit(K, {Doc});
                                      true -> null
                                    end
                                end.
                            """.trimIndent()
                ),
                ViewName("by_label_and_string$prefix") to KouchDesign.View(
                    /*language=Erlang*/ map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label$prefix">>, Doc, null),
                                    S = proplists:get_value(<<"string">>, Doc, null),
                                    Emit([K, S], {Doc})
                                end.
                            """.trimIndent()
                ),
                ViewName("reduce$prefix") to KouchDesign.View(
                    /*language=Erlang*/ map = """
                                fun({Doc}) ->
                                    K = proplists:get_value(<<"label$prefix">>, Doc, null),
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
