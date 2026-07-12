# GymTracker Function Overview

This is a practical map of the project functions. Data classes and Room relation/entity classes are not listed unless they contain functions, because they mostly define stored or transferred data shapes.

## Big Picture

The phone app owns workout templates and completed workout history. It builds a `WorkoutTemplateDto` and sends it to the watch.

The watch app owns the live workout flow. It receives a template, lets you complete reps/weight/rest, persists in-progress state, then sends a `CompletedWorkout` back to the phone.

The phone receives the completed workout, stores it in Room, sends an ACK back to the watch, and updates the past-workouts UI.

## Phone App Entry Points

### `GymTrackerApp.onCreate`
File: `app/src/main/java/com/example/gymtrackerphone/GymTrackerApp.kt`

Initializes the phone app process. It creates the notification channel and lazily provides the Room database.

### `MainActivity.onCreate`
File: `app/src/main/java/com/example/gymtrackerphone/MainActivity.kt`

Creates the `WorkoutViewModel`, sets Compose content, applies the app theme, and hosts the phone navigation.

### `WorkoutViewModelFactory.create`
File: `app/src/main/java/com/example/gymtrackerphone/viewmodel/WorkoutViewModelFactory.kt`

Constructs `WorkoutViewModel` with the repository. This is needed because the ViewModel requires dependencies.

## Phone UI Functions

### `HomeScreen`
File: `app/src/main/java/com/example/gymtrackerphone/ui/HomeScreen.kt`

Main phone screen. It owns the bottom tab state and switches between `MyWorkoutsScreen` and `PastWorkoutsScreen`.

### `MyWorkoutsScreen`
File: `app/src/main/java/com/example/gymtrackerphone/ui/MyWorkoutsScreen.kt`

Shows template workouts. It lets you add, edit, delete, import CSV templates, tap into details, and swipe right to send a workout to the watch.

### `MyWorkoutInputBar`
File: `app/src/main/java/com/example/gymtrackerphone/MyWorkoutInputBar.kt`

Older/simple workout input bar for adding or updating a workout name. Most current behavior lives directly in `MyWorkoutsScreen`.

### `WorkoutDetailsScreen`
File: `app/src/main/java/com/example/gymtrackerphone/ui/WorkoutDetailsScreen.kt`

Template editor. It shows exercises and sets, supports adding/deleting exercises and sets, and edits rep range, weight, and rest time.

### `WheelPicker`
File: `app/src/main/java/com/example/gymtrackerphone/ui/WorkoutDetailsScreen.kt`

Horizontal snapping picker used for weight and rest values. It tracks the centered item and commits changes after real user scrolling stops.

### `PastWorkoutsScreen`
File: `app/src/main/java/com/example/gymtrackerphone/ui/PastWorkoutsScreen.kt`

Shows completed workouts. It supports filtering by workout name, importing/exporting CSV, editing completed set values, expanding workout details, and swipe-right delete.

### `SetChip`
File: `app/src/main/java/com/example/gymtrackerphone/ui/PastWorkoutsScreen.kt`

Small UI pill for one completed set. Shows set number, reps, and weight, and opens edit mode when clicked.

### `EditSetDialog`
File: `app/src/main/java/com/example/gymtrackerphone/ui/PastWorkoutsScreen.kt`

Dialog for editing reps and weight of a completed set.

### Past workout format helpers
File: `app/src/main/java/com/example/gymtrackerphone/ui/PastWorkoutsScreen.kt`

`formatDateOnly`, `formatTimeOnly`, `formatDateTime`, `formatDuration`, and `formatWeight` convert stored numbers/timestamps into UI text.

### `exportCsv`
File: `app/src/main/java/com/example/gymtrackerphone/ui/PastWorkoutsScreen.kt`

Builds a CSV from completed workout history, writes it to cache, creates a `FileProvider` URI, and launches Android share UI.

### `rememberKeyboardOpen`
File: `app/src/main/java/com/example/gymtrackerphone/ui/util/Keyboard.kt`

Composable helper that observes IME insets and returns whether the keyboard is open.

