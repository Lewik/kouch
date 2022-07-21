package kouch

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ClassName(val value: String) {
    override fun toString() = value
}


