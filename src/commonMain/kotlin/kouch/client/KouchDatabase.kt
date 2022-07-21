package kouch.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kouch.DatabaseName
import kouch.KouchDesign
import kouch.KouchEntity
import kouch.Settings

class KouchDatabase {
    @Serializable
    data class GetResponse(
        val cluster: Cluster,
        val compact_running: Boolean,
        val db_name: DatabaseName,
        val disk_format_version: Int,
        val doc_count: Int,
        val doc_del_count: Int,
        val instance_start_time: String, // always = "0"
        val purge_seq: String,
        val sizes: Sizes,
        val update_seq: String,
        val props: Props,
    ) {
        @Serializable
        data class Cluster(
            val n: Int,
            val q: Int,
            val r: Int,
            val w: Int,
        )

        @Serializable
        data class Sizes(
            val active: Int,
            val external: Int,
            val file: Int,
        )

        @Serializable
        data class Props(
            val partitioned: Boolean? = null,
        )
    }

    @Serializable
    data class HistoryResponse(
        val session_id: String,
        val start_time: String, //TODO datetime
        val end_time: String, //TODO datetime
        val recorded_seq: String,
        val end_last_seq: String,
        val start_last_seq: Int,
        val missing_checked: Int,
        val missing_found: Int,
        val docs_read: Int,
        val docs_written: Int,
        val doc_write_failures: Int,
    )

    /**
     * The create_target parameter is not destructive. If the database already exists, the replication proceeds as normal.
     */
    data class PullReplicationRequestInput(
        val sourceSettings: Settings,
        val sourceDb: DatabaseName,
        val targetDb: DatabaseName,
        val createTargetDb: Boolean = false,
        val continuous: Boolean = false,
        val cancel: Boolean = false,
    )

    @Serializable
    data class ReplicationRequest(
        val source: UrlWithHeader,
        val target: UrlWithHeader,
        val create_target: Boolean = false,
        val continuous: Boolean = false,
        val cancel: Boolean = false,
    )

    @Serializable
    data class ReplicationResponse(
        val ok: Boolean? = null,
        val session_id: String? = null,
        val source_last_seq: String? = null,
        val replication_id_version: Int? = null,
        val history: List<HistoryResponse>? = null,
        val _local_id: String? = null,
        val error: String? = null,
        val reason: String? = null,
    )

    @Serializable
    data class UrlWithHeader(
        val url: String = "",
        val headers: DbHeader = DbHeader(),
    )

    @Serializable
    data class DbHeader(
        val Authorization: String = "",
    )


    @Serializable
    data class ChangesRequest(
        val doc_ids: List<KouchEntity.Id> = emptyList(),
        val conflicts: Boolean = false,
        val descending: Boolean = false,
//        val feed: Feed,
        val filter: String? = null,
        val heartbeat: Int = 60_000,
        val include_docs: Boolean = false,
        val attachments: Boolean = false,
        val att_encoding_info: Boolean = false,
        @SerialName("last-event-id")
        val last_event_id: Boolean = false,
        val limit: Int? = null,
        val since: String = "now",
        val style: Style = Style.MAIN_ONLY,
        val timeout: Int? = null,
        val view: KouchDesign.ViewName? = null,
        val seq_interval: Int? = null,
    ) {
//        @Serializable
//        enum class Feed {
//            @SerialName("normal")
//            NORMAL,
//
//            @SerialName("longpoll")
//            LONG_POLL,
//
//            @SerialName("continuous")
//            CONTINUOUS,
//
//            @SerialName("eventsource")
//            EVENT_SOURCE,
//        }

        @Serializable
        enum class Style {
            @SerialName("main_only")
            MAIN_ONLY,

            @SerialName("all_docs")
            ALL_DOCS,
        }
    }

    @Serializable
    data class ChangesResponse(
        val last_seq: String,
        val pending: Int,
        val results: List<Result>,
    ) {
        @Serializable
        data class Result(
            val changes: List<RevOnly>,
            val id: KouchEntity.Id,
            val seq: String,
            val deleted: Boolean = false,
            val doc: KouchEntity? = null
        ) {
            @Serializable
            class RevOnly(
                val rev: KouchEntity.Rev
            )
        }

        @Serializable
        data class RawResult(
            val changes: List<Result.RevOnly>,
            val id: KouchEntity.Id,
            val seq: String,
            val deleted: Boolean = false,
            val doc: JsonObject? = null
        )

        @Serializable
        data class RawLastResult(
            val last_seq: String,
            val pending: Int
        )
    }

    @Serializable
    data class BulkGetRawResult(
        val results: List<Result>
    ) {
        @Serializable
        data class Result(
            val id: KouchEntity.Id,
            val docs: List<Doc>,
        ) {
            @Serializable
            data class Doc(
                val ok: JsonObject? = null,
                val error: Error? = null
            ) {
                @Serializable
                data class Error(
                    val id: KouchEntity.Id,
                    val rev: KouchEntity.Rev,
                    val error: String,
                    val reason: String,
                )
            }
        }
    }

    @Serializable
    data class BulkGetResult<T : KouchEntity>(
        val entities: List<T>,
        val errors: List<BulkGetRawResult.Result.Doc.Error>
    )

    @Serializable
    data class BulkUpsertRequest(
        val docs: JsonArray,
        val new_edits: Boolean = true
    )

    @Serializable
    data class BulkUpsertResponse(
        val id: KouchEntity.Id,
        val rev: KouchEntity.Rev? = null,
        val ok: Boolean? = null,
        val error: String? = null,
        val reason: String? = null
    )
}
