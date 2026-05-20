# AGENTS guidance

## Project goal
Build a compact, battery-safe Android weather app focused on a persistent forecast notification sourced from Weather.gov.

## Core product loop
Saved coarse location -> points lookup (cached) -> hourly forecast fetch -> compact daily summarization -> cache -> Compose UI + persistent notification -> manual/background refresh.

## Battery-safety rules
- No exact alarms.
- No background location requests.
- Use WorkManager unique periodic work with network + battery-not-low constraints.
- Conservative refresh intervals (default ~3h).
- No normal refresh work in foreground service.
- No weather network work from Application.onCreate.

## Architecture expectations
- Keep UI declarative with state/action flow.
- Keep weather IO, cache/state, summarization, notification rendering, and worker scheduling separated.
- Prefer direct, testable implementations over abstraction-heavy patterns.

## Validation commands
- ./gradlew test
- ./gradlew lint
- ./gradlew assembleDebug

## Known non-goals
- Radar/maps/widgets/multi-location/accounts/fancy onboarding.
