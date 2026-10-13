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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.studysync.app.data.model.*
import com.studysync.app.ui.components.DuolingoRoadmap
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.components.TactileChip
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

enum class QuizTab { ATTEND_QUIZ, LEADERBOARD, ANALYZE_RESULT }

@Composable
fun QuizScreen(
    viewModel: StudySyncViewModel
) {
    val quizConfig by viewModel.quizConfig.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val aiQuestions by viewModel.aiQuizQuestions.collectAsState()
    val isGenerating by viewModel.isGeneratingQuiz.collectAsState()
    val generationError by viewModel.quizGenerationError.collectAsState()
    val roadmaps by viewModel.subjectRoadmaps.collectAsState()

    var activeTab by remember { mutableStateOf(QuizTab.ATTEND_QUIZ) }
    var selectedCourseCode by remember { mutableStateOf<String?>(null) }
    var selectedModuleTitle by remember { mutableStateOf("") }
    var selectedTopicTitle by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }

    // Quiz Execution State
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var answerSubmitted by remember { mutableStateOf(false) }
    var isQuizCompleted by remember { mutableStateOf(false) }
    var questionAttempts by remember { mutableStateOf(listOf<QuizQuestionAttempt>()) }

    // When new questions arrive, initialize test session
    LaunchedEffect(aiQuestions) {
        if (aiQuestions.isNotEmpty()) {
            currentQuestionIndex = 0
            score = 0
            selectedOptionIndex = null
            answerSubmitted = false
            isQuizCompleted = false
            questionAttempts = emptyList()
        }
    }

    if (isGenerating) {
        // AI Generation Loading State
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
                        text = "Preparing AI Quiz",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Analyzing syllabus & study notes with NVIDIA Nemotron...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PencilGray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CircularProgressIndicator(color = EagerGreen)
                }
            }
        }
    } else if (isQuizCompleted && aiQuestions.isNotEmpty()) {
        // Quiz Results & Score Summary
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Topic: ${selectedTopicTitle.ifBlank { quizConfig.subject }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PencilGray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                        text = "Continue Learning",
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
                            text = selectedTopicTitle.ifBlank { quizConfig.subject },
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
                            if (!answerSubmitted) selectedOptionIndex = index
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (!answerSubmitted) selectedOptionIndex = index
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
                            val isCorrect = selectedOptionIndex == currentQ.correctIndex
                            if (isCorrect) score += 10
                            questionAttempts = questionAttempts + QuizQuestionAttempt(
                                question = currentQ.question,
                                options = currentQ.options,
                                userSelected = selectedOptionIndex!!,
                                correctIndex = currentQ.correctIndex,
                                explanation = currentQ.explanation
                            )
                        }
                    },
                    enabled = selectedOptionIndex != null,
                    backgroundColor = EagerGreen,
                    bottomShadowColor = DarkGreenContainer,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            } else {
                TactileButton(
                    text = if (currentQuestionIndex < aiQuestions.size - 1) "Next Question" else "Finish & Save",
                    onClick = {
                        if (currentQuestionIndex < aiQuestions.size - 1) {
                            currentQuestionIndex++
                            selectedOptionIndex = null
                            answerSubmitted = false
                        } else {
                            // Persist quiz attempt in Supabase (Requirement 12)
                            val courseCode = selectedCourseCode ?: "GENERAL"
                            val correctCount = score / 10
                            viewModel.saveQuizAttempt(
                                courseCode = courseCode,
                                moduleTitle = selectedModuleTitle,
                                topicTitle = selectedTopicTitle,
                                difficulty = quizConfig.difficulty,
                                totalQuestions = aiQuestions.size,
                                correctAnswers = correctCount,
                                score = score,
                                questionsData = questionAttempts
                            )
                            isQuizCompleted = true
                        }
                    },
                    backgroundColor = EagerGreen,
                    bottomShadowColor = DarkGreenContainer,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        }
    } else {
        // --- Requirement 8: QUIZ HOME WITH EXACTLY THREE PRIMARY OPTIONS ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaperWhite)
        ) {
            // Three Primary Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerLow)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple(QuizTab.ATTEND_QUIZ, "Attend Quiz", Icons.Default.SportsEsports),
                    Triple(QuizTab.LEADERBOARD, "Leaderboard", Icons.Default.EmojiEvents),
                    Triple(QuizTab.ANALYZE_RESULT, "Analyze Result", Icons.Default.Analytics)
                ).forEach { (tab, label, icon) ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryGreen else Color.Transparent)
                            .clickable {
                                activeTab = tab
                                if (tab != QuizTab.ATTEND_QUIZ) selectedCourseCode = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) PaperWhite else PencilGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) PaperWhite else PencilGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Tab Content
            when (activeTab) {
                QuizTab.ATTEND_QUIZ -> {
                    if (selectedCourseCode == null) {
                        // Subject Selection (Requirement 9)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column {
                                    Text(
                                        text = "Select a Subject to Attend Quiz",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OnSurfaceText
                                    )
                                    Text(
                                        text = "Choose your course to open the interactive Duolingo-style learning roadmap.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PencilGray
                                    )
                                }
                            }

                            if (courses.isEmpty()) {
                                item {
                                    TactileCard(
                                        backgroundColor = PaperWhite,
                                        bottomShadowColor = SurfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No enrolled courses found. Choose from standard subjects below:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PencilGray
                                        )
                                    }
                                }
                                val fallbackSubjects = listOf(
                                    Pair("CSE1001", "Problem Solving and Programming"),
                                    Pair("CSE2005", "Operating Systems"),
                                    Pair("CSE2004", "Database Management Systems"),
                                    Pair("CSE1002", "Data Structures and Algorithms")
                                )
                                items(fallbackSubjects) { (code, title) ->
                                    TactileCard(
                                        backgroundColor = PaperWhite,
                                        bottomShadowColor = StorybookGreen,
                                        onClick = {
                                            selectedCourseCode = code
                                            viewModel.loadOrGenerateRoadmap(code, title)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(code, color = PrimaryGreen, style = MaterialTheme.typography.labelLarge)
                                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryGreen)
                                        }
                                    }
                                }
                            } else {
                                items(courses) { course ->
                                    TactileCard(
                                        backgroundColor = PaperWhite,
                                        bottomShadowColor = StorybookGreen,
                                        onClick = {
                                            selectedCourseCode = course.courseCode
                                            viewModel.loadOrGenerateRoadmap(course.courseCode, course.title)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = course.courseCode,
                                                    color = PrimaryGreen,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = course.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnSurfaceText
                                                )
                                                Text(
                                                    text = "Faculty: ${course.faculty}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = PencilGray
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = PrimaryGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Duolingo-style Learning Roadmap for Selected Subject (Requirement 10)
                        val code = selectedCourseCode!!
                        val courseObj = courses.firstOrNull { it.courseCode == code }
                        val courseTitle = courseObj?.title ?: code
                        val roadmap = roadmaps[code]

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PaperWhite)
                        ) {
                            // Back Navigation Bar
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceContainerLow)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedCourseCode = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = courseTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Interactive Learning Roadmap",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PrimaryGreen
                                        )
                                    }
                                }
                            }

                            // The Duolingo-style Roadmap
                            if (roadmap == null || roadmap.modules.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = EagerGreen)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Loading canonical roadmap for $code...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = PencilGray
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    DuolingoRoadmap(
                                        modules = roadmap.modules,
                                        onSelectTopic = { module, topic ->
                                            selectedModuleTitle = module.title
                                            selectedTopicTitle = topic.title
                                            showConfigDialog = true
                                        },
                                        onSelectModuleTest = { module ->
                                            selectedModuleTitle = module.title
                                            selectedTopicTitle = "Module ${module.moduleNumber} Comprehensive Mastery Test"
                                            showConfigDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                QuizTab.LEADERBOARD -> {
                    // Leaderboard Screen (Requirement 13)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "StudySync Leaderboard",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurfaceText
                            )
                            Text(
                                text = "Real rankings aggregated from persisted quiz attempts in Supabase.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PencilGray
                            )
                        }

                        if (leaderboard.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No quiz submissions yet. Be the first on the leaderboard!")
                                }
                            }
                        } else {
                            items(leaderboard) { user ->
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

                QuizTab.ANALYZE_RESULT -> {
                    // Analyze Result Tab (Requirement 14)
                    AnalyzeResultScreen(
                        viewModel = viewModel,
                        onBack = { activeTab = QuizTab.ATTEND_QUIZ }
                    )
                }
            }
        }
    }

    // Quiz Configuration Dialog (Question count & Difficulty selector)
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text(
                    text = "Configure Quiz",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Topic: $selectedTopicTitle",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )

                    Text(
                        text = "NUMBER OF QUESTIONS",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (quizConfig.questionCount > 3) viewModel.updateQuizConfig(questions = quizConfig.questionCount - 1) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                        }
                        Text(
                            text = "${quizConfig.questionCount} Questions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { if (quizConfig.questionCount < 10) viewModel.updateQuizConfig(questions = quizConfig.questionCount + 1) }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                        }
                    }

                    Text(
                        text = "DIFFICULTY",
                        style = MaterialTheme.typography.labelLarge,
                        color = PencilGray,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Easy", "Medium", "Hard").forEach { diff ->
                            val sel = quizConfig.difficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) EagerGreen else SurfaceContainer)
                                    .clickable { viewModel.updateQuizConfig(difficulty = diff) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff,
                                    color = if (sel) PaperWhite else OnSurfaceText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfigDialog = false
                        viewModel.generateAiQuiz(
                            subject = selectedCourseCode ?: quizConfig.subject,
                            questionCount = quizConfig.questionCount,
                            difficulty = quizConfig.difficulty,
                            moduleTitle = selectedModuleTitle,
                            topicTitle = selectedTopicTitle
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Start Quiz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
