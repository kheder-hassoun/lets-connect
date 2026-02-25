# Let's-Connect

**Forked from** [https://github.com/devapro/LANwalkieTalkie](https://github.com/devapro/LANwalkieTalkie)

Let's-Connect is an enhanced Android walkie-talkie application that enables voice communication over local WiFi networks. This fork addresses bugs from the original project, significantly enhances the user interface, adds more robust functionality, and implements a comprehensive floor control mechanism for organized group conversations.

## Features

### Core Functionality
- **Push-to-Talk (PTT) Communication**: Real-time voice transmission over local WiFi networks
- **Device Discovery**: Automatic discovery of nearby devices on the same network
- **Multi-device Support**: Connect and communicate with multiple peers simultaneously
- **Foreground Service**: Continuous operation with background notification support

### Enhanced Features
- **Floor Control Mechanism**: Prevents simultaneous talking with visual floor ownership indicators
- **Talk Duration Management**: Configurable talk time limits with countdown timers
- **Improved UI**: Modern Material Design 3 interface with adaptive layouts
- **Landscape/Portrait Support**: Optimized layouts for both orientations
- **Voice Visualization**: Real-time waveform display during recording
- **Robust Network Handling**: Enhanced connection stability and error recovery

### Technical Improvements
- **Bug Fixes**: Resolved stability issues from the original implementation
- **Performance Optimizations**: Improved audio processing and network efficiency
- **Better Error Handling**: Comprehensive error reporting and user feedback
- **Enhanced Permissions**: Proper handling of Android runtime permissions

## Architecture

The application follows a modular architecture with clear separation of concerns:

- **app**: Main application module with UI components
- **core**: Shared utilities and base classes
- **core-ui**: Reusable UI components and themes
- **feature-ptt**: Push-to-talk functionality with floor control
- **feature-chat**: Chat and messaging features
- **feature-settings**: Application configuration and preferences
- **service-network**: Network discovery and communication
- **service-voice**: Audio recording and playback services

## Production Roadmap

A detailed phased plan for production-grade scalability and reliability is available here:

- [PRODUCTION_ROADMAP_README.md](PRODUCTION_ROADMAP_README.md)

Phase 0 execution docs:

- [docs/README.md](docs/README.md)

## Technical Stack

- **Platform**: Android (API 24+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVI (Model-View-Intent) pattern
- **Dependency Injection**: Koin
- **Build System**: Gradle with Kotlin DSL

## Key Features Explained

### Floor Control Mechanism
The enhanced floor control system prevents multiple users from speaking simultaneously:
- Visual indicators showing who currently holds the floor
- Automatic floor release after timeout
- Queue management for pending speakers
- Priority-based floor assignment

### Enhanced UI/UX
- Modern Material Design 3 components
- Adaptive layouts for different screen sizes
- Intuitive PTT button with haptic feedback
- Real-time connection status indicators
- Voice waveform visualization

### Robust Network Communication
- Improved device discovery algorithms
- Better handling of network interruptions
- Automatic reconnection capabilities
- Enhanced audio quality with reduced latency

## Installation

1. Clone the repository:
```bash
git clone https://github.com/your-username/lets-connect.git
cd lets-connect
```

2. Open the project in Android Studio
3. Build and run the application on an Android device or emulator

## Requirements

- **Android Version**: API 24 (Android 7.0) or higher
  - **Tested on**: Android 9 (API 28) and Android 14 (API 34)
  - **Note**: Android 9 support was added in this enhanced version
- **Permissions**: 
  - Record Audio
  - Internet
  - Network State
  - WiFi State
  - Foreground Service
  - Notifications
  - Location (for network discovery)

## Usage

1. **Connect to WiFi**: Ensure all devices are on the same local network
2. **Launch App**: Open Let's-Connect on all participating devices
3. **Discover Devices**: The app will automatically detect nearby devices
4. **Start Talking**: Press and hold the PTT button to speak
5. **Floor Control**: Only one person can speak at a time - the floor control system manages turn-taking

## Contributing

This project welcomes contributions! Please feel free to submit issues and enhancement requests.

## License

This project maintains the same license as the original LANwalkieTalkie project.

## Acknowledgments

- Original project: [LANwalkieTalkie](https://github.com/devapro/LANwalkieTalkie) by devapro
- Thanks to the open-source community for the various libraries and tools used in this project

## Version History

- **v0.2.2**: Current version with enhanced floor control and UI improvements
- **v0.1.x**: Initial fork with bug fixes and basic enhancements

---

*Note: This application is designed for local network communication only and does not require internet connectivity for device-to-device communication.*
