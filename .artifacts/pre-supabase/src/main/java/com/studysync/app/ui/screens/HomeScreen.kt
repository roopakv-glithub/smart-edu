package com.studysync.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.components.TactileStatCard
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun HomeScreen(
    viewModel: StudySyncViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToJarvis: () -> Unit,
    onNavigateToNotes: () -> Unit
) {
    val userName by viewModel.userName.collectAsState()
    val userStreak by viewModel.userStreak.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val scheduleEvents by viewModel.scheduleEvents.collectAsState()

    val activeCount = tasks.count { !it.isCompleted }
    val doneCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Greeting & Motivation Bar
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WELCOME BACK,",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurfaceText,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = PencilGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "B.Sc Computer Science • Term 4",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray
                    )
                }
            }
        }

        // Task Overview Cards
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Task Overview",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText
                    )
                    Text(
                        text = "Details →",
                        style = MaterialTheme.typography.labelLarge,
                        color = SecondaryBlue,
                        modifier = Modifier.clickable { onNavigateToTasks() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TactileStatCard(
                        number = "%02d".format(activeCount),
                        label = "Active",
                        badgeText = "In Flight",
                        badgeColor = StorybookGreen,
                        badgeTextColor = DarkGreenContainer,
                        numberColor = EagerGreen,
                        modifier = Modifier.weight(1f)
                    )
                    TactileStatCard(
                        number = "$doneCount",
                        label = "Done",
                        badgeText = "${if (totalCount > 0) (doneCount * 100 / totalCount) else 0}% Rate",
                        badgeColor = SurfaceContainer,
                        badgeTextColor = OnSurfaceText,
                        modifier = Modifier.weight(1f)
                    )
                    TactileStatCard(
                        number = "$totalCount",
                        label = "Total",
                        badgeText = "Target 30",
                        badgeColor = SurfaceContainerLow,
                        badgeTextColor = PencilGray,
                        numberColor = PencilGray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Jarvis AI Tactile Banner
        item {
            TactileCard(
                backgroundColor = Color(0xFFF4FBF0),
                bottomShadowColor = Color(0xFFC4E8AD),
                onClick = onNavigateToJarvis,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StorybookGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CO-PILOT MODE",
                                style = MaterialTheme.typography.labelLarge,
                                color = DarkGreenContainer,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Ask Jarvis AI",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurfaceText
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = EagerGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Jarvis is ready to assist with coursework, schedule optimization, and revision doubts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PencilGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                TactileButton(
                    text = "Ask Jarvis",
                    onClick = onNavigateToJarvis,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Upcoming Schedule Agenda
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Upcoming Schedule",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurfaceText
                        )
                        Text(
                            text = "Today's Agenda",
                            style = MaterialTheme.typography.bodySmall,
                            color = PencilGray
                        )
                    }

                    Text(
                        text = "View Schedule →",
                        style = MaterialTheme.typography.labelLarge,
                        color = SecondaryBlue,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigateToSchedule() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scheduleEvents.take(3).forEach { event ->
                        TactileCard(
                            backgroundColor = PaperWhite,
                            bottomShadowColor = SurfaceContainerHigh,
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
    }
}