### `GymTrackerPhoneTheme`
File: `app/src/main/java/com/example/gymtrackerphone/ui/theme/Theme.kt`

Applies the app's Material theme around Compose content.

## Phone ViewModel Functions

### Template actions
File: `app/src/main/java/com/example/gymtrackerphone/viewmodel/WorkoutViewModel.kt`

`addWorkout`, `deleteWorkout`, and `updateWorkout` wrap repository calls for workout templates.

`addExercise`, `deleteExercise`, `addSet`, `deleteSet`, `updateRepRange`, `updateWeight`, and `updateRest` edit the current template structure.

### Completed workout actions
File: `app/src/main/java/com/example/gymtrackerphone/viewmodel/WorkoutViewModel.kt`

`updateCompletedSet` edits a saved past set.

`deleteCompletedWorkout` removes one completed workout from history.

### `buildWorkoutTemplate`
File: `app/src/main/java/com/example/gymtrackerphone/viewmodel/WorkoutViewModel.kt`

Builds the payload sent to the watch. It loads the selected workout template, attaches only the latest previous completed session for that template, maps exercises/sets into DTOs, and returns `WorkoutTemplateDto`.

### `sendWorkoutToWatch`
File: `app/src/main/java/com/example/gymtrackerphone/viewmodel/WorkoutViewModel.kt`

Calls `buildWorkoutTemplate`, then sends the DTO to the watch through `WorkoutSender`.

### CSV import functions
File: `app/src/main/java/com/example/gymtrackerphone/viewmodel/WorkoutViewModel.kt`

`importCompletedCsv` imports past workout rows and stores them as completed workouts.

`importTemplateCsv` imports workout template rows and creates workouts, exercises, and sets.

`readText` reads a selected file URI.

`parseCsv` splits a CSV file into rows.

`parseCsvLine` handles one CSV line, including quoted commas and escaped quotes.

`headerIndex` finds a header column by accepted names.

`parseDateTime` converts imported date text into epoch milliseconds.

`launch` is a small helper for `viewModelScope.launch`.

## Phone Repository Functions

### Template reads
File: `app/src/main/java/com/example/gymtrackerphone/data/repository/WorkoutRepository.kt`

`getWorkoutById`, `getWorkoutIdsByName`, `getExercisesForWorkout`, and `getSetsForExercise` read template data from Room.

`getRecentCompletedWorkouts` loads previous completed workouts by template ID.

`getRecentCompletedWorkoutsByName` loads previous completed workouts by normalized workout name. It is now mostly a fallback for imported data.

### Template writes
File: `app/src/main/java/com/example/gymtrackerphone/data/repository/WorkoutRepository.kt`

`addWorkout`, `updateWorkout`, `deleteWorkout`, `addExercise`, `deleteExercise`, `addSet`, `deleteSet`, `updateRepRange`, `updateWeight`, and `updateRest` change template data.

`importTemplateWorkouts` bulk-imports template workouts from parsed CSV rows.

### Completed workout writes
File: `app/src/main/java/com/example/gymtrackerphone/data/repository/WorkoutRepository.kt`

`addCompletedWorkout` stores a completed workout transactionally. It prevents duplicates, inserts workout/exercise/set records, and updates template weights from completed values.

`updateCompletedSet` edits a saved completed set.

`deleteCompletedWorkout` deletes a completed workout.

`updateTemplateFromCompleted` updates matching template set weights after a completed workout is saved.

## Phone DAO Functions

File: `app/src/main/java/com/example/gymtrackerphone/data/dao/WorkoutDao.kt`

The DAO is the raw Room SQL layer.

Workout template methods: `insertWorkout`, `insertWorkoutReturningId`, `updateWorkout`, `getWorkoutById`, `getWorkoutIdsByName`, `getWorkoutsWithExercises`, and `deleteWorkoutById`.

Exercise methods: `insertExercise`, `insertExerciseReturningId`, `getExercisesForWorkout`, and `deleteExerciseById`.

