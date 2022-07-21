package kouch

import kotlinx.serialization.Serializable
import kouch.KouchEntity.CommonId
import kouch.KouchEntity.Rev
import kouch.client.KouchClientImpl
import kouch.client.KouchDocument
import kotlin.test.*

internal class KouchDocumentTest {
    @KouchEntityMetadata
    @Serializable
    data class TestEntity(
        override val id: CommonId,
        override val revision: Rev? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)

    private fun getEntity() = TestEntity(
        id = CommonId("some-id"),
        revision = Rev("some-revision"),
        string = "some-string",
        label = "some label"
    )

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }

    @Test
    fun notExistedId() = runTest {
        val document = kouch.doc.get<TestEntity>(Id("notExistedId1"))
        assertNull(document)
    }

    @Test
    fun updateWithNullRevisionShouldFails() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )

        kouch.db.createForEntity(entity)
        kouch.doc.insert(entity)

        assertFailsWith<RevisionIsNullException> { kouch.doc.update(entity) }
    }


    @Test
    fun updateShouldUpdateRevisionAndData() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )

        kouch.db.createForEntity(entity)
        val insertedEntity = kouch.doc.insert(entity).getUpdatedEntity()
        val entity2 = insertedEntity.copy(label = "label2")
        val updatedEntity = kouch.doc.update(entity2).getUpdatedEntity()

        assertNotNull(updatedEntity.revision)
        assertNotEquals(updatedEntity.revision, entity2.revision)
        assertEquals("label2", updatedEntity.label)
    }

    @Test
    fun upsertShouldUpdateRevisionAndData() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )

        kouch.db.createForEntity(entity)
        val insertResult = kouch.doc.insert(entity).getUpdatedEntity()
        val entity2 = insertResult.copy(label = "label2")
        val (updateResponse, updatedEntity) = kouch.doc.upsert(entity2).getResponseAndUpdatedEntity()

        assertNotNull(updateResponse.ok)
        assertTrue(updateResponse.ok!!)
        assertNotNull(updatedEntity.revision)
        assertEquals(updatedEntity.revision, updateResponse.rev)
        assertNotEquals(updatedEntity.revision, entity2.revision)
        assertEquals("label2", updatedEntity.label)
    }


    @Test
    fun deleteShouldDelete() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )
        kouch.db.createForEntity(entity)
        val entity1 = kouch.doc.insert(entity).getUpdatedEntity()
        val result3 = kouch.doc.delete(entity1)()

        assertTrue(result3.ok ?: false)

        val entity4 = kouch.doc.get<TestEntity>(entity1.id)
        assertNull(entity4)
    }


    @Test
    fun deleteWithNullRevisionShouldFails() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )
        kouch.db.createForEntity(entity)
        kouch.doc.insert(entity)
        assertFailsWith<RevisionIsNullException> { kouch.doc.delete(entity) }
    }

    @Test
    fun deleteWithWrongRevisionShouldFails() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )
        kouch.db.createForEntity(entity)
        kouch.doc.insert(entity)
        val entityWithWrongRevision = entity.copy(revision = Rev("wrongRev"))
        val exception = assertFailsWith<KouchDocumentException> { kouch.doc.delete(entityWithWrongRevision) }

        assertNotNull(exception.message)
        assertTrue("Invalid rev format" in exception.message!!)
    }


    @Test
    fun getShouldReturnTheEntity() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )
        kouch.db.createForEntity(entity)

        val entityInserted = kouch.doc.insert(entity).getUpdatedEntity()
        val entityReturned = kouch.doc.get<TestEntity>(entityInserted.id)
        assertNotNull(entityReturned)
        assertEquals(entityReturned, entityInserted)
    }


    @Test
    fun getRevisionsTest() = runTest {
        val entity = getEntity().copy(
            revision = null,
            label = "label1"
        )
        kouch.db.createForEntity(entity)

        val insertedEntity = kouch.doc.insert(entity).getUpdatedEntity()
        val entity2 = insertedEntity.copy(label = "label2")
        val updatedEntity = kouch.doc.update(entity2).getUpdatedEntity()
        val revisionsResult = kouch.doc.getWithResponse<TestEntity>(
            id = updatedEntity.id,
            parameters = KouchDocument.GetQueryParameters(
                revs = true
            )
        )

        assertEquals(2, revisionsResult.first._revisions?.ids?.count())
    }
}
