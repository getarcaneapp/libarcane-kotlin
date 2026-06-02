# Changelog

## 0.1.0 (Unreleased)

Initial release of the Arcane Kotlin SDK.

- **`arcane-core`** (pure Kotlin/JVM): `ArcaneClient` + `ArcaneConfiguration`, Ktor-based transport with a manual 401-refresh + idempotent-retry loop, coroutine-safe `AuthManager` (Mutex + de-duplicated refresh), sealed `ArcaneError` with Arcane/Huma mapping, `TokenStore` + `InMemoryTokenStore`, `ServerCapabilities` v1/v2 detection.
- **Models**: all 69 domain types across 33 packages, with a polymorphic `JsonValue`, custom serializers for `User` (v1/v2 role synthesis) and `DockerInfo` (catch-all), and `Permission` constants.
- **Services**: all 26 resource services (`containers`, `images`, `users`, `swarm`, `system`, …).
- **Streaming**: NDJSON and WebSocket streams as `Flow`s (`logs`, `stats`), bidirectional `TerminalSession`, multipart upload, and a `Flow`-based paginator.
- **`arcane-android`**: `AndroidSecureTokenStore` (AndroidKeyStore AES-GCM + DataStore), deprecated `EncryptedPrefsTokenStore` fallback, and `OidcAuthenticator` (Custom Tabs + device-code flow).
- **Tests**: 42 unit tests (error mapping, v1/v2 `User` decoding, role models, transport/auth incl. concurrent-refresh dedup, NDJSON streaming) via Ktor `MockEngine`, plus an `ARCANE_TEST_URL`-gated integration test.
