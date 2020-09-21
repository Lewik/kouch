package kouch

import kouch.client.KouchClientImpl
import kouch.client.KouchDatabaseService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class KouchServerTest {

    private val kouch = KouchClientImpl(KouchTestHelper.defaultContext)

    @BeforeTest
    fun beforeTest() = runTest {
        KouchTestHelper.removeAllDbsAndCreateSystemDbsServer(KouchTestHelper.defaultContext)
    }

    @Test
    fun rootTest() = runTest {
        val response = kouch.server.root()
        assertEquals("Welcome", response.couchdb)
        assertEquals("3.1.1", response.version)
    }

    @Test
    fun allDbsTest() = runTest {
        assertEquals(KouchDatabaseService.systemDbs.map { it.value }.sorted(), kouch.db.getAll().map { it.value }.sorted())
    }
}
