package com.studysync.app.data.model

enum class TaskPriority { HIGH, MEDIUM, STANDARD }

data class TaskItem(
    val id: String,
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val category: String,
    val dueDate: String,
    val isCompleted: Boolean = false,
    val dueAt: String = ""
)

data class ScheduleEvent(
    val id: String,
    val title: String,
    val category: String,
    val time: String,
    val duration: String,
    val location: String,
    val dateDay: Int,
    val date: String = "",
    val readOnly: Boolean = false
)

data class AcademicCourse(
    val courseCode: String,
    val title: String,
    val faculty: String,
    val classId: String,
    val slots: String,
    val venue: String,
    val attendance: String = ""
) {
    val label: String get() = "$courseCode · $title"
}

data class AcademicSnapshot(
    val semesterId: String,
    val courses: List<AcademicCourse>,
    val exams: List<ScheduleEvent>,
    val warnings: List<String> = emptyList()
)

data class StudyNote(
    val id: String,
    val title: String,
    val category: String,
    val teacher: String,
    val fileCount: Int,
    val lastUpdated: String,
    val noteType: String,
    val isCommon: Boolean = false
)

data class QuizConfig(
    val subject: String = "Requirements",
    val materialSource: String = "Lecture 8: Concurrency Primitives",
    val questionCount: Int = 10,
    val difficulty: String = "Medium",
    val questionType: String = "Multiple Choice"
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String = ""
)

data class LeaderboardUser(
    val rank: Int,
    val name: String,
    val points: Int,
    val streakDays: Int,
    val avatarInitial: String
)

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isAssistant: Boolean = false,
    val isUser: Boolean = false,
    val categoryBadge: String? = null
)

data class StudyPod(
    val id: String,
    val name: String,
    val teacher: String,
    val activeTopic: String,
    val membersCount: Int,
    val isLive: Boolean = true,
    val initials: String,
    val courseCode: String = "",
    val podIds: List<String> = listOf(id),
    val kind: String = "THEORY"
)
