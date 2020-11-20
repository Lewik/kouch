package kouch

expect fun <T : KouchEntity> T.copyWithRevision(revision: String): T
