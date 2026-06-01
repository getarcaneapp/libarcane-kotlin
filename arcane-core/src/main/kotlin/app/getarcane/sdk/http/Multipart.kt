package app.getarcane.sdk.http

import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.errors.fromResponse
import app.getarcane.sdk.streaming.collectNdjson
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

private const val NDJSON_ACCEPT = "application/x-ndjson, application/x-json-stream, application/json"

/** A file part for a multipart/form-data upload. Mirrors Swift `MultipartFile`. */
public class MultipartFile(
    public val fieldName: String,
    public val filename: String,
    public val content: ByteArray,
    public val contentType: String = "application/octet-stream",
)

internal fun buildMultipart(fields: Map<String, String>, files: List<MultipartFile>): List<PartData> = formData {
    fields.forEach { (key, value) -> append(key, value) }
    files.forEach { file ->
        append(
            file.fieldName,
            file.content,
            Headers.build {
                append(HttpHeaders.ContentType, file.contentType)
                append(HttpHeaders.ContentDisposition, "filename=\"${file.filename}\"")
            },
        )
    }
}

/**
 * Uploads a multipart/form-data request and decodes the `APIResponse<T>` envelope. Port of Swift
 * `multipartUpload`. Refreshes once on a 401, mirroring the unary request path.
 */
public suspend fun <T> ArcaneTransport.multipartUpload(
    path: String,
    deserializer: KSerializer<T>,
    files: List<MultipartFile>,
    method: String = "POST",
    query: List<Pair<String, String>> = emptyList(),
    fields: Map<String, String> = emptyMap(),
): T {
    var didRefresh = false
    while (true) {
        val headers = authManager.authenticationHeaders()
        val response = httpClient.request(buildUrl(path, query)) {
            this.method = HttpMethod.parse(method)
            accept(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(MultiPartFormDataContent(buildMultipart(fields, files)))
        }
        val status = response.status.value
        val bytes: ByteArray = response.body()
        if (status == 401 && !didRefresh && authManager.hasRefreshCredential()) {
            authManager.refreshTokens()
            didRefresh = true
            continue
        }
        if (status !in 200..299) {
            throw ArcaneError.fromResponse(status, bytes.decodeToString(), response.headers, json)
        }
        return try {
            json.decodeFromString(ApiResponse.serializer(deserializer), bytes.decodeToString()).data
        } catch (e: SerializationException) {
            throw ArcaneError.Decoding(e.message ?: e.toString())
        } catch (e: IllegalArgumentException) {
            throw ArcaneError.Decoding(e.message ?: e.toString())
        }
    }
}

/**
 * Uploads a multipart/form-data request and streams an NDJSON response as a [Flow]. Port of Swift
 * `multipartUploadStream` (e.g. `POST /images/upload`).
 */
public fun <T> ArcaneTransport.multipartUploadNdjson(
    path: String,
    deserializer: KSerializer<T>,
    files: List<MultipartFile>,
    method: String = "POST",
    query: List<Pair<String, String>> = emptyList(),
    fields: Map<String, String> = emptyMap(),
): Flow<T> = flow {
    var didRefresh = false
    while (true) {
        val headers = authManager.authenticationHeaders()
        val statement = httpClient.prepareRequest(buildUrl(path, query)) {
            this.method = HttpMethod.parse(method)
            header(HttpHeaders.Accept, NDJSON_ACCEPT)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(MultiPartFormDataContent(buildMultipart(fields, files)))
        }
        val retry = statement.execute { response ->
            val status = response.status.value
            if (status == 401 && !didRefresh && authManager.hasRefreshCredential()) {
                return@execute true
            }
            if (!response.status.isSuccess()) {
                throw ArcaneError.fromResponse(status, response.bodyAsText(), response.headers, json)
            }
            collectNdjson(response.bodyAsChannel(), deserializer, json)
            false
        }
        if (retry) {
            authManager.refreshTokens()
            didRefresh = true
            continue
        }
        break
    }
}
