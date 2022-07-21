package kouch

import kouch.client.KouchDatabase
import kouch.client.KouchDocument
import kouch.client.KouchMetadata
import kotlin.reflect.KClass

actual fun <T : KouchDocument> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity {
    val annotation = kClass.annotations.firstOrNull { it is KouchDocumentMetadata } as? KouchDocumentMetadata
        ?: throw NoMetadataAnnotationException("No KouchDocumentMetadata annotation for ${kClass.qualifiedName}. KouchDocumentMetadata should be specified for each KouchDocument")


    val databaseName = when {
        annotation.databaseName.isNotEmpty() -> annotation.databaseName
        annotation.generateDatabaseName -> settings.autoGenerate.generateDatabaseName(kClass)
        else -> when (settings.databaseNaming) {
            is Settings.DatabaseNaming.Predefined -> settings.databaseNaming.databaseName.value
            Settings.DatabaseNaming.AutoGenerate -> settings.autoGenerate.generateDatabaseName(kClass)
        }
    }

    val className = when {
        annotation.className.isNotEmpty() -> annotation.className
        else -> settings.autoGenerate.generateClassName(kClass)
    }

    if (databaseName.isBlank()) {
        throw IllegalArgumentException("databaseName is blank for $kClass")
    }
    if (className.isBlank()) {
        throw IllegalArgumentException("className is blank for $kClass")
    }

    return KouchMetadata.Entity(KouchDatabase.Name(databaseName), ClassName(className))
}
