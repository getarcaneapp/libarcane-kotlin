package app.getarcane.sdk.models.template

import app.getarcane.sdk.models.env.EnvVariable
import app.getarcane.sdk.models.meta.TemplateMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A compose template. Mirrors Swift `Template` (Models/Template/Template.swift). */
@Serializable
public data class Template(
    public val id: String,
    public val name: String,
    public val description: String,
    public val content: String,
    public val envContent: String? = null,
    public val isCustom: Boolean,
    public val isRemote: Boolean,
    public val registryId: String? = null,
    public val registry: TemplateRegistry? = null,
    public val metadata: TemplateMetadata? = null,
)

/**
 * A template entry from a remote registry. Mirrors Swift `RemoteTemplate`, whose `CodingKeys` map
 * several fields to snake_case.
 */
@Serializable
public data class RemoteTemplate(
    public val id: String,
    public val name: String,
    public val description: String,
    public val version: String,
    public val author: String,
    @SerialName("compose_url")
    public val composeUrl: String,
    @SerialName("env_url")
    public val envUrl: String,
    @SerialName("documentation_url")
    public val documentationUrl: String,
    public val tags: List<String>,
)

/** A configured template registry. Mirrors Swift `TemplateRegistry`. */
@Serializable
public data class TemplateRegistry(
    public val id: String,
    public val name: String,
    public val description: String,
    public val url: String,
    public val enabled: Boolean,
    public val lastFetchError: String? = null,
)

/**
 * The parsed contents of a remote registry document. Mirrors Swift `RemoteTemplateRegistry`, whose
 * `CodingKeys` map `schema` to the JSON key `$schema`.
 */
@Serializable
public data class RemoteTemplateRegistry(
    @SerialName("\$schema")
    public val schema: String? = null,
    public val name: String,
    public val description: String,
    public val url: String,
    public val version: String,
    public val author: String,
    public val templates: List<RemoteTemplate>,
)

/** Resolved template content plus parsed services and env variables. Mirrors Swift `TemplateContent`. */
@Serializable
public data class TemplateContent(
    public val template: Template,
    public val content: String,
    public val envContent: String,
    public val services: List<String>,
    public val envVariables: List<EnvVariable>,
)

/** Mirrors Swift `CreateTemplate`. */
@Serializable
public data class CreateTemplate(
    public val name: String,
    public val description: String = "",
    public val content: String,
    public val envContent: String = "",
)

/** Mirrors Swift `UpdateTemplate`. */
@Serializable
public data class UpdateTemplate(
    public val name: String,
    public val description: String = "",
    public val content: String,
    public val envContent: String = "",
)

/** Mirrors Swift `DefaultTemplates`. */
@Serializable
public data class DefaultTemplates(
    public val composeTemplate: String,
    public val swarmStackTemplate: String,
    public val swarmStackEnvTemplate: String,
    public val envTemplate: String,
)

/** Mirrors Swift `SaveDefaultTemplates`. */
@Serializable
public data class SaveDefaultTemplates(
    public val composeContent: String,
    public val envContent: String = "",
)

/** Mirrors Swift `CreateTemplateRegistry`. */
@Serializable
public data class CreateTemplateRegistry(
    public val name: String,
    public val url: String,
    public val description: String = "",
    public val enabled: Boolean = true,
)

/** Mirrors Swift `UpdateTemplateRegistry`. */
@Serializable
public data class UpdateTemplateRegistry(
    public val name: String,
    public val url: String,
    public val description: String = "",
    public val enabled: Boolean = true,
)
