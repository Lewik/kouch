package kouch.kouch

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kouch.*
import kouch.KouchTestHelper
import kouch.client.KouchClientImpl
import kouch.client.KouchDocument
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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
}
