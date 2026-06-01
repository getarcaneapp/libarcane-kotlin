package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.jobschedule.JobListResponse
import app.getarcane.sdk.models.jobschedule.JobRunResponse
import app.getarcane.sdk.models.jobschedule.JobScheduleConfig
import app.getarcane.sdk.models.jobschedule.UpdateJobScheduleConfig

/** Background job schedules and manual execution for an environment. Port of Swift `JobsService`. */
public class JobsService internal constructor(private val rest: RestService) {
    /**
     * Get configured cron schedules for background jobs.
     *
     * This endpoint returns the raw config object directly (without an `APIResponse` envelope), so
     * the request is unwrapped manually via the transport.
     */
    public suspend fun getSchedules(envId: EnvironmentId? = null): JobScheduleConfig =
        rest.transport.requestDecoded(rest.environmentPath(envId, "job-schedules"))

    /** Update background job cron schedules. Only non-null fields are applied. */
    public suspend fun updateSchedules(
        body: UpdateJobScheduleConfig,
        envId: EnvironmentId? = null,
    ): JobScheduleConfig = rest.put(rest.environmentPath(envId, "job-schedules"), body = body)

    /**
     * List all background jobs with their status, schedule, and metadata.
     *
     * This endpoint returns the list object directly (without an `APIResponse` envelope), so the
     * request is unwrapped manually via the transport.
     */
    public suspend fun list(envId: EnvironmentId? = null): JobListResponse =
        rest.transport.requestDecoded(rest.environmentPath(envId, "jobs"))

    /**
     * Manually trigger a background job to run immediately.
     *
     * This endpoint returns the run-response body directly (without an `APIResponse` envelope), so
     * the request is unwrapped manually via the transport.
     */
    public suspend fun run(jobId: String, envId: EnvironmentId? = null): JobRunResponse =
        rest.transport.requestDecoded(rest.environmentPath(envId, "jobs/$jobId/run"), method = "POST")
}
