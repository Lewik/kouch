package kouch

internal actual fun <T : KouchEntity> T.copyWithRevision(revision: KouchEntity.Rev): T {
    TODO()
//    val copy = this::class.members.first { it.name == "copy" }
//    val instanceParam = copy.parameters.first { it.kind == KParameter.Kind.INSTANCE }
//    val revisionParam = copy.parameters.first { it.name == "revision" }
//    @Suppress("UNCHECKED_CAST")
//    return copy.callBy(mapOf(instanceParam to this, revisionParam to revision)) as T

}
