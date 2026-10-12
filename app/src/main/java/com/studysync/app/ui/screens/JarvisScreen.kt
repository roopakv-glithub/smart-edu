package com.studysync.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.components.TactileChip
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun JarvisScreen(
    viewModel: StudySyncViewModel
) {
    val jarvisMessages by viewModel.jarvisMessages.collectAsState()
    val isThinking by viewModel.isJarvisThinking.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
    ) {
        // Status Context Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StorybookGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jarvis AI Co-Pilot",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isThinking) SparkBlue else EagerGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isThinking) "NVIDIA Nemotron Thinking..." else "Online • Connected to Supabase",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isThinking) SecondaryBlue else PrimaryGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = { viewModel.clearJarvisChat() },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("New", fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Recommended Prompt Cards Carousel
            item {
                Column {
                    Text(
                        text = "RECOMMENDED OBJECTIVES",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            PromptCard(
                                title = "Schedule My Day",
                                desc = "Optimize study blocks for my high-priority tasks.",
                                icon = Icons.Default.CalendarMonth,
                                iconBg = SecondaryContainerBlue,
                                onSelect = { viewModel.sendJarvisQuery("Can you help me create an optimal study schedule for my pending tasks today?") }
                            )
                        }
                        item {
                            PromptCard(
                                title = "Review My Subjects",
                                desc = "Analyze enrolled courses and give study guidance.",
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                iconBg = StorybookGreen,
                                onSelect = { viewModel.sendJarvisQuery("What subjects am I currently taking and what should be my study focus?") }
                            )
                        }
                        item {
                            PromptCard(
                                title = "Pending Tasks Help",
                                desc = "Help plan deadlines and break down assignments.",
                                icon = Icons.AutoMirrored.Filled.HelpOutline,
                                iconBg = TertiaryContainerPurple,
                                onSelect = { viewModel.sendJarvisQuery("Summarize my upcoming tasks and advise which ones I should complete first.") }
                            )
                        }
                    }
                }
            }

            // Quick Course Context Chips
            if (courses.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ASK ABOUT YOUR COURSES",
                            style = MaterialTheme.typography.labelLarge,
                            color = PencilGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(courses) { course ->
                                TactileChip(
                                    text = "📚 ${course.title.take(18)}",
                                    selected = false,
                                    onClick = {
                                        viewModel.sendJarvisQuery("Explain the main concepts and recommended study strategy for ${course.title} (${course.courseCode}).")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Task Context Chips
            if (tasks.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ASK ABOUT PENDING TASKS",
                            style = MaterialTheme.typography.labelLarge,
                            color = PencilGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tasks.take(5)) { task ->
                                TactileChip(
                                    text = "⚡ ${task.title.take(20)}",
                                    selected = false,
                                    onClick = {
                                        viewModel.sendJarvisQuery("Help me complete my task: \"${task.title}\" for course ${task.category}.")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Chat Messages Stream
            items(jarvisMessages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DarkGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PaperWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (msg.isUser) SecondaryBlue else SurfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Column {
                            if (msg.categoryBadge != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(StorybookGreen)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = msg.categoryBadge,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkGreenContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (msg.isUser) PaperWhite else OnSurfaceText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = msg.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (msg.isUser) PaperWhite.copy(alpha = 0.7f) else PencilGray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (msg.isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SecondaryContainerBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AC",
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSecondaryContainerBlue,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Thinking Indicator Bubble
            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DarkGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PaperWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = EagerGreen
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Jarvis is analyzing your subjects & thinking...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PencilGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Query Input Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask Jarvis about your subjects, tasks, or concepts...") },
                shape = RoundedCornerShape(24.dp),
                enabled = !isThinking,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        viewModel.sendJarvisQuery(inputQuery)
                        inputQuery = ""
                    }
                },
                containerColor = EagerGreen,
                contentColor = PaperWhite
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask")
            }
        }
    }
}

@Composable
fun PromptCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    onSelect: () -> Unit
) {
    TactileCard(
        backgroundColor = PaperWhite,
        onClick = onSelect,
        modifier = Modifier.width(200.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PencilGray, modifier = Modifier.size(14.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = OnSurfaceText
        )

        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = PencilGray,
            fontSize = 11.sp
        )
    }
}
