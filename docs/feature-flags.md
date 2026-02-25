# Feature Flags (Phase 0)

Runtime flags were added to support safe, incremental rollout.

## Current Flags

- `mqttControl` (`ff_mqtt_control` in roadmap terms)
- `webrtcAudio` (`ff_webrtc_audio`)
- `centralSettings` (`ff_central_settings`)
- `floorV2` (`ff_floor_v2`)
- `observabilityV2` (`ff_observability_v2`)

All default to `false`.

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

- `mqttControl` now starts/stops a minimal MQTT control-plane controller in `WalkieService`.
- It is a skeleton integration only (no topic publish/subscribe flow yet).
- Default broker URL is `tcp://127.0.0.1:1883` and is intentionally placeholder for now.
