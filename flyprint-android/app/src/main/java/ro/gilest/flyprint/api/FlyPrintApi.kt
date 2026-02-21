package ro.gilest.flyprint.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for FlyPush server agent endpoints.
 */
interface FlyPrintApi {

    @POST("/api/labels/agent/heartbeat")
    suspend fun sendHeartbeat(@Body request: HeartbeatRequest): Response<HeartbeatResponse>

    @GET("/api/labels/agent/jobs")
    suspend fun getPendingJobs(): List<PrintJob>

    @POST("/api/labels/agent/jobs/{id}/claim")
    suspend fun claimJob(@Path("id") jobId: String): Response<PrintJob>

    @GET("/api/labels/agent/jobs/{id}/image")
    suspend fun getJobImage(@Path("id") jobId: String): ResponseBody

    @POST("/api/labels/agent/jobs/{id}/start")
    suspend fun startPrinting(@Path("id") jobId: String): Response<PrintJob>

    @POST("/api/labels/agent/jobs/{id}/complete")
    suspend fun completeJob(
        @Path("id") jobId: String,
        @Body result: JobResult
    ): Response<PrintJob>
}
