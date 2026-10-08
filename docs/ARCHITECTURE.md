# Architecture and package layout

This app follows Google's UI/data separation and optional domain-layer guidance,
using the existing MVI ViewModels, StateFlow, Compose, coroutines, and Koin.
Google defines responsibilities and dependency boundaries rather than one required
package tree. The layout below makes those responsibilities visible in this project.

## Module packages

All prefixes start with `com.kumpello.whereiseveryone`.

| Module | Package layout |
| --- | --- |
| `:app` | `app` for startup, `app.logging` for release logging, and the existing Android activity packages. |
| `:core` | `core.presentation`, `core.ui.components`, `core.ui.theme`, `core.ui.util`, `core.util`, and `core.testing` test fixtures. |
| `:data` | `data.repository`, `data.repository.preferences`, `data.model`, `data.network.api`, `data.network.model`, `data.network.interceptor`, `data.local.database`, `data.local.preferences`, `data.device`, `data.session`, `data.logging`, and `data.di`. |
| `:feature:authentication` | `feature.authentication.ui.{login,signup,splash,components}`, `domain.{usecase,model}`, `navigation`, and `di`. |
| `:feature:main` | `feature.main.ui.{map,friends,settings,components,model,mapper,permissions}`, `domain.{usecase,manager}`, `location`, `sharing.nfc`, `navigation`, and `di`. |

Kotlin file paths mirror their package declarations. Tests mirror the package of
the class or responsibility they exercise. Source packages contain actual files;
empty package directories are removed.

## Audit and changes

The existing five-module dependency graph, immutable UI state, lifecycle-aware
collection, suspend operations, and repository-based networking already fit the
recommended approach. Koin stays as the project's dependency injection framework.

The old layout mixed responsibilities: APIs and repository implementations were
under `domain`, data models were filed under feature packages, and ViewModels lived
outside `ui`. The packages now follow their module and layer. Display models and
formatting mappers belong to UI rather than the domain layer.

Data ownership issues were fixed along with the packages:

- Friends polling and Room caching moved from a feature manager to
  `data.repository.FriendsStateRepository`. Its existing polling, cache, retry,
  cancellation, and lifecycle behavior are preserved; it calls the network
  repository directly instead of depending on a feature use case.
- Credential persistence moved into `data.repository.preferences`.
- The location service now uses `UserLocationRepository` and the existing immutable
  `LocationData` model, rather than accessing a Room DAO or entity directly.

Room databases, DAOs, entities, network API interfaces, concrete repository
implementations, and encrypted storage are internal to data. Features consume
repository APIs, data models, and the existing session operations. Constructor
injection remains the default; Android components use the established Koin bindings.

The optional feature use cases remain where they help reuse and testing. This audit
adds no module, dependency, architectural layer, or replacement framework.

## Compatibility

App IDs, API paths, JSON field names, stored credentials, database tables, columns,
version, and filename remain unchanged. Room's generated schema identity is checked
after the package moves. Navigation routes have explicit serial names matching
their previous identifiers so route identities remain stable.

Android activities and the location/NFC services keep their existing component
class names for launcher, ADB, notification, and NFC compatibility. Those few
packages are intentional exceptions to the library prefix convention; other code
uses the new module prefixes.

## References

- [Architecture overview](https://developer.android.com/topic/architecture)
- [Data layer and repository boundaries](https://developer.android.com/topic/architecture/data-layer)
- [Optional domain layer](https://developer.android.com/topic/architecture/domain-layer)
- [Module responsibilities and encapsulation](https://developer.android.com/topic/modularization/patterns)
