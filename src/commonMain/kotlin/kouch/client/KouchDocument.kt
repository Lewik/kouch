package kouch.client

import kotlinx.serialization.Serializable

class KouchDocument {

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
        val rev: String? = null,
        val revs: Boolean = false,
        val revs_info: Boolean = false,
    )

    @Serializable
    data class GetResponse(
        val _id: String? = null,
        val _rev: String? = null,
        val _deleted: Boolean? = null,
        val _attachments: Attachments? = null,
        val _conflicts: List<String>? = null,
        val _deleted_conflicts: List<String>? = null,
        val _local_seq: String? = null,
        val _revs_info: List<String>? = null,
        val _revisions: Revisions? = null,

        val error: String? = null,
        val reason: String? = null,
    ) : KouchResponse {
        @Serializable
        class Attachments

        @Serializable
        data class Revisions(
            val ids: List<String>,
            val start: Int
        )
    }

    @Serializable
    class PutQueryParameters(
        val rev: String? = null,
        /**
         * possible values: [null|"ok"]
         */
        val batch: String? = null,
        val new_edits: Boolean = true,
    )

    @Serializable
    data class PutResponse(
        val id: String? = null,
        val ok: Boolean? = null,
        val rev: String? = null,
        val error: String? = null,
        val reason: String? = null,
    ) : KouchResponse

    @Serializable
    class DeleteQueryParameters(
        val rev: String,
        /**
         * possible values: [null|"ok"]
         */
        val batch: String? = null,
    )

    @Serializable
    data class DeleteResponse(
        val id: String? = null,
        val ok: Boolean? = null,
        val rev: String? = null,
        val error: String? = null,
        val reason: String? = null,
    ) : KouchResponse
}
