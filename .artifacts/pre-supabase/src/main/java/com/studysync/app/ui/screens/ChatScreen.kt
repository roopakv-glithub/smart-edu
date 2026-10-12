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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun ChatScreen(
    viewModel: StudySyncViewModel
) {
    val pods by viewModel.studyPods.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var inputMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(StorybookGreen)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "4 Unread",
                            color = DarkGreenContainer,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "New Conversation",
                        tint = OnSurfaceText
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations or peers...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PencilGray) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Active Study Pods Carousel
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE STUDY PODS",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "3 LIVE",
                        style = MaterialTheme.typography.labelLarge,
                        color = SecondaryBlue,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pods, key = { it.id }) { pod ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { }
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(SecondaryContainerBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pod.initials,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = OnSecondaryContainerBlue
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(EagerGreen)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pod.name.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Active Group Chat Messages List
        item {
            Text(
                text = "STUDY GROUP MESSAGES",
                style = MaterialTheme.typography.labelLarge,
                color = PencilGray,
                fontSize = 11.sp
            )
        }

        items(chatMessages, key = { it.id }) { msg ->
            TactileCard(
                backgroundColor = if (msg.isUser) SurfaceContainerLow else PaperWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (msg.isUser) SecondaryContainerBlue else StorybookGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (msg.isUser) "AC" else msg.senderName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (msg.isUser) OnSecondaryContainerBlue else DarkGreenContainer,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg.senderName,
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSurfaceText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = msg.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                color = PencilGray,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceText
                        )
                    }
                }
            }
        }

        // Input Box
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Type a message to study pod...") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.sendChatMessage(inputMessage)
                            inputMessage = ""
                        }
                    },
                    containerColor = EagerGreen,
                    contentColor = PaperWhite
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
