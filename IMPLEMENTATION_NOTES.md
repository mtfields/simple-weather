# IMPLEMENTATION NOTES

## Major decisions made
- Implemented single-app-state persistence in DataStore (JSON serialized) to keep cache/diagnostics simple.
- Weather.gov integration uses points->hourly flow and caches hourly endpoint for ~24h before re-resolving points.
- Background refresh uses one unique periodic WorkManager job (`weather_periodic_refresh`) with conservative constraints.
- Notification content is rendered only from cached state; no network/location work in notification rendering path.

## Completed functionality
- Compose main screen with location, forecast rows, last success/failure, error text, toggles, manual refresh button.
- Coarse location permission and saved lat/lon via fused last location.
- Weather.gov hourly fetch and compact daily summarization.
- Cached forecast rendering for UI + notification.
- Persistent low-importance forecast notification with refresh action.
- Manual refresh as one-time worker.
- Unique periodic refresh scheduling.
- Diagnostics strings for endpoint/cache/error/timestamp state.
- Unit tests for summarizer and hourly parsing.

## Known limitations
- Device-only behaviors (notification runtime permission prompt handling on API 33+, fused location edge cases) require emulator/device verification.
- WorkManager diagnostics are surfaced indirectly in UI state rather than querying full WorkInfo list.

## Blockers
- None yet.

## Commands run
- gradle wrapper

## Test/build/lint results
- Pending final `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug` run.

## Follow-up tasks worth doing next
- Add richer diagnostics from WorkManager WorkInfo.
- Add stale-cache threshold indicator and explicit stale badge.
- Improve settings section with explicit interval selector persisted in DataStore.
