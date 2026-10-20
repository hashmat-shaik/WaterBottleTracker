package com.example.waterbottletracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.example.waterbottletracker.data.WaterRepository
import com.example.waterbottletracker.data.WaterState
import com.example.waterbottletracker.ui.history.HistorySection
import com.example.waterbottletracker.ui.theme.WaterBottleTrackerTheme
import com.example.waterbottletracker.util.formatVolume
import com.example.waterbottletracker.widget.WaterWidget
import com.example.waterbottletracker.workers.MidnightResetWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule the midnight reset worker
        MidnightResetWorker.schedule(this)

        setContent {
            WaterBottleTrackerTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repository = remember { WaterRepository(context) }
    val state by repository.waterStateFlow.collectAsState(initial = WaterState())
    val history by repository.historyFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = { Text("Water Tracker", fontWeight = FontWeight.Bold) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calendar at the top
            HistorySection(history = history, todayGoalMl = state.dailyGoalMl)

            HorizontalDivider()

            // Compressed Dashboard with progress ring and stats
            CompressedDashboardSection(state)

            // Quick Add section
            QuickAddSection(state) { ml ->
                scope.launch {
                    repository.logAmount(ml)
                    WaterWidget().updateAll(context)
                }
            }

            // Bottle Size + Add/Remove Controls
            BottleControlRow(state, state.bottleSizeMl,
                onIncrement = {
                    scope.launch {
                        repository.logAmount(state.bottleSizeMl)
                        WaterWidget().updateAll(context)
                    }
                },
                onDecrement = {
                    scope.launch {
                        repository.undoLastAmount(state.bottleSizeMl)
                        WaterWidget().updateAll(context)
                    }
                }
            )

            HorizontalDivider()

            // Update the Settings call to handle both goal and bottle size
            SettingsSection(state) { newGoal, newSize ->
                scope.launch {
                    repository.updateSettings(newGoal, newSize)
                    WaterWidget().updateAll(context)
                }
            }
        }
    }
}

@Composable
fun CompressedDashboardSection(state: WaterState) {
    val percentage = if (state.dailyGoalMl > 0) {
        (state.mlDrank.toFloat() / state.dailyGoalMl).coerceIn(0f, 1f)
    } else 0f
    val remainingMl = maxOf(0, state.dailyGoalMl - state.mlDrank)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2E))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left: Circular Progress Ring
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                ProgressRingCanvas(progress = percentage)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = formatVolume(state.mlDrank),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "of ${formatVolume(state.dailyGoalMl)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9AA0A8)
                    )
                }
            }

            // Right: Stats
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Goal", value = "${(percentage * 100).toInt()}%", icon = "🥤")
                StatCard(label = "Daily Goal", value = formatVolume(state.dailyGoalMl), icon = "🎯")
                StatCard(label = "Remaining", value = formatVolume(remainingMl), icon = "⏱️")
            }
        }
    }
}

@Composable
private fun ProgressRingCanvas(progress: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val center = this.center
        val radius = minOf(size.width, size.height) / 2 - 8.dp.toPx()
        val strokeWidth = 8.dp.toPx()

        // Track
        drawArc(
            color = Color(0xFF2A2D34),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            ),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            topLeft = center - androidx.compose.ui.geometry.Offset(radius, radius)
        )

        // Progress fill
        if (progress > 0f) {
            drawArc(
                color = Color(0xFF2F8CFF),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                topLeft = center - androidx.compose.ui.geometry.Offset(radius, radius)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1419), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA0A8))
    }
}

@Composable
fun QuickAddSection(state: WaterState, onQuickAdd: (Int) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Quick Add", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Surface(
            color = Color(0xFF1A1F2E),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(250, 500, 750, 1000)) { ml ->
                    QuickAddButton(
                        ml = ml,
                        isSelected = ml == state.lastAmountMl,
                        onClick = { onQuickAdd(ml) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddButton(ml: Int, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF2A2D34)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val labelText = formatVolume(ml)
        Text(
            text = "$labelText",
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Color.White else Color(0xFF9AA0A8),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BottleControlRow(state: WaterState, bottleSizeMl: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: Bottle Size Display
        Surface(
            color = Color(0xFF1A1F2E),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1.2f) // Increased weight (was 2f out of 5f) to expand the box
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bottle Size", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA0A8))
                Text(formatVolume(bottleSizeMl), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Right: +/- Controls
        Surface(
            color = Color(0xFF1A1F2E),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f) // Decreased weight (was 3f out of 5f) to squish the gap
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                // Removed the Spacer(weight=1f) from the middle. This spacedBy handles the gap perfectly.
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0F1419), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Undo", tint = Color.White)
                }

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF2196F3), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }
        }
    }
}

// 3. Add the Bottle Size TextField to Settings
@Composable
fun SettingsSection(state: WaterState, onUpdate: (Int, Int) -> Unit) {
    var goal by remember(state.dailyGoalMl) { mutableStateOf(state.dailyGoalMl.toString()) }
    var size by remember(state.bottleSizeMl) { mutableStateOf(state.bottleSizeMl.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = goal,
            onValueChange = {
                goal = it
                val g = it.toIntOrNull() ?: state.dailyGoalMl
                val s = size.toIntOrNull() ?: state.bottleSizeMl
                onUpdate(g, s)
            },
            label = { Text("Daily Goal (ml)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = size,
            onValueChange = {
                size = it
                val g = goal.toIntOrNull() ?: state.dailyGoalMl
                val s = it.toIntOrNull() ?: state.bottleSizeMl
                onUpdate(g, s)
            },
            label = { Text("Bottle Size (ml)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}