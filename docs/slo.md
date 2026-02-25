# SLOs and Acceptance Targets (Phase 0)

This document defines measurable targets for validating production readiness progress.

## Scope

- Product: LANwalkieTalkie / Let's-Connect
- Platforms: Android 9 to Android 15 (API 28 to API 35)
- Network: Same LAN (WiFi)

## Service Level Objectives

## 1) PTT Start Latency

Definition:
- Time from local PTT press to first audible audio on remote device.

Target:
- p50 <= 350 ms
- p95 <= 700 ms
- p99 <= 1000 ms

Failure threshold:
- p95 > 1000 ms in two consecutive runs.

## 2) Floor Conflict Rate

Definition:
- Number of events where more than one user can effectively talk at the same time.

Target:
- 0 conflicts per 1000 floor requests.

Failure threshold:
- Any reproducible conflict in controlled contention test.

## 3) Reconnect Recovery Time

Definition:
- Time for a client to recover usable session state after temporary disconnect/reconnect.

Target:
- p95 <= 5 seconds for WiFi bounce under 10 seconds outage.

Failure threshold:
- p95 > 8 seconds.

## 4) Session Stability

Definition:
- Crash-free and ANR-free operation during soak session.

Target:
- Crash-free sessions >= 99.5%
- ANR count = 0 in 60-minute soak test.

Failure threshold:
- Any ANR in soak run or crash-free < 99%.

## 5) Audio Continuity

Definition:
- Audible interruptions while one speaker holds floor continuously for 30 seconds.

Target:
- <= 1 short interruption (>100 ms) per 30-second window at p95.

Failure threshold:
- Frequent dropouts in normal LAN profile.

## Test Profiles

- Profile A: Clean LAN
  - latency: 5-20 ms
  - loss: <1%
- Profile B: Busy LAN
  - latency: 20-80 ms
  - jitter: moderate
  - loss: 1-3%
- Profile C: Stress
  - latency spikes up to 200 ms
  - loss bursts up to 5%

## Phase 0 Required Evidence

- Baseline report stored in `docs/reports/phase0-baseline.md`.
- For each KPI, provide:
  - sample size
  - p50/p95/p99 where applicable
  - pass/fail status

## Notes

- These targets are initial and can be revised after first baseline run.
- Any revision must include date, reason, and owner.
