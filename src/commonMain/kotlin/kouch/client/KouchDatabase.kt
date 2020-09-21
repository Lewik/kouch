package kouch.client

import kotlinx.serialization.Serializable
import kouch.DatabaseName
import kouch.Settings

class KouchDatabase {
//
//    @Serializable
//    data class StandardResponse(
//        val ok: Boolean? = null,
//        val id: String? = null,
//        val rev: String? = null,
//        val error: String? = null,
//        val reason: String? = null
//    )

    @Serializable
    data class GetResponse(
        val cluster: Cluster,
        val compact_running: Boolean,
        val db_name: String,
        val disk_format_version: Int,
        val doc_count: Int,
        val doc_del_count: Int,
        val instance_start_time: String, // always = "0"
        val purge_seq: String,
        val sizes: Sizes,
        val update_seq: String,
        val props: Props,
    ) : KouchResponse {
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
            val partitioned: Boolean? = null
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
        val doc_write_failures: Int
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
        val cancel: Boolean = false
    )

    @Serializable
    data class ReplicationRequest(
        val source: UrlWithHeader,
        val target: UrlWithHeader,
        val create_target: Boolean = false,
        val continuous: Boolean = false,
        val cancel: Boolean = false
    )

    @Serializable
    data class ReplicationResponse(
        val ok: Boolean? = null,
        val session_id: String? = null,
        val source_last_seq: String? = null,
        val replication_id_version: Int? = null,
        val history: List<HistoryResponse>? = null,
        val error: String? = null,
        val reason: String? = null,
        val _local_id: String? = null,
    )

    @Serializable
    data class UrlWithHeader(
        val url: String = "",
        val headers: DbHeader = DbHeader()
    )

    @Serializable
    data class DbHeader(
        val Authorization: String = ""
    )
}
