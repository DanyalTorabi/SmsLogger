# Contributing to SMS Logger

Thank you for your interest in contributing to SMS Logger! This document provides guidelines and instructions for contributing to the project.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Guidelines](#development-guidelines)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

This project follows a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

### Prerequisites
- Android Studio (latest stable version recommended)
- JDK 11 or higher
- Android SDK with API 23+ installed
- Git

### Setting Up Development Environment

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/SmsLogger.git
   cd SmsLogger
   ```

3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/DanyalTorabi/SmsLogger.git
   ```

4. Open the project in Android Studio
5. Sync Gradle files
6. Build the project to ensure everything works

### Running the App
1. Connect an Android device or start an emulator
2. Click Run in Android Studio
3. Grant necessary permissions when prompted

## Development Guidelines

Please refer to our comprehensive guidelines in [`.github/copilot-instructions.md`](.github/copilot-instructions.md), which includes:

- **Git Conventions**: Commit message format, branch naming
- **Code Style**: Kotlin conventions, Android best practices
- **Pre-Commit Checklist**: Quality checks before committing
- **Code Review Checklist**: Guidelines for reviewing code
- **Architecture Guidelines**: Project structure and patterns

### Quick Reference

#### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert

**Example**:
```
feat(service): add SMS export functionality

Implement export feature to save SMS logs to CSV file.
Users can now export their SMS history for backup purposes.

Closes #45
```

#### Branch Naming
```
<type>/<issue-number>-<short-description>
```

**Example**: `feat/45-add-export-functionality`

## Submitting Changes

### Before Submitting a Pull Request

Run through the [Pre-Commit Checklist](.github/copilot-instructions.md#pre-commit-checklist):

- [ ] Code compiles without errors
- [ ] Code passes lint checks (`./gradlew lint`)
- [ ] Tests pass (`./gradlew test`)
- [ ] Code follows style guidelines
- [ ] Documentation updated (if needed)
- [ ] Commit messages follow conventions

### Pull Request Process

1. **Update your fork**:
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```

2. **Create a feature branch**:
   ```bash
   git checkout -b feat/123-your-feature
   ```

3. **Make your changes**:
   - Write clear, focused commits
   - Follow code style guidelines
   - Add tests for new features
   - Update documentation

4. **Test your changes**:
   ```bash
   ./gradlew test
   ./gradlew lint
   ```

5. **Push to your fork**:
   ```bash
   git push origin feat/123-your-feature
   ```

6. **Create a Pull Request**:
   - Go to the original repository on GitHub
   - Click "New Pull Request"
   - Select your fork and branch
   - Fill in the PR template
   - Link related issues (e.g., "Closes #123")

### Pull Request Guidelines

- **Title**: Use a clear, descriptive title
- **Description**: Explain what changes were made and why
- **Screenshots**: Include before/after screenshots for UI changes
- **Testing**: Describe how you tested the changes
- **Breaking Changes**: Clearly mark any breaking changes
- **Issue Reference**: Link to related issues

### Code Review Process

- At least one maintainer will review your PR
- Address feedback promptly
- Keep the conversation focused and professional
- Once approved, a maintainer will merge your PR

## Reporting Issues

### Before Creating an Issue

- Search existing issues to avoid duplicates
- Check if the issue is already fixed in the latest version
- Gather relevant information (device, Android version, logs)

### Creating a Good Issue

Use the appropriate issue template and include:

**For Bug Reports**:
- Clear, descriptive title
- Steps to reproduce
- Expected behavior
- Actual behavior
- Device and Android version
- Relevant logs or screenshots
- Possible solution (if you have one)

**For Feature Requests**:
- Clear, descriptive title
- Problem statement (what problem does this solve?)
- Proposed solution
- Alternative solutions considered
- Additional context

**For Questions**:
- Clear, specific question
- What you've already tried
- Relevant code or configuration

## Development Tips

### Useful Gradle Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Run lint
./gradlew lint

# Clean build
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

### Debugging

- Use Android Studio's debugger
- Check Logcat for runtime logs
- Use "Log All SMS" feature to verify database contents
- Enable verbose logging in SmsLoggingService for detailed logs

### Testing

- Write unit tests for business logic
- Test on multiple Android versions
- Test on different screen sizes
- Test edge cases (null values, empty data, etc.)

## Project Structure

```
SmsLogger/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/smslogger/
│   │   │   │   ├── data/          # Database, repositories
│   │   │   │   ├── domain/        # Business logic
│   │   │   │   ├── ui/            # Activities, ViewModels
│   │   │   │   ├── service/       # Background services
│   │   │   │   ├── receiver/      # Broadcast receivers
│   │   │   │   └── util/          # Utilities
│   │   │   ├── res/               # Resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                  # Unit tests
│   │   └── androidTest/           # Instrumentation tests
│   └── build.gradle
├── .github/
│   └── copilot-instructions.md    # Detailed guidelines
├── CONTRIBUTING.md                # This file
└── README.md                      # Project documentation
```

## Communication

- **Issues**: For bugs, features, and questions
- **Pull Requests**: For code contributions
- **Discussions**: For general questions and ideas

## License

By contributing to SMS Logger, you agree that your contributions will be licensed under the same license as the project.

## Recognition

Contributors will be recognized in the project. Significant contributions may be highlighted in release notes.

## Questions?

If you have questions not covered in this guide, please:
1. Check the [README](README.md)
2. Check existing issues and discussions
3. Create a new issue with the "question" label

Thank you for contributing to SMS Logger! 🎉

