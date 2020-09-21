package kouch

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals


internal class KotlinSerializerTest {

    @KouchEntityMetadata("test-data","test-data")
    @Serializable
    data class TestData(
        override val id: String,
        override val revision: String? = null,
        val someData: String,
        val someDefData: String = "theDefData"
    ) : KouchEntity

    @KouchEntityMetadata("test-data-not-sorted-fields","test-data-not-sorted-fields")
    @Serializable
    data class TestDataNotSortedFields(
        override val id: String,
        override val revision: String? = null,
        val someDefData: String = "theDefData",
        val test: TestDataNotSortedFields? = null,
        val someData: String
    ) : KouchEntity

    private val context = KouchTestHelper.defaultContext

    @Test
    fun serializeAllSet() {
        val data = TestData(
            id = "theId",
            revision = "theRevision",
            someData = "someData",
            someDefData = "theDefData2"
        )
        val json =
            """{"_id":"theId","_rev":"theRevision","someData":"someData","someDefData":"theDefData2","class__":"test-data"}"""

        val str = context.encodeToKouchEntity(data, context.getMetadata(data::class).className)
        assertEquals(json, str)
    }

    @Test
    fun serializeWithDefaultRev() {
        val data = TestData(
            id = "theId",
            someData = "someData",
            someDefData = "theDefData2"
        )
        val json = """{"_id":"theId","someData":"someData","someDefData":"theDefData2","class__":"test-data"}"""

        val str = context.encodeToKouchEntity(data, context.getMetadata(data::class).className)
        assertEquals(json, str)
    }

    @Test
    fun serializeWithDefaultData() {
        val data = TestData(
            id = "theId",
            revision = "theRevision",
            someData = "someData",
            //someDefData = "theDefData"
        )
        val json =
            """{"_id":"theId","_rev":"theRevision","someData":"someData","someDefData":"theDefData","class__":"test-data"}"""

        val str = context.encodeToKouchEntity(data, context.getMetadata(data::class).className)
        assertEquals(json, str)
    }

    @Test
    fun serializeWithDefaultRevAndData() {
        val data = TestData(
            id = "theId",
            someData = "someData",
            //someDefData = "theDefData"
        )
        val json = """{"_id":"theId","someData":"someData","someDefData":"theDefData","class__":"test-data"}"""

        val str = context.encodeToKouchEntity(data, context.getMetadata(data::class).className)
        assertEquals(json, str)
    }

    @Test
    fun deserialize() {
        val data = TestDataNotSortedFields(
            id = "theId",
            revision = "theRevision",
            someData = "someData",
            someDefData = "theDefData2",
            test = TestDataNotSortedFields(
                id = "SUBtheId",
                revision = "SUBtheRevision",
                someData = "SUBsomeData",
                someDefData = "SUBtheDefData2"
            )
        )

        val json =
            """{"_id":"theId","_rev":"theRevision","someData":"someData","someDefData":"theDefData2","test":{"id":"SUBtheId","revision":"SUBtheRevision","someData":"SUBsomeData","someDefData":"SUBtheDefData2","test":null}}"""


        val result = context.decodeKouchEntityFromJsonElement<TestDataNotSortedFields>(
            context.responseJson.parseToJsonElement(json)
        )
        assertEquals(data, result)
    }


}
