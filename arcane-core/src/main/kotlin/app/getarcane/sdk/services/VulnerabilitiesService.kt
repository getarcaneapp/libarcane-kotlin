package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.vulnerability.EnvironmentVulnerabilitySummary
import app.getarcane.sdk.models.vulnerability.IgnoredVulnerability
import app.getarcane.sdk.models.vulnerability.Vulnerability
import app.getarcane.sdk.models.vulnerability.VulnerabilityIgnorePayload
import app.getarcane.sdk.models.vulnerability.VulnerabilityScanResult
import app.getarcane.sdk.models.vulnerability.VulnerabilityScanSummariesRequest
import app.getarcane.sdk.models.vulnerability.VulnerabilityScanSummariesResponse
import app.getarcane.sdk.models.vulnerability.VulnerabilityScanSummary
import app.getarcane.sdk.models.vulnerability.VulnerabilityScannerStatus
import app.getarcane.sdk.models.vulnerability.VulnerabilityWithImage
import app.getarcane.sdk.pagination.PaginatedResponse

/** Trivy vulnerability scanning, summaries, and ignore records. */
public class VulnerabilitiesService internal constructor(private val rest: RestService) {
    // MARK: - Scans (per-image)

    /**
     * Initiate a new Trivy scan for an image. Returns the resulting scan record once the scan
     * completes.
     */
    public suspend fun scanImage(envId: EnvironmentId? = null, imageId: String): VulnerabilityScanResult =
        rest.post(rest.environmentPath(envId, "images/$imageId/vulnerabilities/scan"))

    /** Most recent full scan result for an image. */
    public suspend fun scanResult(envId: EnvironmentId? = null, imageId: String): VulnerabilityScanResult =
        rest.get(rest.environmentPath(envId, "images/$imageId/vulnerabilities"))

    /** Compact severity summary for an image. Suitable for list views. */
    public suspend fun scanSummary(envId: EnvironmentId? = null, imageId: String): VulnerabilityScanSummary =
        rest.get(rest.environmentPath(envId, "images/$imageId/vulnerabilities/summary"))

    /** Batch lookup of scan summaries keyed by image ID. */
    public suspend fun scanSummaries(
        envId: EnvironmentId? = null,
        imageIds: List<String>,
    ): VulnerabilityScanSummariesResponse =
        rest.post(
            rest.environmentPath(envId, "images/vulnerabilities/summaries"),
            body = VulnerabilityScanSummariesRequest(imageIds = imageIds),
        )

    /** Paginated list of vulnerabilities for a specific image. */
    public suspend fun listForImage(
        envId: EnvironmentId? = null,
        imageId: String,
        query: SearchPaginationSort = SearchPaginationSort(),
        severity: String? = null,
    ): PaginatedResponse<Vulnerability> {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            severity?.let { add("severity" to it) }
        }
        return rest.transport.paginated(
            rest.environmentPath(envId, "images/$imageId/vulnerabilities/list"),
            query.start ?: 0,
            query.limit ?: 20,
            items,
        )
    }

    // MARK: - Environment-wide

    /** Status of the bundled Trivy scanner. */
    public suspend fun scannerStatus(envId: EnvironmentId? = null): VulnerabilityScannerStatus =
        rest.get(rest.environmentPath(envId, "vulnerabilities/scanner-status"))

    /** Aggregated vulnerability counts across all images in the environment. */
    public suspend fun environmentSummary(envId: EnvironmentId? = null): EnvironmentVulnerabilitySummary =
        rest.get(rest.environmentPath(envId, "vulnerabilities/summary"))

    /** Paginated list of vulnerabilities across all scanned images in the environment. */
    public suspend fun listAll(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
        severity: String? = null,
        imageName: String? = null,
    ): PaginatedResponse<VulnerabilityWithImage> {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            severity?.let { add("severity" to it) }
            imageName?.let { add("imageName" to it) }
        }
        return rest.transport.paginated(
            rest.environmentPath(envId, "vulnerabilities/all"),
            query.start ?: 0,
            query.limit ?: 20,
            items,
        )
    }

    /** Distinct image names available for vulnerability filtering. */
    public suspend fun imageOptions(
        envId: EnvironmentId? = null,
        severity: String? = null,
    ): List<String> {
        val items = buildList { severity?.let { add("severity" to it) } }
        return rest.get(rest.environmentPath(envId, "vulnerabilities/image-options"), items)
    }

    // MARK: - Ignore records

    /** Create an ignore record for a specific vulnerability finding. */
    public suspend fun ignore(
        envId: EnvironmentId? = null,
        payload: VulnerabilityIgnorePayload,
    ): IgnoredVulnerability =
        rest.post(rest.environmentPath(envId, "vulnerabilities/ignore"), body = payload)

    /** Remove an existing ignore record. */
    public suspend fun unignore(envId: EnvironmentId? = null, ignoreId: String) {
        rest.deleteVoid(rest.environmentPath(envId, "vulnerabilities/ignore/$ignoreId"))
    }

    /** Paginated list of currently ignored vulnerabilities. */
    public suspend fun listIgnored(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
    ): PaginatedResponse<IgnoredVulnerability> =
        rest.transport.paginated(
            rest.environmentPath(envId, "vulnerabilities/ignored"),
            query.start ?: 0,
            query.limit ?: 20,
            query.nonPaginationQueryItems,
        )
}
