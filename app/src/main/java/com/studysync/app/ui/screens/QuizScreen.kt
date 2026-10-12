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
import com.studysync.app.data.model.QuizQuestion
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
    val courses by viewModel.courses.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val aiQuestions by viewModel.aiQuizQuestions.collectAsState()
    val isGenerating by viewModel.isGeneratingQuiz.collectAsState()
    val generationError by viewModel.quizGenerationError.collectAsState()

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var answerSubmitted by remember { mutableStateOf(false) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    // When new AI questions arrive, reset quiz progress
    LaunchedEffect(aiQuestions) {
        if (aiQuestions.isNotEmpty()) {
            currentQuestionIndex = 0
            score = 0
            selectedOptionIndex = null
            answerSubmitted = false
            isQuizCompleted = false
        }
    }

    if (isGenerating) {
        // Generating AI Quiz State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperWhite)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            TactileCard(
                backgroundColor = PaperWhite,
                bottomShadowColor = StorybookGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(StorybookGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Generating AI Quiz",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Powered by NVIDIA Nemotron for \"${quizConfig.subject}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PencilGray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CircularProgressIndicator(color = EagerGreen)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Analyzing curriculum & generating questions...",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    } else if (isQuizCompleted && aiQuestions.isNotEmpty()) {
        // Quiz Completed Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperWhite)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TactileCard(
                backgroundColor = PaperWhite,
                bottomShadowColor = StorybookGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StorybookGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Quiz Finished!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Topic: ${quizConfig.subject}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PencilGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$score / ${aiQuestions.size * 10} Points",
                        style = MaterialTheme.typography.headlineLarge,
                        color = EagerGreen,
                        fontWeight = FontWeight.ExtraBold
                    )

                    val accuracy = if (aiQuestions.isNotEmpty()) (score * 10) / aiQuestions.size else 0
                    Text(
                        text = "Accuracy: $accuracy%",
                        style = MaterialTheme.typography.labelLarge,
                        color = DarkGreenContainer
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TactileButton(
                        text = "Take Another Quiz",
                        onClick = {
                            viewModel.clearAiQuiz()
                            isQuizCompleted = false
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        backgroundColor = EagerGreen,
                        bottomShadowColor = DarkGreenContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else if (aiQuestions.isNotEmpty() && currentQuestionIndex < aiQuestions.size) {
        // Active Quiz Questions UI
        val currentQ = aiQuestions[currentQuestionIndex]

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
                    Column {
                        Text(
                            text = "Question ${currentQuestionIndex + 1} of ${aiQuestions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = PencilGray
                        )
                        Text(
                            text = "Topic: ${quizConfig.subject}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { viewModel.clearAiQuiz() }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / aiQuestions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = EagerGreen,
                    trackColor = SurfaceContainer
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = currentQ.question,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceText,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                currentQ.options.forEachIndexed { index, optionText ->
                    val isSelected = selectedOptionIndex == index
                    val isCorrect = index == currentQ.correctIndex

                    val cardBg = when {
                        answerSubmitted && isCorrect -> StorybookGreen
                        answerSubmitted && isSelected && !isCorrect -> Color(0xFFFFE0E0)
                        isSelected -> StorybookGreen
                        else -> PaperWhite
                    }

                    val cardShadow = when {
                        answerSubmitted && isCorrect -> EagerGreen
                        answerSubmitted && isSelected && !isCorrect -> ErrorRed
                        isSelected -> EagerGreen
                        else -> SurfaceContainerHigh
                    }

                    TactileCard(
                        backgroundColor = cardBg,
                        bottomShadowColor = cardShadow,
                        onClick = {
                            if (!answerSubmitted) {
                                selectedOptionIndex = index
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!answerSubmitted) {
                                        selectedOptionIndex = index
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (answerSubmitted && currentQ.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "💡 Explanation",
                                style = MaterialTheme.typography.labelLarge,
                                color = DarkGreenContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentQ.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceText
                            )
                        }
                    }
                }
            }

            if (!answerSubmitted) {
                TactileButton(
                    text = "Submit Answer",
                    onClick = {
                        if (selectedOptionIndex != null) {
                            answerSubmitted = true
                            if (selectedOptionIndex == currentQ.correctIndex) {
                                score += 10
                            }
                        }
                    },
                    enabled = selectedOptionIndex != null,
                    backgroundColor = EagerGreen,
                    bottomShadowColor = DarkGreenContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            } else {
                TactileButton(
                    text = if (currentQuestionIndex < aiQuestions.size - 1) "Next Question" else "Finish Quiz",
                    onClick = {
                        if (currentQuestionIndex < aiQuestions.size - 1) {
                            currentQuestionIndex++
                            selectedOptionIndex = null
                            answerSubmitted = false
                        } else {
                            isQuizCompleted = true
                        }
                    },
                    backgroundColor = EagerGreen,
                    bottomShadowColor = DarkGreenContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }
        }
    } else {
        // Quiz Configuration & Setup Dashboard
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
                            text = "AI ADAPTIVE ENGINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryGreen,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "AI Quiz Generator",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Generate quizzes tailored to your courses and pending tasks with NVIDIA Nemotron.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray
                    )
                }
            }

            if (generationError != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorContainerRed)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = generationError ?: "",
                                color = OnErrorContainerRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Quick Action Card: Start AI Quiz
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
                                text = "✨ NVIDIA Nemotron AI",
                                color = DarkGreenContainer,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PrimaryGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Quiz on \"${quizConfig.subject}\"",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText
                    )

                    Text(
                        text = "Generates ${quizConfig.questionCount} ${quizConfig.difficulty} multiple-choice questions grounded in course syllabus and pending assignments.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PencilGray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TactileButton(
                        text = "Start AI Quiz",
                        onClick = {
                            viewModel.generateAiQuiz(
                                subject = quizConfig.subject,
                                questionCount = quizConfig.questionCount,
                                difficulty = quizConfig.difficulty
                            )
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        backgroundColor = EagerGreen,
                        bottomShadowColor = DarkGreenContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Enrolled Subjects from Database / Supabase
            if (courses.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ENROLLED SUBJECTS (SUPABASE / VTOP)",
                            style = MaterialTheme.typography.labelLarge,
                            color = PencilGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(courses) { course ->
                                val selected = quizConfig.subject.equals(course.title, ignoreCase = true) || quizConfig.subject.equals(course.courseCode, ignoreCase = true)
                                TactileChip(
                                    text = "${course.courseCode} · ${course.title.take(18)}",
                                    selected = selected,
                                    onClick = { viewModel.updateQuizConfig(subject = course.title) }
                                )
                            }
                        }
                    }
                }
            }

            // Pending Tasks from Supabase
            if (tasks.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ASSIGNED TASKS TOPICS",
                            style = MaterialTheme.typography.labelLarge,
                            color = PencilGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tasks.take(6)) { task ->
                                val selected = quizConfig.subject.equals(task.title, ignoreCase = true)
                                TactileChip(
                                    text = "📝 ${task.title.take(20)}",
                                    selected = selected,
                                    onClick = { viewModel.updateQuizConfig(subject = task.title) }
                                )
                            }
                        }
                    }
                }
            }

            // General Subject Quick Chips
            item {
                Column {
                    Text(
                        text = "COMMON SUBJECTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("Operating Systems", "Java Concurrency", "Database Management", "Computer Networks", "Software Engineering", "Discrete Mathematics")) { topic ->
                            val selected = quizConfig.subject == topic
                            TactileChip(
                                text = topic,
                                selected = selected,
                                onClick = { viewModel.updateQuizConfig(subject = topic) }
                            )
                        }
                    }
                }
            }

            // Quantity & Difficulty Card
            item {
                TactileCard(
                    backgroundColor = SurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quiz Preferences",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                            IconButton(onClick = { if (quizConfig.questionCount > 3) viewModel.updateQuizConfig(questions = quizConfig.questionCount - 1) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                            }
                            Text(
                                text = "${quizConfig.questionCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurfaceText,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { if (quizConfig.questionCount < 10) viewModel.updateQuizConfig(questions = quizConfig.questionCount + 1) }) {
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
                            text = "StudySync Leaderboard",
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
                                text = "Sprint Active",
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
                                backgroundColor = PaperWhite,
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
