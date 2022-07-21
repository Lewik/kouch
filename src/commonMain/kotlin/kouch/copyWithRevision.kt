package kouch

import kouch.client.KouchDocument

internal expect fun <T : KouchDocument> T.copyWithRevision(revision: KouchDocument.Rev): T
