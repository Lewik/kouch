package kouch

import java.util.*

actual fun ByteArray.toBase64() = Base64.getEncoder().encodeToString(this)
actual fun String.fromBase64() = Base64.getDecoder().decode(this)
