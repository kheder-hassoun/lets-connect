# LANwalkieTalkie - Architecture Guide (for new Kotlin developers)

This document explains how this project is organized and how data flows at runtime.
It is written for developers who are already strong in Java/Dart/Node but new to Kotlin/Android architecture.

## 1. What this app is

`LANwalkieTalkie` (`Droid PTT`) is a **local-network push-to-talk app**.

- No internet backend is required.
- Every app instance acts as both:
  - a local server (accepting socket connections)
  - a local client (connecting to peers)
- Devices discover each other using Android **NSD** (`NsdManager`).
- Audio is captured with `AudioRecord`, transmitted over sockets, and played with `AudioTrack`.
- Text chat messages are also sent over the same network layer.

## 2. Module architecture

This is a multi-module Android project:

- `:app` - Android application entry point, root navigation, foreground service, top-level DI
- `:core` - reusable MVI infrastructure (`ActionProcessor`, `Reducer`, `MviViewModel`)
- `:core-ui` - shared Compose theme primitives
- `:feature-ptt` - Push-to-talk screen logic and UI
- `:feature-chat` - Chat screen logic and UI
- `:feature-settings` - Settings tab (currently minimal)
- `:serivce-network` - network discovery + socket communication + in-memory repositories
- `:service-voice` - microphone recorder + audio player

Notes:

- The module name is spelled `serivce-network` (typo in folder/module name). Keep this spelling when referencing Gradle modules.
- The architecture is intentionally feature/module split with shared infrastructure in `core` and services.

## 3. Tech stack

- Language: Kotlin
- UI: Jetpack Compose + Material3 Adaptive Navigation
- DI: Koin
- Concurrency: Kotlin Coroutines + Flow
- Logging: Timber
- Build: Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`)

## 4. Startup and app lifecycle

### 4.1 App startup

1. `PttApplication` starts Koin and registers all modules (`appModule`).
2. `MainActivity` renders `RootContent()`.
3. `RootContent` sends `MainScreenAction.InitApp` to `MainViewModel`.
4. Main reducers check permissions.
5. If permissions are granted:
   - `VoiceRecorder.create()` is called
   - `MainScreenEvent.StartService` is emitted
6. `RootContent` receives this event and starts `WalkieService`.

### 4.2 Foreground service responsibility

`WalkieService` is the long-running runtime owner.

On `onCreate()` it:

- starts network discovery (`ClientController.startDiscovery()`)
- acquires a wakelock
- creates audio playback (`VoicePlayer.create()`)

On `onStartCommand()` it:

- creates notification channel
- runs as a foreground service

On `onDestroy()` it:

- stops recording/playback
- stops discovery/network
- releases wakelock

## 5. MVI pattern used in this project

The app uses a custom MVI engine from `:core`.

### 5.1 Core classes

- `Reducer<ActionType, State, NextAction, Event>`
  - pure-ish state transition unit (can call side effects)
- `ActionProcessor`
  - holds `StateFlow` state
  - emits one-time events via `Channel`
  - dispatches actions to matching reducer by action class
  - can chain follow-up action (`result.action`)
- `MviViewModel`
  - wraps processor for UI; exposes `state` + `event`; `onAction()` entry point

### 5.2 Important behavior

`ActionProcessor` limits parallelism to 1 (`limitedParallelism(1)`), so reducer processing is serialized. This helps keep UI updates deterministic when socket events come fast.

### 5.3 Why this is good for you (Java background)

Think of it as:

- Redux-like unidirectional flow
- ViewModel is just a dispatcher + state holder
- Reducers are small command handlers
- Side effects are done inside reducers/services

## 6. Dependency injection (Koin)

Top-level DI is in `app/src/main/java/.../di/appDi.kt`.

`appModule` registers:

- core dependencies (`CoroutineContextProvider`, `PermissionState`, `NotificationController`)
- app factories/reducers/viewmodels
- feature DI (`registerPttDi`, `registerChatDi`)
- service DI (`registerServiceNetworkDi`, `registerVoiceDi`)

This creates one graph where features can inject repositories/services directly.

## 7. Data and control flow (end-to-end)

## 7.1 PTT voice flow

1. User presses PTT button -> `PttAction.StartRecording`
2. `StartRecordingReducer` calls `VoiceRecorder.startRecord()`
3. `VoiceRecorder` reads PCM chunks from mic (`AudioRecord.read`)
4. Each chunk is sent via `MessageController.sendMessage(ByteBuffer)`
5. `ChanelControllerImpl` forwards data to both socket client and socket server queues
6. On receiver side, socket read loop classifies payload:
   - payload size > 20 bytes -> treated as voice bytes
7. `SocketServer.dataListener` sends bytes to `VoicePlayer`
8. `VoicePlayer` writes to `AudioTrack` and emits bytes on `voiceDataFlow`
9. `PttViewModel` collects `voiceDataFlow` -> dispatches `PttAction.VoiceDataReceived`
10. UI can render waveform/state updates from `PttScreenState.voiceData`

## 7.2 Chat flow

Outgoing:

1. UI sends `ChatAction.SendMessage(content)`
2. `SendMessageReducer` sends content bytes via `MessageController`
3. Reducer also appends optimistic local message to state (`sender = "me"`)

Incoming:

1. Socket layer receives small payload (<= 20 bytes) as text command/message
2. If payload is not `ping`/`pong`, `TextMessagesRepository.addMessage(...)`
3. `ChatViewModel` collects `TextMessagesRepository.messages`
4. Dispatches `ChatAction.MessagesReceived(messages)`
5. `MessageReceivedReducer` maps to UI model and merges by `id`

## 7.3 Device discovery/connection flow

1. `ChanelControllerImpl.startDiscovery()` initializes server socket and registers NSD service
2. On service registration callback, app starts NSD discovery
3. For each discovered peer service:
   - resolve service to socket address
   - update `ConnectedDevicesRepository`
   - connect via `SocketClient.addClient(...)`
4. Repository emits `clientsFlow`
5. Both `PttViewModel` and `ChatViewModel` collect this flow and update feature states

## 8. Repository layer (current state)

All repositories are currently in-memory (no database persistence):

- `ConnectedDevicesRepository`
  - maintains map of clients and publishes as flow
- `TextMessagesRepository`
  - stores chat messages in-memory and emits list flow
- `DeviceInfoRepository`
  - gets current device metadata and IP

This keeps the app simple but means data resets on app restart.

## 9. UI architecture

- Root UI is in `RootContent`.
- Adaptive navigation:
  - Portrait -> `BottomTabs`
  - Landscape -> `RailTabs`
- Current tab comes from `MainScreenState.currentTab`.
- Feature tabs are rendered by `TabsContent`:
  - `PTTContent`
  - `ChatTab`
  - `SettingsContent`
  - `OffContent`

Each feature owns its own ViewModel and MVI state machine.

## 10. Permissions and Android components

Declared in manifest:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`
- `RECORD_AUDIO`
- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `WAKE_LOCK`

