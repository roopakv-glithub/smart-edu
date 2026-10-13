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
import com.studysync.app.data.model.QuizAttempt
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeResultScreen(
    viewModel: StudySyncViewModel,
    onBack: () -> Unit
) {
    val attempts by viewModel.quizAttempts.collectAsState()

    val totalQuizzes = attempts.size
    val totalQuestions = attempts.sumOf { it.totalQuestions }
    val totalCorrect = attempts.sumOf { it.correctAnswers }
    val totalIncorrect = totalQuestions - totalCorrect
    val overallAccuracy = if (totalQuestions > 0) (totalCorrect * 100) / totalQuestions else 0

    // Group by course code for subject-wise accuracy
    val subjectStats = remember(attempts) {
        attempts.groupBy { it.courseCode }.map { (code, attList) ->
            val qCount = attList.sumOf { it.totalQuestions }
            val cCount = attList.sumOf { it.correctAnswers }
            val acc = if (qCount > 0) (cCount * 100) / qCount else 0
            Triple(code, qCount, acc)
        }
    }

    // Identify weak topics (< 60% accuracy)
    val weakTopics = remember(attempts) {
        attempts.filter { it.accuracyPercent < 60 && it.topicTitle.isNotBlank() }
            .map { it.topicTitle }
            .distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Result", fontWeight = FontWeight.Bold)
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
            // Overall Metric Cards
            item {
                TactileCard(
                    backgroundColor = SurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Overall Performance Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceText
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(label = "Accuracy", value = "$overallAccuracy%", color = EagerGreen)
                            MetricItem(label = "Quizzes", value = "$totalQuizzes", color = PrimaryGreen)
                            MetricItem(label = "Answered", value = "$totalQuestions", color = SecondaryBlue)
                            MetricItem(label = "Correct", value = "$totalCorrect", color = DarkGreenContainer)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { overallAccuracy / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = EagerGreen,
                            trackColor = SurfaceContainerHigh
                        )
                    }
                }
            }

            // Weak Topics & Revision Suggestions
            if (weakTopics.isNotEmpty()) {
                item {
                    TactileCard(
                        backgroundColor = Color(0xFFFFF9E6),
                        bottomShadowColor = Color(0xFFFFD56B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFC78300))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Suggested Revision Areas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7A4F00)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Based on past quiz attempts, you scored below 60% in these topics:",
                                style = MaterialTheme.typography.bodySmall,
                                color = PencilGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            weakTopics.forEach { topic ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFC78300), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subject-Wise Breakdown
            if (subjectStats.isNotEmpty()) {
                item {
                    Text(
                        text = "Subject-Wise Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceText
                    )
                }

                items(subjectStats) { (code, qCount, acc) ->
                    TactileCard(
                        backgroundColor = PaperWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                                Text(
                                    text = "$qCount questions attempted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PencilGray
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (acc >= 70) StorybookGreen else Color(0xFFFFECEC))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$acc% Acc",
                                    color = if (acc >= 70) DarkGreenContainer else ErrorRed,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Recent Quiz Attempt History
            item {
                Text(
                    text = "Previous Quiz Attempts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceText
                )
            }

            if (attempts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No quizzes attempted yet. Attend your first quiz to see detailed analytics!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PencilGray
                        )
                    }
                }
            } else {
                items(attempts) { attempt ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${attempt.courseCode} · ${attempt.topicTitle.ifBlank { "General Quiz" }}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceText
                                    )
                                    Text(
                                        text = "Difficulty: ${attempt.difficulty} • ${attempt.timestamp}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PencilGray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (attempt.accuracyPercent >= 70) StorybookGreen else SurfaceContainerHigh)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${attempt.score} pts (${attempt.accuracyPercent}%)",
                                        color = if (attempt.accuracyPercent >= 70) DarkGreenContainer else OnSurfaceText,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Mistake Analysis / Explanation Expansion
                            val wrongAnswers = attempt.questionsData.filter { it.userSelected != it.correctIndex }
                            if (wrongAnswers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = SurfaceContainer)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Mistakes Review (${wrongAnswers.size} incorrect):",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ErrorRed,
                                    fontSize = 11.sp
                                )
                                wrongAnswers.take(2).forEach { mistake ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Q: ${mistake.question}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurfaceText
                                    )
                                    Text(
                                        text = "💡 Explanation: ${mistake.explanation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PencilGray,
                                        fontSize = 11.sp
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

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PencilGray,
            fontSize = 11.sp
        )
    }
}
