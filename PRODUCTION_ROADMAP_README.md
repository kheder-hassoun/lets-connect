# Fast Production Roadmap (Serverless LAN-First, Android 9-15)

This roadmap is now aligned to the product requirement: **serverless operation on local WiFi/hotspot with auto-discovery**.

## Product Direction (Locked)

- No required external broker/server for normal operation.
- Must work when one phone enables hotspot and other phones join it.
- Auto-discovery must bring peers online with minimal manual setup.
- Keep app runnable after every implementation slice.

## Core Strategy

- Reuse proven libraries where they fit serverless LAN constraints.
- Keep fallback paths while migrating behavior.
- Ship in small vertical slices with build+smoke gate after each slice.
- Prioritize reliability over adding infrastructure complexity.

## Compatibility Target

- Android API 28 to API 35 (Android 9 to Android 15).
- Validate every phase on API 28 and API 35 devices/emulators.

## Architecture Target (Serverless)

- Discovery plane: NSD (mDNS/DNS-SD) + robustness improvements.
- Control plane: peer-to-peer app protocol over socket transport (no broker).
- Data plane: voice and chat over LAN sockets (optimized framing and buffering).
- Floor control: distributed but deterministic arbitration (single active speaker).
- Settings sync: one elected coordinator node in-cluster, no external server.

## Feature Flags

- `ff_serverless_control` (new target path)
- `ff_floor_v2`
- `ff_central_settings`
- `ff_audio_pipeline_v2`
- `ff_observability_v2`

Notes:
- Existing MQTT flags/logic are now **experimental/deprecated** and not part of the production path.

## Progress Status (Live)

- [x] Phase 0 started
- [~] Phase 1 in progress
- [~] Phase 2 in progress
- [ ] Phase 3 not started
- [ ] Phase 4 not started
- [ ] Phase 5 not started
- [ ] Phase 6 not started

## Phase Plan (Always Runnable)

## Phase 0 - Baseline and Safety Net

### Work

- Feature flag plumbing.
  - Status: [x] Done
- SLO/KPI definitions.
  - Status: [x] Done (`docs/slo.md`)
- Smoke checklist.
  - Status: [x] Done (`docs/smoke-test-checklist.md`)
- Baseline report template.
  - Status: [x] Done (`docs/reports/phase0-baseline.md`)

### Exit Criteria

- `main` builds and runs.
- Baseline report filled with real measurements.

---

## Phase 1 - Serverless Discovery and Control Stabilization

### Goal

Get robust peer discovery/control without external broker.

### Work

- Harden NSD discovery lifecycle:
  - duplicate suppression
  - stale-peer cleanup
  - reconnect strategy
- Introduce explicit control message envelope for serverless transport:
  - message type, source id, sequence, timestamp
- Keep socket fallback path during migration.
- De-prioritize broker-dependent MQTT path from production flow.

### Current Status

- [x] Control-plane status visibility added in UI.
- [x] Floor/chat workstreams started under feature-flag migration.
- [~] Serverless envelope adopted for membership heartbeat and cluster status.
- [x] MQTT experimental path isolated from default production path.

### Exit Criteria

- 2-5 devices auto-discover reliably on hotspot/WiFi.
- No manual broker/server config required.
- PTT/chat/floor operate end-to-end serverless.

---

## Phase 2 - Deterministic Floor Arbitration (Serverless)

### Goal

Ensure only one active speaker at a time under contention.

### Work

- Deterministic floor arbitration policy:
  - lease owner
  - lease timeout
  - release/renew
- Conflict resolution when simultaneous press occurs.
- Recovery on disconnect while holding floor.

### Current Status

- [x] Leader-authoritative `FLOOR_REQUEST/GRANT/RELEASE` control messages added.
- [x] PTT now waits for grant before transmit in serverless mode.
- [~] Queueing implemented (busy response + FIFO grant), renew policy pending.
- [~] Request-timeout fallback implemented; disconnect recovery lease cleanup still needs hardening.

### Exit Criteria

- No reproducible dual-speaker conflict in contention tests.
- Floor recovers cleanly after owner disconnect.

---

## Phase 3 - Centralized Settings (In-Cluster Coordinator)

### Goal

Cluster-wide settings sync without external backend.

### Work

- Coordinator election policy within LAN cluster.
- Versioned settings snapshot distribution.
- Join-time sync for new peers.
- Safe fallback to last good local config.

### Exit Criteria

- New peer receives active settings automatically.
- Settings consistency verified across peers.

---

## Phase 4 - Audio Pipeline Quality and Scale (Serverless)

### Goal

Improve voice quality and scalability without central media server.

### Work

- Better framing and jitter handling.
- Packet ordering/drop handling.
- Bandwidth/CPU tuning for hotspot scenarios.
- Optional codec optimization path.

### Exit Criteria

- Better latency/continuity vs baseline.
- Stable group call behavior at target room size.

---

## Phase 5 - Reliability and Diagnostics

### Goal

Operational visibility and predictable recovery.

### Work

- Diagnostics panel:
  - discovery state
  - peer count
  - floor owner
  - transport health
- Incident-friendly logging and export bundle.
- Reconnect/backoff hardening.

### Exit Criteria

- Field failures are diagnosable from logs/status UI.
- Recovery after WiFi churn is stable.

---

## Phase 6 - Security and Rollout

### Goal

Secure serverless LAN operation and staged release.

### Work

- Device trust model and join policy.
- Replay/spoof mitigation for control messages.
- Pilot rollout and KPI gates.

### Exit Criteria

- Unauthorized peer cannot take floor/control.
- Pilot passes SLO thresholds.

## What Is Out of Scope (for this plan)

- Mandatory broker/server dependencies for basic operation.
- Cloud-first architecture for same-LAN use cases.

## Execution Rules Per PR

- One vertical slice per PR.
- Keep app runnable at all times.
- `./gradlew assembleDebug` must pass.
- Run 2-device hotspot smoke test for every control-plane/floor change.

## Immediate Next Step

1. Harden lease timeout/cleanup and recovery on leader disconnect.
2. Add explicit request queue (FIFO) for fair floor arbitration.
3. Add deterministic failover behavior when leader disappears.
