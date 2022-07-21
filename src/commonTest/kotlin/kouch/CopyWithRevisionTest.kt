package kouch.kouch

import kotlinx.serialization.Serializable
import kouch.TestId
import kouch.client.KouchDocument
import kouch.client.KouchDocument.Rev
import kouch.copyWithRevision
import kotlin.test.Test
import kotlin.test.assertEquals


internal class CopyWithRevisionTest {

    @Serializable
    data class TestData(
        override val id: TestId,
        override val revision: Rev? = null,
        val someData: String,
        val someDefData: String = "theDefData",
    ) : KouchDocument

    @Test
    fun copyWithRevisionTest1() {
        val test1 = TestData(id = TestId("someId"), someData = "someData")
        val target = test1.copy(revision = Rev("rev"))

        val result = test1.copyWithRevision(Rev("rev"))
        assertEquals(target, result)
    }

    @Test
    fun copyWithRevisionTest2() {
        val test1 = TestData(id = TestId("someId"), revision = Rev("someRevision"), someData = "someData")
        val target = test1.copy(revision = Rev("rev"))

        val result = test1.copyWithRevision(Rev("rev"))
        assertEquals(target, result)
    }


}
