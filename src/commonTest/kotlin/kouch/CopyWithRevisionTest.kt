package kouch.kouch

import kotlinx.serialization.Serializable
import kouch.KouchEntity
import kouch.copyWithRevision
import kotlin.test.Test
import kotlin.test.assertEquals



internal class CopyWithRevisionTest {
    
    @Serializable
    data class TestData(
        override val id: String,
        override val revision: String? = null,
        val someData: String,
        val someDefData: String = "theDefData"
    ) : KouchEntity

    @Test
    fun copyWithRevisionTest1() {
        val test1 = TestData(id = "someId", someData = "someData")
        val target = test1.copy(revision = "rev")

        val result = test1.copyWithRevision("rev")
        assertEquals(target, result)
    }

    @Test
    fun copyWithRevisionTest2() {
        val test1 = TestData(id = "someId", revision = "someRevision", someData = "someData")
        val target = test1.copy(revision = "rev")

        val result = test1.copyWithRevision("rev")
        assertEquals(target, result)
    }


}
