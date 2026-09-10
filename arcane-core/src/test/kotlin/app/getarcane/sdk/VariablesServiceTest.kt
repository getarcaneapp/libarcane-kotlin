package app.getarcane.sdk

import app.getarcane.sdk.models.role.Permission
import app.getarcane.sdk.models.variable.CreateGlobalVariableRequest
import app.getarcane.sdk.models.variable.EnvironmentSyncStatus
import app.getarcane.sdk.models.variable.GlobalVariable
import app.getarcane.sdk.models.variable.GlobalVariableMutationResponse
import app.getarcane.sdk.models.variable.UpdateGlobalVariableRequest
import app.getarcane.sdk.models.variable.VariableSyncState
import app.getarcane.sdk.serialization.ArcaneJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VariablesServiceTest {
    private val json = ArcaneJson.default

    @Test
    fun oldAndFuturePayloadsDecodeWithoutLosingSafeDefaults() {
        val variable = json.decodeFromString<GlobalVariable>(
            """{"id":"var-1","key":"TOKEN","isSecret":true,"createdAt":"2026-09-09T00:00:00Z"}""",
        )
        assertEquals("", variable.value)
        assertTrue(variable.isSecret)
        assertFalse(variable.allEnvironments)
        assertTrue(variable.environmentIds.isEmpty())
        assertNull(variable.updatedAt)
        assertFalse(variable.copy(value = "TOKEN_VALUE").toString().contains("TOKEN_VALUE"))

        val statuses = json.decodeFromString<List<EnvironmentSyncStatus>>(
            """[{"environmentId":"0","status":"pending"},{"environmentId":"edge","status":"queued"},{"environmentId":"missing"}]""",
        )
        assertEquals(VariableSyncState.PENDING, statuses[0].status)
        assertEquals(VariableSyncState.UNKNOWN, statuses[1].status)
        assertEquals(VariableSyncState.UNKNOWN, statuses[2].status)

        val mutation = json.decodeFromString<GlobalVariableMutationResponse>("{}")
        assertNull(mutation.variable)
        assertTrue(mutation.syncResults.isEmpty())
    }

    @Test
    fun updateSerializationDistinguishesPreservedSecretFromClearedScope() {
        val preserveSecret = json.parseToJsonElement(
            json.encodeToString(
                UpdateGlobalVariableRequest(
                    key = "TOKEN",
                    value = null,
                    isSecret = true,
                    allEnvironments = false,
                    environmentIds = emptyList(),
                ),
            ),
        ).jsonObject
        assertNull(preserveSecret["value"])
        assertEquals(0, preserveSecret["environmentIds"]?.jsonArray?.size)
        assertTrue(preserveSecret["isSecret"]!!.jsonPrimitive.content.toBoolean())

        val replacement = UpdateGlobalVariableRequest(value = "replacement-secret")
        val replacementJson = json.parseToJsonElement(json.encodeToString(replacement)).jsonObject
        assertEquals("replacement-secret", replacementJson["value"]?.jsonPrimitive?.content)
        assertFalse(replacement.toString().contains("replacement-secret"))
    }

    @Test
    fun serviceUsesManagerRoutesMethodsAndTypedEnvelopes() = runTest {
        val requests = mutableListOf<Pair<HttpRequestData, String>>()
        val responses = ArrayDeque(
            listOf(
                variableListEnvelope(),
                mutationEnvelope("created"),
                mutationEnvelope("updated"),
                """{"success":true,"data":{"syncResults":[{"environmentId":"0","status":"pending"}]}}""",
                """{"success":true,"data":[{"environmentId":"0","environmentName":"Local","status":"synced"}]}""",
                """{"success":true,"data":[{"environmentId":"edge","status":"future"}]}""",
            ),
        )
        val engine = MockEngine { request ->
            requests += request to renderBody(request)
            respond(
                responses.removeFirst(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            assertEquals("TOKEN", client.variables.list().single().key)
            assertEquals(
                "created",
                client.variables.create(
                    CreateGlobalVariableRequest(
                        key = "REGION",
                        value = "us-central",
                        allEnvironments = false,
                        environmentIds = listOf("0", "edge"),
                    ),
                ).variable?.id,
            )
            assertEquals(
                "updated",
                client.variables.update(
                    id = "var:1",
                    body = UpdateGlobalVariableRequest(value = null, environmentIds = emptyList()),
                ).variable?.id,
            )
            assertEquals(VariableSyncState.PENDING, client.variables.delete("var:1").syncResults.single().status)
            assertEquals(VariableSyncState.SYNCED, client.variables.sync().single().status)
            assertEquals(VariableSyncState.UNKNOWN, client.variables.syncStatus().single().status)
        }

        assertEquals(
            listOf(
                "/api/variables",
                "/api/variables",
                "/api/variables/var:1",
                "/api/variables/var:1",
                "/api/variables/sync",
                "/api/variables/sync-status",
            ),
            requests.map { it.first.url.encodedPath },
        )
        assertEquals(
            listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete, HttpMethod.Post, HttpMethod.Get),
            requests.map { it.first.method },
        )

        val createBody = json.parseToJsonElement(requests[1].second).jsonObject
        assertEquals("REGION", createBody["key"]?.jsonPrimitive?.content)
        assertEquals(listOf("0", "edge"), createBody["environmentIds"]?.jsonArray?.map { it.jsonPrimitive.content })
        val updateBody = json.parseToJsonElement(requests[2].second).jsonObject
        assertNull(updateBody["value"])
        assertEquals(0, updateBody["environmentIds"]?.jsonArray?.size)
    }

    @Test
    fun variablesPermissionConstantsMatchArcane() {
        assertEquals("variables:read", Permission.Variables.READ)
        assertEquals("variables:create", Permission.Variables.CREATE)
        assertEquals("variables:update", Permission.Variables.UPDATE)
        assertEquals("variables:delete", Permission.Variables.DELETE)
        assertEquals("variables:sync", Permission.Variables.SYNC)
    }

    private fun variableListEnvelope(): String =
        """{"success":true,"data":[{"id":"var-1","key":"TOKEN","value":"","isSecret":true,"allEnvironments":true,"environmentIds":[],"createdAt":"2026-09-09T00:00:00Z"}]}"""

    private fun mutationEnvelope(id: String): String =
        """{"success":true,"data":{"variable":{"id":"$id","key":"REGION","value":"us-central","isSecret":false,"allEnvironments":false,"environmentIds":["0","edge"],"createdAt":"2026-09-09T00:00:00Z"},"syncResults":[]}}"""

    private suspend fun renderBody(request: HttpRequestData): String =
        when (val content = request.body) {
            is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
            is OutgoingContent.ReadChannelContent -> content.readFrom().toByteArray().decodeToString()
            is OutgoingContent.WriteChannelContent -> coroutineScope {
                val channel = ByteChannel()
                val reader = async { channel.toByteArray() }
                content.writeTo(channel)
                channel.close()
                reader.await().decodeToString()
            }
            else -> ""
        }
}
