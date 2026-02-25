# Production Roadmap (LANwalkieTalkie / Let's-Connect)

This document defines a phased implementation plan to evolve the app from current LAN P2P behavior into a production-grade system suitable for high-density deployments (restaurants, malls, campuses, events).

## Goals

- Support reliable group push-to-talk in medium and large venues.
- Prevent floor conflicts and split-brain behavior during node failures.
- Centralize runtime settings via leader/master authority.
- Improve audio quality and latency under network stress.
- Add observability, security hardening, and predictable operations.

## Non-Goals (for now)

- Cloud backend dependency for core LAN operation.
- Internet-wide communication between different physical sites.
- Video/media streaming beyond push-to-talk voice and control messages.

## Current Baseline (Summary)

- Peer-to-peer LAN with each node acting as server + client.
- Simple floor packets (`TAKEN`/`RELEASED`) over text payload.
- Audio payload prefixed by one byte and flooded to connected peers.
- In-memory settings and repositories, no durable control state.

This baseline works for small groups but is not enough for high-density production environments.

## Target Architecture

- Control Plane: leader-based coordination (membership, floor token, settings).
- Data Plane: star/relay forwarding to avoid full-mesh fanout.
- Roles: `leader`, `relay`, `participant` (role can change at runtime).
- Protocol: explicit binary envelope with message type, version, sequence, source, and CRC.
- Settings: versioned, leader-owned configuration with ACK and rollback safety.

## Phase Plan

## Phase 0 - Discovery, SLOs, and Test Harness

### Purpose
Define measurable quality targets and create repeatable tests before protocol and topology changes.

### Work Items

- Define SLOs:
  - PTT start latency target (button press to remote playback start).
  - Floor grant latency target.
  - Packet loss tolerance.
  - Re-election convergence time target.
- Add synthetic load test harness for:
  - 10, 25, 50, 100 logical participants.
  - burst talkers and churn (join/leave/reconnect).
- Add deterministic network simulation profiles:
  - latency/jitter/loss/reordering.
- Create baseline report from current implementation.

### Deliverables

- `/docs/slo.md`
- `/docs/test-harness.md`
- baseline KPI report in `/docs/reports/phase0-baseline.md`

### Exit Criteria

- SLOs agreed and documented.
- Baseline numbers captured and reproducible.

---

## Phase 1 - Protocol v2 Foundation

### Purpose
Replace ad-hoc message parsing with explicit framed protocol that can safely evolve.

### Work Items

- Introduce envelope schema:
  - `version`, `msgType`, `sessionId`, `sourceNodeId`, `seq`, `timestamp`, `flags`, `payloadLen`, `crc`.
- Define message types:
  - `HEARTBEAT`, `HELLO`, `ROLE_ANNOUNCE`, `FLOOR_REQUEST`, `FLOOR_GRANT`, `FLOOR_RELEASE`, `SETTINGS_SNAPSHOT`, `SETTINGS_ACK`, `VOICE_FRAME`, `TEXT_MESSAGE`, `NACK`.
- Create codec layer in `serivce-network`:
  - serializer/deserializer.
  - strict validation and compatibility handling.
- Add dual-stack compatibility:
  - protocol v1 and v2 during migration window.

### Deliverables

- protocol spec document `/docs/protocol-v2.md`
- codec implementation + unit tests
- compatibility matrix in `/docs/protocol-compatibility.md`

### Exit Criteria

- All control and voice messages can traverse protocol v2 in local tests.
- Invalid packets are rejected safely without crash loops.

---

## Phase 2 - Membership and Leader Election

### Purpose
Establish deterministic cluster leadership and avoid split-brain floor control.

### Work Items

- Implement membership table with heartbeat TTL and monotonic term.
- Implement election policy:
  - deterministic priority function (node class/capabilities + stable nodeId).
  - tie-break by nodeId lexicographic order.
- Implement transitions:
  - follower -> candidate -> leader.
  - leader resignation and failover.
- Introduce split-brain guard:
  - higher-term leader always wins.
  - stale leader self-demotes.
- Add election metrics:
  - election attempts, failures, convergence time.

### Deliverables

- `/docs/election-design.md`
- membership and election module + tests (unit + fault injection)
- election timeline logs in debug diagnostics

### Exit Criteria

- Single leader under stable network.
- Re-election converges within target after leader death.
- No persistent dual-leader state in fault tests.

---

## Phase 3 - Centralized Floor Control (Leader-Owned)

### Purpose
Move floor authority to leader to guarantee consistent arbitration.

### Work Items

- Implement floor request/grant/release as leader-authoritative workflow.
- Add queue policy options:
  - FIFO default, optional priority lanes.
- Enforce lease-based grant:
  - grant has expiry timestamp and lease id.
- Add renew and preemption behavior:
  - auto-expire stale speaker.
  - optional emergency preemption role.
- Update UI state model:
  - pending request, granted lease, denied reason, queue position.

### Deliverables

- `/docs/floor-control-v2.md`
- floor state machine tests
- end-to-end contention tests (N talkers racing)

### Exit Criteria