Set methods: `insertSet`, `getSetsForExercise`, `getNextSetOrderIndex`, `deleteSetById`, `updateRepRange`, `updateWeight`, `updateWeightForSetOrder`, and `updateRest`.

Completed workout methods: `insertCompletedWorkout`, `insertCompletedExercise`, `insertCompletedSets`, `updateCompletedSet`, `getCompletedWorkouts`, `deleteCompletedWorkoutById`, `findCompletedWorkoutId`, `getRecentCompletedWorkouts`, and `getRecentCompletedWorkoutsByName`.

## Phone Mapper And Sync Functions

### Room to UI mappers
File: `app/src/main/java/com/example/gymtrackerphone/data/mapper/Mappers.kt`

`WorkoutWithExercises.toUi`, `ExerciseWithSets.toUi`, `CompletedWorkoutWithExercises.toUi`, and `CompletedExerciseWithSets.toUi` convert Room relation objects into UI models.

### `CompletedWorkoutDto.toEntities`
File: `app/src/main/java/com/example/gymtrackerphone/sync/mapper/CompletedWorkoutMapper.kt`

Converts a received completed workout DTO into Room-style relation objects. This is mostly a mapper utility; current saving logic mainly uses repository inserts.

### `mapToTemplate`
File: `app/src/main/java/com/example/gymtrackerphone/sync/mapper/WorkoutTemplateMapper.kt`

Converts a UI workout template into a DTO. Some current send logic uses `WorkoutViewModel.buildWorkoutTemplate` instead.

### `WorkoutSender.sendWorkout`
File: `app/src/main/java/com/example/gymtrackerphone/sync/sender/WorkoutSender.kt`

Serializes a workout template to JSON and sends it to the watch using Wear OS DataClient.

### `WorkoutSyncManager.sendWorkout`
File: `app/src/main/java/com/example/gymtrackerphone/sync/WorkoutSyncManager.kt`

Wrapper around workout sending. It appears to be an older or alternate sync entry point.

### `WorkoutResultReceiverService.onDataChanged`
File: `app/src/main/java/com/example/gymtrackerphone/sync/receiver/WorkoutResultReceiverService.kt`

Receives completed workout data from the watch, decodes it, stores it through the repository, shows a phone notification if inserted, and sends an ACK.

### `WorkoutResultReceiverService.sendAckMessage`
File: `app/src/main/java/com/example/gymtrackerphone/sync/receiver/WorkoutResultReceiverService.kt`

Sends an ACK back to the watch using both MessageClient and DataClient, so the watch can clear pending sync state.

### `WorkoutResultReceiverService.showWorkoutNotification`
File: `app/src/main/java/com/example/gymtrackerphone/sync/receiver/WorkoutResultReceiverService.kt`

Shows a phone notification after a completed workout is saved.

### `WorkoutResultReceiverService.onDestroy`
File: `app/src/main/java/com/example/gymtrackerphone/sync/receiver/WorkoutResultReceiverService.kt`

Cancels the service coroutine scope.

## Watch App Entry Points

### `GymTrackerWatchApp.onCreate`, `onStart`, `onStop`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/GymTrackerWatchApp.kt`

Tracks whether the watch app process is visible using lifecycle callbacks.

### `MainActivity.onCreate`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/MainActivity.kt`

Attaches app context to the active workout ViewModel, enables wake-on-alarm behavior, asks notification permission when needed, and sets watch Compose content.

### `MainActivity.onResume` and `onPause`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/MainActivity.kt`

Mark foreground visibility and start/stop foreground rest countdown work.

### `MainActivity.dispatchGenericMotionEvent`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/MainActivity.kt`

Handles watch rotary/bezel input and routes it to `ActiveWorkoutViewModel.handleRotaryDelta`.

## Watch Navigation And Screens

### `WatchNavGraph`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/navigation/WatchNavGraph.kt`

Controls watch routes: idle, incoming workout, active workout flow, and completed workout screen.

### `IdleScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/IdleScreen.kt`

Default watch screen when no workout is loaded.

### `IncomingWorkoutScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/IncomingWorkoutScreen.kt`

