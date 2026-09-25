package com.morningrefresh.app

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.morningrefresh.app.data.AlarmEntity
import com.morningrefresh.app.data.TaskEntity
import com.morningrefresh.app.data.TaskKind
import com.morningrefresh.app.data.WeeklyGoalEntity
import com.morningrefresh.app.ui.MorningUiState
import com.morningrefresh.app.ui.MorningViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val Ink = Color(0xFF081426)
private val Panel = Color(0xFF11243E)
private val PanelAlt = Color(0xFF17304E)
private val Accent = Color(0xFFF6C945)
private val Mint = Color(0xFF7DE2B8)
private val TextMuted = Color(0xFFA9B6C7)

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val app = application as MorningRefreshApplication
        val initialTab = if (intent?.getBooleanExtra(EXTRA_OPEN_CHECK_IN, false) == true) 2 else 0
        setContent {
            MorningRefreshTheme {
                val vm: MorningViewModel = viewModel(
                    factory = MorningViewModel.Factory(
                        repository = app.repository,
                        settingsStore = app.settingsStore,
                        alarmScheduler = app.alarmScheduler,
                    ),
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                MorningRefreshApp(state, vm, initialTab)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_CHECK_IN = "open_check_in"
    }
}

@Composable
private fun MorningRefreshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Ink,
            background = Ink,
            surface = Panel,
            onSurface = Color.White,
            secondary = Mint,
        ),
        content = content,
    )
}

@Composable
private fun MorningRefreshApp(state: MorningUiState, viewModel: MorningViewModel, initialTab: Int) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val message = state.message
    Scaffold(containerColor = Ink) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppHeader(selectedTab)
            if (message != null) {
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    action = { TextButton(onClick = viewModel::clearMessage) { Text("Close") } },
                ) { Text(message) }
            }
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> TodayScreen(state, viewModel)
                    1 -> AlarmScreen(state, viewModel)
                    2 -> CheckInScreen(state, viewModel)
                    else -> GoalsScreen(state, viewModel)
                }
            }
            BottomTabs(selectedTab) { selectedTab = it }
        }
    }
}

@Composable
private fun AppHeader(selectedTab: Int) {
    val titles = listOf("Today", "Alarms", "Morning check-in", "Weekly goals")
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("MORNING REFRESH", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Spacer(Modifier.height(4.dp))
        Text(titles[selectedTab], fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Today", "Alarm", "Check-in", "Goals")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEachIndexed { index, label ->
            Text(
                text = label,
                color = if (selected == index) Accent else TextMuted,
                fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clickable { onSelect(index) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun TodayScreen(state: MorningUiState, viewModel: MorningViewModel) {
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.now()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}",
                color = TextMuted,
            )
        }
        item { NextAlarmCard(state.snapshot.alarms) }
        item { RecommendationCard(state) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Today's plan", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAddTask = true }) { Text("+ Add task") }
            }
        }
        items(state.snapshot.tasks, key = { it.id }) { task ->
            TaskCard(task, viewModel::completeTask)
        }
        item {
            OutlinedButton(onClick = viewModel::simulateDelay, modifier = Modifier.fillMaxWidth()) {
                Text("Simulate a 30-minute delay")
            }
        }
        item { GoalSummary(state.snapshot.goals) }
    }
    if (showAddTask) AddTaskDialog(onDismiss = { showAddTask = false }, onSave = viewModel::addTask)
}

@Composable
private fun NextAlarmCard(alarms: List<AlarmEntity>) {
    val next = alarms.firstOrNull { it.enabled }
    Card(colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("NEXT ALARM", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(next?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "Not set", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Text(next?.label ?: "Set a reliable alarm for tomorrow", color = TextMuted)
            }
            Text(if (next == null) "○" else "●", color = if (next == null) TextMuted else Mint, fontSize = 32.sp)
        }
    }
}

