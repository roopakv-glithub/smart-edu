package com.studysync.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun SettingsScreen(
    viewModel: StudySyncViewModel,
    onSignOut: () -> Unit
) {
    val userName by viewModel.userName.collectAsState()
    val vibrations by viewModel.tactileVibrations.collectAsState()
    val aiCopilot by viewModel.aiCopilotEnabled.collectAsState()
    val notifications by viewModel.notificationsEnabled.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurfaceText,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Preferences, profile, and app configuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = PencilGray
                )
            }
        }

        // Student Profile Header Card
        item {
            TactileCard(
                backgroundColor = SurfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SecondaryContainerBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AC",
                                style = MaterialTheme.typography.headlineMedium,
                                color = OnSecondaryContainerBlue
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurfaceText
                            )
                            Text(
                                text = "alex.chen@university.edu",
                                style = MaterialTheme.typography.bodySmall,
                                color = PencilGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(StorybookGreen)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Focus Plan • Term 4",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkGreenContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = SecondaryBlue)
                    }
                }
            }
        }

        // Tactile & AI Preferences
        item {
            Column {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelLarge,
                    color = PencilGray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                TactileCard(
                    backgroundColor = PaperWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Vibrations Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = PrimaryGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Tactile Press Feedback", style = MaterialTheme.typography.labelLarge, color = OnSurfaceText)
                                Text("Haptic vibrations on button clicks", style = MaterialTheme.typography.bodySmall, color = PencilGray)
                            }
                        }
                        Switch(
                            checked = vibrations,
                            onCheckedChange = { viewModel.toggleTactileVibrations() },
                            colors = SwitchDefaults.colors(checkedThumbColor = EagerGreen)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceContainer)

                    // AI Co-Pilot Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = SecondaryBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Jarvis AI Co-Pilot Mode", style = MaterialTheme.typography.labelLarge, color = OnSurfaceText)
                                Text("Contextual study suggestions", style = MaterialTheme.typography.bodySmall, color = PencilGray)
                            }
                        }
                        Switch(
                            checked = aiCopilot,
                            onCheckedChange = { viewModel.toggleAiCopilot() },
                            colors = SwitchDefaults.colors(checkedThumbColor = EagerGreen)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceContainer)

                    // Notifications Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = TertiaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Study Reminders", style = MaterialTheme.typography.labelLarge, color = OnSurfaceText)
                                Text("Deadline alerts & streak reminders", style = MaterialTheme.typography.bodySmall, color = PencilGray)
                            }
                        }
                        Switch(
                            checked = notifications,
                            onCheckedChange = { viewModel.toggleNotifications() },
                            colors = SwitchDefaults.colors(checkedThumbColor = EagerGreen)
                        )
                    }
                }
            }
        }

        // Account & Cloud Sync
        item {
            Column {
                Text(
                    text = "CLOUD & DATA SYNC",
                    style = MaterialTheme.typography.labelLarge,
                    color = PencilGray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                TactileCard(
                    backgroundColor = PaperWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Automatic Cloud Backup", style = MaterialTheme.typography.labelLarge, color = OnSurfaceText)
                                Text("Last synced: 5 minutes ago", style = MaterialTheme.typography.bodySmall, color = PencilGray)
                            }
                        }
                        TextButton(onClick = { }) {
                            Text("Sync Now", color = SecondaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sign Out Button
        item {
            Spacer(modifier = Modifier.height(8.dp))

            TactileButton(
                text = "Sign Out",
                onClick = {
                    viewModel.logout()
                    onSignOut()
                },
                backgroundColor = ErrorRed,
                bottomShadowColor = OnErrorContainerRed,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
