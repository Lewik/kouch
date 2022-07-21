package kouch.kouch

import kotlinx.serialization.Serializable
import kouch.KouchEntity
import kouch.copyWithRevision
import kotlin.test.Test
import kotlin.test.assertEquals


internal class CopyWithRevisionTest {

    @Serializable
    data class TestData(
        override val id: KouchEntity.Id,
        override val revision: KouchEntity.Rev? = null,
        val someData: String,
        val someDefData: String = "theDefData",
    ) : KouchEntity

    @Test
    fun copyWithRevisionTest1() {
        val test1 = TestData(id = KouchEntity.Id("someId"), someData = "someData")
        val target = test1.copy(revision = KouchEntity.Rev("rev"))

        val result = test1.copyWithRevision(KouchEntity.Rev("rev"))
        assertEquals(target, result)
    }

    @Test
    fun copyWithRevisionTest2() {
        val test1 = TestData(id = KouchEntity.Id("someId"), revision = KouchEntity.Rev("someRevision"), someData = "someData")
        val target = test1.copy(revision = KouchEntity.Rev("rev"))

        val result = test1.copyWithRevision(KouchEntity.Rev("rev"))
        assertEquals(target, result)
    }


}
