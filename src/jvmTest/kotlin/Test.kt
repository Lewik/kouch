import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.coroutines.runBlocking

import kouch.Kouch
import kouch.KouchTyped
import kotlin.test.Test


class Test {

    @Test
    fun test1() {
        runBlocking {
            val kouch = Kouch(
                client = HttpClient {
                    install(JsonFeature) {
                        serializer = KotlinxSerializer().apply {
                            register<KouchTyped.Root>()
                        }
                    }
                })
            println(KouchTyped(kouch).root())
        }
    }

//    @Test
//    fun test2() {
//        runBlocking {
//            val kouch = Kouch()
//            println(kouch.root<String>())
//        }
//    }

}
