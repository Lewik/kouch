package kouch.client

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

interface KouchDocument {

    val id: Id
    val revision: Rev?

    interface Id {
        val value: String
        fun isBlank() = value.isBlank()
    }

    @Serializable
    @JvmInline
    value class Rev(val value: String)

    @Serializable
    @JvmInline
    value class CommonId(override val value: String) : Id

    companion object {
        const val HIGHEST_KEY = '\ufff0'
    }

    @Serializable
    class GetQueryParameters(
        val attachments: Boolean = false,
        val att_encoding_info: Boolean = false,
        val atts_since: List<String>? = null,
        val conflicts: Boolean = false,
        val deleted_conflicts: Boolean = false,
        val latest: Boolean = false,
        val local_seq: Boolean = false,
        val meta: Boolean = false,
        //TODO: support "all" value
        val open_revs: List<String>? = null,
        val rev: KouchDocument.Rev? = null,
        val revs: Boolean = false,
        val revs_info: Boolean = false,
    )

    @Serializable
    data class GetResponse(
        val _id: CommonId? = null,
        val _rev: KouchDocument.Rev? = null,
        val _deleted: Boolean? = null,
        val _attachments: Attachments? = null,
        val _conflicts: List<String>? = null,
        val _deleted_conflicts: List<String>? = null,
        val _local_seq: String? = null,
        val _revs_info: List<String>? = null,
        val _revisions: Revisions? = null,

        val error: String? = null,
        val reason: String? = null,
    ) {
        @Serializable
        class Attachments

        @Serializable
        data class Revisions(
            val ids: List<CommonId>,
            val start: Int,
        )
    }

    @Serializable
    class PutQueryParameters(
        val rev: KouchDocument.Rev? = null,
        /**
         * possible values: [null|"ok"]
         */
        val batch: String? = null,
        val new_edits: Boolean = true,
    )

    @Serializable
    data class PutResponse(
        val id: CommonId? = null,
        val ok: Boolean? = null,
        val rev: KouchDocument.Rev? = null,
        val error: String? = null,
        val reason: String? = null,
    )

    @Serializable
    class DeleteQueryParameters(
        val rev: KouchDocument.Rev,
        /**
         * possible values: [null|"ok"]
         */
        val batch: String? = null,
    )

    @Serializable
    data class DeleteResponse(
        val id: CommonId? = null,
        val ok: Boolean? = null,
        val rev: KouchDocument.Rev? = null,
        val error: String? = null,
        val reason: String? = null,
    )
}
