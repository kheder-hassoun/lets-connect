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

- `mqttControl` now starts/stops MQTT control-plane logic in `WalkieService`.
- MQTT now uses runtime config from `MqttConfigRepository` (`brokerHost`, `brokerPort`, `clusterId`).
- Topic flow implemented:
  - `cluster/{clusterId}/presence` (subscribe + heartbeat publish)
  - `cluster/{clusterId}/chat` (publish + subscribe)
- `cluster/{clusterId}/floor` is now implemented (publish acquire/release + subscribe updates).
- Chat send path is MQTT-first when `mqttControl=true`, with socket fallback when MQTT is unavailable.
- Floor acquire/release path is MQTT-first when `mqttControl=true`, with socket fallback when MQTT is unavailable.
