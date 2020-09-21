package kouch

expect inline fun <reified T : KouchEntity> T.copyWithRevision(revision: String): T