Runtime flow currently checks:

- notification permission (Android 13+)
- record audio permission

Service component:

- `WalkieService` is non-exported foreground service.

## 11. Important implementation notes and caveats

- Module name typo: `serivce-network` is used everywhere; renaming requires full refactor.
- `ConnectedDevicesRepository.storeDataReceivedTime()` currently reuses old timestamp instead of writing current time (likely bug).
- Chat has placeholder logic:
  - `LoadChatHistoryReducer` TODO
  - `MessageSentReducer` currently no-op
- `ChatTab` creates `sampleMessages` list but does not render it.
- Text/voice packet classification uses payload size threshold (`>20` bytes = voice), which is simple but fragile.

## 12. How to extend safely (recommended path)

For a new feature, follow this structure:

1. Create actions/state/events in a feature module.
2. Add reducers per action.
3. Create `InitStateFactory` and `ActionProcessor`.
4. Create ViewModel extending `MviViewModel`.
5. Register DI in feature `.../di/*.kt` and call it from `appModule`.
6. Build Compose UI that only talks to ViewModel through `onAction` and state collection.

For network/audio changes:

- Keep socket IO logic in `:serivce-network`.
- Keep microphone/playback in `:service-voice`.
- Keep feature reducers unaware of socket implementation details (depend on interfaces like `MessageController`).

## 13. Suggested next refactors (high value)

1. Replace packet-size protocol with explicit message envelope (type + payload).
2. Persist chat messages (Room).
3. Fix `storeDataReceivedTime()` timestamp update.
4. Add unit tests for reducers and repositories.
5. Clean naming typos (`Chanel`/`serivce`) in a dedicated migration PR.

## 14. Quick orientation map (where to read first)

If you are joining this codebase, read in this order:

1. `app/src/main/java/pro/devapp/walkietalkiek/ui/RootContent.kt`
2. `core/src/main/java/pro/devapp/walkietalkiek/core/mvi/ActionProcessor.kt`
3. `feature-ptt/src/main/java/pro/devapp/walkietalkiek/feature/ptt/PttViewModel.kt`
4. `feature-chat/src/main/java/pro/devapp/walkietalkiek/feature/chat/ChatViewModel.kt`
5. `serivce-network/src/main/java/pro/devapp/walkietalkiek/serivce/network/ChanelControllerImpl.kt`
6. `service-voice/src/main/java/pro/devapp/walkietalkiek/service/voice/VoiceRecorder.kt`

That sequence gives you UI -> architecture engine -> feature logic -> transport/audio internals.
