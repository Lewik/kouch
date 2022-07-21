package kouch.kouch

import kotlinx.serialization.Serializable
import kouch.Id
import kouch.KouchEntity
import kouch.KouchEntity.Rev
import kouch.copyWithRevision
import kotlin.test.Test
import kotlin.test.assertEquals


internal class CopyWithRevisionTest {

    @Serializable
    data class TestData(
        override val id: KouchEntity.Id,
        override val revision: Rev? = null,
        val someData: String,
        val someDefData: String = "theDefData",
    ) : KouchEntity

    @Test
    fun copyWithRevisionTest1() {
        val test1 = TestData(id = Id("someId"), someData = "someData")
        val target = test1.copy(revision = Rev("rev"))

        val result = test1.copyWithRevision(Rev("rev"))
        assertEquals(target, result)
    }

    @Test
    fun copyWithRevisionTest2() {
        val test1 = TestData(id = Id("someId"), revision = Rev("someRevision"), someData = "someData")
        val target = test1.copy(revision = Rev("rev"))

        val result = test1.copyWithRevision(Rev("rev"))
        assertEquals(target, result)
    }


}
