package kouch.kouch

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kouch.*
import kouch.client.KouchClientImpl
import kouch.client.KouchDatabase
import kouch.client.KouchDatabaseService
import kotlin.test.*

internal class KouchReplicationTest {
    private val replicationDelay = 10_000L

    @KouchEntityMetadata("test_entity", "test_entity")
    @Serializable
    data class TestEntity(
        override val id: KouchEntity.Id,
        override val revision: KouchEntity.Rev? = null,
        val string: String,
        val label: String,
    ) : KouchEntity

    private val sourceContext = KouchTestHelper.defaultContext
    private val targetContext = KouchTestHelper.secondaryContext
    private val sourceClient = KouchClientImpl(sourceContext)
    private val targetClient = KouchClientImpl(targetContext)

    private fun getEntity() = TestEntity(
        id = KouchEntity.Id("some-id"),
        revision = KouchEntity.Rev("some-revision"),
        string = "some-string",
        label = "some label"
    )

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.secondaryContext)
    }

    private val sourceDatabaseName = DatabaseName("sourcedb")
    private val targetDatabaseName = DatabaseName("targetdb")

    @Test
    fun singleReplicateTest() = runTest {
        sourceClient.db.create(sourceDatabaseName)
        targetClient.db.create(targetDatabaseName)
        val originalEntity = getEntity()
        val entity = originalEntity.copy(
            revision = null,
            label = "label1"
        )
        val (response, insertedEntity) = sourceClient.doc.insert(
            entity = entity,
            metadata = sourceContext.getMetadata(entity::class).copy(databaseName = sourceDatabaseName),
        ).getResponseAndUpdatedEntity()
        assertTrue(response.ok ?: false)
        assertNotNull(insertedEntity.revision)
        assertEquals(originalEntity.id, insertedEntity.id)

        targetClient.server.pullReplicate(
            KouchDatabase.PullReplicationRequestInput(
                sourceDb = sourceDatabaseName,
                sourceSettings = sourceContext.settings.copy(host = "first"),
                targetDb = targetDatabaseName,
            )
        ).also {
            assertNotNull(it.ok)
            assertTrue(it.ok!!)
        }

        targetClient.db.getAll().also { dbs ->
            assertEquals(KouchDatabaseService.systemDbs.size + 1, dbs.size)
            assertTrue(targetDatabaseName in dbs)
        }

        val replicatedEntity = targetClient.doc.get<TestEntity>(originalEntity.id, targetDatabaseName)
        assertNotNull(replicatedEntity)
        assertEquals(insertedEntity, replicatedEntity)
    }

    @Test
    fun singleReplicateCreateTargetTest() = runTest {

        sourceClient.db.create(sourceDatabaseName)

        val originalEntity = getEntity()
        val entity = originalEntity.copy(
            revision = null,
            label = "label1"
        )
        val (response, insertedEntity) = sourceClient.doc.insert(
            entity = entity,
            metadata = sourceContext.getMetadata(entity::class).copy(databaseName = sourceDatabaseName),
        ).getResponseAndUpdatedEntity()
        assertTrue(response.ok ?: false)
        assertNotNull(insertedEntity.revision)
        assertEquals(originalEntity.id, insertedEntity.id)

        targetClient.server.pullReplicate(
            KouchDatabase.PullReplicationRequestInput(
                sourceDb = sourceDatabaseName,
                sourceSettings = sourceContext.settings.copy(host = "first"),
                targetDb = targetDatabaseName,
                createTargetDb = true
            )
        ).also {
            assertNotNull(it.ok)
            assertTrue(it.ok!!)
        }

        targetClient.db.getAll().also { dbs ->
            assertEquals(KouchDatabaseService.systemDbs.size + 1, dbs.size)
            assertTrue(targetDatabaseName in dbs)
        }

        val replicatedEntity = targetClient.doc.get<TestEntity>(originalEntity.id, targetDatabaseName)
        assertNotNull(replicatedEntity)
        assertEquals(insertedEntity, replicatedEntity)
    }

    @Test
    fun continuouslyReplicateAndCancelTest() = runTest {

        sourceClient.db.create(sourceDatabaseName)
        targetClient.db.create(targetDatabaseName)

        val replicateRequest = KouchDatabase.PullReplicationRequestInput(
            sourceDb = sourceDatabaseName,
            sourceSettings = sourceContext.settings.copy(host = "first"),
            targetDb = targetDatabaseName,
            continuous = true,
        )
        targetClient.server.pullReplicate(replicateRequest).also {
            assertNotNull(it.ok)
            assertTrue(it.ok!!)
        }

        val originalEntity = getEntity()
        val entity = originalEntity.copy(
            revision = null,
            label = "label1"
        )
        val (response, insertedEntity) = sourceClient.doc.insert(
            entity = entity,
            metadata = sourceContext.getMetadata(entity::class).copy(databaseName = sourceDatabaseName),
        ).getResponseAndUpdatedEntity()
        assertTrue(response.ok ?: false)
        assertNotNull(insertedEntity.revision)
        assertEquals(originalEntity.id, insertedEntity.id)

        delay(replicationDelay)

        targetClient.db.getAll().also { dbs ->
            assertEquals(KouchDatabaseService.systemDbs.size + 1, dbs.size)
            assertTrue(targetDatabaseName in dbs)
        }

        val replicatedEntity = targetClient.doc.get<TestEntity>(originalEntity.id, targetDatabaseName)
        assertNotNull(replicatedEntity)
        assertEquals(insertedEntity, replicatedEntity)

        val cancelReplicateRequest = replicateRequest.copy(cancel = true)
        targetClient.server.pullReplicate(cancelReplicateRequest).also {
            assertEquals(true, it.ok)
        }

        val resultDeletion = sourceClient.doc.delete(
            id = insertedEntity.id,
            revision = insertedEntity.revision,
            db = sourceDatabaseName
        )()
        assertTrue(resultDeletion.ok ?: false)

        delay(replicationDelay)

        val replicatedEntityStillHere = targetClient.doc.get<TestEntity>(originalEntity.id, targetDatabaseName)
        assertNotNull(replicatedEntityStillHere)
        assertEquals(insertedEntity, replicatedEntityStillHere)
    }

    @Test
    fun continuouslyReplicateCreateTargetTest() = runTest {

        sourceClient.db.create(sourceDatabaseName)

        val replicateRequest = KouchDatabase.PullReplicationRequestInput(
            sourceDb = sourceDatabaseName,
            sourceSettings = sourceContext.settings.copy(host = "first"),
            targetDb = targetDatabaseName,
            createTargetDb = true,
            continuous = true,
        )
        targetClient.server.pullReplicate(replicateRequest).also {
            assertNotNull(it.ok)
            assertTrue(it.ok!!)
        }

        val originalEntity = getEntity()
        val entity = originalEntity.copy(
            revision = null,
            label = "label1"
        )
        val (response, insertedEntity) = sourceClient.doc.insert(
            entity = entity,
            metadata = sourceContext.getMetadata(entity::class).copy(databaseName = sourceDatabaseName),
        ).getResponseAndUpdatedEntity()
        assertTrue(response.ok ?: false)
        assertNotNull(insertedEntity.revision)
        assertEquals(originalEntity.id, insertedEntity.id)

        delay(replicationDelay)

        targetClient.db.getAll().also { dbs ->
            assertEquals(KouchDatabaseService.systemDbs.size + 1, dbs.size)
            assertTrue(targetDatabaseName in dbs)
        }

        val replicatedEntity = targetClient.doc.get<TestEntity>(originalEntity.id, targetDatabaseName)
        assertNotNull(replicatedEntity)
        assertEquals(insertedEntity, replicatedEntity)

        val cancelReplicateRequest = replicateRequest.copy(cancel = true)
        targetClient.server.pullReplicate(cancelReplicateRequest).also {
            assertEquals(true, it.ok)
        }
    }
}
