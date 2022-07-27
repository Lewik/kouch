package kouch

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ClassName(val value: String) {
    override fun toString() = value
}


