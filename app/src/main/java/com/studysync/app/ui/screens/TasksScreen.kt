package com.studysync.app.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import com.studysync.app.data.mapper.PresentationMappers
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var showAddTaskDialog by remember { mutableStateOf(false) }

    val filteredTasks = when (taskFilter) {
        "ACTIVE" -> tasks.filter { !it.isCompleted }
        "DONE" -> tasks.filter { it.isCompleted }
        else -> tasks
    }

    val doneCount = tasks.count { it.isCompleted }
    val progressFraction = if (tasks.isNotEmpty()) doneCount.toFloat() / tasks.size else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Action Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tasks",
                            style = MaterialTheme.typography.headlineLarge,
                            color = OnSurfaceText,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Fall Semester Roadmap",
                            style = MaterialTheme.typography.bodySmall,
                            color = PencilGray
                        )
                    }

                    TactileButton(
                        text = "+ Add Task",
                        onClick = { showAddTaskDialog = true },
                        backgroundColor = EagerGreen,
                        bottomShadowColor = DarkGreenContainer
                    )
                }
            }

            // Filter Chips
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
                        text = "Done (${doneCount})",
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
                    onToggleComplete = { viewModel.toggleTaskCompleted(task.id) }
                )
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            pods = viewModel.studyPods.collectAsState().value,
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
    onToggleComplete: () -> Unit
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
            // Checkbox Circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) EagerGreen else SurfaceContainerHighest)
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = PaperWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = priorityText,
                            color = priorityFg,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(0.45f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PencilGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.dueDate,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = PencilGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (task.isCompleted) PencilGray else OnSurfaceText,
                    fontSize = 16.sp,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PencilGray,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.category,
                        color = OnSurfaceText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    pods: List<com.studysync.app.data.model.StudyPod>,
    onAddTask: (String, String, String, TaskPriority, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var personal by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(java.text.SimpleDateFormat("dd/MM/yy",java.util.Locale.ENGLISH).format(java.util.Date())) }
    var dueTime by remember { mutableStateOf("05:00 PM") }
    var priority by remember { mutableStateOf(TaskPriority.STANDARD) }
    val wordCount=PresentationMappers.wordCount(title)
    val choice=pods.firstOrNull { it.id==category }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add New Task")},text={
        Column(Modifier.fillMaxWidth().heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value=title,onValueChange={if(PresentationMappers.wordCount(it)<=25 && it.length<=2000)title=it},label={Text("Task title")},supportingText={Text("$wordCount / 25 words")},maxLines=3,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=description,onValueChange={description=it},label={Text("Description")},minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=personal || category=="OTHERS",enabled=category!="OTHERS",onCheckedChange={personal=it})
                Text("Personal task")
            }
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {Text(if(category=="OTHERS") "Others" else choice?.name ?: "Choose subject")}
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    pods.forEach {pod->DropdownMenuItem(text={Column{Text(pod.name);if(pod.teacher.isNotBlank())Text(pod.teacher,style=MaterialTheme.typography.bodySmall)}},onClick={category=pod.id;expanded=false})}
                    DropdownMenuItem(text={Text("Others")},onClick={category="OTHERS";expanded=false})
                }
            }
            if(choice!=null) Text(PresentationMappers.subjectCode(choice.courseCode),style=MaterialTheme.typography.bodySmall)
            if(category=="OTHERS") Text("Others tasks are visible only to you.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=dueDate,onValueChange={dueDate=it},label={Text("Date (dd/mm/yy)")},singleLine=true,modifier=Modifier.weight(0.62f))
                OutlinedTextField(value=dueTime,onValueChange={dueTime=it},label={Text("Time")},supportingText={Text("hh:mm AM/PM")},singleLine=true,modifier=Modifier.weight(0.38f))
            }
            Text("Priority",style=MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                TaskPriority.entries.forEach {p->FilterChip(selected=priority==p,onClick={priority=p},label={Text(p.name.lowercase().replaceFirstChar{it.uppercase()})})}
            }
        }
    },confirmButton={TextButton(enabled=title.isNotBlank() && wordCount<=25 && category.isNotBlank() && dueDate.isNotBlank() && dueTime.isNotBlank(),onClick={onAddTask(title.trim(),description,category,priority,dueDate,dueTime,personal || category=="OTHERS")}) {Text("Save task")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
