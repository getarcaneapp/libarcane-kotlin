package app.getarcane.sdk.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistence for the [TokenPair]. The Android module provides a Keystore-backed implementation;
 * [InMemoryTokenStore] is the default for tests and ephemeral use.
 */
public interface TokenStore {
    public suspend fun loadTokens(): TokenPair?
    public suspend fun saveTokens(tokens: TokenPair)
    public suspend fun clearTokens()
}

/** In-memory [TokenStore]. Mutex-guarded. */
public class InMemoryTokenStore(initial: TokenPair? = null) : TokenStore {
    private val mutex = Mutex()
    private var tokens: TokenPair? = initial

    override suspend fun loadTokens(): TokenPair? = mutex.withLock { tokens }

    override suspend fun saveTokens(tokens: TokenPair) {
        mutex.withLock { this.tokens = tokens }
    }

    override suspend fun clearTokens() {
        mutex.withLock { tokens = null }
    }
}
