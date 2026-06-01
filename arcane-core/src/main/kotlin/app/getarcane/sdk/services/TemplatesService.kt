package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.env.EnvVariable
import app.getarcane.sdk.models.env.EnvVariables
import app.getarcane.sdk.models.template.CreateTemplate
import app.getarcane.sdk.models.template.CreateTemplateRegistry
import app.getarcane.sdk.models.template.DefaultTemplates
import app.getarcane.sdk.models.template.RemoteTemplateRegistry
import app.getarcane.sdk.models.template.SaveDefaultTemplates
import app.getarcane.sdk.models.template.Template
import app.getarcane.sdk.models.template.TemplateContent
import app.getarcane.sdk.models.template.TemplateRegistry
import app.getarcane.sdk.models.template.UpdateTemplate
import app.getarcane.sdk.models.template.UpdateTemplateRegistry
import app.getarcane.sdk.pagination.PaginatedResponse

/**
 * Manages compose templates, registries, default templates, and per-environment template variables.
 * Port of Swift `TemplatesService`.
 */
public class TemplatesService internal constructor(private val rest: RestService) {
    // Templates

    /** List templates with pagination. Server-side filters: `type` (comma-separated, e.g. `"true,false"`). */
    public suspend fun listPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
        type: String? = null,
    ): PaginatedResponse<Template> {
        val query = buildList {
            search?.let { add("search" to it) }
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
            type?.let { add("type" to it) }
        }
        return rest.transport.paginated<Template>("templates", start, limit, query)
    }

    /** List all templates without pagination. */
    public suspend fun listAll(): List<Template> = rest.get("templates/all")

    /** Get a template by ID. */
    public suspend fun get(id: String): Template = rest.get("templates/$id")

    /** Get a template's content with parsed services and env variables. */
    public suspend fun getContent(id: String): TemplateContent = rest.get("templates/$id/content")

    /** Create a new local template. */
    public suspend fun create(body: CreateTemplate): Template = rest.post("templates", body = body)

    /** Update a local template. */
    public suspend fun update(id: String, body: UpdateTemplate): Template = rest.put("templates/$id", body = body)

    /** Delete a template. */
    public suspend fun delete(id: String) {
        rest.deleteVoid("templates/$id")
    }

    /** Download a remote template into local storage. */
    public suspend fun download(id: String): Template = rest.post("templates/$id/download")

    // Default templates

    /** Get the default compose and env templates. */
    public suspend fun getDefaults(): DefaultTemplates = rest.get("templates/default")

    /** Save the default compose and env templates. */
    public suspend fun saveDefaults(body: SaveDefaultTemplates) {
        rest.postVoid("templates/default", body = body)
    }

    // Registries

    /** Get all configured template registries. */
    public suspend fun listRegistries(): List<TemplateRegistry> = rest.get("templates/registries")

    /** Create a new template registry. */
    public suspend fun createRegistry(body: CreateTemplateRegistry): TemplateRegistry =
        rest.post("templates/registries", body = body)

    /** Update an existing template registry. */
    public suspend fun updateRegistry(id: String, body: UpdateTemplateRegistry) {
        rest.putVoid("templates/registries/$id", body = body)
    }

    /** Delete a template registry. */
    public suspend fun deleteRegistry(id: String) {
        rest.deleteVoid("templates/registries/$id")
    }

    /** Fetch the contents of a remote registry by URL. */
    public suspend fun fetchRegistry(url: String): RemoteTemplateRegistry =
        rest.get("templates/fetch", query = listOf("url" to url))

    // Per-environment global variables

    /** Get the global template variables for an environment. */
    public suspend fun getGlobalVariables(envId: EnvironmentId? = null): List<EnvVariable> =
        rest.get(rest.environmentPath(envId, "templates/variables"))

    /** Update the global template variables for an environment. */
    public suspend fun updateGlobalVariables(variables: List<EnvVariable>, envId: EnvironmentId? = null) {
        rest.putVoid(rest.environmentPath(envId, "templates/variables"), body = EnvVariables(variables = variables))
    }
}
