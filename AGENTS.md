# WhereIsEveryone — Agent Instructions

## Project overview

WhereIsEveryone is a native Android application for sharing and viewing friends'
locations on a map.

Primary technologies:

- Kotlin and Gradle Kotlin DSL
- Jetpack Compose and Material 3
- Coroutines and Flow
- Room for local persistence
- Koin for dependency injection
- Retrofit for HTTP communication
- Mapbox SDK for maps

## Architecture

Follow the project's existing architecture. Before creating components, inspect
similar implementations and follow their conventions.

- UI: Compose screens and ViewModels, with immutable StateFlow UI state.
- Domain: existing use cases for reusable or complex business logic where needed.
- Data: repositories and data sources; keep implementation details out of domain models.
- Use unidirectional data flow.
- Do not introduce new architectural layers or patterns unless necessary; explain the need.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) when changing boundaries between
layers or modules.

## Kotlin and coroutines

- Prefer idiomatic Kotlin, immutable data, and existing utilities over duplicates.
- Avoid unnecessary nullable values and abstractions.
- Use structured concurrency; never use GlobalScope.
- Do not block coroutine threads or perform blocking I/O on the main thread.
- Propagate coroutine cancellation correctly.
- Prefer suspend functions for one-shot async operations and Flow for observable data.
- Check imports in every modified Kotlin file and remove unused imports, including
  those made obsolete by refactoring. Keep implicitly used imports, such as
  delegated-property operators. Retain an otherwise unused import only for a
  compelling, documented reason.
- Place public, protected, and internal functions before private helpers in each
  file or class. Compose previews are the exception and belong at the bottom.
- Place newly added model declarations (data classes, enums, sealed types, and
  other model types) after production functions and before Compose previews, at
  the bottom of their file or containing class.

## Jetpack Compose

- Keep composables focused on rendering UI and as stateless as practical.
- Hoist state where appropriate.
- Use lifecycle-aware Flow collection; collect ViewModel state with
  `collectAsStateWithLifecycle()`.
- Do not perform expensive work or launch ongoing work directly from composable bodies.
- Preserve the existing Material theme, UI conventions, and screen/navigation patterns.
- Declare any `modifier: Modifier` parameter first, before required and optional
  parameters. Update positional calls when reordering parameters.
- Use `LocalWindowInfo.current.containerSize` for window dimensions instead of
  `Configuration.screenWidthDp` / `screenHeightDp`. Convert pixels with LocalDensity
  only where dp is needed. Keep LocalConfiguration for data such as locales.

### Previews

- Put all @Preview functions at the very bottom of the file, after production
  functions, helpers, and data declarations, regardless of visibility.
- Provide themed previews for visual screens and reusable UI components, using
  sample data and callbacks that do nothing.
- Keep previews independent of dependency injection, live services, network access,
  and device hardware. Use existing inspection-mode fallbacks for native maps.
- Exercise state/effect helpers and activity navigation wiring through their visual
  components rather than standalone previews.

## Dependency injection

- Use Koin and follow the existing module definitions.
- Prefer constructor injection.
- Avoid service-locator access in business logic.

  ## Database and networking

- Access Room through the existing data layer.
- Respect existing API contracts and error handling; preserve cancellation in suspend operations.
- Do not log credentials, tokens, request/response bodies containing personal data,
  or precise user locations.
- Inspect the related backend before changing Android API models, as described below.

### Schema migrations

- Preserve persisted data; do not use destructive migration fallbacks.
- For every Room schema change, increment the database version, export and commit
  the schema, implement and register an explicit migration from each supported
  version, and test migration paths with existing rows to verify preservation.
- Keep previously exported schemas unchanged. Version 4 is the first exported
  baseline; do not invent historical schemas or migration paths without verifying
  the released schema.
- Treat changes to persisted backend data and indexes as migrations too. Provide a
  versioned, repeatable migration or idempotent additive initialization, document
  rollout, and test preservation of existing data.

## Maps and location

- Keep Mapbox-specific code within the existing map integration boundaries.
- Respect lifecycle and resource cleanup.
- Preserve location permission and privacy behavior.
- Document the implications of changes to background location behavior.

## Testing and verification

Use the project's Gradle wrapper. After relevant changes, run:

```sh
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

- Fix failures caused by your changes.
- For Room schema changes, test migration paths with existing rows.
- For Flow-based logic, test emissions and cancellation where relevant.
- Inspect lint reports, including warnings in modified files. A successful lint
  task alone does not mean there are no findings.
- Prefer solutions recommended by lint and current platform/library APIs; fix
  relevant findings before finishing. Deviate or suppress only for a compelling,
  documented reason.
- Check unused imports separately; Android lint does not report all Kotlin unused imports.
  - Do not claim a build or test passed unless executed. Report commands that failed
    or could not be run.

## Change policy

Prefer focused changes over broad refactoring. Do not modify unrelated code.
Preserve public APIs unless the task requires changes.

Before modifying architecture or public APIs:

1. Inspect the existing implementation.
2. Explain the intended change.
3. Prefer the smallest change that solves the problem.

- Do not add dependencies unless they provide a clear benefit; prefer libraries
  already used by the project.
- Ask before destructive or irreversible operations unless already authorized.
- Never commit secrets or local configuration files.

### New files and staging

After creating a file intended to be committed, stage it with `git add -- <path>`.
Before finishing, check `git status --short` to confirm new files are staged.
Do not stage unrelated changes, force-add ignored files, or create commits unless
explicitly asked.

## Related repositories

The Go backend is located at:

`/home/kumpel/GolandProjects/whereiseveryone-srv`

For tasks involving API contracts, networking, users, friends, locations, status,
or authentication, inspect the backend implementation before changing Android API
models. Read the backend's own AGENTS.md before modifying backend code.

## Routine command approvals

The user preapproves routine local validation within the requested task:

- The project's Gradle wrapper.
- Go tests, builds, and vet for the backend.
- `golangci-lint run`, including `/tmp/whereiseveryone-golangci-lint`.
- Docker image builds/pulls, temporary container runs, inspection, logs, exec, and
  cleanup of containers owned by the task.

Continue without extra conversational confirmation for these operations.
Persistent command approvals are in `.codex/rules/validation.rules` and apply to
Codex sessions in this repository independently of the related repository's rules.
Keep the project trusted and restart Codex after editing rules.

- Run allowed commands directly, without shell wrappers, output redirection, or
  leading shell environment assignments, so prefix rules can match.
- Use the tool's output capture for logs.
- Run temporary containers with `docker run --rm` (keep --rm first), unique
  task-specific names, and unused loopback-bound ports.
- Stop only containers owned by the current task. Preserve existing containers,
  volumes, and data.

For local MongoDB integration tests, use the permitted form:

```sh
env 'MONGO_TEST_URI=mongodb://127.0.0.1:27028/?replicaSet=rs0&directConnection=true' go test -race ./...
```

Use the required outside-sandbox execution request when needed. These approvals
do not override sandbox restrictions or managed approval requirements.

## Completion
Summarize:
- What changed and why.
- What was verified.
- Remaining risks or limitations.

Include a concise proposed commit message in the final response only when you
actually modified files during the current task.