@Composable
private fun RecommendationCard(state: MorningUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today's refresh", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (state.recommendation.score == 0) "—" else "${state.recommendation.score}/100", color = Accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(state.recommendation.title, color = Accent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(state.recommendation.body, color = TextMuted, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun TaskCard(task: TaskEntity, onToggle: (TaskEntity) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle(task) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle(task) })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.completed) TextMuted else Color.White,
                )
                Text(
                    "${task.startMinutes.toClock()} · ${task.durationMinutes} min",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            }
            Text(
                if (task.kind == TaskKind.FIXED.name) "FIXED" else "FLEX",
                color = if (task.kind == TaskKind.FIXED.name) Accent else Mint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GoalSummary(goals: List<WeeklyGoalEntity>) {
    if (goals.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("This week's goals", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        goals.take(2).forEach { goal ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Text(goal.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("${goal.completedCount}/${goal.targetCount}", color = Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (goal.completedCount.toFloat() / goal.targetCount).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Accent,
                        trackColor = PanelAlt,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmScreen(state: MorningUiState, viewModel: MorningViewModel) {
    val context = LocalContext.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Make tomorrow easier", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("The alarm opens a short morning check-in so the plan can match your real energy.", color = TextMuted, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Set an alarm") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Default snooze", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { viewModel.changeSnooze(-5) }) { Text("−") }
                Text("${state.settings.snoozeMinutes} min", color = Accent)
                TextButton(onClick = { viewModel.changeSnooze(5) }) { Text("+") }
            }
        }
        items(state.snapshot.alarms, key = { it.id }) { alarm ->
            AlarmRow(alarm, viewModel::toggleAlarm)
        }
        item {
            Text("Android may ask for notification and exact-alarm permissions. If exact timing is unavailable, the demo uses the best available idle-safe alarm.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
    if (showTimePicker) {
        TimePickerDialogHost(
            context = context,
            onTimeSelected = { hour, minute -> viewModel.saveAlarm(hour, minute); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun TimePickerDialogHost(
    context: android.content.Context,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val dialog = remember(context) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(hour, minute) },
            7,
            30,
            true,
        ).apply { setOnDismissListener { onDismiss() } }
    }
    LaunchedEffect(dialog) { dialog.show() }
    DisposableEffect(dialog) { onDispose { dialog.dismiss() } }
}

@Composable
private fun AlarmRow(alarm: AlarmEntity, onToggle: (AlarmEntity) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("%02d:%02d".format(alarm.hour, alarm.minute), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Repeats every day · ${alarm.label}", color = TextMuted)
            }
            Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm) })
        }
    }
}

@Composable
private fun CheckInScreen(state: MorningUiState, viewModel: MorningViewModel) {
    var sleep by rememberSaveable { mutableFloatStateOf(7f) }
    var energy by rememberSaveable { mutableFloatStateOf(6f) }
    var mood by rememberSaveable { mutableFloatStateOf(6f) }
    var stress by rememberSaveable { mutableFloatStateOf(4f) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("This takes about 20 seconds. It is not a medical test; it only helps the demo make a more realistic plan.", color = TextMuted, lineHeight = 20.sp)
        }
        item { CheckInSlider("Sleep hours", sleep, 0f..10f, "${sleep.toInt()} h") { sleep = it } }
        item { CheckInSlider("Energy", energy, 0f..10f, energy.toInt().toString()) { energy = it } }
        item { CheckInSlider("Mood", mood, 0f..10f, mood.toInt().toString()) { mood = it } }
        item { CheckInSlider("Stress", stress, 0f..10f, stress.toInt().toString()) { stress = it } }
        item {
            Button(
                onClick = { viewModel.saveCheckIn(sleep.toInt(), energy.toInt(), mood.toInt(), stress.toInt()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Refresh today's plan") }
        }
        item {
            state.snapshot.checkIn?.let { check ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Saved for today", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Readiness score: ${check.readiness}/100", color = Accent)
                        Text("The recommendation is generated locally from these values.", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueLabel: String, onValueChange: (Float) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row {
                Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(valueLabel, color = Accent, fontWeight = FontWeight.Bold)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = range)
        }
    }
}

@Composable
private fun GoalsScreen(state: MorningUiState, viewModel: MorningViewModel) {
    var showAddGoal by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Plan the week, not just today", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Set a repeatable target and let the app add flexible sessions around fixed commitments.", color = TextMuted, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showAddGoal = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Add weekly goal") }
                }
            }
        }
        items(state.snapshot.goals, key = { it.id }) { goal ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(goal.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${goal.completedCount} of ${goal.targetCount} sessions completed", color = TextMuted)
                        }
                        Text("${((goal.completedCount.toFloat() / goal.targetCount) * 100).toInt()}%", color = Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (goal.completedCount.toFloat() / goal.targetCount).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Mint,
                        trackColor = PanelAlt,
                    )
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = { viewModel.scheduleGoalTask(goal) }) { Text("Add a session today") }
                }
            }
        }
    }
    if (showAddGoal) AddGoalDialog(onDismiss = { showAddGoal = false }, onSave = viewModel::addGoal)
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, Int, Int, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00") }
    var duration by remember { mutableStateOf("45") }
    var fixed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Task name") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Start time, e.g. 10:00") }, singleLine = true)
                OutlinedTextField(duration, { duration = it }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = fixed, onCheckedChange = { fixed = it })
                    Text("Fixed-time task")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parts = time.split(":")
                val minutes = ((parts.getOrNull(0)?.toIntOrNull() ?: 10) * 60) + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                onSave(title, minutes, duration.toIntOrNull() ?: 45, fixed)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("Workout") }
    var target by remember { mutableStateOf("3") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add weekly goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Goal") }, singleLine = true)
                OutlinedTextField(target, { target = it }, label = { Text("Times this week") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, target.toIntOrNull() ?: 3); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Int.toClock(): String = "%02d:%02d".format(this / 60, this % 60)
