package kouch.kouch

import kotlinx.serialization.Serializable
import kouch.KouchEntity
import kouch.KouchEntityMetadata
import kouch.KouchTestHelper
import kouch.client.KouchClientImpl
import kouch.runTest
import kotlin.test.*

internal class KouchDocumentSpeedTest {
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
        revision = "some-revision",
        string = "some-string",
        label = "some label"
    )


    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }

    @Test
    fun notExistedId() = runTest {
        val document = kouch.doc.get<TestEntity>("notExistedId1",)
        assertNull(document)
    }

    @Test
    fun getShouldReturnTheEntity() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )
        kouch.db.createForEntity(entity)

        val entityInserted = kouch.doc.insert(entity).getUpdatedEntity()

        val entityReturned = kouch.doc.get<TestEntity>(entityInserted.id,)
        assertNotNull(entityReturned)
        assertEquals(entityReturned, entityInserted)
    }


}
