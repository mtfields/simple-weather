# Project Guidance
## Goal
Deliver a compact local weather app centered on persistent, battery-safe forecast notifications.
## Core loop
saved coarse location -> Weather.gov points/hourly fetch -> summarize to daily rows -> cache -> render Compose + notification -> manual and periodic refresh.
## Battery-safety rules
- no background location
- no exact alarms
- no foreground service for normal refresh
- one unique periodic WorkManager job with network+battery constraints
- render from cache when refresh fails
## Architecture expectations
Keep UI action/state simple; repository handles fetch/cache; worker triggers repository refresh; notification rendering reads cache only.
## Validation commands
Run: `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`.
## Non-goals
No maps/radar/widgets/multi-location/account/onboarding polish.
