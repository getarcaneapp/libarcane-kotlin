package app.getarcane.sdk.models.project

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.models.base.PaginationResponse
import app.getarcane.sdk.models.containerregistry.ContainerRegistryCredential
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A project-related file (compose include, env file, or other on-disk file shown in the project
 * view). Mirrors Swift `IncludeFile` (Models/project/Project.swift).
 */
@Serializable
public data class IncludeFile(
    public val path: String,
    public val relativePath: String,
    public val content: String? = null,
)

/** Body for `POST /environments/{id}/projects`. Mirrors Swift `CreateProject`. */
@Serializable
public data class CreateProject(
    public val name: String,
    public val composeContent: String,
    public val envContent: String? = null,
)

/** Body for `PUT /environments/{id}/projects/{id}`. Mirrors Swift `UpdateProject`. */
@Serializable
public data class UpdateProject(
    public val name: String? = null,
    public val composeContent: String? = null,
    public val envContent: String? = null,
)

/** Configures the deploy/up call. Mirrors Swift `DeployOptions`. */
@Serializable
public data class DeployOptions(
    public val pullPolicy: String? = null,
    public val forceRecreate: Boolean? = null,
)

/** Body for the include file update endpoint. Mirrors Swift `UpdateIncludeFile`. */
@Serializable
public data class UpdateIncludeFile(
    public val relativePath: String,
    public val content: String,
)

/** Live container state for one compose service. Mirrors Swift `RuntimeService`. */
@Serializable
public data class RuntimeService(
    public val name: String,
    public val image: String,
    public val status: String,
    public val containerId: String? = null,
    public val containerName: String? = null,
    public val ports: List<String>? = null,
    public val health: String? = null,
    public val iconUrl: String? = null,
    public val serviceConfig: Map<String, JsonValue>? = null,
    public val redeployDisabled: Boolean? = null,
)

/** Summarises image update status for the whole project. Mirrors Swift `ProjectUpdateInfo`. */
@Serializable
public data class ProjectUpdateInfo(
    public val status: String,
    public val hasUpdate: Boolean,
    public val imageCount: Int,
    public val checkedImageCount: Int,
    public val imagesWithUpdates: Int,
    public val errorCount: Int,
    public val errorMessage: String? = null,
    public val imageRefs: List<String>? = null,
    public val updatedImageRefs: List<String>? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastCheckedAt: Instant? = null,
)

/** Response for `POST projects`. Mirrors Swift `ProjectCreateResponse`. */
@Serializable
public data class ProjectCreateResponse(
    public val id: String,
    public val name: String,
    public val dirName: String? = null,
    public val relativePath: String? = null,
    public val path: String,
    public val status: String,
    public val statusReason: String? = null,
    public val serviceCount: Int = 0,
    public val runningCount: Int = 0,
    public val gitOpsManagedBy: String? = null,
    public val isArchived: Boolean = false,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val archivedAt: Instant? = null,
    public val createdAt: String,
    public val updatedAt: String,
)

/** The full project view returned by the list/details endpoints. Mirrors Swift `ProjectDetails`. */
@Serializable
public data class ProjectDetails(
    public val id: String,
    public val name: String,
    public val dirName: String? = null,
    public val relativePath: String? = null,
    public val path: String,
    public val iconUrl: String? = null,
    public val urls: List<String>? = null,
    public val composeContent: String? = null,
    public val composeFileName: String? = null,
    public val envContent: String? = null,
    public val includeFiles: List<IncludeFile>? = null,
    public val directoryFiles: List<IncludeFile>? = null,
    public val status: String,
    public val statusReason: String? = null,
    public val serviceCount: Int = 0,
    public val runningCount: Int = 0,
    public val isArchived: Boolean = false,
    public val isDiscovered: Boolean? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val archivedAt: Instant? = null,
    public val createdAt: String,
    public val updatedAt: String,
    public val services: List<Map<String, JsonValue>>? = null,
    public val runtimeServices: List<RuntimeService>? = null,
    public val updateInfo: ProjectUpdateInfo? = null,
    public val hasBuildDirective: Boolean? = null,
    public val redeployDisabled: Boolean? = null,
    public val gitOpsManagedBy: String? = null,
    public val lastSyncCommit: String? = null,
    public val gitRepositoryURL: String? = null,
)

/** Body for the destroy endpoint. Mirrors Swift `DestroyProject`. */
@Serializable
public data class DestroyProject(
    public val removeFiles: Boolean? = null,
    public val removeVolumes: Boolean? = null,
)

/** Summarises project status counts for an environment. Mirrors Swift `ProjectStatusCounts`. */
@Serializable
public data class ProjectStatusCounts(
    public val runningProjects: Int = 0,
    public val stoppedProjects: Int = 0,
    public val totalProjects: Int = 0,
    public val archivedProjects: Int = 0,
)

/** Body for `POST projects/{id}/pull`. Mirrors Swift `ImagePullRequest`. */
@Serializable
public data class ImagePullRequest(
    public val credentials: List<ContainerRegistryCredential>? = null,
)

/** Body for `POST projects/{id}/build`. Mirrors Swift `BuildProjectRequest`. */
@Serializable
public data class BuildProjectRequest(
    public val services: List<String>? = null,
    public val provider: String? = null,
    public val push: Boolean? = null,
    public val load: Boolean? = null,
)

/** One frame of the streaming pull progress. Mirrors Swift `PullProgressEvent`. */
@Serializable
public data class PullProgressEvent(
    public val status: String? = null,
    public val id: String? = null,
    public val progress: String? = null,
    public val progressDetail: Detail? = null,
    public val error: String? = null,
    /**
     * Human-readable build/lifecycle output emitted by Docker Compose operations (deploy, build,
     * pull-images). Absent for pure image-pull layer frames.
     */
    public val stream: String? = null,
) {
    /** Mirrors Swift `PullProgressEvent.Detail`. */
    @Serializable
    public data class Detail(
        public val current: Long? = null,
        public val total: Long? = null,
    )
}

/** The page envelope for `GET /environments/{id}/projects`. Mirrors Swift `ProjectListPage`. */
@Serializable
public data class ProjectListPage(
    public val success: Boolean,
    public val data: List<ProjectDetails>,
    public val pagination: PaginationResponse,
)
