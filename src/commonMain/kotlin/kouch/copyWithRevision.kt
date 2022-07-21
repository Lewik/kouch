package kouch

internal expect fun <T : KouchEntity> T.copyWithRevision(revision: KouchEntity.Rev): T
