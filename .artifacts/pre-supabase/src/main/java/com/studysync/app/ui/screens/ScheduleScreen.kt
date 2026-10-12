package com.studysync.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.data.model.ScheduleEvent
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun ScheduleScreen(
    viewModel: StudySyncViewModel
) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val events by viewModel.scheduleEvents.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }

    val filteredEvents = events.filter { it.dateDay == selectedDay }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurfaceText,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Full Month Calendar Grid Card (October 2024)
            item {
                TactileCard(
                    backgroundColor = PaperWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                            Text(
                                text = "October 2024",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurfaceText
                            )
                        }

                        Row {
                            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day of week header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = PencilGray,
                                fontSize = 12.sp,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Full Month Matrix (Weeks 1 to 5: Oct 1 - Oct 31)
                    val weeks = listOf(
                        listOf(null, null, 1, 2, 3, 4, 5),
                        listOf(6, 7, 8, 9, 10, 11, 12),
                        listOf(13, 14, 15, 16, 17, 18, 19),
                        listOf(20, 21, 22, 23, 24, 25, 26),
                        listOf(27, 28, 29, 30, 31, null, null)
                    )

                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { dayNum ->
                                if (dayNum == null) {
                                    Spacer(modifier = Modifier.size(32.dp))
                                } else {
                                    val isSelected = selectedDay == dayNum
                                    val hasEvent = events.any { it.dateDay == dayNum }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) EagerGreen else Color.Transparent)
                                            .clickable { viewModel.setSelectedDay(dayNum) },
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            color = if (isSelected) PaperWhite else OnSurfaceText,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(3.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (hasEvent) (if (isSelected) PaperWhite else EagerGreen) else Color.Transparent
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Events List Header
            item {
                Text(
                    text = "Events for Oct $selectedDay, 2024",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurfaceText
                )
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No scheduled lectures or study sessions for this date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PencilGray
                        )
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    TactileCard(
                        backgroundColor = PaperWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainer)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = event.time,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = OnSurfaceText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = event.duration,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PencilGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(SecondaryContainerBlue)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = event.category,
                                        color = OnSecondaryContainerBlue,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnSurfaceText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "📍 ${event.location}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PencilGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Event Dialog
    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onAddEvent = { title, cat, time ->
                viewModel.addScheduleEvent(title, cat, time)
                showAddEventDialog = false
            }
        )
    }
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAddEvent: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Lecture") }
    var time by remember { mutableStateOf("2:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Schedule Event", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Lecture/Study Pod/Exam)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 2:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TactileButton(
                text = "Save Event",
                onClick = {
                    if (title.isNotBlank()) {
                        onAddEvent(title, category, time)
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PencilGray)
            }
        }
    )
}
