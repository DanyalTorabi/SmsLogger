# SMS Logger

[![Code Coverage](https://codecov.io/gh/DanyalTorabi/SmsLogger/branch/main/graph/badge.svg)](https://codecov.io/gh/DanyalTorabi/SmsLogger)

A robust Android application that monitors, logs, and tracks all SMS messages on your device in real-time. This app runs as a background service and maintains a comprehensive database of all incoming and outgoing SMS messages with detailed metadata.

## Features

### Core Functionality
- **Real-time SMS Monitoring**: Automatically captures all incoming and outgoing SMS messages
- **Background Service**: Runs continuously in the background with foreground service notification
- **Auto-start on Boot**: Automatically starts monitoring when device boots up
- **Duplicate Prevention**: Intelligent deduplication to prevent logging the same message multiple times
- **Database Storage**: Persistent SQLite database storage using Room

### Rich SMS Data Capture
- **Basic Information**: Phone number, message body, timestamp, SMS type
- **Advanced Metadata**:
  - Thread ID (groups related messages in conversations)
  - Date sent vs date received timestamps
  - Contact name resolution (if phone number matches contacts)
  - Original SMS provider ID for cross-reference
  - Event timestamps for audit trails

### Smart Synchronization
- **Startup Sync**: Scans and imports all existing SMS messages on first run
- **Incremental Updates**: Only processes new messages to avoid duplicates
- **Content Provider Integration**: Reads directly from Android's SMS content provider
- **Delayed Processing**: 5-second delay for incoming SMS to ensure data availability

## Technical Architecture

### Components
- **MainActivity**: User interface for permissions and service control
- **SmsLoggingService**: Background foreground service for continuous monitoring
- **SmsReceiver**: Broadcast receiver for real-time SMS detection
- **BootCompletedReceiver**: Auto-starts service on device boot
- **Room Database**: SQLite database with migration support

### Database Schema
Table: `sms_log` (Version 2)

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long (PK) | Auto-incrementing unique identifier |
| `smsId` | Long? | Original SMS ID from Android provider |
| `smsTimestamp` | Long | When SMS was received/sent (Unix timestamp) |
| `eventTimestamp` | Long | When event was logged (Unix timestamp) |
| `phoneNumber` | String | Sender/receiver phone number |
| `body` | String | SMS message content |
| `eventType` | String | RECEIVED, SENT, OUTBOX, DRAFT, FAILED, QUEUED |
| `threadId` | Long? | Conversation thread grouping ID |
| `dateSent` | Long? | When SMS was actually sent (sender perspective) |
| `person` | String? | Contact name if phone number matches contacts |

## Installation & Setup

### Prerequisites
- Android 6.0 (API 23) or higher
- SMS permissions (granted at runtime)

### Installation Steps
1. Clone the repository
```bash
git clone <repository-url>
cd SmsLogger
```

2. Open in Android Studio
3. Build and run the project
4. Grant required permissions when prompted:
   - READ_SMS: To read existing SMS messages
   - RECEIVE_SMS: To monitor incoming SMS messages

### First Run Setup
1. Launch the app
2. Tap "Start Service" button
3. Grant SMS permissions when prompted
4. The service will automatically:
   - Start background monitoring
   - Sync all existing SMS messages
   - Begin logging new messages

## Usage

### Starting the Service
- Use the "Start Service" button in the main activity
- Service automatically starts on device boot (after first manual start)
- Foreground notification indicates service is running

### Viewing Logged Data
- Tap "Log All SMS" button to output all stored messages to Logcat
- Use Android Studio's Logcat viewer to see detailed SMS logs
- Each log entry includes all metadata fields

### Service Management
- Service runs continuously until manually stopped
- Survives app closure and device restarts
- Minimal battery impact due to efficient event-driven architecture

## Development

### Code Coverage & Quality

The project uses JaCoCo for code coverage analysis. Current coverage is at 24%, with a goal to increase this over time.

To run coverage analysis locally:
```bash
./gradlew jacocoTestReport
```

The HTML coverage report will be generated at: `app/build/reports/jacoco/html/index.html`

We maintain minimum coverage thresholds to ensure code quality:
- Current minimum threshold: 24%
- Target threshold: 80%

### Development Workflow

We follow a structured branching strategy to ensure clean development and safe releases. Please read our [Contributing Guidelines](CONTRIBUTING.md) for detailed information.

#### Quick Start for Contributors

1. **Fork and Clone**
   ```bash
   git clone https://github.com/YOUR_USERNAME/SmsLogger.git
   cd SmsLogger
   git remote add upstream https://github.com/DanyalTorabi/SmsLogger.git
   ```

2. **Create Feature Branch**
   ```bash
   git checkout develop
   git pull upstream develop
   git checkout -b feature/your-feature-name
   ```

3. **Make Changes and Test**
   ```bash
   # Make your changes
   ./gradlew test
   ./gradlew assembleDebug
   ```

4. **Submit Pull Request**
   - Push your branch to your fork
   - Create PR against `develop` branch
   - Follow the PR template and guidelines

#### Branch Structure
- **`main`** - Production-ready code (protected)
- **`develop`** - Integration branch for features
- **`feature/*`** - Feature development branches
- **`fix/*`** - Bug fix branches  
- **`hotfix/*`** - Critical production fixes
- **`release/*`** - Release preparation branches
- **`devops/*`** - Infrastructure and tooling changes

#### Branch Naming Examples
- `feature/sms-backup-restore`
- `fix/duplicate-message-detection`
- `hotfix/service-crash-android-14`
- `release/v1.3.0`
- `devops/github-actions-ci`

For complete workflow details, see [CONTRIBUTING.md](CONTRIBUTING.md).

### Key Files
- `MainActivity.kt`: Main UI and permission handling
- `SmsLoggingService.kt`: Core background service with SMS processing
- `SmsReceiver.kt`: Broadcast receiver for incoming SMS
- `BootCompletedReceiver.kt`: Auto-start on boot functionality
- `SmsMessage.kt`: Database entity model
- `SmsDao.kt`: Database access operations
- `AppDatabase.kt`: Room database configuration with migrations

### Database Migrations
The app supports seamless database upgrades:
- Version 1 → 2: Added threadId, dateSent, and person fields
- Migration preserves all existing data

### Building
```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build
```

## Permissions

### Required Permissions
- `android.permission.RECEIVE_SMS`: Monitor incoming SMS messages
- `android.permission.READ_SMS`: Access existing SMS database
- `android.permission.RECEIVE_BOOT_COMPLETED`: Auto-start on boot
- `android.permission.FOREGROUND_SERVICE`: Run background service

### Runtime Permissions
SMS permissions are requested at runtime with user-friendly prompts and explanations.

## Privacy & Security

- **Local Storage Only**: All data stored locally on device
- **No Network Access**: No internet permissions, data never transmitted
- **User Control**: Users can stop service and clear data at any time
- **Permission Transparency**: Clear explanation of why permissions are needed

## Troubleshooting

### Common Issues

**Service not starting**
- Ensure SMS permissions are granted
- Check if battery optimization is disabled for the app
- Verify notification permissions (Android 13+)

**Missing SMS messages**
- Check that app is set as default SMS app (if required by device)
- Ensure sufficient storage space
- Verify READ_SMS permission is granted

**Duplicate messages**
- App includes built-in deduplication logic
- If issues persist, clear app data and restart

### Debugging
- Enable verbose logging in SmsLoggingService
- Use "Log All SMS" button to verify database contents
- Check Logcat for detailed error messages

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

[Add your license information here]

## Version History

### Version 2.0
- Added thread ID, date sent, and contact name fields
- Improved SMS synchronization logic
- Enhanced database schema with migrations
- Fixed initialization issues in SMS receiver

### Version 1.0
- Initial release
- Basic SMS monitoring and logging
- Background service implementation
- Room database integration

## Support

For issues, questions, or feature requests, please [create an issue](link-to-issues) in the repository.
