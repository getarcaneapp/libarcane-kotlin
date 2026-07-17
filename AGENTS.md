# Arcane Kotlin SDK Agent Guidance

`libarcane-kotlin` is the hand-written Kotlin client for the Arcane API. It is the compatibility
boundary between Arcane servers and Kotlin/Android consumers.

## Collaboration Scope

Michael Kaltner is an authorized collaborator. Changes may cover API parity, models, services,
serialization, transport, authentication, streaming, tests, Gradle, AGP, Kotlin, JDK, wrapper, and
dependency maintenance.

Use a focused branch by default. Do not push, merge to `main`, tag, publish Maven artifacts, or
create a release unless explicitly requested.

Always inspect `git status` first and preserve unrelated modifications and untracked files.

## Modules and Boundaries

- `arcane-core/` — pure Kotlin/JVM client. It owns configuration, auth, transport, REST helpers,
  pagination, resource services, serializable models, WebSocket streams, and NDJSON streams.
- `arcane-android/` — thin Android integration for secure token storage and OIDC browser flows.

Keep `arcane-core` free of Android dependencies. Android-specific APIs belong in
`arcane-android`.

`ArcaneClient` owns the Ktor client, auth manager, transport, and per-resource services. Extend
these existing layers rather than adding a parallel client, transport, or service registry.

The project uses Kotlin explicit API mode. Public declarations must intentionally specify
visibility and public types.

## Arcane API Parity

The Arcane repository at `../arcane` is the wire-contract source of truth:

- shared response/request shapes generally live under `../arcane/types/`;
- Huma/Echo API handlers live under `../arcane/backend/api/handlers/`;
- WebSocket and streaming behavior may also live in handler or service packages.

For an SDK parity change:

1. Inspect the exact Arcane type, JSON names, optionality, endpoint path, query parameters, and
   streaming frame variants.
2. Update the corresponding SDK model or service in place.
3. Add MockEngine, serialization, or streaming tests covering the changed contract.
4. Preserve compatibility with older servers when practical.
5. Build Android against the sibling SDK when the change serves an Android feature.

Do not invent an endpoint contract from frontend usage alone. Do not silently rename wire fields,
make optional server fields mandatory, or remove public API without an explicit compatibility
decision.

## Implementation Conventions

- Services should use the existing `RestService`/`ArcaneTransport` stack.
- Use `suspend` functions for request/response operations and `Flow` for streams.
- Reuse the shared `ArcaneJson` configuration and existing serializers.
- Map transport and response failures into the existing `ArcaneError` hierarchy.
- Preserve the single-refresh behavior and concurrent refresh de-duplication in authentication.
- Close owned clients and coroutine scopes; cancellation must close active streams.
- Keep `EnvironmentId` scoping explicit for environment-specific endpoints.
- Model new enums defensively with an `UNKNOWN` fallback when the server can add values.
- Include all server-emitted streaming frame variants and optional payload fields.
- Prefer modifying an existing model, service, serializer, or helper over creating a duplicate.
- Avoid generated API clients unless the project deliberately changes its hand-written-client
  strategy.

## Gradle and Dependency Maintenance

- Keep Kotlin and serialization plugin versions compatible.
- Keep the Android Gradle Plugin aligned with the sibling Android application when composite
  builds are expected.
- Run Gradle with JDK 21 while preserving the configured Java/JVM 17 output target.
- Update the version catalog, build scripts, wrapper, README/toolchain documentation, and CI
  together when a toolchain change requires them.
- Prefer stable dependency upgrades and review public API or Android minimum-SDK effects.
- Never commit `local.properties`, Gradle caches, build output, credentials, keystores, or
  machine-specific paths.

## Verification

Run the repository CI baseline:

```sh
./gradlew :arcane-core:test :arcane-android:assembleRelease
```

For Android-facing changes, also run from `../android`:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Live integration tests require `ARCANE_TEST_URL` and must only target a server the user has placed
in scope. Unit tests should remain deterministic and use Ktor `MockEngine` unless live behavior is
specifically under test.

Report the exact checks run. Do not claim Android consumer compatibility when only `arcane-core`
tests passed.
