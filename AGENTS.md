# Project Guidance
- Goal: battery-safe local forecast app with persistent notification using cached data first.
- Core loop: saved coarse location -> Weather.gov points/hourly fetch -> summarize daily rows -> cache -> render Compose + notification -> refresh manually/periodically.
- Battery rules: no exact alarms, no foreground service for normal refresh, unique periodic WorkManager work only, conservative 2-3h refresh.
- Architecture: keep UI simple; repository handles fetch/cache; notification renderer reads cached data only; diagnostics must expose refresh/error/permissions/work state.
- Validation commands: `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`.
- Non-goals: radar, maps, multiple locations, widgets, account/login, heavy onboarding.
