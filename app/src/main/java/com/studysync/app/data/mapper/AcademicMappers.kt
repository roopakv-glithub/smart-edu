package com.studysync.app.data.mapper

import com.google.gson.JsonObject
import com.studysync.app.data.model.AcademicCourse
import com.studysync.app.data.model.ScheduleEvent
import java.text.SimpleDateFormat
import java.util.Locale

object AcademicMappers {
    private fun JsonObject.text(key: String): String = get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()

    fun courses(body: JsonObject): List<AcademicCourse> {
        val rows = body.getAsJsonObject("attRes")?.getAsJsonArray("attendance")
            ?: throw IllegalArgumentException("UniCC attendance response has changed")
        return rows.mapNotNull { element ->
            val row = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val code = row.text("courseCode")
            val title = row.text("courseTitle")
            if (code.isBlank() || title.isBlank()) return@mapNotNull null
            AcademicCourse(code, title, row.text("faculty"), row.text("classId"),
                row.text("slotName"), row.text("slotVenue"), row.text("attendancePercentage"))
        }.distinctBy { listOf(it.courseCode, it.classId, it.faculty, it.slots) }
    }

    fun exams(body: JsonObject): List<ScheduleEvent> {
        val groups = body.getAsJsonObject("Schedule")
            ?: throw IllegalArgumentException("UniCC exam response has changed")
        return groups.entrySet().flatMap { (group, rows) ->
            if (!rows.isJsonArray) return@flatMap emptyList()
            rows.asJsonArray.mapNotNull { element ->
                val row = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val date = normalizeDate(row.text("examDate")) ?: return@mapNotNull null
                val title = row.text("courseTitle").ifBlank { row.text("courseCode") }
                if (title.isBlank()) return@mapNotNull null
                ScheduleEvent("exam:${row.text("courseCode")}:$group:$date:${row.text("examTime")}",
                    title, group, row.text("examTime"), row.text("examSession"), row.text("venue"),
                    date.takeLast(2).toInt(), date, true)
            }
        }
    }

    fun normalizeDate(value: String): String? {
        for (pattern in listOf("dd-MMM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")) {
            val parser = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
            val position = java.text.ParsePosition(0)
            val parsed = parser.parse(value, position)
            if (parsed != null && position.index == value.length) {
                return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(parsed)
            }
        }
        return null
    }
}
