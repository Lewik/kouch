package kouch.repository


import kouch.KouchClient
import kouch.client.KouchDatabase
import kouch.client.KouchDesign
import kouch.client.KouchDocument
import kotlin.reflect.KClass


interface KouchMigrationContainer {
    fun getMigrations(): List<Migration>


    sealed class Migration(
        val priority: Int = Int.MAX_VALUE,
        val migrate: suspend () -> Unit,
    ) {
        class CommonMigration(migrate: suspend () -> Unit) : Migration(migrate = migrate)
        class CreateDb(
            kouchClient: KouchClient,
            databaseNames: List<KouchDatabase.Name>,
        ) : Migration(priority = 0, migrate = {
            databaseNames
                .minus(kouchClient.db.getAll())
                .forEach { kouchClient.db.create(it) }
        })

        class CreateDbForEntities(
            kouchClient: KouchClient,
            kClasses: List<KClass<out KouchDocument>>,
        ) : Migration(priority = 0, migrate = {
            kouchClient.db.createForEntitiesIfNotExists(kClasses)
        })

        class UpsertDesign(
            kouchClient: KouchClient,
            databaseName: KouchDatabase.Name,
            design: KouchDesign,
        ) : Migration(priority = 1_000, migrate = {
            val existedRev = kouchClient.design.getWithResponse(design.id, databaseName).second?.revision
            kouchClient.design.upsert(
                ddoc = design.copy(revision = existedRev),
                db = databaseName
            )
            Unit
        })
    }
}
