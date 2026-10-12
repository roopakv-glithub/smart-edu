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
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun JarvisScreen(
    viewModel: StudySyncViewModel
) {
    val jarvisMessages by viewModel.jarvisMessages.collectAsState()
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
                                .background(EagerGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Online • Connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryGreen,
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
                onClick = { },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

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
                                title = "Schedule a Day",
                                desc = "Plan optimal study blocks based on high-priority tasks.",
                                icon = Icons.Default.CalendarMonth,
                                iconBg = SecondaryContainerBlue,
                                onSelect = { viewModel.sendJarvisQuery("Can you help me schedule my study day optimal blocks?") }
                            )
                        }
                        item {
                            PromptCard(
                                title = "Discuss Studies",
                                desc = "Interactively explore topics using uploaded notes.",
                                icon = Icons.Default.MenuBook,
                                iconBg = StorybookGreen,
                                onSelect = { viewModel.sendJarvisQuery("Let's discuss my Java concurrency and multithreading notes.") }
                            )
                        }
                        item {
                            PromptCard(
                                title = "Ask a Doubt",
                                desc = "Resolve specific questions regarding algorithms or formulas.",
                                icon = Icons.Default.HelpOutline,
                                iconBg = TertiaryContainerPurple,
                                onSelect = { viewModel.sendJarvisQuery("Explain the difference between synchronized blocks and ReentrantLock.") }
                            )
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
                placeholder = { Text("Ask Jarvis anything about your studies...") },
                shape = RoundedCornerShape(24.dp),
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
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PencilGray, modifier = Modifier.size(14.dp))
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
