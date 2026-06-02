package app.getarcane.sdk

/**
 * Retry configuration for idempotent requests, with backoff durations expressed in milliseconds.
 * Backoff for attempt `n` is `min(baseBackoffMillis * 2^(n-1), maxBackoffMillis)`.
 */
public data class RetryPolicy(
    public val maxAttempts: Int = 3,
    public val baseBackoffMillis: Long = 150,
    public val maxBackoffMillis: Long = 2_000,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    }

    public companion object {
        public val DEFAULT: RetryPolicy = RetryPolicy()
    }
}
