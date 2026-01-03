# Notification When Export Complete

## User Story

**As a** scoring admin
**I want to** get a notification when CSV export finishes
**So that** I don't need to guess when background job finishes or reload a file explorer to figure it out

## Overview

Add in-app notification (Material 3 Snackbar) to inform users when CSV export completes successfully or fails. The notification will only show if the user remains on the leaderboard screen during export.

## Requirements

### Functional Requirements

1. **Success Notification**
   - Display snackbar with message: "Export complete"
   - Auto-dismiss after ~4 seconds (Material 3 short duration)
   - No action button

2. **Failure Notification**
   - Display snackbar with message: "Export failed: [error reason]"
   - Show actual exception message from error
   - Auto-dismiss after ~4 seconds (Material 3 short duration)
   - No action button

3. **Navigation Behavior**
   - If user navigates away from LeaderboardScreen during export: continue export in background, do NOT show notification
   - If user remains on LeaderboardScreen: show notification when export completes

4. **Multiple Exports**
   - If user triggers multiple exports rapidly, replace previous notification with latest
   - Each new export notification supersedes previous one

### Non-Functional Requirements

1. **Performance**
   - No progress indication during export (export is already fast)
   - Export continues asynchronously (already implemented)

2. **UI/UX**
   - Use Material 3 Snackbar component
   - Same visual style for both success and error notifications
   - Show at LeaderboardScreen level (not app-wide scaffold)

3. **Error Handling**
   - Generic catch-all exception handling
   - Display `error.message` in failure notification
   - Handle any exception type (IOException, SecurityException, etc.)

4. **Logging**
   - No additional logging/analytics required
   - Rely on existing error handling infrastructure

## Technical Design

### Architecture

```
EventScoreViewModel
├── exportScores() - existing async export function
├── snackbarMessage: StateFlow<String?> - NEW
└── clearSnackbarMessage() - NEW

LeaderboardScreen
├── Observe snackbarMessage StateFlow
├── Show snackbar when message != null
└── Call clearSnackbarMessage() after showing
```

### ViewModel Changes

**EventScoreViewModel.kt**

Add new state:
```kotlin
private val _snackbarMessage = MutableStateFlow<String?>(null)
val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
```

Modify export function:
```kotlin
fun exportScores(...) {
    viewModelScope.launch {
        try {
            // existing export logic
            csvExchangeRepository.exportRiders(...)
            _snackbarMessage.value = "Export complete"
        } catch (e: Exception) {
            _snackbarMessage.value = "Export failed: ${e.message}"
        }
    }
}

fun clearSnackbarMessage() {
    _snackbarMessage.value = null
}
```

### UI Changes

**LeaderboardScreen.kt**

Add snackbar host and state management:
```kotlin
@Composable
fun LeaderboardScreen(viewModel: EventScoreViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // ... rest of screen
    ) { ... }
}
```

## Testing Strategy

### Unit Tests

**EventScoreViewModelTest.kt**

1. `exportScores_setsSuccessMessage_onSuccess()`
   - Mock successful export
   - Verify `snackbarMessage` emits "Export complete"

2. `exportScores_setsErrorMessage_onFailure()`
   - Mock repository throwing exception
   - Verify `snackbarMessage` emits "Export failed: [message]"

3. `clearSnackbarMessage_clearsMessage()`
   - Set snackbar message
   - Call clearSnackbarMessage()
   - Verify message is null

### UI Tests (Compose Testing)

**LeaderboardScreenTest.kt**

1. `exportSuccess_displaysSnackbar()`
   - Trigger export via UI
   - Verify snackbar with "Export complete" appears
   - Verify snackbar auto-dismisses

2. `exportFailure_displaysErrorSnackbar()`
   - Mock export failure
   - Trigger export via UI
   - Verify snackbar with error message appears

3. `navigateAway_snackbarNotShown()`
   - Start export
   - Navigate to different screen
   - Complete export in background
   - Verify no snackbar on new screen

## Implementation Notes

1. **Existing Code**: Export is already asynchronous using coroutines in `CsvExchangeRepository`, so no refactoring of export logic is needed

2. **File Location**: Export saves to user-selected location (via SAF/system file picker), path not needed in notification

3. **Scope**: Snackbar lives only at LeaderboardScreen level. If user navigates away before completion, notification is skipped entirely (meeting the "don't show if not on screen" requirement)

4. **State Management**: Using StateFlow instead of LiveData for consistency with modern Kotlin patterns. Screen collects as State in Compose.

## Open Questions / Future Enhancements

- None at this time

## Acceptance Criteria

- [ ] When CSV export succeeds and user is on LeaderboardScreen, snackbar shows "Export complete"
- [ ] When CSV export fails and user is on LeaderboardScreen, snackbar shows "Export failed: [reason]"
- [ ] Snackbar auto-dismisses after ~4 seconds
- [ ] If user navigates away during export, no snackbar is shown
- [ ] Multiple rapid exports replace previous notification
- [ ] Unit tests cover ViewModel export state management
- [ ] UI tests verify snackbar appearance and behavior
