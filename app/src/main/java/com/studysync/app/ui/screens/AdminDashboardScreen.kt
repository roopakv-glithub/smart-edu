package com.studysync.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.studysync.app.data.model.AppUser
import com.studysync.app.data.model.Role
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: StudySyncViewModel,
    onBack: () -> Unit
) {
    val restrictions by viewModel.appRestrictions.collectAsState()
    val users by viewModel.userList.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.registrationNumber.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Console",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperWhite)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperWhite)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Restriction Controls Section (Requirement 2)
            item {
                TactileCard(
                    backgroundColor = SurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Global Restriction Toggles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceText
                            )
                        }

                        Text(
                            text = "Independent controls to manage who can create shared content. Persistent in Supabase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PencilGray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle A: Restrict Tasks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Restrict Shared Tasks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                                Text(
                                    text = if (restrictions.restrictTasks)
                                        "Only Admins & Moderators can create shared tasks."
                                    else
                                        "All verified students can create shared tasks.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (restrictions.restrictTasks) ErrorRed else PencilGray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = restrictions.restrictTasks,
                                onCheckedChange = { viewModel.setTaskRestriction(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = SurfaceContainerHigh
                        )

                        // Toggle B: Restrict Notes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Restrict Shared Notes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                                Text(
                                    text = if (restrictions.restrictNotes)
                                        "Only Admins & Moderators can upload shared notes/syllabuses."
                                    else
                                        "All verified students can upload shared notes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (restrictions.restrictNotes) ErrorRed else PencilGray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = restrictions.restrictNotes,
                                onCheckedChange = { viewModel.setNoteRestriction(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                            )
                        }
                    }
                }
            }

            // User Management Section (Requirement 1)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "User & Moderator Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceText
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(StorybookGreen)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${users.size} Users",
                                color = DarkGreenContainer,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search users by name or reg number...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // User Cards List
            if (filteredUsers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users found matching query.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PencilGray
                        )
                    }
                }
            } else {
                items(filteredUsers, key = { it.id }) { user ->
                    TactileCard(
                        backgroundColor = PaperWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
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
                                            .background(
                                                when (user.role) {
                                                    Role.ADMIN -> TertiaryContainerPurple
                                                    Role.MODERATOR -> SecondaryContainerBlue
                                                    Role.STUDENT -> SurfaceContainer
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = when (user.role) {
                                                Role.ADMIN -> Color(0xFF6B21A8)
                                                Role.MODERATOR -> OnSecondaryContainerBlue
                                                Role.STUDENT -> PencilGray
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurfaceText
                                        )
                                        Text(
                                            text = user.registrationNumber.ifBlank { "ID: ${user.id.take(8)}" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PencilGray
                                        )
                                    }
                                }

                                // Role Badge
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            when (user.role) {
                                                Role.ADMIN -> TertiaryContainerPurple
                                                Role.MODERATOR -> SecondaryContainerBlue
                                                Role.STUDENT -> SurfaceContainer
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = user.role.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = when (user.role) {
                                            Role.ADMIN -> Color(0xFF6B21A8)
                                            Role.MODERATOR -> OnSecondaryContainerBlue
                                            Role.STUDENT -> PencilGray
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Moderator Management Actions (Promote / Revoke)
                            if (user.role != Role.ADMIN) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (user.role == Role.STUDENT) {
                                        OutlinedButton(
                                            onClick = { viewModel.setUserRole(user.id, Role.MODERATOR) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Promote to Moderator", fontSize = 12.sp)
                                        }
                                    } else if (user.role == Role.MODERATOR) {
                                        OutlinedButton(
                                            onClick = { viewModel.setUserRole(user.id, Role.STUDENT) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.RemoveModerator, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Revoke Moderator", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
