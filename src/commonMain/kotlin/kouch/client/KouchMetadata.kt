package kouch.client

import kouch.ClassName

sealed class KouchMetadata {
    data class Entity(
        val databaseName: KouchDatabase.Name,
        val className: ClassName,
    ) : KouchMetadata()

    data class Design(
        val databaseName: KouchDatabase.Name,
    ) : KouchMetadata()
}
