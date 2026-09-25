# Morning Refresh Android Demo

This is the first local Android Studio demo for the Morning Refresh project.

## Included in version 1

- Kotlin and Jetpack Compose Material 3 UI
- AlarmManager alarm scheduling and alarm notifications
- Room database for alarms, tasks, check-ins and weekly goals
- DataStore for small app settings such as snooze length
- Fixed-time and flexible tasks
- One-tap task completion and a simple flexible-task rescheduling rule
- Five-field morning check-in
- Local rule-based daily recommendation engine
- Weekly goals such as three workouts per week
- Boot/time-zone receiver to restore enabled alarms

## Open and run

1. Open the `android` folder in Android Studio.
2. Let Gradle sync the Android Gradle Plugin and AndroidX dependencies.
3. Use an Android 8.0+ emulator or device.
4. Run the `app` configuration.

The app seeds a small plan on the first launch so the Today screen is immediately demonstrable. The `Simulate a 30-minute delay` action is included to show flexible-task rescheduling without waiting for a real late task.

## Demo flow

1. Set an alarm from the Alarm tab.
2. Open Morning check-in and save the sliders.
3. Review the refreshed recommendation on Today.
4. Complete tasks or simulate a delay.
5. Open Weekly goals and add a session to today's flexible plan.

The first demo deliberately keeps recommendations local and deterministic. Online AI, cloud sync and sensor-based wake-up verification are extension points for the next version.

## Demo screenshots

See [第一版demo效果.md](第一版demo效果.md) for the first-version demo flow and screenshots.
