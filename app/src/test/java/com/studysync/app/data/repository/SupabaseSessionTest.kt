package com.studysync.app.data.repository

import com.google.gson.JsonParser
import com.studysync.app.data.session.MemorySessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class SupabaseSessionTest {
    private fun saved(expires:Long)=MemorySessionStore().apply { write(JsonParser.parseString("""{"access_token":"old","refresh_token":"refresh-one","expires_at":$expires,"user_id":"user-one","registration_number":"TESTUSER","semester_id":"CH20262701"}""").asJsonObject) }
    private val renewed="""{"access_token":"new","refresh_token":"refresh-two","expires_at":4102444800,"user":{"id":"user-one"}}"""
    @Test fun `expired token refreshes without university login and persists rotation`()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            val store=saved(0)
            val repo=SupabaseRepository(store,server.url("/").toString())
            server.enqueue(MockResponse().setBody(renewed));server.enqueue(MockResponse().setBody("[]"))
            repo.rows("tasks")
            assertEquals("/auth/v1/token?grant_type=refresh_token",server.takeRequest().path)
            assertEquals("Bearer new",server.takeRequest().getHeader("Authorization"))
            assertEquals("refresh-two",store.read()!!.string("refresh_token"))
            val reopened=SupabaseRepository(store,server.url("/").toString())
            assertTrue(reopened.hasSession);assertEquals("TESTUSER",reopened.registrationNumber)
            server.enqueue(MockResponse().setBody("[]"));reopened.rows("tasks")
            assertTrue(server.takeRequest().path!!.startsWith("/rest/v1/tasks"))
        } finally {server.shutdown()}
    }
    @Test fun `rejected access token retries once using renewed app session`()=runBlocking {
        val server=MockWebServer();server.start()
        try {
            val repo=SupabaseRepository(saved(4102444800),server.url("/").toString())
            server.enqueue(MockResponse().setResponseCode(401));server.enqueue(MockResponse().setBody(renewed));server.enqueue(MockResponse().setBody("[]"))
            repo.rows("pods")
            assertEquals("Bearer old",server.takeRequest().getHeader("Authorization"))
            assertTrue(server.takeRequest().path!!.startsWith("/auth/v1/token"))
            assertEquals("Bearer new",server.takeRequest().getHeader("Authorization"))
        } finally {server.shutdown()}
    }
    @Test fun `sign out removes persisted identity and snapshot`() {
        val store=saved(4102444800)
        val repo=SupabaseRepository(store)
        repo.logout()
        assertNull(store.read());assertFalse(SupabaseRepository(store).hasSession)
    }
}
