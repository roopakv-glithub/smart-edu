package com.studysync.app.data.mapper

import com.studysync.app.data.model.*
import java.text.SimpleDateFormat
import java.util.*

object PresentationMappers {
    fun subjectCode(code:String)=code.replace(Regex("\\s*\\([^)]*\\)\\s*$"),"").trim()
    fun title(value:String, code:String)=value.replace(Regex("\\s+")," ").trim().removePrefix(subjectCode(code)+" - ").trim()
    fun teacher(value:String):String {
        val clean=value.replace(Regex("\\s+")," ").trim()
        if(clean.uppercase() in listOf("","N/A","NA","NIL","NULL","-","NOT ASSIGNED","NOT AVAILABLE")) return ""
        return clean.replace(Regex("^\\d+\\s+"),"").replace(Regex("\\s*-?\\s+(SCOPE|SSL|SAS|ACAD)$"),"").trim().trimEnd('-').trim()
    }
    fun groups(pods:List<StudyPod>):List<StudyPod> = pods.groupBy { subjectCode(it.courseCode)+"|"+teacher(it.teacher) }.values.map { group ->
        val sorted=group.sortedBy { if(it.courseCode.endsWith("(T)")) 0 else 1 }
        val first=sorted.first()
        val lab=group.any { it.courseCode.endsWith("(L)") || it.courseCode.endsWith("(P)") }
        val theory=group.any { it.courseCode.endsWith("(T)") }
        first.copy(name=title(first.name,first.courseCode),teacher=teacher(first.teacher),podIds=sorted.flatMap{it.podIds}.distinct(),kind=if(lab&&theory) "EMBEDDED" else if(lab) "LAB" else "THEORY")
    }.sortedBy { it.name }
    fun wordCount(value:String)=value.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    fun taskEvent(task:TaskItem, zone:TimeZone=TimeZone.getDefault()):ScheduleEvent? {
        val raw=task.dueAt.replace(Regex("\\.\\d+(?=[Z+-])"),"")
        val parser=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.ENGLISH).apply{isLenient=false}
        val parsed=runCatching{parser.parse(raw)}.getOrNull() ?: return null
        fun format(pattern:String)=SimpleDateFormat(pattern,Locale.ENGLISH).apply{timeZone=zone}.format(parsed)
        val date=format("yyyy-MM-dd")
        return ScheduleEvent("task:${task.id}",task.title,if(task.isCompleted) "Task · Done" else "Task",format("h:mm a"),"","",date.takeLast(2).toInt(),date,true)
    }
}
