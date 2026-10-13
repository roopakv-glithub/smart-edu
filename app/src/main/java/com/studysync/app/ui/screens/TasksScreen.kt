package com.studysync.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.data.model.Role
import com.studysync.app.data.model.StudyPod
import com.studysync.app.data.model.TaskItem
import com.studysync.app.data.model.TaskPriority
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.components.TactileChip
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun TasksScreen(
    viewModel: StudySyncViewModel
) {
    val taskFilter by viewModel.taskFilter.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val restrictions by viewModel.appRestrictions.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredTasks = when (taskFilter) {
        "ACTIVE" -> tasks.filter { !it.isCompleted }
        "DONE" -> tasks.filter { it.isCompleted }
        else -> tasks
    }

    val doneCount = tasks.count { it.isCompleted }
    val progressFraction = if (tasks.isNotEmpty()) doneCount.toFloat() / tasks.size else 0f

    // Requirement 4: Delete Confirmation Dialog (For Me vs For Everyone)
    if (showDeleteDialog && taskToDelete != null) {
        val task = taskToDelete!!
        val isPrivileged = userRole == Role.ADMIN || userRole == Role.MODERATOR

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                taskToDelete = null
            },
            title = { Text("Delete Task Options", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (task.isPersonal)
                        "Delete this personal task?"
                    else
                        "This is a shared task. You can hide it for yourself or permanently delete it for the entire class."
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!task.isPersonal) {
                        Button(
                            onClick = {
                                viewModel.hideTaskForMe(task.id)
                                showDeleteDialog = false
                                taskToDelete = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                        ) {
                            Text("1. For me (Hide from my view)")
                        }
                    }

                    if (task.isPersonal || isPrivileged) {
                        Button(
                            onClick = {
                                viewModel.deleteTaskGlobally(task.id)
                                showDeleteDialog = false
                                taskToDelete = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text(if (task.isPersonal) "Delete permanently" else "2. For everyone (Global delete)")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showDeleteDialog = false
                            taskToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3. Cancel")
                    }
                }
            },
            dismissButton = {}
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "ACADEMIC TASKS",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryGreen,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Task Center",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (restrictions.restrictTasks && userRole == Role.STUDENT) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF4E5))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🔒 Shared task creation is restricted by admins. Personal tasks available.",
                                color = Color(0xFFB76E00),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TactileChip(
                        text = "All (${tasks.size})",
                        selected = taskFilter == "ALL",
                        onClick = { viewModel.setTaskFilter("ALL") },
                        modifier = Modifier.weight(1f)
                    )
                    TactileChip(
                        text = "Active (${tasks.count { !it.isCompleted }})",
                        selected = taskFilter == "ACTIVE",
                        onClick = { viewModel.setTaskFilter("ACTIVE") },
                        modifier = Modifier.weight(1f)
                    )
                    TactileChip(
                        text = "Done ($doneCount)",
                        selected = taskFilter == "DONE",
                        onClick = { viewModel.setTaskFilter("DONE") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Milestone Progress Card
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(StorybookGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Weekly Milestone",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OnSurfaceText
                                )
                                Text(
                                    text = "$doneCount of ${tasks.size} objectives completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PencilGray
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .width(80.dp)
                                .height(10.dp)
                                .clip(CircleShape),
                            color = EagerGreen,
                            trackColor = SurfaceContainerHighest
                        )
                    }
                }
            }

            // Tasks List
            items(filteredTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { viewModel.toggleTaskCompleted(task.id) },
                    onDeleteClick = {
                        taskToDelete = task
                        showDeleteDialog = true
                    }
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = PrimaryGreen,
            contentColor = PaperWhite
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            pods = viewModel.studyPods.collectAsState().value,
            restrictTasks = restrictions.restrictTasks && userRole == Role.STUDENT,
            onAddTask = { title, desc, cat, priority, dueDate, dueTime, personal ->
                viewModel.addTask(title, desc, cat, priority, dueDate, dueTime, personal)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val (priorityBg, priorityFg, priorityText) = when (task.priority) {
        TaskPriority.HIGH -> Triple(ErrorContainerRed, OnErrorContainerRed, "High")
        TaskPriority.MEDIUM -> Triple(SecondaryContainerBlue, OnSecondaryContainerBlue, "Medium")
        TaskPriority.STANDARD -> Triple(SurfaceContainer, PencilGray, "Standard")
    }

    TactileCard(
        backgroundColor = if (task.isCompleted) SurfaceContainerLow else PaperWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) EagerGreen else SurfaceContainer)
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PaperWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (task.isCompleted) PencilGray else OnSurfaceText,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(priorityBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = priorityText,
                            color = priorityFg,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PencilGray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.dueDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = PencilGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Delete / Hide Button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete or Hide",
                    tint = PencilGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    pods: List<StudyPod>,
    restrictTasks: Boolean,
    onAddTask: (title: String, desc: String, category: String, priority: TaskPriority, dueDate: String, dueTime: String, isPersonal: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPodId by remember { mutableStateOf(if (pods.isNotEmpty() && !restrictTasks) pods.first().id else "OTHERS") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf("15/10/26") }
    var dueTime by remember { mutableStateOf("05:00 PM") }
    var isPersonal by remember { mutableStateOf(restrictTasks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Read Operating Systems Chapter 3") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "TARGET SUBJECT",
                    style = MaterialTheme.typography.labelLarge,
                    color = PencilGray,
                    fontSize = 11.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val available = if (restrictTasks) listOf("OTHERS") else pods.map { it.id } + listOf("OTHERS")
                    available.take(3).forEach { podId ->
                        val label = pods.firstOrNull { it.id == podId }?.courseCode ?: "Personal"
                        val sel = selectedPodId == podId
                        TactileChip(
                            text = label,
                            selected = sel,
                            onClick = {
                                selectedPodId = podId
                                if (podId == "OTHERS") isPersonal = true
                            }
                        )
                    }
                }

                Text(
                    text = "PRIORITY",
                    style = MaterialTheme.typography.labelLarge,
                    color = PencilGray,
                    fontSize = 11.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        val sel = selectedPriority == p
                        TactileChip(
                            text = p.name,
                            selected = sel,
                            onClick = { selectedPriority = p }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date (DD/MM/YY)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Time (HH:MM AM/PM)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TactileButton(
                text = "Save Task",
                enabled = title.isNotBlank(),
                onClick = {
                    onAddTask(
                        title.trim(),
                        description.trim(),
                        selectedPodId,
                        selectedPriority,
                        dueDate.trim(),
                        dueTime.trim(),
                        isPersonal || restrictTasks
                    )
                },
                backgroundColor = PrimaryGreen,
                bottomShadowColor = DarkGreenContainer
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
