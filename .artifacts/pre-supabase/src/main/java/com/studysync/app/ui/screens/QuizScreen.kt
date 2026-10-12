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
import com.studysync.app.data.model.LeaderboardUser
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.components.TactileChip
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun QuizScreen(
    viewModel: StudySyncViewModel
) {
    val quizConfig by viewModel.quizConfig.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()

    var isQuizInProgress by remember { mutableStateOf(false) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }

    val sampleQuestions = listOf(
        Pair("What is the primary difference between synchronized and ReentrantLock in Java?", listOf("Intrinsic monitors vs Explicit Lock object", "Thread count limits", "Heap vs Stack allocation", "No difference")),
        Pair("In software engineering, what does a Requirements Traceability Matrix link?", listOf("Use cases to functional test cases", "Database schemas to CSS", "Variables to memory addresses", "Commits to Git branches")),
        Pair("What happens when thread interrupt() is called on a sleeping thread in Java?", listOf("InterruptedException is thrown", "Thread immediately terminates", "Thread ignores it", "Memory is garbage collected"))
    )

    if (isQuizInProgress) {
        val currentQ = sampleQuestions[currentQuestionIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperWhite)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentQuestionIndex + 1} of ${sampleQuestions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray
                    )
                    IconButton(onClick = { isQuizInProgress = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / sampleQuestions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = EagerGreen,
                    trackColor = SurfaceContainer
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = currentQ.first,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurfaceText,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                currentQ.second.forEachIndexed { index, optionText ->
                    val isSelected = selectedOptionIndex == index
                    TactileCard(
                        backgroundColor = if (isSelected) StorybookGreen else PaperWhite,
                        bottomShadowColor = if (isSelected) EagerGreen else SurfaceContainerHigh,
                        onClick = { selectedOptionIndex = index },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedOptionIndex = index },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnSurfaceText
                            )
                        }
                    }
                }
            }

            TactileButton(
                text = if (currentQuestionIndex < sampleQuestions.size - 1) "Next Question" else "Finish Quiz",
                onClick = {
                    if (selectedOptionIndex == 0) score += 10
                    selectedOptionIndex = null
                    if (currentQuestionIndex < sampleQuestions.size - 1) {
                        currentQuestionIndex++
                    } else {
                        isQuizInProgress = false
                        currentQuestionIndex = 0
                    }
                },
                backgroundColor = EagerGreen,
                bottomShadowColor = DarkGreenContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }
    } else {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ADAPTIVE ENGINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryGreen,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "AI Quiz",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Test your knowledge with adaptive AI generation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray
                    )
                }
            }

            // Quick Action Card: Take AI Quiz
            item {
                TactileCard(
                    backgroundColor = PaperWhite,
                    bottomShadowColor = EagerGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(StorybookGreen)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✨ Dynamic Deck",
                                color = DarkGreenContainer,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PencilGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Take AI Quiz",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText
                    )

                    Text(
                        text = "Generate custom revision questions directly from your uploaded lecture notes and study material.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PencilGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TactileButton(
                        text = "Start AI Quiz",
                        onClick = { isQuizInProgress = true },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        backgroundColor = EagerGreen,
                        bottomShadowColor = DarkGreenContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quiz Configuration Setup
            item {
                TactileCard(
                    backgroundColor = SurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quiz Configuration",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SELECT TOPIC",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Requirements", "Java", "Discrete Math", "Microeconomics").forEach { topic ->
                            val selected = quizConfig.subject == topic
                            TactileChip(
                                text = topic,
                                selected = selected,
                                onClick = { viewModel.updateQuizConfig(subject = topic) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "QUANTITY & DIFFICULTY",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Questions: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceText
                            )
                            IconButton(onClick = { if (quizConfig.questionCount > 5) viewModel.updateQuizConfig(questions = quizConfig.questionCount - 5) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                            }
                            Text(
                                text = "${quizConfig.questionCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurfaceText,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { if (quizConfig.questionCount < 30) viewModel.updateQuizConfig(questions = quizConfig.questionCount + 5) }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                            }
                        }

                        Row {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                val sel = quizConfig.difficulty == diff
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (sel) EagerGreen else SurfaceContainer)
                                        .clickable { viewModel.updateQuizConfig(difficulty = diff) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = diff,
                                        color = if (sel) PaperWhite else PencilGray,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                }
            }

            // Leaderboard Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Global Leaderboard",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurfaceText
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SecondaryContainerBlue)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Sprint Term 2",
                                color = OnSecondaryContainerBlue,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        leaderboard.forEach { user ->
                            TactileCard(
                                backgroundColor = if (user.name == "Alex Chen") StorybookGreen else PaperWhite,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${user.rank}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = if (user.rank == 1) EagerGreen else PencilGray,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SecondaryContainerBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.avatarInitial,
                                                color = OnSecondaryContainerBlue,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = OnSurfaceText
                                            )
                                            Text(
                                                text = "🔥 ${user.streakDays} Day Streak",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PencilGray
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${user.points} pts",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
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
