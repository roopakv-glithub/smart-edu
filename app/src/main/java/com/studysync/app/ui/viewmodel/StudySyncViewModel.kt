package com.studysync.app.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.studysync.app.data.session.EncryptedSessionStore
import androidx.lifecycle.viewModelScope
import com.studysync.app.data.model.*
import com.studysync.app.data.repository.*
import com.studysync.app.data.config.AcademicConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.studysync.app.data.mapper.PresentationMappers
import com.studysync.app.data.network.ai.NvidiaAiService
import com.studysync.app.data.network.ai.AiChatMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Authenticated : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class StudySyncViewModel(application: Application) : AndroidViewModel(application) {
    private val academicRepository = AcademicRepository()
    val hasVtopSession get() = academicRepository.currentSession != null
    private val cloud = SupabaseRepository(EncryptedSessionStore(application))
    private val aiService = NvidiaAiService()
    private val gson = Gson()

    private var loginJob: Job? = null
    private var syncJob: Job? = null
    private var selectedPodId: String? = null
    private var rawPods: List<JsonObject> = emptyList()
    private var rawNotes: List<JsonObject> = emptyList()
    private var examEvents: List<ScheduleEvent> = emptyList()

    private val _semesterId = MutableStateFlow(cloud.selectedSemester)
    val semesterId = _semesterId.asStateFlow()
    private val _courses = MutableStateFlow<List<AcademicCourse>>(emptyList())
    val courses = _courses.asStateFlow()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError = _syncError.asStateFlow()
    private val _cloudError = MutableStateFlow<String?>(null)
    val cloudError = _cloudError.asStateFlow()
    fun clearCloudError() { _cloudError.value = null }

    private val _calendarMonth = MutableStateFlow(format("yyyy-MM", Date()))
    val calendarMonth = _calendarMonth.asStateFlow()
    private val _isLoggedIn = MutableStateFlow(cloud.hasSession)
    val isLoggedIn = _isLoggedIn.asStateFlow()
    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState = _loginState.asStateFlow()
    private val _userName = MutableStateFlow(cloud.displayName)
    val userName = _userName.asStateFlow()
    private val _userStreak = MutableStateFlow(0)
    val userStreak = _userStreak.asStateFlow()

    // --- Role & Admin Management ---
    private val _currentUserRole = MutableStateFlow(Role.STUDENT)
    val currentUserRole = _currentUserRole.asStateFlow()
    private val _appRestrictions = MutableStateFlow(AppRestrictions())
    val appRestrictions = _appRestrictions.asStateFlow()
    private val _userList = MutableStateFlow<List<AppUser>>(emptyList())
    val userList = _userList.asStateFlow()

    // --- Tasks State ---
    private val _taskFilter = MutableStateFlow("ALL")
    val taskFilter = _taskFilter.asStateFlow()
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks = _tasks.asStateFlow()
    private val _hiddenTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenTaskIds = _hiddenTaskIds.asStateFlow()

    // --- Schedule State ---
    private val _selectedDay = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val selectedDay = _selectedDay.asStateFlow()
    private val _scheduleEvents = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    val scheduleEvents = _scheduleEvents.asStateFlow()

    // --- Notes & Syllabus State ---
    private val _notesSearchQuery = MutableStateFlow("")
    val notesSearchQuery = _notesSearchQuery.asStateFlow()
    private val _notesFilter = MutableStateFlow("ALL")
    val notesFilter = _notesFilter.asStateFlow()
    private val _studyNotes = MutableStateFlow<List<StudyNote>>(emptyList())
    val studyNotes = _studyNotes.asStateFlow()
    private val _hiddenNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenNoteIds = _hiddenNoteIds.asStateFlow()

    // Canonical Syllabuses and Single-Generated Roadmaps (Cached per subject)
    private val _subjectSyllabuses = MutableStateFlow<Map<String, SubjectSyllabus>>(emptyMap())
    val subjectSyllabuses = _subjectSyllabuses.asStateFlow()
    private val _subjectRoadmaps = MutableStateFlow<Map<String, SubjectRoadmap>>(emptyMap())
    val subjectRoadmaps = _subjectRoadmaps.asStateFlow()
    private val _noteSummaries = MutableStateFlow<List<NoteSummary>>(emptyList())
    val noteSummaries = _noteSummaries.asStateFlow()

    // --- Quiz & Analytics State ---
    private val _quizConfig = MutableStateFlow(QuizConfig())
    val quizConfig = _quizConfig.asStateFlow()
    private val _aiQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val aiQuizQuestions = _aiQuizQuestions.asStateFlow()
    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz = _isGeneratingQuiz.asStateFlow()
    private val _quizGenerationError = MutableStateFlow<String?>(null)
    val quizGenerationError = _quizGenerationError.asStateFlow()
    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard = _leaderboard.asStateFlow()
    private val _quizAttempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val quizAttempts = _quizAttempts.asStateFlow()

    // --- Study Pods & Chat ---
    private val _studyPods = MutableStateFlow<List<StudyPod>>(emptyList())
    val studyPods = _studyPods.asStateFlow()
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    // --- Jarvis AI Co-Pilot ---
    private val _jarvisMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                "intro",
                "Jarvis AI",
                "Hello! I am your Jarvis AI study co-pilot. I am connected to your enrolled subjects, course materials, and pending tasks. How can I help your studies today?",
                "",
                isAssistant = true
            )
        )
    )
    val jarvisMessages = _jarvisMessages.asStateFlow()
    private val _isJarvisThinking = MutableStateFlow(false)
    val isJarvisThinking = _isJarvisThinking.asStateFlow()

    // --- Preferences ---
    private val _tactileVibrations = MutableStateFlow(true)
    val tactileVibrations = _tactileVibrations.asStateFlow()
    private val _aiCopilotEnabled = MutableStateFlow(true)
    val aiCopilotEnabled = _aiCopilotEnabled.asStateFlow()
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    init {
        cloud.cachedSnapshot(_semesterId.value)?.let { applySnapshot(it) }
        if (cloud.hasSession) refreshAcademics()
    }

    private fun applySnapshot(snapshot: AcademicSnapshot) {
        _courses.value = snapshot.courses
        examEvents = snapshot.exams
        _scheduleEvents.value = examEvents
    }

    private fun format(pattern: String, date: Date) = SimpleDateFormat(pattern, Locale.ENGLISH).format(date)
    private fun failure(e: Exception) = if (e is CloudException || e is IllegalArgumentException) e.message ?: "Check your input." else "Connection failed. Retry; your changes were not confirmed."
    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _cloudError.value = failure(e) }
        }
    }

    fun authenticate(userId: String, pass: String) {
        if (loginJob?.isActive == true) return
        if (userId.isBlank() || pass.isBlank()) {
            _loginState.value = LoginUiState.Error("Enter your user ID and password.")
            return
        }
        _loginState.value = LoginUiState.Loading
        loginJob = viewModelScope.launch {
            when (val result = academicRepository.login(userId, pass)) {
                is AuthResult.Success -> {
                    try {
                        val snapshot = cloud.sync(result.session, _semesterId.value)
                        applySnapshot(snapshot)
                        _userName.value = cloud.displayName
                        _isLoggedIn.value = true
                        _loginState.value = LoginUiState.Authenticated
                        _syncError.value = snapshot.warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
                        refreshCloud()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { _loginState.value = LoginUiState.Error(failure(e)) }
                }
                is AuthResult.Error -> _loginState.value = LoginUiState.Error(result.message)
            }
        }
    }

    /**
     * Requirement 1: Separate Admin Login option.
     * Supports master admin credentials (ID: roopak2007 / Pass: roopak2007)
     * as well as standard Supabase user_roles authentication.
     */
    fun adminLogin(
        adminId: String,
        adminPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (loginJob?.isActive == true) return
        val trimmedId = adminId.trim()
        val trimmedPass = adminPass.trim()

        if (trimmedId.isBlank() || trimmedPass.isBlank()) {
            onError("Enter Admin ID and password.")
            return
        }

        _loginState.value = LoginUiState.Loading

        // Master hardcoded admin check
        if (trimmedId.equals("roopak2007", ignoreCase = true) && trimmedPass == "roopak2007") {
            _currentUserRole.value = Role.ADMIN
            _isLoggedIn.value = true
            _loginState.value = LoginUiState.Authenticated
            viewModelScope.launch {
                runCatching { loadCloud() }
                onSuccess()
            }
            return
        }

        loginJob = viewModelScope.launch {
            try {
                val uid = cloud.loginWithPassword(trimmedId, adminPass)
                val roleRow = cloud.rows("user_roles", mapOf("user_id" to "eq.$uid")).firstOrNull()
                val roleStr = roleRow?.string("role") ?: "student"

                if (!roleStr.equals("admin", ignoreCase = true)) {
                    _loginState.value = LoginUiState.Error("Access Denied: This account is not authorized as an administrator.")
                    onError("Access Denied: Only authorized administrators may log in here.")
                    return@launch
                }

                _currentUserRole.value = Role.ADMIN
                _isLoggedIn.value = true
                _loginState.value = LoginUiState.Authenticated
                loadCloud()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: "Admin authentication failed."
                _loginState.value = LoginUiState.Error(msg)
                onError(msg)
            }
        }
    }

    fun logout() {
        viewModelScope.coroutineContext.cancelChildren()
        academicRepository.logout()
        cloud.logout()
        loginJob = null
        syncJob = null
        selectedPodId = null
        _isLoggedIn.value = false
        _loginState.value = LoginUiState.Idle
        _userName.value = ""
        _currentUserRole.value = Role.STUDENT
        _courses.value = emptyList()
        _tasks.value = emptyList()
        _scheduleEvents.value = emptyList()
        _studyPods.value = emptyList()
        _studyNotes.value = emptyList()
        _chatMessages.value = emptyList()
        rawPods = emptyList()
        rawNotes = emptyList()
        examEvents = emptyList()
        _syncError.value = null
        _cloudError.value = null
        _isSyncing.value = false
    }

    fun setSemester(value: String) {
        if (value !in AcademicConfig.semesters || value == _semesterId.value || _isSyncing.value) return
        _semesterId.value = value
        cloud.selectedSemester = value
        _courses.value = emptyList()
        examEvents = emptyList()
        _scheduleEvents.value = emptyList()
        _studyPods.value = emptyList()
        _chatMessages.value = emptyList()
        _studyNotes.value = emptyList()
        _tasks.value = emptyList()
        rawPods = emptyList()
        selectedPodId = null
        refreshAcademics()
    }

    fun refreshAcademics(fromVtop: Boolean = false) {
        if (!_isLoggedIn.value || syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val snapshot = if (fromVtop) {
                    val session = academicRepository.currentSession ?: throw CloudException("Reconnect VTOP to import new data. Saved app data remains available.")
                    cloud.sync(session, _semesterId.value)
                } else cloud.fetchSnapshot(_semesterId.value)

                if (snapshot != null) applySnapshot(snapshot)
                loadCloud()
                _syncError.value = snapshot?.warnings?.takeIf { it.isNotEmpty() }?.joinToString(" ")
                    ?: if (snapshot == null || snapshot.courses.isEmpty()) "No subjects saved for this semester. Choose your enrolled semester or import from VTOP." else null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _syncError.value = failure(e) }
            finally { _isSyncing.value = false }
        }
    }

    fun refreshCloud() = action { loadCloud() }

    private suspend fun loadCloud() {
        val profile = cloud.rows("profiles", mapOf("id" to "eq.${cloud.userId}")).firstOrNull()
        val name = profile?.string("display_name").orEmpty().takeUnless { it == profile?.string("registration_number") }.orEmpty()
        cloud.saveDisplayName(name)
        _userName.value = cloud.displayName

        // 1. User Role
        val roleRow = runCatching { cloud.rows("user_roles", mapOf("user_id" to "eq.${cloud.userId}")).firstOrNull() }.getOrNull()
        val roleStr = roleRow?.string("role") ?: "student"
        _currentUserRole.value = when (roleStr.lowercase()) {
            "admin" -> Role.ADMIN
            "moderator" -> Role.MODERATOR
            else -> Role.STUDENT
        }

        // 2. App Restrictions
        val restrRow = runCatching { cloud.rows("app_restrictions", mapOf("id" to "eq.1")).firstOrNull() }.getOrNull()
        if (restrRow != null) {
            _appRestrictions.value = AppRestrictions(
                restrictTasks = restrRow.get("restrict_tasks")?.asBoolean == true,
                restrictNotes = restrRow.get("restrict_notes")?.asBoolean == true
            )
        }

        // 3. User Hidden Tasks & Notes
        val hiddenTasks = runCatching {
            cloud.rows("user_hidden_tasks", mapOf("user_id" to "eq.${cloud.userId}")).map { it.string("task_id") }.toSet()
        }.getOrDefault(emptySet())
        val hiddenNotes = runCatching {
            cloud.rows("user_hidden_notes", mapOf("user_id" to "eq.${cloud.userId}")).map { it.string("note_id") }.toSet()
        }.getOrDefault(emptySet())
        _hiddenTaskIds.value = hiddenTasks
        _hiddenNoteIds.value = hiddenNotes

        // 4. Pods and Tasks
        val pods = cloud.rows("pods", mapOf("semester_id" to "eq.${_semesterId.value}", "order" to "course_title.asc"))
        val members = cloud.rows("pod_members", mapOf("active" to "eq.true"))
        val tasks = cloud.rows("tasks", mapOf("order" to "due_at.asc"))
        val completed = cloud.rows("task_user_state", mapOf("user_id" to "eq.${cloud.userId}")).associate { it.string("task_id") to (it.get("is_completed")?.asBoolean == true) }
        val notes = cloud.rows("notes", mapOf("semester_id" to "eq.${_semesterId.value}", "order" to "created_at.desc"))
        val events = cloud.rows("personal_events", mapOf("order" to "event_date.asc"))
        val settings = cloud.rows("user_settings", mapOf("user_id" to "eq.${cloud.userId}")).firstOrNull()
        rawPods = pods
        rawNotes = notes

        _studyPods.value = PresentationMappers.groups(pods.map { p ->
            StudyPod(p.string("id"), p.string("course_title"), p.string("faculty"), p.string("course_code") + " · " + p.string("class_id"), members.count { it.string("pod_id") == p.string("id") }, false, p.string("course_code").take(2), courseCode = p.string("course_code"))
        })

        // Filter out hidden tasks for current user
        _tasks.value = tasks
            .filter { !hiddenTasks.contains(it.string("id")) }
            .map { t ->
                TaskItem(
                    id = t.string("id"),
                    title = t.string("title"),
                    description = t.string("description"),
                    priority = runCatching { TaskPriority.valueOf(t.string("priority")) }.getOrDefault(TaskPriority.STANDARD),
                    category = t.string("course_code").ifBlank { "Others" },
                    dueDate = t.string("due_at").replace('T', ' ').take(16) + " UTC",
                    isCompleted = completed[t.string("id")] == true,
                    dueAt = t.string("due_at"),
                    isPersonal = t.get("is_personal")?.asBoolean == true,
                    creatorId = t.string("creator_id")
                )
            }

        // Filter out hidden notes for current user
        _studyNotes.value = notes
            .filter { !hiddenNotes.contains(it.string("id")) }
            .map { n ->
                StudyNote(
                    id = n.string("id"),
                    title = n.string("title"),
                    category = n.string("course_code"),
                    teacher = n.string("faculty"),
                    fileCount = 1,
                    lastUpdated = n.string("created_at").take(10),
                    noteType = n.string("mime_type").substringAfter('/'),
                    isCommon = n.get("is_common")?.asBoolean == true,
                    uploaderId = n.string("uploader_id")
                )
            }

        _scheduleEvents.value = examEvents + _tasks.value.mapNotNull { PresentationMappers.taskEvent(it) } + events.map { e ->
            val date = e.string("event_date")
            ScheduleEvent(e.string("id"), e.string("title"), e.string("category"), e.string("time_label"), "", "", date.takeLast(2).toInt(), date)
        }

        settings?.let {
            _tactileVibrations.value = it.get("vibrations").asBoolean
            _notificationsEnabled.value = it.get("notifications").asBoolean
            _aiCopilotEnabled.value = true
        }

        // 5. Subject Syllabuses & Roadmaps
        val syllabuses = runCatching { cloud.rows("subject_syllabuses") }.getOrDefault(emptyList())
        _subjectSyllabuses.value = syllabuses.associate { s ->
            val code = s.string("course_code")
            code to SubjectSyllabus(
                id = s.string("id"),
                courseCode = code,
                courseTitle = s.string("course_title"),
                syllabusText = s.string("syllabus_text"),
                version = s.get("version")?.asInt ?: 1
            )
        }

        val roadmaps = runCatching { cloud.rows("subject_roadmaps") }.getOrDefault(emptyList())
        _subjectRoadmaps.value = roadmaps.associate { r ->
            val code = r.string("course_code")
            val modulesJson = r.getAsJsonArray("modules") ?: JsonArray()
            code to SubjectRoadmap(
                id = r.string("id"),
                courseCode = code,
                syllabusVersion = r.get("syllabus_version")?.asInt ?: 1,
                modules = parseModulesFromJson(modulesJson),
                generatedAt = r.string("generated_at")
            )
        }

        // 6. Quiz Attempts & Analytics
        val attempts = runCatching {
            cloud.rows("quiz_attempts", mapOf("user_id" to "eq.${cloud.userId}", "order" to "created_at.desc"))
        }.getOrDefault(emptyList())

        _quizAttempts.value = attempts.map { a ->
            val qData = runCatching {
                a.getAsJsonArray("questions_data")?.map { qElem ->
                    val qObj = qElem.asJsonObject
                    QuizQuestionAttempt(
                        question = qObj.get("question")?.asString ?: "",
                        options = qObj.getAsJsonArray("options")?.map { it.asString } ?: emptyList(),
                        userSelected = qObj.get("userSelected")?.asInt ?: 0,
                        correctIndex = qObj.get("correctIndex")?.asInt ?: 0,
                        explanation = qObj.get("explanation")?.asString ?: ""
                    )
                } ?: emptyList()
            }.getOrDefault(emptyList())

            QuizAttempt(
                id = a.string("id"),
                userId = a.string("user_id"),
                courseCode = a.string("course_code"),
                moduleTitle = a.string("module_title"),
                topicTitle = a.string("topic_title"),
                difficulty = a.string("difficulty").ifBlank { "Medium" },
                totalQuestions = a.get("total_questions")?.asInt ?: 0,
                correctAnswers = a.get("correct_answers")?.asInt ?: 0,
                score = a.get("score")?.asInt ?: 0,
                accuracyPercent = a.get("accuracy_percent")?.asInt ?: 0,
                timestamp = a.string("created_at").take(16).replace('T', ' '),
                questionsData = qData
            )
        }

        // 7. Dynamic Leaderboard aggregation
        val allAttempts = runCatching { cloud.rows("quiz_attempts") }.getOrDefault(emptyList())
        if (allAttempts.isNotEmpty()) {
            val userScores = allAttempts.groupBy { it.string("user_id") }
            val ranked = userScores.entries.map { (uid, attList) ->
                val totalPts = attList.sumOf { it.get("score")?.asInt ?: 0 }
                val uName = if (uid == cloud.userId) cloud.displayName else "Student ${uid.take(4)}"
                Pair(uName, totalPts)
            }.sortedByDescending { it.second }

            _leaderboard.value = ranked.mapIndexed { idx, (uName, pts) ->
                LeaderboardUser(
                    rank = idx + 1,
                    name = uName,
                    points = pts,
                    streakDays = (pts / 40).coerceAtLeast(1),
                    avatarInitial = uName.take(1).uppercase()
                )
            }
        }

        // 8. If Admin, load all registered users
        if (_currentUserRole.value == Role.ADMIN) {
            val allProfiles = runCatching { cloud.rows("profiles") }.getOrDefault(emptyList())
            val allRoles = runCatching { cloud.rows("user_roles") }.getOrDefault(emptyList()).associate { it.string("user_id") to it.string("role") }
            _userList.value = allProfiles.map { p ->
                val pId = p.string("id")
                val rStr = allRoles[pId] ?: "student"
                AppUser(
                    id = pId,
                    name = p.string("display_name"),
                    registrationNumber = p.string("registration_number"),
                    role = when (rStr.lowercase()) {
                        "admin" -> Role.ADMIN
                        "moderator" -> Role.MODERATOR
                        else -> Role.STUDENT
                    }
                )
            }
        }
    }

    // --- Task Actions & Personal Hiding ---
    fun setTaskFilter(filter: String) { _taskFilter.value = filter }

    fun toggleTaskCompleted(taskId: String) = action {
        val task = _tasks.value.firstOrNull { it.id == taskId } ?: return@action
        cloud.insert("task_user_state", mapOf("task_id" to taskId, "user_id" to cloud.userId, "is_completed" to !task.isCompleted), true)
        loadCloud()
    }

    fun addTask(
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: String,
        dueTime: String,
        isPersonal: Boolean = false
    ) = action {
        require(title.isNotBlank() && title.length <= 2000 && PresentationMappers.wordCount(title) <= 25) { "Use a task title of 1 to 25 words." }
        val parsed = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.ENGLISH).apply { isLenient = false }.parse("$dueDate $dueTime")
            ?: throw IllegalArgumentException("Use dd/mm/yy and hh:mm AM/PM.")
        val pod = rawPods.firstOrNull { it.string("id") == category }
        val personal = isPersonal || category == "OTHERS"
        require(personal || pod != null) { "Choose a verified subject before sharing a task." }

        // Requirement 2: Restrict Tasks enforcement
        if (!personal && _appRestrictions.value.restrictTasks) {
            val isPrivileged = _currentUserRole.value == Role.ADMIN || _currentUserRole.value == Role.MODERATOR
            require(isPrivileged) { "Shared tasks are restricted by administrators. You can create a personal task." }
        }

        val due = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(parsed)
        cloud.insert("tasks", mapOf(
            "creator_id" to cloud.userId,
            "pod_id" to if (personal) null else pod!!.string("id"),
            "title" to title.trim(),
            "description" to description,
            "priority" to priority.name,
            "course_code" to (pod?.string("course_code") ?: "Others"),
            "due_at" to due,
            "is_personal" to personal
        ))
        loadCloud()
    }

    /**
     * Requirement 3 & 4: Personal Hiding vs Global Deletion
     */
    fun hideTaskForMe(taskId: String) = action {
        cloud.insert("user_hidden_tasks", mapOf("user_id" to cloud.userId, "task_id" to taskId), upsert = true)
        loadCloud()
    }

    fun deleteTaskGlobally(taskId: String) = action {
        val task = _tasks.value.firstOrNull { it.id == taskId }
        if (task != null && !task.isPersonal) {
            val isPrivileged = _currentUserRole.value == Role.ADMIN || _currentUserRole.value == Role.MODERATOR
            require(isPrivileged) { "Only administrators or moderators can globally delete shared tasks." }
        }
        cloud.deleteById("tasks", taskId)
        loadCloud()
    }

    fun hideNoteForMe(noteId: String) = action {
        cloud.insert("user_hidden_notes", mapOf("user_id" to cloud.userId, "note_id" to noteId), upsert = true)
        loadCloud()
    }

    fun deleteNoteGlobally(noteId: String) = action {
        val isPrivileged = _currentUserRole.value == Role.ADMIN || _currentUserRole.value == Role.MODERATOR
        require(isPrivileged) { "Only administrators or moderators can globally delete shared notes." }
        cloud.deleteById("notes", noteId)
        loadCloud()
    }

    // --- Schedule Actions ---
    fun setSelectedDay(day: Int) { _selectedDay.value = day }
    fun moveCalendarMonth(delta: Int) {
        val c = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(_calendarMonth.value + "-01")!!
            add(Calendar.MONTH, delta)
        }
        _calendarMonth.value = format("yyyy-MM", c.time)
        _selectedDay.value = 1
    }
    fun addScheduleEvent(title: String, category: String, time: String) = action {
        cloud.insert("personal_events", mapOf(
            "user_id" to cloud.userId,
            "title" to title,
            "category" to category,
            "event_date" to _calendarMonth.value + "-" + _selectedDay.value.toString().padStart(2, '0'),
            "time_label" to time
        ))
        loadCloud()
    }

    // --- Notes & Canonical Syllabus Actions ---
    fun setNotesSearchQuery(query: String) { _notesSearchQuery.value = query }
    fun setNotesFilter(filter: String) { _notesFilter.value = filter }

    fun uploadNote(resolver: ContentResolver, uri: Uri, podId: String, title: String, isCommon: Boolean = false) = action {
        // Requirement 2: Restrict Notes enforcement
        if (_appRestrictions.value.restrictNotes) {
            val isPrivileged = _currentUserRole.value == Role.ADMIN || _currentUserRole.value == Role.MODERATOR
            require(isPrivileged) { "Notes upload is currently restricted to moderators and administrators." }
        }

        val pod = rawPods.firstOrNull { it.string("id") == podId } ?: throw IllegalArgumentException("Choose a verified subject.")
        require(title.isNotBlank()) { "Enter a note title." }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        require(mime in listOf("application/pdf", "text/plain", "image/jpeg", "image/png", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            "Choose PDF, text, PNG, JPG or DOCX."
        }
        val bytes = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    require(out.size() + n <= 20 * 1024 * 1024) { "Files must be at most 20 MB." }
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            } ?: throw IllegalArgumentException("Cannot read this file.")
        }
        require(bytes.isNotEmpty()) { "The selected file is empty." }
        val path = cloud.userId + "/" + UUID.randomUUID().toString()
        cloud.upload(path, bytes, mime)
        cloud.insert("notes", mapOf(
            "uploader_id" to cloud.userId,
            "pod_id" to podId,
            "semester_id" to _semesterId.value,
            "course_code" to pod.string("course_code"),
            "faculty" to if (isCommon) "" else pod.string("faculty"),
            "is_common" to isCommon,
            "title" to title.trim(),
            "object_path" to path,
            "mime_type" to mime,
            "size_bytes" to bytes.size
        ))
        loadCloud()
    }

    fun openNote(id: String, open: (String) -> Unit) = action {
        val note = rawNotes.firstOrNull { it.string("id") == id } ?: return@action
        open(cloud.noteUrl(note.string("object_path")))
    }

    /**
     * Requirement 5, 6, 7: Canonical Syllabus Upload & Single Roadmap Generation
     */
    fun uploadSyllabus(courseCode: String, courseTitle: String, syllabusText: String) = action {
        if (_appRestrictions.value.restrictNotes) {
            val isPrivileged = _currentUserRole.value == Role.ADMIN || _currentUserRole.value == Role.MODERATOR
            require(isPrivileged) { "Only administrators or moderators can update the canonical syllabus." }
        }
        require(syllabusText.isNotBlank()) { "Syllabus text cannot be empty." }

        cloud.insert("subject_syllabuses", mapOf(
            "course_code" to courseCode,
            "course_title" to courseTitle,
            "syllabus_text" to syllabusText.trim(),
            "uploaded_by" to cloud.userId
        ), upsert = true)

        loadCloud()
        loadOrGenerateRoadmap(courseCode, courseTitle, syllabusText, forceRegenerate = true)
    }

    /**
     * Requirement 7: GENERATE ROADMAP ONLY ONCE
     * Checks if roadmap already exists in Supabase. If so, loads it without calling AI.
     */
    fun loadOrGenerateRoadmap(
        courseCode: String,
        courseTitle: String,
        providedSyllabus: String = "",
        forceRegenerate: Boolean = false
    ) {
        viewModelScope.launch {
            val cached = _subjectRoadmaps.value[courseCode]
            if (cached != null && cached.modules.isNotEmpty() && !forceRegenerate) {
                // Reusing cached roadmap from Supabase without any AI call
                return@launch
            }

            if (!forceRegenerate) {
                val dbRows = runCatching { cloud.rows("subject_roadmaps", mapOf("course_code" to "eq.$courseCode")) }.getOrNull()
                if (!dbRows.isNullOrEmpty()) {
                    val row = dbRows.first()
                    val modulesJson = row.getAsJsonArray("modules") ?: JsonArray()
                    val parsed = parseModulesFromJson(modulesJson)
                    if (parsed.isNotEmpty()) {
                        val loaded = SubjectRoadmap(
                            id = row.string("id"),
                            courseCode = courseCode,
                            syllabusVersion = row.get("syllabus_version")?.asInt ?: 1,
                            modules = parsed,
                            generatedAt = row.string("generated_at")
                        )
                        _subjectRoadmaps.update { it + (courseCode to loaded) }
                        return@launch
                    }
                }
            }

            // If not found in Supabase or explicitly regenerating:
            val syllabusText = providedSyllabus.ifBlank {
                _subjectSyllabuses.value[courseCode]?.syllabusText.orEmpty().ifBlank {
                    "Syllabus for $courseTitle ($courseCode): Module 1: Foundations and Architecture. Module 2: Core Protocols and Systems. Module 3: Advanced Optimization and Design. Module 4: Practical Applications and Testing."
                }
            }

            _isGeneratingQuiz.value = true
            try {
                val modules = aiService.analyzeSyllabus(syllabusText, courseTitle)
                val jsonTree = gson.toJsonTree(modules)

                cloud.insert("subject_roadmaps", mapOf(
                    "course_code" to courseCode,
                    "syllabus_version" to (_subjectSyllabuses.value[courseCode]?.version ?: 1),
                    "modules" to jsonTree
                ), upsert = true)

                val generated = SubjectRoadmap(
                    id = UUID.randomUUID().toString(),
                    courseCode = courseCode,
                    syllabusVersion = _subjectSyllabuses.value[courseCode]?.version ?: 1,
                    modules = modules,
                    generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
                )
                _subjectRoadmaps.update { it + (courseCode to generated) }
            } catch (e: Exception) {
                _quizGenerationError.value = "Failed to analyze syllabus: ${e.message}"
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    private fun parseModulesFromJson(modulesJson: JsonArray): List<SyllabusModule> {
        return modulesJson.mapIndexed { modIdx, modElem ->
            val modObj = modElem.asJsonObject
            val topicsArray = modObj.getAsJsonArray("topics") ?: JsonArray()
            val parsedTopics = topicsArray.mapIndexed { topIdx, topElem ->
                val topObj = topElem.asJsonObject
                val subArray = topObj.getAsJsonArray("subtopics") ?: JsonArray()
                val parsedSubtopics = subArray.mapIndexed { subIdx, subElem ->
                    val subObj = subElem.asJsonObject
                    SyllabusSubtopic(
                        id = subObj.get("id")?.asString ?: "sub_${modIdx}_${topIdx}_$subIdx",
                        title = subObj.get("title")?.asString ?: "Subtopic ${subIdx + 1}",
                        description = subObj.get("description")?.asString ?: ""
                    )
                }
                SyllabusTopic(
                    id = topObj.get("id")?.asString ?: "top_${modIdx}_$topIdx",
                    title = topObj.get("title")?.asString ?: "Topic ${topIdx + 1}",
                    subtopics = parsedSubtopics
                )
            }
            SyllabusModule(
                id = modObj.get("id")?.asString ?: "mod_$modIdx",
                moduleNumber = modObj.get("moduleNumber")?.asInt ?: (modIdx + 1),
                title = modObj.get("title")?.asString ?: "Module ${modIdx + 1}",
                topics = parsedTopics
            )
        }
    }

    // --- Quiz Generation Grounded in Notes & Topics ---
    fun updateQuizConfig(subject: String? = null, questions: Int? = null, difficulty: String? = null) {
        _quizConfig.update { it.copy(
            subject = subject ?: it.subject,
            questionCount = questions ?: it.questionCount,
            difficulty = difficulty ?: it.difficulty
        ) }
    }

    /**
     * Requirement 11: Generates quiz using relevant study notes and syllabus topic
     */
    fun generateAiQuiz(
        subject: String,
        questionCount: Int = 5,
        difficulty: String = "Medium",
        moduleTitle: String = "",
        topicTitle: String = ""
    ) {
        if (_isGeneratingQuiz.value) return
        _isGeneratingQuiz.value = true
        _quizGenerationError.value = null
        viewModelScope.launch {
            try {
                // Find matching course and relevant study notes
                val matchingCourse = _courses.value.firstOrNull {
                    it.title.contains(subject, ignoreCase = true) || it.courseCode.contains(subject, ignoreCase = true)
                }
                val matchingNotes = _studyNotes.value.filter {
                    it.category.contains(subject, ignoreCase = true) || it.title.contains(subject, ignoreCase = true)
                }
                val notesText = matchingNotes.joinToString("\n\n") { "Note Title: ${it.title} (Faculty: ${it.teacher})" }

                val questions = aiService.generateQuiz(
                    subject = subject,
                    questionCount = questionCount.coerceIn(3, 15),
                    difficulty = difficulty,
                    courseContext = matchingCourse?.let { "Course: ${it.courseCode} ${it.title} (Faculty: ${it.faculty})" }.orEmpty(),
                    studyNotesContent = notesText,
                    moduleTitle = moduleTitle,
                    topicTitle = topicTitle
                )

                if (questions.isEmpty()) {
                    _quizGenerationError.value = "AI did not return questions. Please retry."
                } else {
                    _aiQuizQuestions.value = questions
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _quizGenerationError.value = "Failed to generate quiz: ${e.message ?: "Connection timeout"}"
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    fun clearAiQuiz() {
        _aiQuizQuestions.value = emptyList()
        _quizGenerationError.value = null
    }

    /**
     * Requirement 12: Persist Quiz Attempts in Supabase
     */
    fun saveQuizAttempt(
        courseCode: String,
        moduleTitle: String,
        topicTitle: String,
        difficulty: String,
        totalQuestions: Int,
        correctAnswers: Int,
        score: Int,
        questionsData: List<QuizQuestionAttempt>
    ) {
        val accuracy = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
        viewModelScope.launch {
            try {
                val qJson = gson.toJsonTree(questionsData)
                cloud.insert("quiz_attempts", mapOf(
                    "user_id" to cloud.userId,
                    "course_code" to courseCode,
                    "module_title" to moduleTitle,
                    "topic_title" to topicTitle,
                    "difficulty" to difficulty,
                    "total_questions" to totalQuestions,
                    "correct_answers" to correctAnswers,
                    "score" to score,
                    "accuracy_percent" to accuracy,
                    "questions_data" to qJson
                ))
                loadCloud()
            } catch (_: Exception) {}
        }
    }

    // --- Admin & Restriction Controls ---
    fun setTaskRestriction(enabled: Boolean) = action {
        cloud.patch("app_restrictions", "1", mapOf("restrict_tasks" to enabled, "updated_by" to cloud.userId))
        _appRestrictions.update { it.copy(restrictTasks = enabled) }
    }

    fun setNoteRestriction(enabled: Boolean) = action {
        cloud.patch("app_restrictions", "1", mapOf("restrict_notes" to enabled, "updated_by" to cloud.userId))
        _appRestrictions.update { it.copy(restrictNotes = enabled) }
    }

    fun setUserRole(targetUserId: String, newRole: Role) = action {
        cloud.insert("user_roles", mapOf(
            "user_id" to targetUserId,
            "role" to newRole.name.lowercase(),
            "granted_by" to cloud.userId
        ), upsert = true)
        loadCloud()
    }

    // --- Chat & Jarvis ---
    fun selectPod(id: String?) {
        selectedPodId = id
        _chatMessages.value = emptyList()
    }

    suspend fun reloadChat() {
        val id = selectedPodId ?: return
        val list = cloud.rows("pod_messages", mapOf("pod_id" to "eq.$id", "order" to "created_at.asc"))
        val members = cloud.rows("pod_members", mapOf("pod_id" to "eq.$id"))
        val names = members.associate { it.string("user_id") to "Peer " + it.string("user_id").take(4) }.toMutableMap()
        names[cloud.userId] = "You"
        _chatMessages.value = list.map { m ->
            val sender = m.string("sender_id")
            val isAi = m.string("message_type") == "ai_response"
            ChatMessage(
                m.string("id"),
                if (isAi) "AI Assistant" else names[sender] ?: "Student",
                m.string("body"),
                m.string("created_at").take(16).replace('T', ' '),
                isAssistant = isAi,
                isUser = sender == cloud.userId && !isAi
            )
        }
    }

    fun sendChatMessage(text: String) = action {
        val id = selectedPodId ?: return@action
        if (text.isBlank()) return@action
        cloud.insert("pod_messages", mapOf("pod_id" to id, "sender_id" to cloud.userId, "body" to text.trim()))
        reloadChat()
    }

    fun deleteMessage(id: String) = action {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        cloud.patch("pod_messages", id, mapOf("body" to "Message deleted", "deleted_at" to now))
        reloadChat()
    }

    fun sendJarvisQuery(query: String) {
        if (query.isBlank() || _isJarvisThinking.value) return
        val userMsgId = UUID.randomUUID().toString()
        val timeNow = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
        val userMsg = ChatMessage(userMsgId, _userName.value.ifBlank { "You" }, query.trim(), timeNow, isUser = true)
        _jarvisMessages.update { it + userMsg }
        _isJarvisThinking.value = true

        viewModelScope.launch {
            try {
                val coursesContext = if (_courses.value.isNotEmpty()) {
                    _courses.value.joinToString("\n") { "- ${it.courseCode} · ${it.title} (Faculty: ${it.faculty}, Attendance: ${it.attendance})" }
                } else "No enrolled subjects loaded yet."

                val tasksContext = if (_tasks.value.isNotEmpty()) {
                    _tasks.value.joinToString("\n") { "- ${it.title} [Subject: ${it.category}, Due: ${it.dueDate}, Priority: ${it.priority}, Done: ${it.isCompleted}]" }
                } else "No active tasks."

                val notesContext = if (_studyNotes.value.isNotEmpty()) {
                    _studyNotes.value.joinToString("\n") { "- ${it.title} (${it.category})" }
                } else "No uploaded notes."

                val systemPrompt = """You are Jarvis, an intelligent and friendly AI study co-pilot built into StudySync for university students.
The student is: ${_userName.value.ifBlank { "Student" }}
Current Semester: ${_semesterId.value}

Enrolled Subjects:
$coursesContext

Assigned / Pending Tasks:
$tasksContext

Uploaded Study Notes:
$notesContext

Ground your responses in their actual subjects and tasks. Provide clear, encouraging, and concise explanations."""

                val recentMessages = _jarvisMessages.value.takeLast(6).map { m ->
                    AiChatMessage(
                        role = if (m.isUser) "user" else "assistant",
                        content = m.text
                    )
                }

                val reply = aiService.chat(recentMessages, systemPrompt)
                val replyTime = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
                _jarvisMessages.update {
                    it + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderName = "Jarvis AI",
                        text = reply,
                        timestamp = replyTime,
                        isAssistant = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errTime = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
                _jarvisMessages.update {
                    it + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderName = "Jarvis AI",
                        text = "I encountered an error connecting to the AI service: ${e.message ?: "Connection timeout"}. Please retry.",
                        timestamp = errTime,
                        isAssistant = true
                    )
                }
            } finally {
                _isJarvisThinking.value = false
            }
        }
    }

    fun clearJarvisChat() {
        _jarvisMessages.value = listOf(
            ChatMessage(
                "intro",
                "Jarvis AI",
                "Conversation refreshed! Ask me anything about your enrolled subjects, pending tasks, or academic concepts.",
                SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date()),
                isAssistant = true
            )
        )
    }

    fun toggleTactileVibrations() = action {
        cloud.insert("user_settings", mapOf("user_id" to cloud.userId, "vibrations" to !_tactileVibrations.value), true)
        loadCloud()
    }

    fun toggleAiCopilot() { _aiCopilotEnabled.update { !it } }
    fun toggleNotifications() = action {
        cloud.insert("user_settings", mapOf("user_id" to cloud.userId, "notifications" to !_notificationsEnabled.value), true)
        loadCloud()
    }
}
