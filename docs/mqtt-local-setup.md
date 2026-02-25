# Local MQTT Setup (Dev/Test)

This guide sets up a local LAN MQTT broker for Phase 1 control-plane testing.

## Recommended Broker Options

- Option A (recommended): Mosquitto
- Option B: EMQX

Use Option A first for simplicity.

## Option A: Mosquitto Quick Start

## 1) Install

Linux:
- `sudo apt update`
- `sudo apt install -y mosquitto mosquitto-clients`

Windows:
- Install Mosquitto from official installer.
- Ensure service is running.

## 2) Minimal config (dev only)

Create file `mosquitto.conf`:

```conf
listener 1883
allow_anonymous true
persistence false
```

Start broker:
- `mosquitto -c ./mosquitto.conf`

## 3) Verify from terminal

Subscriber:
- `mosquitto_sub -h <broker-ip> -t 'cluster/test/#' -v`

Publisher:
- `mosquitto_pub -h <broker-ip> -t 'cluster/test/ping' -m 'hello'`

Pass if subscriber receives the message.

## Android App Integration Inputs

Define runtime config values (debug build):
- Broker host (LAN IP)
- Broker port (1883)
- Username/password (optional for later phases)
- Cluster ID (example: `venue-dev-01`)

Current implementation note:
- Broker host/port/cluster ID are configurable at runtime in app Settings (`MQTT Control Config` section).
- Initial implemented topic: `cluster/{clusterId}/presence` (online heartbeat logging).

## Topic Convention (Phase 1)

- `cluster/{clusterId}/presence`
- `cluster/{clusterId}/floor`
- `cluster/{clusterId}/settings`
- `cluster/{clusterId}/chat`

## QA Sanity Test

- Start broker on laptop/server in same LAN.
- Connect 2-3 app instances.
- Confirm publish/subscribe behavior using terminal subscriber.

## Security Note

The anonymous setup is for development only.
Production rollout must use:
- authentication,
- topic ACLs,
- optional TLS/mTLS based on venue requirements.
