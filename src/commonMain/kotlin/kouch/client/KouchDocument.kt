package kouch.client

import kotlinx.serialization.Serializable
import kouch.KouchEntity

class KouchDocument {

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
        val rev: KouchEntity.Rev? = null,
        val revs: Boolean = false,
        val revs_info: Boolean = false,
    )

    @Serializable
    data class GetResponse(
        val _id: KouchEntity.Id? = null,
        val _rev: KouchEntity.Rev? = null,
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
            val ids: List<KouchEntity.Id>,
            val start: Int,
        )
    }

    @Serializable
    class PutQueryParameters(
        val rev: KouchEntity.Rev? = null,
        /**
         * possible values: [null|"ok"]
         */
        val batch: String? = null,
        val new_edits: Boolean = true,
    )

    @Serializable
    data class PutResponse(
        val id: KouchEntity.Id? = null,
        val ok: Boolean? = null,
        val rev: KouchEntity.Rev? = null,
        val error: String? = null,
        val reason: String? = null,
    )

    @Serializable
    class DeleteQueryParameters(
        val rev: KouchEntity.Rev,
        /**
         * possible values: [null|"ok"]
         */
        val batch: String? = null,
    )

    @Serializable
    data class DeleteResponse(
        val id: KouchEntity.Id? = null,
        val ok: Boolean? = null,
        val rev: KouchEntity.Rev? = null,
        val error: String? = null,
        val reason: String? = null,
    )
}
