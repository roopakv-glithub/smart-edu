package com.studysync.app.data.mapper

import com.studysync.app.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class PresentationMappersTest {
    private fun pod(id:String,code:String,teacher:String="50289 M. PREMALATHA SCOPE")=StudyPod(id,"Database Systems",teacher,"",1,false,"DS",code)
    @Test fun `theory and lab form one embedded subject without losing channel ids`() {
        val group=PresentationMappers.groups(listOf(pod("lab","IASWE206(L)"),pod("theory","IASWE206(T)"))).single()
        assertEquals("EMBEDDED",group.kind);assertEquals("theory",group.id)
        assertEquals(listOf("theory","lab"),group.podIds)
        assertEquals("M. PREMALATHA",group.teacher)
    }
    @Test fun `lab only and different teachers remain distinguishable`() {
        assertEquals("LAB",PresentationMappers.groups(listOf(pod("a","CSE101(L)"))).single().kind)
        assertEquals(2,PresentationMappers.groups(listOf(pod("a","CSE101(T)","Teacher A"),pod("b","CSE101(L)","Teacher B"))).size)
        assertEquals("",PresentationMappers.teacher("N/A"))
    }
    @Test fun `calendar uses local due date across midnight`() {
        val task=TaskItem("t","Assignment","",TaskPriority.HIGH,"CSE","",false,"2026-10-03T20:00:00.123+00:00")
        val event=PresentationMappers.taskEvent(task,TimeZone.getTimeZone("Asia/Kolkata"))!!
        assertEquals("2026-10-04",event.date);assertEquals(4,event.dateDay);assertEquals("1:30 AM",event.time)
        assertTrue(event.readOnly);assertEquals("task:t",event.id)
    }
    @Test fun `word count treats repeated whitespace and newlines consistently`() {
        assertEquals(25,PresentationMappers.wordCount((1..25).joinToString(" \n "){"word"}))
        assertEquals(26,PresentationMappers.wordCount((1..26).joinToString(" "){"word"}))
        assertEquals(0,PresentationMappers.wordCount("   "))
    }
}
