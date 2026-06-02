package app.getarcane.sdk.models.environment

import kotlinx.serialization.Serializable

/** DeploymentSnippetFile is one generated PEM file used by edge mTLS deployments. */
@Serializable
public data class DeploymentSnippetFile(
    public val name: String,
    public val content: String? = null,
    public val downloadUrl: String? = null,
    public val sensitive: Boolean? = null,
    public val containerPath: String,
    public val permissions: String,
)

/** DeploymentSnippetMTLS bundles Arcane-generated mTLS deployment assets. */
@Serializable
public data class DeploymentSnippetMTLS(
    public val dockerRun: String,
    public val dockerCompose: String,
    public val files: List<DeploymentSnippetFile> = emptyList(),
    public val hostDirHint: String,
)

/** DeploymentSnippet is the response payload for `GET environments/{id}/deployment`. */
@Serializable
public data class DeploymentSnippet(
    public val dockerRun: String,
    public val dockerCompose: String,
    public val mtls: DeploymentSnippetMTLS? = null,
)
