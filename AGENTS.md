# AGENTS Guidance

## Project goal
Build a compact, battery-safe Android weather app focused on a persistent notification powered by cached Weather.gov forecast data.

## Core product loop
Saved location (coarse foreground permission) -> Weather.gov `/points` metadata lookup (cached) -> hourly forecast fetch -> daily compact summarization -> DataStore cache -> Compose UI + persistent notification -> manual and unique periodic WorkManager refresh.

## Battery-safety rules
- No background location.
- No exact alarms.
- No foreground service for normal refresh.
- No network weather fetch from `Application.onCreate()`.
- Use one unique periodic worker with conservative interval, connected network, battery-not-low constraints.

## Architecture expectations
- Keep simple and direct.
- Separate concerns: fetch client, cache/store, summarizer, notification renderer, worker/scheduler, UI/viewmodel.
- UI reads state and sends actions.
- Prefer cached rendering and graceful stale behavior over aggressive live updates.

## Validation commands
- `./gradlew test`
- `./gradlew lint`
- `./gradlew assembleDebug`

## Known non-goals
No radar, maps, multiple locations, widgets, login/sync, advanced alerts, or visual polish work beyond compact usability.
