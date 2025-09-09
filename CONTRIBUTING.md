# Contributing to SMS Logger

Thank you for your interest in contributing to SMS Logger! This document outlines the development workflow, branching strategy, and contribution guidelines.

## Branching Strategy

We follow a Git Flow-inspired branching model to ensure clean, organized development and safe releases.

### Branch Types

#### Main Branches
- **`main`**: Production-ready code. Protected branch with required reviews and status checks.
- **`develop`**: Integration branch where features are merged for testing before release.

#### Supporting Branches
- **`feature/*`**: Feature development branches (e.g., `feature/sms-encryption`, `feature/export-logs`)
- **`fix/*`**: Bug fix branches (e.g., `fix/notification-crash`, `fix/database-migration`)
- **`hotfix/*`**: Critical production fixes (e.g., `hotfix/security-patch`, `hotfix/crash-fix`)
- **`release/*`**: Release preparation branches (e.g., `release/v1.2.0`, `release/v2.0.0`)
- **`devops/*`**: Infrastructure and tooling improvements (e.g., `devops/ci-setup`, `devops/branch-protection`)

### Branch Naming Conventions

Follow these naming patterns for consistency:

```
feature/short-description-of-feature
fix/short-description-of-bug
hotfix/critical-issue-description
release/version-number
devops/infrastructure-change
```

**Examples:**
- `feature/sms-backup-restore`
- `fix/duplicate-message-detection`
- `hotfix/service-crash-android-14`
- `release/v1.3.0`
- `devops/github-actions-ci`

### Workflow

#### Feature Development
1. Create feature branch from `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/your-feature-name
   ```

2. Develop your feature with regular commits
3. Push branch and create Pull Request to `develop`
4. Code review and approval required
5. Merge to `develop` after approval

#### Bug Fixes
1. Create fix branch from `develop` (or `main` for hotfixes):
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b fix/bug-description
   ```

2. Fix the issue with descriptive commits
3. Create Pull Request to appropriate base branch
4. Review and merge after approval

#### Releases
1. Create release branch from `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b release/v1.x.x
   ```

2. Update version numbers, changelog, and documentation
3. Test thoroughly
4. Merge to both `main` and `develop`
5. Tag the release on `main`

#### Hotfixes
1. Create hotfix branch from `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b hotfix/critical-issue
   ```

2. Fix the critical issue
3. Create PRs to both `main` and `develop`
4. Deploy immediately after merge

## Pull Request Guidelines

### Before Creating a PR
- [ ] Branch is up to date with target branch
- [ ] All tests pass locally
- [ ] Code follows project style guidelines
- [ ] Documentation updated if needed
- [ ] No merge conflicts

### PR Requirements
- [ ] Clear, descriptive title
- [ ] Detailed description of changes
- [ ] Link to related issue(s)
- [ ] Screenshots for UI changes
- [ ] Test plan or testing notes

### PR Template
When creating a Pull Request, include:

```markdown
## Description
Brief description of changes

## Related Issues
Fixes #123

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] All existing tests pass

## Screenshots (if applicable)
Add screenshots for UI changes
```

## Code Review Process

### Review Requirements
- All PRs require at least 1 approval before merge
- Maintainers must approve breaking changes
- Automated checks must pass (CI, tests, linting)

### Review Guidelines
- Focus on code quality, security, and maintainability
- Provide constructive feedback
- Test the changes locally when possible
- Check for proper documentation

## Branch Protection Rules

The following branches have protection rules enabled:

### `main` Branch
- Require pull request reviews before merging
- Require status checks to pass before merging
- Require up-to-date branches before merging
- Restrict pushes that create files larger than 100MB
- Do not allow force pushes
- Do not allow deletions

### `develop` Branch
- Require pull request reviews before merging
- Require status checks to pass before merging
- Allow force pushes for maintainers only

## Getting Started

### Setting Up Your Development Environment
1. Fork the repository
2. Clone your fork locally
3. Set up upstream remote:
   ```bash
   git remote add upstream https://github.com/DanyalTorabi/SmsLogger.git
   ```
4. Create your feature branch from `develop`

### Keeping Your Fork Updated
```bash
git checkout develop
git pull upstream develop
git push origin develop
```

## Questions or Issues?

If you have questions about the contribution process, please:
- Check existing issues and discussions
- Create a new issue with the `question` label
- Reach out to maintainers

Thank you for contributing to SMS Logger! 🚀
