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
import com.google.gson.JsonObject
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
    private val academicRepository=AcademicRepository()
    val hasVtopSession get() = academicRepository.currentSession != null
    private val cloud=SupabaseRepository(EncryptedSessionStore(application))
    private var loginJob: Job?=null
    private var syncJob: Job?=null
    private var selectedPodId: String?=null
    private var rawPods: List<JsonObject> = emptyList()
    private var rawNotes: List<JsonObject> = emptyList()
    private var examEvents: List<ScheduleEvent> = emptyList()
    private val _semesterId=MutableStateFlow(cloud.selectedSemester)
    val semesterId=_semesterId.asStateFlow()
    private val _courses=MutableStateFlow<List<AcademicCourse>>(emptyList())
    val courses=_courses.asStateFlow()
    private val _isSyncing=MutableStateFlow(false)
    val isSyncing=_isSyncing.asStateFlow()
    private val _syncError=MutableStateFlow<String?>(null)
    val syncError=_syncError.asStateFlow()
    private val _cloudError=MutableStateFlow<String?>(null)
    val cloudError=_cloudError.asStateFlow()
    fun clearCloudError() { _cloudError.value=null }
    private val _calendarMonth=MutableStateFlow(format("yyyy-MM",Date()))
    val calendarMonth=_calendarMonth.asStateFlow()
    private val _isLoggedIn=MutableStateFlow(cloud.hasSession)
    val isLoggedIn=_isLoggedIn.asStateFlow()
    private val _loginState=MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState=_loginState.asStateFlow()
    private val _userName=MutableStateFlow(cloud.displayName)
    val userName=_userName.asStateFlow()
    private val _userStreak=MutableStateFlow(0)
    val userStreak=_userStreak.asStateFlow()
    private val _taskFilter=MutableStateFlow("ALL")
    val taskFilter=_taskFilter.asStateFlow()
    private val _tasks=MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks=_tasks.asStateFlow()
    private val _selectedDay=MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val selectedDay=_selectedDay.asStateFlow()
    private val _scheduleEvents=MutableStateFlow<List<ScheduleEvent>>(emptyList())
    val scheduleEvents=_scheduleEvents.asStateFlow()
    private val _notesSearchQuery=MutableStateFlow("")
    val notesSearchQuery=_notesSearchQuery.asStateFlow()
    private val _notesFilter=MutableStateFlow("ALL")
    val notesFilter=_notesFilter.asStateFlow()
    private val _studyNotes=MutableStateFlow<List<StudyNote>>(emptyList())
    val studyNotes=_studyNotes.asStateFlow()
    private val aiService = NvidiaAiService()
    private val _quizConfig=MutableStateFlow(QuizConfig())
    val quizConfig=_quizConfig.asStateFlow()
    private val _aiQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val aiQuizQuestions = _aiQuizQuestions.asStateFlow()
    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz = _isGeneratingQuiz.asStateFlow()
    private val _quizGenerationError = MutableStateFlow<String?>(null)
    val quizGenerationError = _quizGenerationError.asStateFlow()
    private val _leaderboard=MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard=_leaderboard.asStateFlow()
    private val _studyPods=MutableStateFlow<List<StudyPod>>(emptyList())
    val studyPods=_studyPods.asStateFlow()
    private val _chatMessages=MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages=_chatMessages.asStateFlow()
    private val _jarvisMessages=MutableStateFlow(listOf(ChatMessage("intro","Jarvis AI","Hello! I'm your Jarvis AI study co-pilot powered by NVIDIA Nemotron. I have direct access to your enrolled courses and tasks. How can I help your studies today?","",isAssistant = true)))
    val jarvisMessages=_jarvisMessages.asStateFlow()
    private val _isJarvisThinking = MutableStateFlow(false)
    val isJarvisThinking = _isJarvisThinking.asStateFlow()
    private val _tactileVibrations=MutableStateFlow(true)
    val tactileVibrations=_tactileVibrations.asStateFlow()
    private val _aiCopilotEnabled=MutableStateFlow(true)
    val aiCopilotEnabled=_aiCopilotEnabled.asStateFlow()
    private val _notificationsEnabled=MutableStateFlow(true)
    val notificationsEnabled=_notificationsEnabled.asStateFlow()

    init {
        cloud.cachedSnapshot(_semesterId.value)?.let { applySnapshot(it) }
        if(cloud.hasSession) refreshAcademics()
    }
    private fun applySnapshot(snapshot: AcademicSnapshot) {
        _courses.value=snapshot.courses; examEvents=snapshot.exams; _scheduleEvents.value=examEvents
    }
    private fun format(pattern:String,date:Date)=SimpleDateFormat(pattern,Locale.ENGLISH).format(date)
    private fun failure(e:Exception)=if(e is CloudException || e is IllegalArgumentException) e.message ?: "Check your input." else "Connection failed. Retry; your changes were not confirmed."
    private fun action(block:suspend ()->Unit) { viewModelScope.launch { try { block() } catch(e:CancellationException){throw e} catch(e:Exception){_cloudError.value=failure(e)} } }
    fun authenticate(userId:String,password:String) {
        if(loginJob?.isActive==true) return
        if(userId.isBlank() || password.isBlank()) { _loginState.value=LoginUiState.Error("Enter your VTOP user ID and password."); return }
        _loginState.value=LoginUiState.Loading
        loginJob=viewModelScope.launch {
            when(val result=academicRepository.login(userId,password)) {
                is AuthResult.Success -> {
                    try {
                        val snapshot=cloud.sync(result.session,_semesterId.value)
                        applySnapshot(snapshot)
                        _userName.value=cloud.displayName; _isLoggedIn.value=true; _loginState.value=LoginUiState.Authenticated
                        _syncError.value=snapshot.warnings.takeIf { it.isNotEmpty() }?.joinToString(" ")
                        refreshCloud()
                    } catch(e:CancellationException) { throw e }
                    catch(e:Exception) { _loginState.value=LoginUiState.Error(failure(e)) }
                }
                is AuthResult.Error -> _loginState.value=LoginUiState.Error(result.message)
            }
        }
    }
    fun logout() {
        viewModelScope.coroutineContext.cancelChildren()
        academicRepository.logout(); cloud.logout(); loginJob=null; syncJob=null; selectedPodId=null
        _isLoggedIn.value=false; _loginState.value=LoginUiState.Idle; _userName.value=""
        _courses.value=emptyList(); _tasks.value=emptyList(); _scheduleEvents.value=emptyList(); _studyPods.value=emptyList(); _studyNotes.value=emptyList(); _chatMessages.value=emptyList()
        rawPods=emptyList(); rawNotes=emptyList(); examEvents=emptyList(); _syncError.value=null; _cloudError.value=null; _isSyncing.value=false
    }
    fun setSemester(value:String) {
        if(value !in AcademicConfig.semesters || value==_semesterId.value || _isSyncing.value) return
        _semesterId.value=value; cloud.selectedSemester=value; _courses.value=emptyList(); examEvents=emptyList(); _scheduleEvents.value=emptyList(); _studyPods.value=emptyList(); _chatMessages.value=emptyList(); _studyNotes.value=emptyList(); _tasks.value=emptyList(); rawPods=emptyList(); selectedPodId=null
        refreshAcademics()
    }
    fun refreshAcademics(fromVtop: Boolean = false) {
        if(!_isLoggedIn.value || syncJob?.isActive==true) return
        syncJob=viewModelScope.launch {
            _isSyncing.value=true; _syncError.value=null
            try {
                val snapshot=if(fromVtop) {
                    val session=academicRepository.currentSession ?: throw CloudException("Reconnect VTOP to import new university data. Saved app data is still available.")
                    cloud.sync(session,_semesterId.value)
                } else cloud.fetchSnapshot(_semesterId.value)
                if(snapshot!=null) applySnapshot(snapshot)
                loadCloud()
                _syncError.value=snapshot?.warnings?.takeIf{it.isNotEmpty()}?.joinToString(" ") ?: if(snapshot==null || snapshot.courses.isEmpty()) "No subjects saved for this semester. Choose your enrolled semester or import from VTOP." else null
            } catch(e:CancellationException){throw e} catch(e:Exception){_syncError.value=failure(e)} finally{_isSyncing.value=false}
        }
    }
    fun refreshCloud()=action { loadCloud() }
    private suspend fun loadCloud() {
        val profile=cloud.rows("profiles",mapOf("id" to "eq.${cloud.userId}")).firstOrNull()
        val name=profile?.string("display_name").orEmpty().takeUnless { it == profile?.string("registration_number") }.orEmpty()
        cloud.saveDisplayName(name);_userName.value=cloud.displayName
        val pods=cloud.rows("pods",mapOf("semester_id" to "eq.${_semesterId.value}","order" to "course_title.asc"))
        val members=cloud.rows("pod_members",mapOf("active" to "eq.true"))
        val tasks=cloud.rows("tasks",mapOf("order" to "due_at.asc"))
        val completed=cloud.rows("task_user_state",mapOf("user_id" to "eq.${cloud.userId}")).associate { it.string("task_id") to (it.get("is_completed")?.asBoolean==true) }
        val notes=cloud.rows("notes",mapOf("semester_id" to "eq.${_semesterId.value}","order" to "created_at.desc"))
        val events=cloud.rows("personal_events",mapOf("order" to "event_date.asc"))
        val settings=cloud.rows("user_settings",mapOf("user_id" to "eq.${cloud.userId}")).firstOrNull()
        rawPods=pods; rawNotes=notes
        _studyPods.value=PresentationMappers.groups(pods.map { p->StudyPod(p.string("id"),p.string("course_title"),p.string("faculty"),p.string("course_code")+" · "+p.string("class_id"),members.count{it.string("pod_id")==p.string("id")},false,p.string("course_code").take(2),courseCode=p.string("course_code")) })
        _tasks.value=tasks.map { t->TaskItem(t.string("id"),t.string("title"),t.string("description"),runCatching{TaskPriority.valueOf(t.string("priority"))}.getOrDefault(TaskPriority.STANDARD),t.string("course_code").ifBlank { "Others" },t.string("due_at").replace('T',' ').take(16)+" UTC",completed[t.string("id")]==true,t.string("due_at")) }
        _studyNotes.value=notes.map { n->StudyNote(n.string("id"),n.string("title"),n.string("course_code"),n.string("faculty"),1,n.string("created_at").take(10),n.string("mime_type").substringAfter('/'),n.get("is_common")?.asBoolean==true) }
        _scheduleEvents.value=examEvents+_tasks.value.mapNotNull { PresentationMappers.taskEvent(it) }+events.map { e->val date=e.string("event_date"); ScheduleEvent(e.string("id"),e.string("title"),e.string("category"),e.string("time_label"),"","",date.takeLast(2).toInt(),date) }
        settings?.let { _tactileVibrations.value=it.get("vibrations").asBoolean; _notificationsEnabled.value=it.get("notifications").asBoolean; _aiCopilotEnabled.value=false }
    }
    fun setTaskFilter(filter:String){_taskFilter.value=filter}
    fun toggleTaskCompleted(taskId:String)=action {
        val task=_tasks.value.firstOrNull{it.id==taskId} ?: return@action
        cloud.insert("task_user_state",mapOf("task_id" to taskId,"user_id" to cloud.userId,"is_completed" to !task.isCompleted),true)
        loadCloud()
    }
    fun addTask(title:String,description:String,category:String,priority:TaskPriority,dueDate:String,dueTime:String,isPersonal:Boolean=false)=action {
        require(title.isNotBlank() && title.length<=2000 && PresentationMappers.wordCount(title)<=25){ "Use a task title of 1 to 25 words." }
        val parsed=SimpleDateFormat("dd/MM/yy hh:mm a",Locale.ENGLISH).apply{isLenient=false}.parse("$dueDate $dueTime") ?: throw IllegalArgumentException("Use dd/mm/yy and hh:mm AM/PM.")
        val pod=rawPods.firstOrNull{it.string("id")==category}
        val personal=isPersonal || category=="OTHERS"
        require(personal || pod!=null){"Choose a verified subject before sharing a task."}
        val due=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.ENGLISH).apply{timeZone=TimeZone.getTimeZone("UTC")}.format(parsed)
        cloud.insert("tasks",mapOf("creator_id" to cloud.userId,"pod_id" to if(personal) null else pod!!.string("id"),"title" to title.trim(),"description" to description,"priority" to priority.name,"course_code" to (pod?.string("course_code") ?: "Others"),"due_at" to due,"is_personal" to personal))
        loadCloud()
    }
    fun setSelectedDay(day:Int){_selectedDay.value=day}
    fun moveCalendarMonth(delta:Int) {
        val c=Calendar.getInstance().apply{time=SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH).parse(_calendarMonth.value+"-01")!!;add(Calendar.MONTH,delta)}
        _calendarMonth.value=format("yyyy-MM",c.time);_selectedDay.value=1
    }
    fun addScheduleEvent(title:String,category:String,time:String)=action {
        cloud.insert("personal_events",mapOf("user_id" to cloud.userId,"title" to title,"category" to category,"event_date" to _calendarMonth.value+"-"+_selectedDay.value.toString().padStart(2,'0'),"time_label" to time));loadCloud()
    }
    fun setNotesSearchQuery(query:String){_notesSearchQuery.value=query}
    fun setNotesFilter(filter:String){_notesFilter.value=filter}
    fun uploadNote(resolver:ContentResolver,uri:Uri,podId:String,title:String,isCommon:Boolean=false)=action {
        val pod=rawPods.firstOrNull{it.string("id")==podId} ?: throw IllegalArgumentException("Choose a verified subject.")
        require(title.isNotBlank()){ "Enter a note title." }
        val mime=resolver.getType(uri) ?: "application/octet-stream"
        require(mime in listOf("application/pdf","text/plain","image/jpeg","image/png","application/vnd.openxmlformats-officedocument.wordprocessingml.document")){"Choose PDF, text, PNG, JPG or DOCX."}
        val bytes=withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { input ->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=20*1024*1024){"Files must be at most 20 MB."};out.write(buffer,0,n)}
                out.toByteArray()
            } ?: throw IllegalArgumentException("Cannot read this file.")
        }
        require(bytes.isNotEmpty()){ "The selected file is empty." }
        val path=cloud.userId+"/"+UUID.randomUUID().toString()
        cloud.upload(path,bytes,mime)
        cloud.insert("notes",mapOf("uploader_id" to cloud.userId,"pod_id" to podId,"semester_id" to _semesterId.value,"course_code" to pod.string("course_code"),"faculty" to if(isCommon) "" else pod.string("faculty"),"is_common" to isCommon,"title" to title.trim(),"object_path" to path,"mime_type" to mime,"size_bytes" to bytes.size));loadCloud()
    }
    fun openNote(id:String,open:(String)->Unit)=action { val note=rawNotes.firstOrNull{it.string("id")==id} ?: return@action;open(cloud.noteUrl(note.string("object_path"))) }
    fun selectPod(id:String?){selectedPodId=id;_chatMessages.value=emptyList()}
    suspend fun reloadChat() {
        val id=selectedPodId ?: return
        try {
            val rows=cloud.rows("pod_messages",mapOf("pod_id" to "in.(${_studyPods.value.firstOrNull { it.id==id }?.podIds?.joinToString(",") ?: id})","order" to "created_at.desc","limit" to "100"))
            val names=cloud.rows("profiles").associate{it.string("id") to it.string("display_name")}
            if(id==selectedPodId) _chatMessages.value=rows.reversed().map{m->ChatMessage(m.string("id"),names[m.string("sender_id")] ?: "Member",if(m.string("deleted_at").isBlank())m.string("body") else "Message deleted",m.string("created_at").replace('T',' ').take(16)+" UTC",false,m.string("sender_id")==cloud.userId)}
        }catch(e:CancellationException){throw e}catch(e:Exception){_cloudError.value=failure(e)}
    }
    fun sendChatMessage(text:String)=action {
        val id=selectedPodId ?: throw IllegalArgumentException("Select a study pod.")
        if(text.isBlank())return@action
        cloud.insert("pod_messages",mapOf("pod_id" to id,"sender_id" to cloud.userId,"body" to text.trim()));reloadChat()
    }
    fun deleteMessage(id:String)=action {
        val now=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.ENGLISH).apply{timeZone=TimeZone.getTimeZone("UTC")}.format(Date())
        cloud.patch("pod_messages",id,mapOf("body" to "Message deleted","deleted_at" to now));reloadChat()
    }
    fun updateQuizConfig(subject:String?=null,questions:Int?=null,difficulty:String?=null){_quizConfig.update{it.copy(subject=subject?:it.subject,questionCount=questions?:it.questionCount,difficulty=difficulty?:it.difficulty)}}

    fun generateAiQuiz(subject: String, questionCount: Int = 5, difficulty: String = "Medium") {
        if (_isGeneratingQuiz.value) return
        _isGeneratingQuiz.value = true
        _quizGenerationError.value = null
        viewModelScope.launch {
            try {
                val matchingCourse = _courses.value.firstOrNull {
                    it.title.contains(subject, ignoreCase = true) || it.courseCode.contains(subject, ignoreCase = true)
                }
                val matchingTasks = _tasks.value.filter {
                    it.title.contains(subject, ignoreCase = true) || it.category.contains(subject, ignoreCase = true)
                }
                val contextBuilder = StringBuilder()
                if (matchingCourse != null) {
                    contextBuilder.append("Course Code: ${matchingCourse.courseCode}, Title: ${matchingCourse.title}, Faculty: ${matchingCourse.faculty}\n")
                }
                if (matchingTasks.isNotEmpty()) {
                    contextBuilder.append("Related Tasks/Topics: ${matchingTasks.joinToString(", ") { it.title }}\n")
                }

                val questions = aiService.generateQuiz(
                    subject = subject,
                    questionCount = questionCount.coerceIn(3, 10),
                    difficulty = difficulty,
                    courseContext = contextBuilder.toString()
                )
                if (questions.isEmpty()) {
                    _quizGenerationError.value = "AI did not generate questions. Please retry."
                } else {
                    _aiQuizQuestions.value = questions
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _quizGenerationError.value = "Failed to generate quiz: ${e.message ?: "Network error"}"
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    fun clearAiQuiz() {
        _aiQuizQuestions.value = emptyList()
        _quizGenerationError.value = null
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
                } else {
                    "No enrolled subjects loaded yet."
                }

                val tasksContext = if (_tasks.value.isNotEmpty()) {
                    _tasks.value.joinToString("\n") { "- ${it.title} [Subject: ${it.category}, Due: ${it.dueDate}, Priority: ${it.priority}, Done: ${it.isCompleted}]" }
                } else {
                    "No active tasks."
                }

                val notesContext = if (_studyNotes.value.isNotEmpty()) {
                    _studyNotes.value.joinToString("\n") { "- ${it.title} (${it.category})" }
                } else {
                    "No uploaded notes."
                }

                val systemPrompt = """You are Jarvis, an intelligent and friendly AI study co-pilot built into the StudySync app for university students.
The student is: ${_userName.value.ifBlank { "Student" }}
Current Semester: ${_semesterId.value}

Enrolled Subjects (from university/Supabase database):
$coursesContext

Assigned / Pending Tasks (from Supabase):
$tasksContext

Uploaded Study Notes:
$notesContext

Guidelines:
1. Always ground your responses in their actual subjects, courses, assignments, and tasks whenever relevant.
2. If they ask about an upcoming task or schedule, check the tasks list above.
3. If they ask conceptual questions about any topic, give clear, pedagogically sound, and concise explanations with examples.
4. Keep responses encouraging, well-formatted, and concise."""

                val recentMessages = _jarvisMessages.value.takeLast(6).map { m ->
                    AiChatMessage(
                        role = if (m.isUser) "user" else "assistant",
                        content = m.text
                    )
                }

                val reply = aiService.chat(recentMessages, systemPrompt)
                val replyTime = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
                val assistantMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderName = "Jarvis AI",
                    text = reply,
                    timestamp = replyTime,
                    isAssistant = true
                )
                _jarvisMessages.update { it + assistantMsg }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errTime = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderName = "Jarvis AI",
                    text = "I encountered an error connecting to the AI service: ${e.message ?: "Connection timeout"}. Please check your network and try again.",
                    timestamp = errTime,
                    isAssistant = true
                )
                _jarvisMessages.update { it + errorMsg }
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

    fun toggleTactileVibrations()=action{cloud.insert("user_settings",mapOf("user_id" to cloud.userId,"vibrations" to !_tactileVibrations.value),true);loadCloud()}
    fun toggleAiCopilot(){_aiCopilotEnabled.update { !it }}
    fun toggleNotifications()=action{cloud.insert("user_settings",mapOf("user_id" to cloud.userId,"notifications" to !_notificationsEnabled.value),true);loadCloud()}
}