Shows a received workout before starting. Start marks the workout active; cancel clears it.

### `WorkoutFlowScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/WorkoutFlowScreen.kt`

Switches between exercise, reps, weight, rest, and complete screens based on `WorkoutUiState`. It also owns the back-button end/cancel overlay.

### `ExerciseScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/ExerciseScreen.kt`

Shows current exercise, set number, target reps/weight, and previous session history. "Complete set" moves to reps confirmation.

### `ConfirmRepsScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/ConfirmRepsScreen.kt`

Lets you adjust completed reps with buttons or rotary input, then moves to weight confirmation.

### `ConfirmWeightScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/ConfirmWeightScreen.kt`

Lets you adjust completed weight, confirms the set, and transitions to rest.

### `RestScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/RestScreen.kt`

Starts/restores the rest timer, shows remaining seconds and the next set preview, and lets you skip rest.

### `WorkoutCompleteScreen`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/screen/WorkoutCompleteScreen.kt`

Shows the completed workout state and sends it back to the phone.

### `GymTrackerWatchTheme`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/theme/GymTrackerWatchTheme.kt`

Applies the watch Material theme.

### `formatHistorySets` and `formatWeight`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/presentation/util/HistoryFormat.kt`

Convert previous workout set history and weights into compact watch text.

## Watch ActiveWorkoutViewModel Functions

File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/viewmodel/ActiveWorkoutViewModel.kt`

### Screen transitions

`goToExercise`, `goToConfirmReps`, `goToConfirmWeight`, and `goToRest` move between workout screens with guards against invalid or duplicate transitions.

`confirmCurrentSet` confirms reps/weight and moves into rest in one guarded path.

`markStarted` changes a received workout from pending-start to active.

`endWorkoutEarly` marks the workout complete before all sets are done.

`cancelWorkout` clears active, incoming, pending, rest alarm, and local state.

### Editing current set values

`updatePendingReps` and `updatePendingWeight` update temporary reps/weight while you are on confirm screens.

`initPendingForCurrentSet` pre-fills confirm screens from completed values if present, otherwise from target values.

`confirmSet` writes confirmed reps, weight, and completion time into the active workout model.

### Workout conversion and sending

`toCompletedWorkout` converts the active workout into the completed workout model sent to the phone. It only includes sets that have reps, weight, and completion time.

`sendWorkoutAndReset` sends the completed workout, persists it as pending until ACK, and starts retry/ACK watchdog logic.

`tryResendPending` reloads and resends a pending completed workout if the previous send was not acknowledged.

`sendCompleted` calls `WorkoutResultSender.send`.

`startRetryLoop` periodically retries sending a completed workout while waiting for ACK.

`startAckWatchdog` polls pending state briefly so the UI resets quickly after ACK.

`handleAck` responds to phone ACK and clears completed workout state.

`resetAfterAck` clears the local workout after successful sync.

### Current workout helpers

`attachContext` stores app context and restores saved active workout state.

`currentExercise` returns the active exercise by current index.

`currentSet` returns the active set by current exercise and set index.

`upcomingSetPreview` builds the next-set summary shown on the rest screen.

### Rest timer logic

`startRest` starts or restores a rest timer and schedules the exact alarm.

`skipRest` stops rest early and advances to the next set/exercise.

`finishRestNormally` handles rest completion, haptic, and progression.

`advanceAfterRest` records rest result, moves to the next set/exercise, or marks the workout complete.

`remainingRestSeconds` calculates remaining time from monotonic elapsed time.

`syncRestFromClock` reconciles saved rest end time with the current clock.

`currentRestToken` builds a unique token for a workout/exercise/set rest event.

`triggerRestHaptic` vibrates when the app is foregrounded and rest finishes.

`startRestJob` runs the foreground countdown loop.

### App lifecycle and input helpers

`loadWorkout` consumes a received workout template and creates an `ActiveWorkout`.

`handleRotaryDelta` applies rotary input to reps or weight depending on current screen.

`onAppVisible` syncs the timer and restarts foreground countdown.

`onAppHidden` stops the foreground countdown job.

`consumeUiAction` debounces button/screen actions to reduce accidental double-press skips.

`persistState` saves active workout state to SharedPreferences.

`restoreFromStore` restores an active workout after app/process recreation.

## Watch Stores

### `IncomingWorkoutStore`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/store/IncomingWorkoutStore.kt`

