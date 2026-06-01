# Arcane Kotlin

Hand-written Kotlin SDK for the [Arcane](https://github.com/getarcaneapp/arcane) API, for Android (and any JVM) apps that talk to an Arcane manager or agent. It mirrors [`libarcane-swift`](../libarcane-swift) feature-for-feature.

## Overview

`libarcane-kotlin` is a single-layer, idiomatic Kotlin client built on [Ktor](https://ktor.io) and [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization). There is no code generation: every DTO and every endpoint method is hand-crafted to mirror the Arcane Go types and HTTP surface (and the Swift SDK that mirrors them).

Two Gradle modules:

- **`arcane-core`** — pure Kotlin/JVM. Auth, token storage interface, environment scoping, REST helpers, WebSocket + NDJSON streams (as `Flow`s), and per-resource services. Runs on any JVM and is unit-tested with Ktor's `MockEngine` (no device/emulator).
- **`arcane-android`** — thin Android layer: a Keystore-backed secure `TokenStore` and the OIDC browser flow (Custom Tabs). Apps using only API-key or username/password auth, or providing their own token storage, can depend on `arcane-core` alone.

Concurrency is coroutines-first: blocking calls are `suspend` functions and streams are `Flow`s — the direct analog of Swift's `async`/`await` and `AsyncSequence`.

## Modules

```kotlin
// settings.gradle.kts of a consuming project (once published):
dependencies {
    implementation("app.getarcane:arcane-core:<version>")   // JVM/Android core
    implementation("app.getarcane:arcane-android:<version>") // Android secure storage + OIDC (optional)
}
```

`arcane-core`: Kotlin/JVM (JVM 17 bytecode). `arcane-android`: `com.android.library`, `minSdk 24`.

## Quickstart

```kotlin
import app.getarcane.sdk.ArcaneClient
import app.getarcane.sdk.ArcaneConfiguration
import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.errors.ArcaneError

val client = ArcaneClient(
    ArcaneConfiguration(
        baseUrl = "https://arcane.example.com",
        // On Android, use AndroidSecureTokenStore(context) from arcane-android.
        defaultEnvironmentId = EnvironmentId("0"),
    ),
)

// The client owns an HttpClient + coroutine scope — close it when done (or use `client.use { }`).
try {
    client.auth.login(username = "admin", password = "password")

    val containers = client.containers.list(envId = EnvironmentId("0"))
    val first = containers.data.first()
    client.containers.start(envId = EnvironmentId("0"), id = first.id)

    // Stream logs as a Flow.
    client.containers.logs(envId = EnvironmentId("0"), id = first.id, follow = true)
        .collect { line -> println(line.text) }
} catch (e: ArcaneError.Unauthorized) {
    // ...
} catch (e: ArcaneError.Validation) {
    e.fields.forEach { (field, messages) -> println("$field: $messages") }
} finally {
    client.close()
}
```

### Authentication

Three paths, matching the Swift SDK:

- **API key** — set `apiKey` on `ArcaneConfiguration`; sent as `X-API-Key` (takes precedence over a bearer token).
- **Username / password** — `client.auth.login(username, password)`. Tokens are cached and persisted via the configured `TokenStore`; a 401 triggers a single `auth/refresh` (concurrent calls are de-duplicated) and one retry.
- **OIDC** — on Android, `OidcAuthenticator(client)` drives the Custom Tabs flow (`startSignIn` → app redirect → `completeSignIn`), or the device-code flow (`beginDeviceFlow` / `pollDeviceToken`).

### Secure token storage (Android)

```kotlin
import app.getarcane.sdk.android.AndroidSecureTokenStore

val client = ArcaneClient(
    ArcaneConfiguration(
        baseUrl = "https://arcane.example.com",
        tokenStore = AndroidSecureTokenStore(context), // AES-256-GCM via AndroidKeyStore + DataStore
    ),
)
```

`InMemoryTokenStore` (in `arcane-core`) is the default and is used in tests.

### Streaming

- `client.containers.logs(...)` / `swarm.serviceLogs(...)` / `projects.logs(...)` → `Flow<LogLine>` (WebSocket)
- `client.containers.stats(...)` / `system.statsStream(...)` → `Flow<...>` (WebSocket)
- `client.containers.exec(...)` → `TerminalSession` (bidirectional: `send(text)` + `output: Flow<ByteArray>`)
- `client.images.pullStream(...)` / `projects.deployStream(...)` → `Flow<...>` (NDJSON progress)

Collecting a stream opens the connection; cancelling the collector closes it.

### Errors

All failures surface as the sealed `ArcaneError`: `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `Validation(fields)`, `RateLimited(retryAfter)`, `Server(code, message)`, `Transport`, `Decoding`, `Unknown`.

### Server capabilities (v1 / v2)

After the first authenticated `User` is decoded, `client.serverCapabilities()` reports whether the server speaks v1 legacy roles or v2 RBAC, so apps can gate role-management UI.

## Services

Each resource is exposed as a service on `ArcaneClient`:

| Service | Endpoints |
| --- | --- |
| `client.auth` | login, logout, refresh, me, password change, OIDC flow |
| `client.users` | user CRUD, role assignments |
| `client.apiKeys` | API key CRUD |
| `client.roles` / `client.oidcRoleMappings` | v2 RBAC roles + OIDC mappings |
| `client.environments` | environment CRUD, agent pairing, mTLS bundle |
| `client.containers` | list, inspect, lifecycle, logs, stats, exec |
| `client.images` | list, inspect, pull, build, prune, upload |
| `client.volumes` | volumes, browse, backups |
| `client.networks` | list, inspect, create, prune, topology |
| `client.projects` | compose projects: up/down/restart/redeploy/build/pull/destroy/archive |
| `client.swarm` | swarm: nodes, services, stacks, configs, secrets, tasks |
| `client.system` | docker info, prune, convert, upgrade, bulk actions |
| `client.dashboard` | env overview, action items |
| `client.events` | audit events |
| `client.webhooks` / `client.notifications` | webhook + notification config |
| `client.templates` / `client.registries` | templates + container registries |
| `client.gitops` / `client.builds` / `client.jobs` | GitOps, build workspaces, scheduled jobs |
| `client.settings` / `client.updater` / `client.vulnerabilities` / `client.ports` / `client.version` | misc |

## Building

```sh
./gradlew :arcane-core:test          # JVM unit tests (no device needed)
./gradlew :arcane-android:assembleRelease
ARCANE_TEST_URL=https://your-arcane ./gradlew :arcane-core:test   # also runs the live /health integration test
```

The type and module layout mirrors the Go packages under `arcane/types/` and the Swift `Sources/Arcane/**`, so engineers moving between the repos pay no translation tax.
