package app.getarcane.sdk.models.settings

import kotlinx.serialization.Serializable

/** A publicly accessible setting value. */
@Serializable
public data class PublicSetting(
    public val key: String,
    public val type: String,
    public val value: String,
) {
    /** Stable identifier; aliases [key]. */
    public val id: String get() = key
}

/** A setting value with visibility metadata. */
@Serializable
public data class SettingDto(
    public val key: String,
    public val type: String,
    public val value: String,
    public val isPublic: Boolean,
) {
    /** Stable identifier; aliases [key]. */
    public val id: String get() = key
}

/**
 * Request body for `PUT /environments/{id}/settings`. All fields are optional strings — only the
 * fields you set are updated.
 */
@Serializable
public data class UpdateSettings(
    public val projectsDirectory: String? = null,
    public val followProjectSymlinks: String? = null,
    public val swarmStackSourcesDirectory: String? = null,
    public val diskUsagePath: String? = null,
    public val autoUpdate: String? = null,
    public val autoUpdateInterval: String? = null,
    public val pollingEnabled: String? = null,
    public val pollingInterval: String? = null,
    public val dockerClientRefreshInterval: String? = null,
    public val autoInjectEnv: String? = null,
    public val environmentHealthInterval: String? = null,
    public val dockerPruneMode: String? = null,
    public val defaultDeployPullPolicy: String? = null,
    public val scheduledPruneEnabled: String? = null,
    public val scheduledPruneInterval: String? = null,
    public val scheduledPruneContainers: String? = null,
    public val scheduledPruneImages: String? = null,
    public val scheduledPruneVolumes: String? = null,
    public val scheduledPruneNetworks: String? = null,
    public val scheduledPruneBuildCache: String? = null,
    public val pruneContainerMode: String? = null,
    public val pruneContainerUntil: String? = null,
    public val pruneImageMode: String? = null,
    public val pruneImageUntil: String? = null,
    public val pruneVolumeMode: String? = null,
    public val pruneNetworkMode: String? = null,
    public val pruneNetworkUntil: String? = null,
    public val pruneBuildCacheMode: String? = null,
    public val pruneBuildCacheUntil: String? = null,
    public val vulnerabilityScanEnabled: String? = null,
    public val vulnerabilityScanInterval: String? = null,
    public val maxImageUploadSize: String? = null,
    public val gitSyncMaxFiles: String? = null,
    public val gitSyncMaxTotalSizeMb: String? = null,
    public val gitSyncMaxBinarySizeMb: String? = null,
    public val baseServerUrl: String? = null,
    public val enableGravatar: String? = null,
    public val defaultShell: String? = null,
    public val dockerHost: String? = null,
    public val accentColor: String? = null,
    public val applicationTheme: String? = null,
    public val authLocalEnabled: String? = null,
    public val oidcEnabled: String? = null,
    public val oidcMergeAccounts: String? = null,
    public val authSessionTimeout: String? = null,
    public val authPasswordPolicy: String? = null,
    public val trivyImage: String? = null,
    public val trivyNetwork: String? = null,
    public val trivySecurityOpts: String? = null,
    public val trivyPrivileged: String? = null,
    public val trivyPreserveCacheOnVolumePrune: String? = null,
    public val trivyResourceLimitsEnabled: String? = null,
    public val trivyCpuLimit: String? = null,
    public val trivyMemoryLimitMb: String? = null,
    public val trivyConcurrentScanContainers: String? = null,
    public val authOidcConfig: String? = null,
    public val oidcClientId: String? = null,
    public val oidcClientSecret: String? = null,
    public val oidcIssuerUrl: String? = null,
    public val oidcScopes: String? = null,
    public val oidcAdminClaim: String? = null,
    public val oidcAdminValue: String? = null,
    public val oidcSkipTlsVerify: String? = null,
    public val oidcAutoRedirectToProvider: String? = null,
    public val oidcProviderName: String? = null,
    public val oidcProviderLogoUrl: String? = null,
    public val mobileNavigationMode: String? = null,
    public val mobileNavigationShowLabels: String? = null,
    public val sidebarHoverExpansion: String? = null,
    public val keyboardShortcutsEnabled: String? = null,
    public val dockerApiTimeout: String? = null,
    public val dockerImagePullTimeout: String? = null,
    public val trivyScanTimeout: String? = null,
    public val gitOperationTimeout: String? = null,
    public val httpClientTimeout: String? = null,
    public val registryTimeout: String? = null,
    public val proxyRequestTimeout: String? = null,
    public val autoUpdateExcludedContainers: String? = null,
    public val autoHealEnabled: String? = null,
    public val autoHealInterval: String? = null,
    public val autoHealExcludedContainers: String? = null,
    public val autoHealMaxRestarts: String? = null,
    public val autoHealRestartWindow: String? = null,
    public val buildProvider: String? = null,
    public val buildsDirectory: String? = null,
    public val buildTimeout: String? = null,
    public val depotProjectId: String? = null,
    public val depotToken: String? = null,
    public val oledMode: String? = null,
)
