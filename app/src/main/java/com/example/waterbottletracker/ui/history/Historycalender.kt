package com.example.waterbottletracker.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterbottletracker.data.DayRecord
import com.example.waterbottletracker.util.formatVolume
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.ui.text.style.TextOverflow

private val BgColor = Color(0xFF1A1F2E)
private val TrackColor = Color(0xFF2A2D34)
private val FillColor = Color(0xFF2F8CFF)
private val YellowHighlight = Color(0xFFFFC107)

@Composable
fun HistorySection(
    history: List<DayRecord>,
    todayGoalMl: Int,
    weeksToShow: Int = 12
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<DayRecord?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val recordMap = remember(history) { history.associateBy { it.date } }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = BgColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with month/date, navigation, and expand button
                CalendarHeader(
                    currentMonth = currentMonth,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Day abbreviations
                DayLabels()

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar grid - compressed or expanded
                if (isExpanded) {
                    ExpandedCalendarGrid(
                        month = currentMonth,
                        recordMap = recordMap,
                        defaultGoalMl = todayGoalMl,
                        onDayClick = { day -> selectedDay = day }
                    )
                } else {
                    CompressedCalendarGrid(
                        recordMap = recordMap,
                        defaultGoalMl = todayGoalMl,
                        onDayClick = { day -> selectedDay = day }
                    )
                }
            }
        }
    }

    selectedDay?.let { day ->
        DayDetailDialog(day = day, onDismiss = { selectedDay = null })
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Calendar icon + month label — weight(1f) so it takes remaining space
        // but never squeezes the buttons on the right
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("📅", fontSize = 20.sp)
            }
            Text(
                currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium, // Shrunk from headlineSmall
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Nav + expand buttons — only show prev/next in expanded view
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isExpanded) {
                NavButton(onClick = onPreviousMonth, icon = Icons.Default.ChevronLeft, desc = "Previous month")
                NavButton(onClick = onNextMonth, icon = Icons.Default.ChevronRight, desc = "Next month")
            }
            NavButton(onClick = onToggleExpand, icon = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, desc = if (isExpanded) "Collapse" else "Expand")
        }
    }
}

@Composable
private fun NavButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DayLabels() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
            Text(
                day,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9AA0A8),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CompressedCalendarGrid(
    recordMap: Map<LocalDate, DayRecord>,
    defaultGoalMl: Int,
    onDayClick: (DayRecord) -> Unit
) {
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() % 7)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (dayOffset in 0..6) {
            val date = startOfWeek.plusDays(dayOffset.toLong())
            val record = recordMap[date]

            CalendarDayCell(
                date = date,
                record = record,
                defaultGoalMl = defaultGoalMl,
                isToday = date == today,
                modifier = Modifier.weight(1f),
                onClick = { onDayClick(record ?: DayRecord(date, 0, defaultGoalMl)) }
            )
        }
    }
}

@Composable
private fun ExpandedCalendarGrid(
    month: YearMonth,
    recordMap: Map<LocalDate, DayRecord>,
    defaultGoalMl: Int,
    onDayClick: (DayRecord) -> Unit
) {
    val firstDay = month.atDay(1)
    val lastDay = month.atEndOfMonth()
    val startDate = firstDay.minusDays(firstDay.dayOfWeek.value.toLong() % 7)
    val today = LocalDate.now()

    val days = mutableListOf<LocalDate>()
    var current = startDate
    while (current <= lastDay.plusDays(6)) {
        days.add(current)
        current = current.plusDays(1)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (weekIndex in 0 until (days.size / 7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (dayIndex in 0..6) {
                    val date = days[weekIndex * 7 + dayIndex]
                    val record = recordMap[date]
                    val isInMonth = date.month == month.month && date.year == month.year

                    CalendarDayCell(
                        date = date,
                        record = record,
                        defaultGoalMl = defaultGoalMl,
                        isToday = date == today,
                        isInMonth = isInMonth,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isInMonth) {
                                onDayClick(record ?: DayRecord(date, 0, defaultGoalMl))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    record: DayRecord?,
    defaultGoalMl: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    isInMonth: Boolean = true,
    onClick: () -> Unit
) {
    val mlDrank = record?.mlDrank ?: 0

    // Determine colors based on state
    val bgColor = when {
        !isInMonth -> Color.Transparent
        isToday -> YellowHighlight
        mlDrank > 0 -> Color.Black
        else -> Color.White
    }

    val textColor = when {
        !isInMonth -> Color(0xFF9AA0A8)
        isToday -> Color.Black
        mlDrank > 0 -> Color.White
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = isInMonth) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DayDetailDialog(day: DayRecord, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Text(
                day.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { day.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = FillColor,
                    trackColor = TrackColor
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Consumed:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatVolume(day.mlDrank),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = FillColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Goal:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatVolume(day.goalMl),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Progress:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${(day.rawProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (day.rawProgress >= 1f) Color(0xFF4CAF50) else FillColor
                        )
                    }
                }
            }
        }
    )
}