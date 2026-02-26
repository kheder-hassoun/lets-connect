# Feature Flags (Phase 0)

Runtime flags were added to support safe, incremental rollout.

## Current Flags

- `serverlessControl` (`ff_serverless_control`)
- `webrtcAudio` (`ff_webrtc_audio`)
- `centralSettings` (`ff_central_settings`)
- `floorV2` (`ff_floor_v2`)
- `observabilityV2` (`ff_observability_v2`)

Defaults:
- `serverlessControl`: `true`
- all other flags: `false`

## Source of Truth

- Repository: `FeatureFlagsRepository`
- Path: `core/src/main/java/pro/devapp/walkietalkiek/core/flags/FeatureFlagsRepository.kt`
- DI registration: `app/src/main/java/pro/devapp/walkietalkiek/di/appDi.kt`

## Runtime Toggle UI

- Settings screen now includes a `Feature Flags` card with switch controls.
- Path: `feature-settings/src/main/java/pro/devapp/walkietalkiek/feature/settings/SettingsContent.kt`

## Usage Pattern (for upcoming phases)

1. Inject `FeatureFlagsRepository`.
2. Read `flags` `StateFlow` and branch behavior by flag.
3. Keep legacy path available until pilot validation is complete.

## Current Status

- Runtime path is serverless LAN (`NSD + sockets`).
- No broker/server configuration is required for normal operation.
- PTT status panel shows control plane as `Serverless`.
- Membership heartbeat is active over serverless control envelope.
- PTT status panel shows cluster role, leader node id, and active member count.
