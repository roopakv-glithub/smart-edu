package com.studysync.app.data.mapper

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class AcademicMappersTest {
    @Test fun `subjects retain verified faculty and class rather than infer from registration`() {
        val body=JsonParser.parseString("""{"attRes":{"attendance":[{"courseCode":"CSE1001(T)","courseTitle":"Programming","faculty":"Faculty A","classId":"CL123","slotName":"A1","slotVenue":"AB101","attendancePercentage":95},{"courseCode":"","courseTitle":"bad"}]}}""").asJsonObject
        val rows=AcademicMappers.courses(body)
        assertEquals(1,rows.size)
        assertEquals("CL123",rows.single().classId)
        assertEquals("Faculty A",rows.single().faculty)
        assertEquals("95",rows.single().attendance)
    }
    @Test(expected=IllegalArgumentException::class) fun `missing attendance fails instead of showing empty success`() {
        AcademicMappers.courses(JsonParser.parseString("{}").asJsonObject)
    }
    @Test fun `exam days include year and month`() {
        val body=JsonParser.parseString("""{"Schedule":{"CAT 1":[{"courseCode":"CSE1001","courseTitle":"Programming","examDate":"03-Oct-2026","examTime":"09:00","venue":"AB101"}]}}""").asJsonObject
        val event=AcademicMappers.exams(body).single()
        assertEquals("2026-10-03",event.date)
        assertTrue(event.readOnly)
    }
    @Test fun `invalid and trailing dates are rejected`() {
        assertNull(AcademicMappers.normalizeDate("31-Feb-2026"))
        assertNull(AcademicMappers.normalizeDate("2026-10-03garbage"))
        assertEquals("2026-10-03",AcademicMappers.normalizeDate("03/10/2026"))
    }
}