`init` loads a saved incoming template into memory.

`store` saves a received workout template to memory and SharedPreferences.

`consume` returns the stored workout and clears it.

`clear` removes the incoming workout state.

### `ActiveWorkoutStore`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/store/ActiveWorkoutStore.kt`

`save` persists active workout state.

`load` restores active workout state.

`clear` removes active workout state.

### `PendingWorkoutStore`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/store/PendingWorkoutStore.kt`

`save` persists a completed workout waiting for ACK.

`load` restores that pending completed workout.

`clear` removes pending sync state.

`hasPending` checks whether a pending completed workout exists.

### `WorkoutAckStore`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/store/WorkoutAckStore.kt`

`signalAck` emits ACK received state.

`consume` resets ACK state after the ViewModel handles it.

### `RestHapticStore`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/RestHapticStore.kt`

`wasFired` checks whether a rest haptic already fired for a token.

`markFired` records that the haptic fired.

`clear` removes rest haptic state.

### `AppVisibilityStore`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/AppVisibilityStore.kt`

`setVisible` records current foreground visibility in memory.

`isVisible` reads that visibility state.

## Watch Sync Functions

### `WorkoutReceiverService.onCreate` and `onDestroy`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/receiver/WorkoutReceiverService.kt`

Lifecycle logging for the watch receiver service.

### `WorkoutReceiverService.onDataChanged`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/receiver/WorkoutReceiverService.kt`

Receives workout templates from phone and ACK data items from phone. It stores incoming workouts or clears pending completed workouts after ACK.

### `WorkoutReceiverService.onMessageReceived`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/receiver/WorkoutReceiverService.kt`

Receives ACK messages from the phone and clears pending completed workout state.

### `WorkoutResultSender.send`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/sync/sender/WorkoutResultSender.kt`

Serializes a completed workout and sends it to the phone with Wear OS DataClient.

### `WorkoutTemplateDto.toActiveWorkout`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/domain/mapper/WorkoutMapper.kt`

Converts the phone DTO into the watch's active workout model, including set targets and history.

## Watch Alarm, Haptic, Notification Functions

### `RestAlarmScheduler.pendingIntent`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/RestAlarmScheduler.kt`

Creates the broadcast PendingIntent fired when rest completes.

### `RestAlarmScheduler.showIntent`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/RestAlarmScheduler.kt`

Creates the PendingIntent used by the system alarm UI to open the app.

### `RestAlarmScheduler.schedule`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/RestAlarmScheduler.kt`

Schedules an exact alarm-clock alarm for the rest end time. This is the screen-off reliable path.

### `RestAlarmScheduler.cancel`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/RestAlarmScheduler.kt`

Cancels the scheduled rest alarm.

### `RestAlarmReceiver.onReceive`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/RestAlarmReceiver.kt`

Runs when the rest alarm fires. It prevents duplicate haptics, vibrates if the app is not visible, takes a short wake lock, and opens the app.

### `HapticUtil.vibrate`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/util/HapticUtil.kt`

Runs the watch vibration pattern with alarm audio attributes.

### `WatchNotificationHelper.showIncomingWorkout`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/notifications/WatchNotificationHelper.kt`

Shows a high-priority notification when a workout arrives while the watch app is not visible.

### `WatchNotificationHelper.showRestComplete`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/notifications/WatchNotificationHelper.kt`

Shows a rest-complete notification with vibration. This is currently more of a fallback/older path compared with the alarm receiver path.

### `WatchNotificationHelper.ensureChannel` and `ensureRestChannel`
File: `gymtrackerwatch/src/main/java/com/example/gymtrackerwatch/notifications/WatchNotificationHelper.kt`

Create Android notification channels for incoming workouts and rest timer alerts.

