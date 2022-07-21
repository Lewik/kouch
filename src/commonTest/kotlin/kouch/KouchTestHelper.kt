package kouch

import kotlinx.serialization.Serializable
import kouch.client.KouchClientImpl
import kouch.client.KouchDatabase
import kouch.client.KouchDocument

@Serializable
@JvmInline
internal value class TestId(override val value: String) : KouchDocument.Id


@Suppress("MemberVisibilityCanBePrivate")
internal object KouchTestHelper {

    val defaultSettings = Settings(
        adminName = "dbadmin",
        adminPassword = "dbadmin",
        databaseNaming = Settings.DatabaseNaming.Predefined(KouchDatabase.Name("defaultdb"))
    )
    val defaultContext = Context(
        settings = defaultSettings,
        strictSystemJson = true
    )

    val secondarySettings = Settings(
        adminName = "dbadmin",
        adminPassword = "dbadmin",
        port = 5985,
        databaseNaming = Settings.DatabaseNaming.Predefined(KouchDatabase.Name("secondadydb"))
    )
    val secondaryContext = Context(
        settings = secondarySettings,
        strictSystemJson = true
    )

    val perEntityDbSettings = Settings(
        adminName = "dbadmin",
        adminPassword = "dbadmin",
        databaseNaming = Settings.DatabaseNaming.AutoGenerate
    )
    val perEntityDbContext = Context(
        settings = perEntityDbSettings,
        strictSystemJson = true
    )


    //    val userSettings = Settings(
//        db = "_users",
//        adminName = "dbadmin",
//        adminPassword = "dbadmin",
//    )
//

    suspend fun removeAllDbsAndCreateSystemDbsServer(context: Context = defaultContext) {
        val client = KouchClientImpl(context)
        client.db.getAll().forEach { client.db.delete(it) }
        client.db.createSystemDbs()
    }

    //
//    suspend fun createUser(user: KouchUser.User, settings: Settings? = null): String? {
//        val userService = user()
//        val result = userService.create(
//            KouchUser.Request(
//                name = user.name,
//                password = user.password,
//                roles = listOf("member"),
//                id = "org.couchdb.user:${user.name}",
//                type = "user"
//            )
//        )
//        return result.id
//    }
//
//    suspend fun createAdmin(user: KouchUser.User, settings: Settings? = null): String? {
//        val userService = user()
//        val result = userService.create(
//            KouchUser.Request(
//                name = user.name,
//                password = user.password,
//                roles = listOf("admin"),
//                id = "org.couchdb.user:${user.name}",
//                type = "user"
//            )
//        )
//        return result.id
//    }
//
//    suspend fun removeAllUsers(settings: Settings = userSettings) {
//        val databaseService = database(settings)
//        val userService = user(settings)
//        val usersList = databaseService.getAllDocs()!!["rows"]?.jsonArray!!
//        usersList
//            .filter {
//                (it as? JsonObject)?.contains("id") ?: false && it.jsonObject.getValue("id").jsonPrimitive.content.contains("org.couchdb.user:")
//            }
//            .forEach {
//                val user = it.jsonObject
//                val id = user.getValue("id").jsonPrimitive.content
//                val res = user.getValue("value").jsonObject.getValue("rev").jsonPrimitive.content
//                userService.delete(
//                    id = id,
//                    revision = res
//                )
//            }
//
//    }
//

}

