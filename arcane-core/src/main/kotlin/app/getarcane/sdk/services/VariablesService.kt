package app.getarcane.sdk.services

import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.models.variable.CreateGlobalVariableRequest
import app.getarcane.sdk.models.variable.EnvironmentSyncStatus
import app.getarcane.sdk.models.variable.GlobalVariable
import app.getarcane.sdk.models.variable.GlobalVariableMutationResponse
import app.getarcane.sdk.models.variable.UpdateGlobalVariableRequest
import app.getarcane.sdk.errors.ArcaneError

/** Manages manager-level global variables and their per-environment materialization state. */
public class VariablesService internal constructor(private val rest: RestService) {
    /** List all global variables. Arcane redacts values for secret entries. */
    public suspend fun list(): List<GlobalVariable> = rest.get("variables")

    /** Create a global variable and return its initial per-environment sync outcomes. */
    public suspend fun create(body: CreateGlobalVariableRequest): GlobalVariableMutationResponse =
        rest.post("variables", body = body)

    /** Partially update a global variable and return its new sync outcomes. */
    public suspend fun update(
        id: String,
        body: UpdateGlobalVariableRequest,
    ): GlobalVariableMutationResponse = rest.put("variables/${validatedId(id)}", body = body)

    /** Delete a global variable and return the resulting per-environment sync outcomes. */
    public suspend fun delete(id: String): GlobalVariableMutationResponse = rest.delete("variables/${validatedId(id)}")

    /** Immediately materialize the effective variable set into every environment. */
    public suspend fun sync(): List<EnvironmentSyncStatus> = rest.post("variables/sync")

    /** Return the last materialization outcome for each environment. */
    public suspend fun syncStatus(): List<EnvironmentSyncStatus> = rest.get("variables/sync-status")

    private fun validatedId(id: String): String {
        if (
            id.isBlank() || id != id.trim() || id == "." || id == ".." ||
            id.any { it.isISOControl() || it == '/' || it == '\\' || it == '?' || it == '#' || it == '%' }
        ) {
            throw ArcaneError.Validation(mapOf("id" to listOf("Variable identifier is invalid.")))
        }
        return id
    }
}
