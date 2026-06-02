package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.gitops.CreateGitOpsSync
import app.getarcane.sdk.models.gitops.CreateGitRepository
import app.getarcane.sdk.models.gitops.GitOpsBranchesResponse
import app.getarcane.sdk.models.gitops.GitOpsBrowseResponse
import app.getarcane.sdk.models.gitops.GitOpsSync
import app.getarcane.sdk.models.gitops.GitOpsSyncResult
import app.getarcane.sdk.models.gitops.GitOpsSyncStatus
import app.getarcane.sdk.models.gitops.GitRepository
import app.getarcane.sdk.models.gitops.GitRepositorySyncRequest
import app.getarcane.sdk.models.gitops.ImportGitOpsSyncRequest
import app.getarcane.sdk.models.gitops.ImportGitOpsSyncResponse
import app.getarcane.sdk.models.gitops.UpdateGitOpsSync
import app.getarcane.sdk.models.gitops.UpdateGitRepository
import app.getarcane.sdk.pagination.PaginatedResponse

/** Manages Git repositories and GitOps sync configurations. */
public class GitOpsService internal constructor(private val rest: RestService) {
    // Git repositories (top-level)

    /** List configured git repositories with pagination. */
    public suspend fun listRepositoriesPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<GitRepository> {
        val query = buildList {
            search?.let { add("search" to it) }
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
        }
        return rest.transport.paginated<GitRepository>("customize/git-repositories", start, limit, query)
    }

    /** Get a git repository by ID. */
    public suspend fun getRepository(id: String): GitRepository = rest.get("customize/git-repositories/$id")

    /** Create a new git repository configuration. */
    public suspend fun createRepository(body: CreateGitRepository): GitRepository =
        rest.post("customize/git-repositories", body = body)

    /** Update an existing git repository. */
    public suspend fun updateRepository(id: String, body: UpdateGitRepository): GitRepository =
        rest.put("customize/git-repositories/$id", body = body)

    /** Delete a git repository configuration. */
    public suspend fun deleteRepository(id: String) {
        rest.deleteVoid("customize/git-repositories/$id")
    }

    /** Test connectivity and authentication for a git repository. */
    public suspend fun testRepository(id: String, branch: String? = null) {
        val query = buildList { branch?.let { add("branch" to it) } }
        rest.postVoid("customize/git-repositories/$id/test", query = query)
    }

    /** List branches available in a git repository. */
    public suspend fun listBranches(id: String): GitOpsBranchesResponse =
        rest.get("customize/git-repositories/$id/branches")

    /** Browse files and directories in a git repository at the given branch. */
    public suspend fun browseRepositoryFiles(
        id: String,
        branch: String,
        path: String? = null,
    ): GitOpsBrowseResponse {
        val query = buildList {
            add("branch" to branch)
            path?.let { add("path" to it) }
        }
        return rest.get("customize/git-repositories/$id/files", query = query)
    }

    /** Sync git repositories from a manager to an agent instance. */
    public suspend fun syncRepositories(body: GitRepositorySyncRequest) {
        rest.postVoid("git-repositories/sync", body = body)
    }

    // GitOps syncs (per environment)

    /** List GitOps syncs in the given environment with pagination. */
    public suspend fun listSyncsPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
        envId: EnvironmentId? = null,
    ): PaginatedResponse<GitOpsSync> {
        val query = buildList {
            search?.let { add("search" to it) }
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
        }
        return rest.transport.paginated<GitOpsSync>(rest.environmentPath(envId, "gitops-syncs"), start, limit, query)
    }

    /** Create a new GitOps sync configuration. */
    public suspend fun createSync(body: CreateGitOpsSync, envId: EnvironmentId? = null): GitOpsSync =
        rest.post(rest.environmentPath(envId, "gitops-syncs"), body = body)

    /** Get a GitOps sync by ID. */
    public suspend fun getSync(id: String, envId: EnvironmentId? = null): GitOpsSync =
        rest.get(rest.environmentPath(envId, "gitops-syncs/$id"))

    /** Update an existing GitOps sync. */
    public suspend fun updateSync(id: String, body: UpdateGitOpsSync, envId: EnvironmentId? = null): GitOpsSync =
        rest.put(rest.environmentPath(envId, "gitops-syncs/$id"), body = body)

    /** Delete a GitOps sync. */
    public suspend fun deleteSync(id: String, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "gitops-syncs/$id"))
    }

    /** Manually trigger a sync operation. */
    public suspend fun performSync(id: String, envId: EnvironmentId? = null): GitOpsSyncResult =
        rest.post(rest.environmentPath(envId, "gitops-syncs/$id/sync"))

    /** Get the current status of a GitOps sync. */
    public suspend fun getSyncStatus(id: String, envId: EnvironmentId? = null): GitOpsSyncStatus =
        rest.get(rest.environmentPath(envId, "gitops-syncs/$id/status"))

    /** Browse files in a synced repository. */
    public suspend fun browseSyncFiles(
        id: String,
        path: String? = null,
        envId: EnvironmentId? = null,
    ): GitOpsBrowseResponse {
        val query = buildList { path?.let { add("path" to it) } }
        return rest.get(rest.environmentPath(envId, "gitops-syncs/$id/files"), query = query)
    }

    /** Import multiple GitOps syncs from a JSON list. */
    public suspend fun importSyncs(
        syncs: List<ImportGitOpsSyncRequest>,
        envId: EnvironmentId? = null,
    ): ImportGitOpsSyncResponse =
        rest.post(rest.environmentPath(envId, "gitops-syncs/import"), body = syncs)
}