- At most one active floor lease at any time in cluster tests.
- Queue behavior deterministic and observable.

---

## Phase 4 - Relay Topology and Scalable Audio Distribution

### Purpose
Eliminate full-mesh traffic growth and improve performance for large groups.

### Work Items

- Introduce role-specific routing:
  - participant sends upstream to leader or nearest relay.
  - relays fan out downstream to assigned members.
- Add relay selection strategy:
  - RSSI/latency/packet-loss aware selection.
  - fallback to leader direct path.
- Add topology manager:
  - dynamic rebalancing when relay overloaded or disappears.
- Introduce duplicate suppression and seq-based ordering on receive.

### Deliverables

- `/docs/relay-topology.md`
- relay routing module + integration tests
- capacity report with participant scaling curves

### Exit Criteria

- Demonstrated traffic reduction versus full mesh.
- Stable audio delivery across target group size in stress tests.

---

## Phase 5 - Audio Pipeline Hardening

### Purpose
Upgrade voice transport quality under real-world WiFi conditions.

### Work Items

- Add codec strategy abstraction (prepare Opus integration path).
- Introduce jitter buffer with adaptive playout delay.
- Add packet loss concealment strategy.
- Add VAD/DTX options for bandwidth savings.
- Tune frame size, buffering, and backpressure.

### Deliverables

- `/docs/audio-pipeline.md`
- objective audio/latency benchmark report
- runtime audio tuning flags

### Exit Criteria

- Meets defined latency + intelligibility targets in lossy profiles.
- No playback underrun storms under target load.

---

## Phase 6 - Settings Centralization and Config Governance

### Purpose
Make leader the source of truth for runtime configuration.

### Work Items

- Define settings schema and versioning:
  - cluster-wide settings, role policy, floor policy, audio policy.
- Leader publishes `SETTINGS_SNAPSHOT(version)`.
- Participants ACK applied version.
- Implement safe update flow:
  - staged rollout and rollback on high failure ratio.
- Persist applied settings locally for restart continuity.

### Deliverables

- `/docs/settings-governance.md`
- settings sync module + migration plan from local-only settings
- config audit log

### Exit Criteria

- New node receives and applies current cluster config on join.
- Config updates are consistent and observable across cluster.

---

## Phase 7 - Security and Access Control

### Purpose
Prevent unauthorized participation and malicious control packets.

### Work Items

- Device identity model with rotating session credentials.
- Signed control packets (or authenticated transport channel).
- Join policy:
  - allowlist / invite token / operator approval mode.
- Replay protection via nonce + sequence window.
- Rate limits and abuse protections.

### Deliverables

- `/docs/security-model.md`
- threat model and mitigations checklist
- security regression tests

### Exit Criteria

- Unauthorized device cannot obtain floor/control authority.
- Replay and spoof attempts are rejected in test scenarios.

---

## Phase 8 - Observability and Operations

### Purpose
Enable real production operation with diagnostics and incident handling.

### Work Items

- Structured event logging with correlation IDs.
- In-app diagnostics screen:
  - leader/role, cluster size, RTT, packet loss, queue depth, jitter stats.
- Exportable diagnostic bundle for support.
- Health score and warning indicators.
- Add runbooks for common incidents:
  - split cluster, degraded relay, high packet loss.

### Deliverables

- `/docs/operations-runbook.md`
- diagnostics UI and logging implementation
- on-call checklist

### Exit Criteria

- Field issue can be diagnosed from logs/diagnostics without reproducing blindly.
- Health indicators correlate with actual user-experienced degradation.

---

## Phase 9 - Gradual Rollout and Production Readiness

### Purpose
Ship safely with controlled risk.

### Work Items

- Introduce feature flags per subsystem:
  - protocol v2, election, relay, centralized settings, new audio pipeline.
- Rollout gates:
  - internal lab -> pilot venue -> multi-venue -> default.
- Capture rollout scorecard:
  - crashes, ANR, SLO breaches, election failures, floor conflicts.
- Finalize versioned migration and downgrade handling.

### Deliverables

- `/docs/rollout-plan.md`
- feature flag matrix
- Go/No-Go checklist

### Exit Criteria

- Pilot venues pass SLOs for agreed burn-in period.
- No critical unresolved issues in go-live checklist.

## Cross-Phase Engineering Standards

- Tests required per phase:
  - unit tests for state machines and codec.
  - integration tests for transport/election/floor.
  - soak tests for long-running stability.
- Backward compatibility:
  - no hard protocol cut without migration window.
- Performance budget:
  - every phase must report impact on CPU, memory, battery, and network.
- Documentation:
  - update architecture docs as part of definition-of-done.

## Suggested Execution Cadence

- Phase 0-1: foundation sprint block.
- Phase 2-4: core distributed behavior block.
- Phase 5-6: quality + governance block.
- Phase 7-9: hardening + rollout block.

Each phase should end with:
- demo scenario,
- measurable KPI report,
- explicit go/no-go decision for next phase.

## Immediate Next Step

Start with Phase 0 and produce the baseline KPI report from current code before changing architecture. This avoids optimizing blindly and gives objective proof of improvement across phases.
