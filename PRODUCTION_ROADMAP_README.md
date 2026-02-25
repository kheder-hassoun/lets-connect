# Fast Production Roadmap (Library-First, Android 9-15)

This plan is optimized for fast delivery by integrating mature libraries instead of building distributed networking primitives from scratch.

## Core Strategy

- Prefer proven libraries and protocols.
- Keep current app runnable after every phase.
- Use feature flags so unfinished work never blocks release.
- De-scope custom leader-election/relay internals unless a library cannot cover it.

## Compatibility Target

- Android API 28 to API 35 (Android 9 to Android 15).
- Every introduced library must be validated on API 28 and API 35 test devices/emulators.

## Selected Building Blocks

- Control plane + settings sync + presence:
  - MQTT protocol.
  - Client: Eclipse Paho Android (`org.eclipse.paho`).
  - Broker: local LAN broker (Mosquitto/EMQX recommended). Optional embedded broker only for demo.
- Group voice transport:
  - WebRTC stack through a production SDK (recommended: LiveKit Android SDK, self-hosted server on LAN/edge).
  - Reason: built-in Opus, jitter buffer, NACK, congestion control, scalable group routing via SFU.
- Serialization:
  - Protobuf (`protobuf-kotlin-lite`) for app-level control payloads.
- Local persistence:
  - Jetpack DataStore (settings/cache) + Room (optional message/event history).
- Observability:
  - Timber (existing) + structured log schema + optional Sentry for crash reporting.

## Architecture (Fast Version)

- Keep Android app modular structure as-is.
- Replace custom socket control signaling with MQTT topics.
- Move group audio to WebRTC SDK (no custom PCM fanout logic for large rooms).
- Keep PTT and floor control in app domain, but transport events over MQTT/WebRTC data channel.

## Feature Flags (Must Exist Before Major Changes)

- `ff_mqtt_control`
- `ff_webrtc_audio`
- `ff_central_settings`
- `ff_floor_v2`
- `ff_observability_v2`

Default all to `false` initially.

## Phased Implementation (Always Runnable)

## Phase 0 - Baseline and Safety Net (1 sprint)

### Work

- Add feature flag plumbing.
- Define acceptance KPIs:
  - PTT start latency
  - floor conflict count
  - reconnect time
  - crash-free sessions
- Add smoke test checklist and script.

### Exit Criteria

- `main` builds and runs exactly as today.
- Baseline KPI report created.

### Run Gate

- `./gradlew assembleDebug`
- Manual 2-device smoke: discover, connect, PTT, chat, reconnect.

---

## Phase 1 - MQTT Control Plane (No Audio Change Yet) (1-2 sprints)

### Work

- Integrate Paho client.
- Introduce MQTT topic model:
  - `cluster/{id}/presence`
  - `cluster/{id}/floor`
  - `cluster/{id}/settings`
  - `cluster/{id}/chat`
- Use retained messages for latest settings/floor snapshot.
- Keep existing socket stack as fallback behind flag.

### Exit Criteria

- With `ff_mqtt_control=true`, presence/chat/floor events flow over MQTT.
- With `false`, old flow still works.

### Run Gate

- Build passes.
- 3-device LAN test with broker restart recovery.

---

## Phase 2 - Centralized Settings via MQTT (1 sprint)

### Work

- Add settings schema + version field.
- One node acts as coordinator (pragmatic approach):
  - either configured static coordinator,
  - or broker-host node as coordinator.
- Settings published as retained config snapshot.
- Clients ACK applied version.

### Exit Criteria

- New node joins and gets current settings automatically.
- Settings changes are consistent across nodes.

### Run Gate

- Toggle talk duration/tone/theme from coordinator and verify propagation.

---

## Phase 3 - Floor Control v2 (Library Transport, Simple Policy) (1 sprint)

### Work

- Implement single authoritative floor state in coordinator service.
- Keep policy simple for speed:
  - FIFO queue
  - lease timeout
  - explicit release
- Publish floor state updates on MQTT.

### Exit Criteria

- No dual-speaker conflict in contention tests.
- Queue order deterministic.

### Run Gate

- 5-device “simultaneous PTT press” test, repeated runs stable.

---

## Phase 4 - Group Audio via WebRTC SDK (2-3 sprints)

### Work

- Integrate WebRTC SDK module (recommended LiveKit Android SDK).
- Move audio path from custom socket PCM to SDK media tracks.
- PTT behavior becomes mute/unmute + floor grant enforcement.
- Keep old voice path behind `ff_webrtc_audio=false` fallback.

### Exit Criteria

- Audio works with 10+ participants in same LAN.
- Latency and quality better than baseline.

### Run Gate

- 5/10/20 participant soak tests.
- Verify API 28 and API 35 devices.

---

## Phase 5 - Reliability and Ops Hardening (1-2 sprints)

### Work

- Add structured diagnostics screen:
  - broker connected/disconnected
  - participant count
  - RTT/jitter/loss (from SDK stats)
  - current floor owner + queue length
- Add reconnect/backoff policies.
- Add watchdog alerts for stale floor lock.

### Exit Criteria

- Common incidents diagnosable from app logs/screen.
- Reconnect behavior stable under WiFi toggles.

### Run Gate

- Network chaos smoke (AP switch, broker restart, packet loss profile).

---

## Phase 6 - Security + Production Rollout (1-2 sprints)

### Work

- MQTT auth (username/password or certs).
- Topic ACLs for floor/settings topics.
- Join policy (venue token / allowlist).
- Progressive rollout plan: pilot venue -> staged expansion.

### Exit Criteria

- Unauthorized client cannot control floor/settings.
- Pilot passes KPI thresholds for agreed burn-in period.

### Run Gate

- Security regression checklist + pilot runbook signoff.

## What We Are Explicitly Not Building (for speed)

- Custom consensus algorithm implementation.
- Custom relay routing protocol for audio.
- Custom jitter buffer/PLC stack.
- Custom binary protocol unless required by a library boundary.

These are replaced by MQTT + WebRTC/SFU capabilities.

## Execution Rules Per PR

- Keep PR scope to one vertical slice.
- Add/keep fallback path behind feature flag.
- `./gradlew assembleDebug` must pass.
- Update smoke checklist evidence in PR description.
- No deletion of legacy path until replacement is proven in pilot.

## Immediate Next Step

Start Phase 0 now:

1. Add feature flags and runtime toggle source.
2. Write baseline KPI checklist under `/docs`.
3. Prepare local MQTT broker setup doc for dev/test.
