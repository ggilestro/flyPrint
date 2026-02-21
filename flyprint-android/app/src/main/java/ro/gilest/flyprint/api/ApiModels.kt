package ro.gilest.flyprint.api

import com.google.gson.annotations.SerializedName

/**
 * A print job returned by the server.
 */
data class PrintJob(
    val id: String,
    @SerializedName("stock_ids") val stockIds: List<String> = emptyList(),
    @SerializedName("label_format") val labelFormat: String = "",
    val copies: Int = 1,
    @SerializedName("code_type") val codeType: String = "qr",
    val status: String = "pending",
    @SerializedName("created_at") val createdAt: String = ""
)

/**
 * Heartbeat payload sent to the server.
 */
data class HeartbeatRequest(
    @SerializedName("printer_name") val printerName: String?,
    @SerializedName("printer_status") val printerStatus: String?
)

/**
 * Heartbeat response from server (may include config_version).
 */
data class HeartbeatResponse(
    @SerializedName("config_version") val configVersion: Int? = null,
    @SerializedName("latest_agent_version") val latestAgentVersion: String? = null
)

/**
 * Result payload sent when completing a job.
 */
data class JobResult(
    val success: Boolean,
    @SerializedName("error_message") val errorMessage: String? = null
)
