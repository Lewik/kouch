package kouch

import kotlin.reflect.KClass

actual fun <T : KouchEntity> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity {
    val annotation = kClass.annotations.firstOrNull { it is KouchEntityMetadata } as? KouchEntityMetadata
        ?: throw NoMetadataAnnotationException("No KouchEntityMetadata annotation for ${kClass.qualifiedName}. KouchEntityMetadata should be specified for each KouchEntity")

    val (databaseName, className) = if (annotation.autoGenerate) {
        settings.autoGenerate.generateDatabaseName(kClass) to settings.autoGenerate.generateClassName(kClass)
    } else {
        when (settings.databaseNaming) {
            Settings.DatabaseNaming.DatabaseNameAnnotation -> annotation.databaseName
            is Settings.DatabaseNaming.Predefined -> settings.databaseNaming.databaseName
        } to annotation.className
    }

    return KouchMetadata.Entity(DatabaseName(databaseName), ClassName(className))
}
