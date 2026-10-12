package com.studysync.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studysync.app.data.model.*
import com.studysync.app.data.repository.AcademicRepository
import com.studysync.app.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Authenticated : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class StudySyncViewModel : ViewModel() {

    private val academicRepository = AcademicRepository()
    private var loginJob: Job? = null

    // Login & User state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _userName = MutableStateFlow("Alex Chen")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userStreak = MutableStateFlow(5)
    val userStreak: StateFlow<Int> = _userStreak.asStateFlow()

    // Task state
    private val _taskFilter = MutableStateFlow("ALL") // ALL, ACTIVE, DONE
    val taskFilter: StateFlow<String> = _taskFilter.asStateFlow()

    private val _tasks = MutableStateFlow(
        listOf(
            TaskItem("1", "Finalize Requirements Traceability Matrix", "Link use cases UC-01 to UC-14 with functional tests", TaskPriority.HIGH, "Requirements", "Today, 5:00 PM", false),
            TaskItem("2", "Java Multithreading Producer-Consumer Exercise", "Implement BlockingQueue using synchronized monitors", TaskPriority.MEDIUM, "Java", "Tomorrow, 11:59 PM", false),
            TaskItem("3", "Microeconomics Elasticity Calculation Sheet", "Chapter 5 problems 3 through 9", TaskPriority.STANDARD, "Microeconomics", "Oct 24, 2:00 PM", false),
            TaskItem("4", "Discrete Mathematics Graph Proofs", "Complete Euler path practice set", TaskPriority.MEDIUM, "Discrete Math", "Oct 25, 6:00 PM", true),
            TaskItem("5", "Qualitative Research Methodology Draft", "Review survey questions for UX study", TaskPriority.STANDARD, "QQS", "Oct 26, 10:00 AM", true)
        )
    )
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // Schedule state
    private val _selectedDay = MutableStateFlow(22)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _scheduleEvents = MutableStateFlow(
        listOf(
            ScheduleEvent("s1", "Discrete Mathematics", "Lecture", "10:00 AM", "90 min", "Room LH-204", 22),
            ScheduleEvent("s2", "Software Requirements Review", "Study Pod", "1:30 PM", "60 min", "Library Pod B", 22),
            ScheduleEvent("s3", "Java Multithreading Lab", "Lab Session", "3:30 PM", "120 min", "CS Lab 3", 22),
            ScheduleEvent("s4", "Microeconomics Quiz", "Exam", "11:00 AM", "45 min", "Hall 101", 24)
        )
    )
    val scheduleEvents: StateFlow<List<ScheduleEvent>> = _scheduleEvents.asStateFlow()

    // Notes state
    private val _notesSearchQuery = MutableStateFlow("")
    val notesSearchQuery: StateFlow<String> = _notesSearchQuery.asStateFlow()

    private val _notesFilter = MutableStateFlow("ALL")
    val notesFilter: StateFlow<String> = _notesFilter.asStateFlow()

    private val _studyNotes = MutableStateFlow(
        listOf(
            StudyNote("n1", "Requirements Engineering & Use Cases", "Requirements", 4, "Updated yesterday", "PDF"),
            StudyNote("n2", "Qualitative Study & Survey Design", "QQS", 2, "Updated Oct 19", "PDF"),
            StudyNote("n3", "Microeconomics Chapter 5 Summary", "Microeconomics", 5, "Updated Oct 18", "PDF"),
            StudyNote("n4", "Java Concurrency & Thread Synchronization", "Java", 6, "Updated 2 hrs ago", "CODE")
        )
    )
    val studyNotes: StateFlow<List<StudyNote>> = _studyNotes.asStateFlow()

    // AI Quiz Config state
    private val _quizConfig = MutableStateFlow(QuizConfig())
    val quizConfig: StateFlow<QuizConfig> = _quizConfig.asStateFlow()

    private val _leaderboard = MutableStateFlow(
        listOf(
            LeaderboardUser(1, "Sandy A.", 1420, 12, "SA"),
            LeaderboardUser(2, "Alex Chen", 1250, 5, "AC"),
            LeaderboardUser(3, "HM Black", 1180, 8, "HB"),
            LeaderboardUser(4, "David K.", 990, 4, "DK"),
            LeaderboardUser(5, "Emma R.", 850, 3, "ER")
        )
    )
    val leaderboard: StateFlow<List<LeaderboardUser>> = _leaderboard.asStateFlow()

    // Chat State
    private val _studyPods = MutableStateFlow(
        listOf(
            StudyPod("p1", "CS-302 Group Study Room", "SRS Document Sync", 6, true, "CS"),
            StudyPod("p2", "Java Multithreading Lab", "Producer-Consumer", 4, true, "JM"),
            StudyPod("p3", "Microeconomics Prep", "Elasticity Quiz", 3, true, "ME")
        )
    )
    val studyPods: StateFlow<List<StudyPod>> = _studyPods.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage("m1", "Sandy", "The deadline for SRS document is moved to 5 PM today!", "12:45 PM", isAssistant = false, isUser = false),
            ChatMessage("m2", "Alex Chen", "Got it, finalizing the traceability matrix now.", "12:47 PM", isAssistant = false, isUser = true),
            ChatMessage("m3", "HM Black", "Count me in for the 1:30 PM study session.", "12:50 PM", isAssistant = false, isUser = false)
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Jarvis AI State
    private val _jarvisMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "j1",
                senderName = "Jarvis AI",
                text = "Hello Alex. I have indexed your current syllabus, 7 active tasks, upcoming Discrete Math lecture, and recent Requirements notes. How can I assist your study session today?",
                timestamp = "Just now",
                isAssistant = true,
                isUser = false
            )
        )
    )
    val jarvisMessages: StateFlow<List<ChatMessage>> = _jarvisMessages.asStateFlow()

    // Settings state
    private val _tactileVibrations = MutableStateFlow(true)
    val tactileVibrations: StateFlow<Boolean> = _tactileVibrations.asStateFlow()

    private val _aiCopilotEnabled = MutableStateFlow(true)
    val aiCopilotEnabled: StateFlow<Boolean> = _aiCopilotEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Actions
    fun authenticate(userId: String, password: String) {
        if (loginJob?.isActive == true) return
        if (userId.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState.Error("User ID and password cannot be empty.")
            return
        }
        _loginState.value = LoginUiState.Loading
        loginJob = viewModelScope.launch {
            when (val result = academicRepository.login(userId, password)) {
                is AuthResult.Success -> {
                    _userName.value = result.session.authorizedID
                    _isLoggedIn.value = true
                    _loginState.value = LoginUiState.Authenticated
                }
                is AuthResult.Error -> {
                    _loginState.value = LoginUiState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        loginJob?.cancel()
        academicRepository.logout()
        _isLoggedIn.value = false
        _userName.value = ""
        _loginState.value = LoginUiState.Idle
    }

    fun setTaskFilter(filter: String) {
        _taskFilter.value = filter
    }

    fun toggleTaskCompleted(taskId: String) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
            }
        }
    }

    fun addTask(title: String, description: String, category: String, priority: TaskPriority, dueDate: String, dueTime: String) {
        val formattedDueDate = if (dueDate.isNotBlank() && dueTime.isNotBlank()) "$dueDate, $dueTime" else "Today, 5:00 PM"
        val newTask = TaskItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = description,
            priority = priority,
            category = category,
            dueDate = formattedDueDate,
            isCompleted = false
        )
        _tasks.update { listOf(newTask) + it }
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun addScheduleEvent(title: String, category: String, time: String) {
        val newEvent = ScheduleEvent(
            id = System.currentTimeMillis().toString(),
            title = title,
            category = category,
            time = time,
            duration = "60 min",
            location = "Study Room",
            dateDay = _selectedDay.value
        )
        _scheduleEvents.update { it + newEvent }
    }

    fun setNotesSearchQuery(query: String) {
        _notesSearchQuery.value = query
    }

    fun setNotesFilter(filter: String) {
        _notesFilter.value = filter
    }

    fun updateQuizConfig(subject: String? = null, questions: Int? = null, difficulty: String? = null) {
        _quizConfig.update { old ->
            old.copy(
                subject = subject ?: old.subject,
                questionCount = questions ?: old.questionCount,
                difficulty = difficulty ?: old.difficulty
            )
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderName = "Alex Chen",
            text = text,
            timestamp = "Just now",
            isAssistant = false,
            isUser = true
        )
        _chatMessages.update { it + userMsg }
    }

    fun sendJarvisQuery(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderName = "Alex Chen",
            text = query,
            timestamp = "Just now",
            isAssistant = false,
            isUser = true
        )
        _jarvisMessages.update { it + userMsg }

        // Generate response
        val jarvisReplyText = when {
            query.contains("synchronized", ignoreCase = true) || query.contains("lock", ignoreCase = true) ->
                "Synchronized blocks use implicit intrinsic monitors (intrinsic locks). ReentrantLock provides explicit lock acquisition, timed lock polling (`tryLock()`), interruptible locks, and multi-condition variables."
            query.contains("schedule", ignoreCase = true) || query.contains("plan", ignoreCase = true) ->
                "I've analyzed your schedule! Your optimal study block is today between 2:00 PM and 3:30 PM before your CS Lab."
            else ->
                "Great query! Based on your course notes, I recommend focusing on key concepts and practicing short quizzes to reinforce memory retention."
        }

        val assistantReply = ChatMessage(
            id = (System.currentTimeMillis() + 1).toString(),
            senderName = "Jarvis AI",
            text = jarvisReplyText,
            timestamp = "Just now",
            isAssistant = true,
            isUser = false,
            categoryBadge = "StudySync Context"
        )
        _jarvisMessages.update { it + assistantReply }
    }

    fun toggleTactileVibrations() {
        _tactileVibrations.update { !it }
    }

    fun toggleAiCopilot() {
        _aiCopilotEnabled.update { !it }
    }

    fun toggleNotifications() {
        _notificationsEnabled.update { !it }
    }
}
