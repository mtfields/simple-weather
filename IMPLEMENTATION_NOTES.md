# IMPLEMENTATION NOTES

## Adopted ideas by PR
- **PR 1**: Expanded structured Weather.gov parsing tests with richer sample payload coverage (without porting implementation directly).
- **PR 2**: Did **not** adopt one-file architecture; retained PR 3 layered structure.
- **PR 3**: Preserved repository/store/client/viewmodel/worker/notification module boundaries.
- **PR 4**: Considered hygiene-oriented text-only improvements, but intentionally did not adopt regex parsing or manifest regressions.

## Intentionally not adopted
- PR 2 single-file architecture.
- PR 4 regex JSON parsing.
- PR 4 missing/incorrect INTERNET permission approach.
- PR 4 UI-thread manual refresh pattern.
- PR 1 permission-request-and-immediate-location-fetch sequencing.

## Completed functionality
- Added persisted refresh interval in app state (`refreshIntervalHours`) with 2h/3h UI preset and default 3h.
- Background scheduling now explicitly schedules/cancels one unique periodic worker based on startup/toggle actions.
- Disabling background refresh now cancels unique periodic work.
- Manual refresh remains one-time work and now uses network+battery constraints.
- Notification toggle now cancels active ongoing forecast notification when disabled.
- Notification rendering remains cache-only.
- Endpoint cache now stores real points metadata (`pointsUrl`, `hourlyUrl`, `gridId`, `gridX`, `gridY`, `fetchedAt`) and refreshes points metadata only when missing/stale.
- Diagnostics expanded to include location, last success/failure/error, permission states, endpoint staleness, background toggle status, and worker unique-name state.
- Weather.gov parsing test strengthened with points+hourly realistic payload validation.

## Known limitations
- WorkManager diagnostics are a synchronous snapshot in diagnostics text; not a fully reactive WorkInfo observer.
- Runtime behavior for location/notification permissions and WorkManager scheduling still needs on-device/emulator validation.

## Blockers
- Could not inspect GitHub PR branches directly from this environment because remote GitHub fetch failed with HTTP CONNECT tunnel 403.
- Gradle wrapper execution blocked due to missing wrapper JAR in repository (`gradle/wrapper/gradle-wrapper.jar`).

## Commands run
- `git checkout -b codex/integrate-pr3-enhancements`
- `git remote add origin https://github.com/mtfields/simple-weather.git && git fetch origin --prune`
- `./gradlew test`
- `./gradlew lint`
- `./gradlew assembleDebug`

## Build/test/lint results
- All gradle commands failed before execution due to missing `org.gradle.wrapper.GradleWrapperMain` class (wrapper JAR missing).

## Follow-up tasks
- Add reactive WorkInfo observation for diagnostics.
- Add Android instrumentation coverage for background toggle cancellation behavior.
- Re-run test/lint/assemble once wrapper JAR/toolchain is restored.
