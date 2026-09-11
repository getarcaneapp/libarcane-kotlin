package app.getarcane.sdk.http

internal const val ACTIVITY_BATCH_ID_HEADER: String = "X-Arcane-Batch-Id"

private val activityBatchIdPattern = Regex("^[A-Za-z0-9_-]{1,64}$")

internal fun activityBatchHeaders(activityBatchId: String?): Map<String, String> {
    if (activityBatchId == null) return emptyMap()
    require(activityBatchIdPattern.matches(activityBatchId)) {
        "Activity batch ID must contain 1-64 ASCII letters, digits, underscores, or hyphens."
    }
    return mapOf(ACTIVITY_BATCH_ID_HEADER to activityBatchId)
}

internal fun validateRequestHeaders(requestHeaders: Map<String, String>) {
    require(requestHeaders.keys.all { key ->
        key.equals("X-Step-Up-Token", ignoreCase = true) ||
            key.equals(ACTIVITY_BATCH_ID_HEADER, ignoreCase = true)
    }) {
        "Only request-scoped step-up and activity-batch headers are supported."
    }
    require(requestHeaders.values.none { value -> value.any(Char::isISOControl) }) {
        "A request-scoped header is invalid."
    }
    requestHeaders.entries.firstOrNull {
        it.key.equals(ACTIVITY_BATCH_ID_HEADER, ignoreCase = true)
    }?.value?.let { batchId ->
        require(activityBatchIdPattern.matches(batchId)) {
            "Activity batch ID must contain 1-64 ASCII letters, digits, underscores, or hyphens."
        }
    }
}
