package kouch

import kotlin.reflect.KClass

actual fun <T : KouchEntity> Context.getMetadata(kClass: KClass<out T>): KouchMetadata.Entity {
    val annotation = kClass.annotations.firstOrNull { it is KouchEntityMetadata } as? KouchEntityMetadata
        ?: throw NoMetadataAnnotationException("No KouchEntityMetadata annotation for ${kClass.qualifiedName}. KouchEntityMetadata should be specified for each KouchEntity")


    val databaseName = annotation.databaseName
        .takeIf { it.isNotEmpty() }
        ?: settings.autoGenerate.generateDatabaseName(kClass)

    val className = annotation.className
        .takeIf { it.isNotEmpty() }
        ?: settings.autoGenerate.generateClassName(kClass)

    if (databaseName.isBlank()) {
        throw IllegalArgumentException("databaseName is blank for $kClass")
    }
    if (className.isBlank()) {
        throw IllegalArgumentException("className is blank for $kClass")
    }

    return KouchMetadata.Entity(DatabaseName(databaseName), ClassName(className))
}
