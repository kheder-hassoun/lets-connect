# Smoke Test Checklist (Phase 0)

Run this checklist before and after each major milestone.

## Test Setup

- Devices: minimum 2 physical devices (recommended 3)
- OS coverage: at least one API 28 and one API 35 device/emulator
- Same WiFi LAN
- Build: latest debug build from current branch

## Pre-Check

- App installs and launches on all devices.
- Required permissions granted (microphone, notifications where needed).
- Foreground service notification appears correctly.

## Core Flow Checks

1. Discovery and presence
- Start app on Device A then Device B.
- Verify each device appears connected.
- Pass if both devices see each other within 10 seconds.

2. Basic PTT
- Hold PTT on Device A for 5 seconds.
- Verify Device B hears audio.
- Repeat B -> A.
- Pass if both directions work without app freeze/crash.

3. Floor arbitration
- Press PTT nearly simultaneously on A and B.
- Pass if only one speaker is effective at a time.

4. Chat delivery
- Send short text message A -> B and B -> A.
- Pass if messages arrive within 3 seconds and no duplication.

5. Reconnect recovery
- Disable WiFi on B for ~8 seconds, then re-enable.
- Pass if B rejoins and can send/receive PTT/chat within 15 seconds.

6. Service continuity
- Put app in background on one device for 60 seconds.
- Return to foreground.
- Pass if session still active and PTT works.

## Regression Quick Check (Feature Flags)

For each enabled flag in the release candidate:
- Run full Core Flow Checks with flag OFF.
- Run full Core Flow Checks with flag ON.
- Pass if both paths are functional and no severe regressions.

## Fail Classification

- Blocker:
  - app crash/ANR
  - no audio path
  - floor conflict reproducible
- Major:
  - reconnect consistently exceeds threshold
  - frequent message loss
- Minor:
  - UI glitches without function loss

## Report Template

- Date:
- Branch/commit:
- Devices/OS:
- Flags enabled:
- Results per step (pass/fail):
- Issues found:
- Attach logs/screenshots:
