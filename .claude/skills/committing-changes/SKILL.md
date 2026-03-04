---
name: committing-changes
description: Create and manage git commits for the TrialsScore project. Use this skill whenever the user wants to commit changes, asks about commit message conventions, or needs guidance on preparing changes for commit. Includes testing requirements, commit message style, and best practices.
---

# Committing Changes to TrialsScore

This skill provides guidance on creating commits for the TrialsScore project, including message conventions and pre-commit best practices.

## Commit Message Style

Use concise commit messages and include only a summary for simple changes. Keep messages clear, focused, and descriptive of what the change accomplishes.

### Guidelines

- **Format**: Keep the message short and to the point. For simple changes, a single line summary is sufficient.
- **Content**: Explain *what* changed and *why*, not just the mechanics of the change.
- **Scope**: One logical change per commit. If you're touching multiple unrelated features, create separate commits.

### Examples

**Good:**
```
Fix critical and high severity issues
```

```
Add proper handling of upside-down cards
```

```
Include kaggle model training
```

**Avoid:**
- Vague messages like "update" or "fix"
- Implementation details in the message ("added getter method to class")
- Multiple unrelated changes in one commit

## Pre-Commit Best Practices

Before committing:

1. **Run tests** — Ensure unit tests pass before committing:
   ```bash
   ./gradlew test
   ```

2. **Verify your changes** — Review staged changes to catch unintended modifications

3. **Keep commits focused** — Each commit should represent a single logical change

4. **Include only relevant files** — Don't commit build artifacts, IDE files, or unrelated changes

## Testing Strategy

The project uses:
- **Unit tests** (`/test/`) — JUnit with Hamcrest assertions, JavaFaker for data generation
- **Instrumented tests** (`/androidTest/`) — Compose testing with `ANDROIDX_TEST_ORCHESTRATOR`
- **Test naming**: `<methodBeingTested>_<expectation>_<optionalConditions>`

Run the appropriate test suite before committing:

```bash
# Unit tests
./gradlew test

# Specific test class
./gradlew testDebugUnitTest --tests "net.yakavenka.trialsscore.viewmodel.RiderStandingTransformationTest"

# Instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

See the `building-project` skill for detailed test commands.

## Workflow

1. Make your changes
2. Run relevant tests to verify functionality
3. Stage your changes
4. Write a clear, concise commit message
5. Create the commit
6. Verify with `git log` if needed

## Additional Resources

- Recent commits: `git log --oneline -10` (shows last 10 commits)
- Review staged changes: `git diff --cached`
- Check working tree status: `git status`
