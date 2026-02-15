# GitHub Copilot Instructions for SMS Logger

This document provides guidelines and conventions for GitHub Copilot when assisting with the SMS Logger Android project.

## Table of Contents
- [Project Overview](#project-overview)
- [Git Conventions](#git-conventions)
- [Code Style Guidelines](#code-style-guidelines)
- [Pre-Commit Checklist](#pre-commit-checklist)
- [Code Review Checklist](#code-review-checklist)
- [Architecture Guidelines](#architecture-guidelines)

## Project Overview

SMS Logger is an Android application that monitors, logs, and tracks all SMS messages on a device in real-time. The app uses:
- **Language**: Kotlin
- **Min SDK**: API 23 (Android 6.0)
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Async**: Kotlin Coroutines
- **DI**: Manual dependency injection (consider Hilt for future)

### Repository Information
- **Repository URL**: https://github.com/DanyalTorabi/SmsLogger
- **Owner**: DanyalTorabi
- **Default Branch**: main
- **Primary Language**: Kotlin

### Key Components
- Background service for continuous SMS monitoring
- Broadcast receivers for SMS events
- Room database for persistent storage
- Content provider integration for SMS access

## Git Conventions

### Commit Message Format

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types
- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Code style changes (formatting, missing semi-colons, etc.)
- **refactor**: Code change that neither fixes a bug nor adds a feature
- **perf**: Performance improvement
- **test**: Adding missing tests or correcting existing tests
- **build**: Changes to build system or dependencies
- **ci**: Changes to CI configuration files and scripts
- **chore**: Other changes that don't modify src or test files
- **revert**: Reverts a previous commit

#### Scopes (Examples)
- **service**: SmsLoggingService related changes
- **receiver**: Broadcast receiver changes
- **database**: Database/Room related changes
- **ui**: User interface changes
- **permissions**: Permission handling changes
- **sync**: SMS synchronization logic
- **network**: Network monitoring or related features

#### Examples

```
feat(service): add notification channel for SMS monitoring

Implement Android O+ notification channels for the foreground service
to provide better user control over notifications.

Closes #42
```

```
fix(receiver): handle null SMS body in rare cases

Some SMS messages may have null body (e.g., class 0 messages).
Added null safety check to prevent crashes.

Fixes #38
```

```
docs(readme): update installation instructions

- Add Android 13+ permission requirements
- Update screenshots
- Fix typos in troubleshooting section
```

```
refactor(database): migrate to Flow for reactive queries

Replace LiveData with StateFlow for better coroutine integration
and more predictable behavior in background services.
```

### Branch Naming Convention

Use descriptive branch names with prefixes:

```
<type>/<issue-number>-<short-description>
```

**Examples:**
- `feat/45-add-export-functionality`
- `fix/38-null-sms-body-crash`
- `docs/37-enhance-copilot-instructions`
- `refactor/50-migrate-to-hilt`
- `chore/52-update-dependencies`

### General Git Guidelines

1. **Keep commits atomic**: Each commit should represent a single logical change
2. **Write descriptive messages**: Explain what and why, not how
3. **Reference issues**: Use `Closes #X`, `Fixes #X`, or `Relates to #X`
4. **Squash WIP commits**: Before merging, clean up your commit history
5. **Pull before push**: Always pull latest changes before pushing
6. **Don't commit secrets**: Never commit API keys, tokens, or sensitive data
7. **Use .gitignore**: Keep build artifacts and local config out of repo

### GitHub Labels

The repository uses the following labels for issue tracking:

**Issue Types:**
- `bug`: Something isn't working
- `enhancement`: New feature or request
- `documentation`: Improvements or additions to documentation
- `question`: Further information is requested
- `invalid`: This doesn't seem right
- `wontfix`: This will not be worked on

**Categories:**
- `architecture`: Architecture and code structure
- `performance`: Performance optimization
- `testing`: Testing related
- `devops`: DevOps and CI/CD related
- `infrastructure`: Infrastructure and deployment
- `code-quality`: Code quality improvements and standards
- `security`: Security vulnerability or improvement

**Priority Levels:**
- `high-priority`: High priority issue (red)
- `medium-priority`: Medium priority issue (yellow)
- `low-priority`: Low priority issue (blue)

**Community:**
- `good first issue`: Good for newcomers
- `help wanted`: Extra attention is needed

When creating or working on issues, please use appropriate labels to help with project organization and prioritization.

### GitHub CLI Workflow

This project uses GitHub CLI (`gh`) for streamlined development workflow:

**Viewing Issues:**
```bash
# List all open issues
gh issue list

# View specific issue
gh issue view 37

# List issues with specific labels
gh issue list --label bug,high-priority
```

**Working with Issues:**
```bash
# Create a new issue
gh issue create --title "Issue title" --body "Description"

# Assign yourself to an issue
gh issue develop 37 --checkout

# Close an issue
gh issue close 37 --comment "Fixed in PR #XX"
```

**Pull Requests:**
```bash
# Create a PR
gh pr create --title "feat: add feature" --body "Description"

# View PR status
gh pr status

# Checkout a PR
gh pr checkout 123
```

**Repository Information:**
```bash
# View repo details
gh repo view

# View repo labels
gh label list
```

Use these commands to maintain consistency and efficiency in your development workflow.

## Code Style Guidelines

### Kotlin Conventions

1. **Follow official Kotlin style guide**
   - Use 4 spaces for indentation
   - Max line length: 120 characters
   - Use trailing commas in multi-line declarations

2. **Naming Conventions**
   - Classes/Interfaces: PascalCase (`SmsLoggingService`, `SmsDao`)
   - Functions/Variables: camelCase (`startService`, `phoneNumber`)
   - Constants: UPPER_SNAKE_CASE (`ACTION_START_SERVICE`, `NOTIFICATION_ID`)
   - Private members: camelCase with underscore prefix (`_smsCount`)

3. **Code Organization**
   ```kotlin
   class Example {
       // Companion object first
       companion object {
           const val CONSTANT = "value"
       }
       
       // Properties
       private val _state = MutableStateFlow<State>(Initial)
       val state: StateFlow<State> = _state
       
       // Init blocks
       init {
           // initialization
       }
       
       // Public methods
       fun publicMethod() { }
       
       // Private methods
       private fun privateMethod() { }
   }
   ```

4. **Null Safety**
   - Prefer non-nullable types
   - Use `?.`, `?:`, and `!!` appropriately (avoid `!!` when possible)
   - Use `lateinit` for properties initialized in onCreate/onStart
   - Use `by lazy` for expensive one-time initialization

5. **Coroutines Best Practices**
   - Use appropriate dispatchers (Main, IO, Default)
   - Proper scope management (lifecycleScope, viewModelScope)
   - Handle exceptions with try-catch or CoroutineExceptionHandler
   - Cancel jobs when no longer needed

### Android-Specific Guidelines

1. **Context Usage**
   - Use `applicationContext` for long-lived operations
   - Use `Activity` context for UI-related operations
   - Avoid memory leaks by not holding Activity references

2. **Resource Management**
   - Use string resources for all user-facing text
   - Use dimension resources for sizes and margins
   - Use color resources for all colors
   - Follow Material Design guidelines

3. **Permissions**
   - Request at appropriate time (when feature is needed)
   - Provide clear rationale
   - Handle permission denial gracefully
   - Check permissions before use

4. **Background Work**
   - Use WorkManager for deferrable tasks
   - Use foreground services for immediate user-facing tasks
   - Implement proper lifecycle management
   - Handle configuration changes

## Pre-Commit Checklist

Before committing code, ensure you've completed these checks:

### Code Quality
- [ ] Code compiles without errors
- [ ] Code passes lint checks (`./gradlew lint`)
- [ ] No new warnings introduced
- [ ] Code follows project style guidelines
- [ ] All TODOs have issue numbers or are resolved
- [ ] No commented-out code (unless with explanation)
- [ ] No debug logging in production code
- [ ] Removed all System.out.println and printStackTrace()

### Functionality
- [ ] Feature works as intended
- [ ] Edge cases handled (null, empty, extreme values)
- [ ] Error cases handled gracefully
- [ ] No crashes or ANRs (Application Not Responding)
- [ ] Tested on minimum supported API level (23)
- [ ] Tested on latest Android version
- [ ] Works in both portrait and landscape modes
- [ ] Works with different screen sizes

### Testing
- [ ] Unit tests written for business logic
- [ ] Tests pass (`./gradlew test`)
- [ ] Manual testing completed
- [ ] Regression testing (didn't break existing features)

### Documentation
- [ ] Code is self-documenting or has comments
- [ ] Public APIs have KDoc comments
- [ ] Complex logic is explained
- [ ] README updated if needed
- [ ] CHANGELOG updated (if maintained)

### Security & Privacy
- [ ] No sensitive data logged
- [ ] User data handled securely
- [ ] Permissions properly requested and checked
- [ ] No hardcoded credentials or API keys
- [ ] ProGuard rules updated (if needed)

### Database
- [ ] Database migrations tested
- [ ] Migration path from previous versions works
- [ ] No data loss during migration
- [ ] Indices created for frequently queried columns

### Performance
- [ ] No blocking operations on main thread
- [ ] Database queries optimized
- [ ] Images properly sized and compressed
- [ ] Memory leaks checked (LeakCanary)
- [ ] Battery impact minimal

### Git
- [ ] Commit message follows conventions
- [ ] Changes are atomic and focused
- [ ] No unrelated changes included
- [ ] No merge conflicts
- [ ] Branch is up to date with main
- [ ] .gitignore updated (if needed)

## Code Review Checklist

When reviewing code (or requesting Copilot to review), use this checklist:

### General
- [ ] **Purpose Clear**: Is the purpose of the change clear?
- [ ] **Scope Appropriate**: Is the change focused and not too large?
- [ ] **Tests Included**: Are tests included and comprehensive?
- [ ] **Documentation Updated**: Is documentation updated as needed?

### Code Quality
- [ ] **Readability**: Is the code easy to read and understand?
- [ ] **Naming**: Are names descriptive and follow conventions?
- [ ] **Complexity**: Is the code unnecessarily complex?
- [ ] **DRY Principle**: Is there code duplication that should be refactored?
- [ ] **SOLID Principles**: Does the code follow SOLID principles?
- [ ] **Error Handling**: Are errors handled appropriately?

### Kotlin/Android Specific
- [ ] **Null Safety**: Is null safety properly handled?
- [ ] **Lifecycle Awareness**: Are Android lifecycle events properly handled?
- [ ] **Memory Leaks**: Are there potential memory leaks (Context, listeners)?
- [ ] **Threading**: Are threading and coroutines used correctly?
- [ ] **Resources**: Are resources properly managed (closed, released)?
- [ ] **Permissions**: Are permissions checked before use?

### Architecture
- [ ] **Separation of Concerns**: Is functionality properly separated?
- [ ] **Single Responsibility**: Does each class have a single responsibility?
- [ ] **Dependencies**: Are dependencies injected and testable?
- [ ] **Layer Boundaries**: Are architectural layers respected?
- [ ] **Data Flow**: Is data flow clear and unidirectional?

### Database
- [ ] **Migrations**: Are database changes properly migrated?
- [ ] **Queries**: Are queries efficient and indexed?
- [ ] **Transactions**: Are transactions used where appropriate?
- [ ] **Data Validation**: Is data validated before storage?

### Security
- [ ] **Data Protection**: Is sensitive data protected?
- [ ] **Input Validation**: Is user input validated?
- [ ] **SQL Injection**: Are queries parameterized (Room handles this)?
- [ ] **Logging**: Is sensitive data kept out of logs?
- [ ] **Permissions**: Are minimum necessary permissions requested?

### Performance
- [ ] **Main Thread**: Are long operations off the main thread?
- [ ] **Memory Usage**: Is memory usage reasonable?
- [ ] **Battery Impact**: Is battery drain minimized?
- [ ] **Network**: Are network calls optimized (if applicable)?
- [ ] **Caching**: Is appropriate caching implemented?

### UI/UX (if applicable)
- [ ] **Material Design**: Follows Material Design guidelines?
- [ ] **Accessibility**: Accessible to users with disabilities?
- [ ] **Responsive**: Works on different screen sizes?
- [ ] **Loading States**: Shows feedback during operations?
- [ ] **Error States**: Displays helpful error messages?

### Testing
- [ ] **Test Coverage**: Is test coverage adequate?
- [ ] **Test Quality**: Are tests meaningful and maintainable?
- [ ] **Edge Cases**: Are edge cases tested?
- [ ] **Test Names**: Are test names descriptive?

### Compatibility
- [ ] **API Levels**: Works on all supported API levels?
- [ ] **Deprecated APIs**: Avoids deprecated APIs or handles properly?
- [ ] **Backwards Compatibility**: Maintains backwards compatibility?

### Documentation
- [ ] **Code Comments**: Complex logic is explained?
- [ ] **KDoc**: Public APIs have KDoc?
- [ ] **README**: README updated if needed?
- [ ] **Change Description**: PR description is clear and complete?

## Architecture Guidelines

### MVVM Pattern
```
View (Activity/Fragment)
  ↓
ViewModel (business logic, UI state)
  ↓
Repository (data abstraction)
  ↓
Data Source (Room, ContentProvider, Network)
```

### Package Structure
```
com.example.smslogger/
├── data/
│   ├── database/
│   │   ├── SmsDao.kt
│   │   ├── AppDatabase.kt
│   │   └── entities/
│   │       └── SmsMessage.kt
│   └── repository/
│       └── SmsRepository.kt
├── domain/
│   ├── model/
│   └── usecase/
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   └── ...
├── service/
│   └── SmsLoggingService.kt
├── receiver/
│   ├── SmsReceiver.kt
│   └── BootCompletedReceiver.kt
└── util/
    ├── NetworkMonitor.kt
    └── Extensions.kt
```

### Dependency Flow
- UI depends on ViewModel
- ViewModel depends on Repository/UseCases
- Repository depends on Data Sources
- No reverse dependencies

### State Management
- Use StateFlow for UI state
- Emit immutable state objects
- Handle loading, success, error states
- Single source of truth principle

## Additional Resources

- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Development Best Practices](https://developer.android.com/kotlin/style-guide)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Material Design Guidelines](https://material.io/design)

---

**Last Updated**: February 15, 2026  
**Version**: 1.0

