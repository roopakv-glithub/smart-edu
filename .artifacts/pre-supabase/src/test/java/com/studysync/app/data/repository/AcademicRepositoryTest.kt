package com.studysync.app.data.repository

import com.google.gson.JsonParser
import com.studysync.app.data.network.unicc.UniCcApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class AcademicRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AcademicRepository
    private val valid = """{"success":true,"cookies":"JSESSIONID=test; Path=/; HttpOnly","csrf":"test-csrf","authorizedID":"TEST123"}"""

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        repository = AcademicRepository(AcademicRepository.createService(server.url("/").toString()))
    }
    @After fun teardown() { server.shutdown() }
    private fun enqueue(code: Int = 200, body: String = valid) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }
    private fun error(result: AuthResult) = (result as AuthResult.Error).message

    @Test fun loginUsesActualContractAndPreservesPassword() = runBlocking {
        enqueue()
        val result = repository.login(" TEST123 ", " password ") as AuthResult.Success
        assertEquals("TEST123", result.session.authorizedID)
        assertEquals("JSESSIONID=test; Path=/; HttpOnly", result.session.cookies)
        assertSame(result.session, repository.currentSession)
        val request = server.takeRequest()
        assertEquals("/api/login", request.path)
        assertEquals("POST", request.method)
        val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(setOf("username", "password"), json.keySet())
        assertEquals("TEST123", json["username"].asString)
        assertEquals(" password ", json["password"].asString)
    }
    @Test fun emptyCredentialsDoNotMakeRequests() = runBlocking {
        assertTrue(error(repository.login("", "p")).contains("empty"))
        assertEquals(0, server.requestCount)
    }
    @Test fun incompleteSessionsNeverAuthenticate() = runBlocking {
        for (field in listOf("cookies", "csrf", "authorizedID")) {
            for (value in listOf("", " ")) {
                val json = JsonParser.parseString(valid).asJsonObject
                json.addProperty(field, value)
                enqueue(body = json.toString())
                assertTrue(error(repository.login("u", "p")).contains("incomplete"))
                assertNull(repository.currentSession)
            }
            val json = JsonParser.parseString(valid).asJsonObject
            json.remove(field)
            enqueue(body = json.toString())
            assertTrue(error(repository.login("u", "p")).contains("incomplete"))
        }
    }
    @Test fun captchaFailureRetriesOnceThenSucceeds() = runBlocking {
        enqueue(401, """{"success":false,"message":"Invalid Captcha"}""")
        enqueue()
        assertTrue(repository.login("u", "p") is AuthResult.Success)
        assertEquals(2, server.requestCount)
    }
    @Test fun repeatedCaptchaFailureStopsAfterTwoAttempts() = runBlocking {
        repeat(2) { enqueue(401, """{"success":false,"message":"Invalid Captcha"}""") }
        assertTrue(error(repository.login("u", "p")).contains("captcha"))
        assertEquals(2, server.requestCount)
    }
    @Test fun invalidPasswordIsNotRetried() = runBlocking {
        enqueue(401, """{"success":false,"message":"Invalid Username / Password"}""")
        assertTrue(error(repository.login("u", "p")).contains("Invalid VTOP"))
        assertEquals(1, server.requestCount)
    }
    @Test fun expiredPasswordHasActionableMessage() = runBlocking {
        enqueue(401, """{"success":false,"message":"Please visit VTOP and change your password, it has expired after the usual 3 month period"}""")
        assertTrue(error(repository.login("u", "p")).contains("official VTOP"))
    }
    @Test fun upstreamCaptchaErrorIsNotInvalidCredentials() = runBlocking {
        enqueue(500, """{"success":false,"error":"Failed to get captcha"}""")
        assertTrue(error(repository.login("u", "p")).contains("retrieve the VTOP captcha"))
    }
    @Test fun gatewayHtmlErrorKeepsHttpClassification() = runBlocking {
        enqueue(502, "<html>Gateway failure</html>")
        assertTrue(error(repository.login("u", "p")).contains("temporarily unavailable"))
    }
    @Test fun malformedSuccessIsAResponseError() = runBlocking {
        enqueue(body = "<html>Not JSON</html>")
        assertTrue(error(repository.login("u", "p")).contains("unexpected response"))
        assertNull(repository.currentSession)
    }
    @Test fun logoutAndFailedReloginClearSession() = runBlocking {
        enqueue()
        repository.login("u", "p")
        repository.logout()
        assertNull(repository.currentSession)
        enqueue()
        repository.login("u", "p")
        enqueue(401, "{}")
        repository.login("u", "wrong")
        assertNull(repository.currentSession)
    }
    @Test fun rateLimitIsNotRetried() = runBlocking {
        enqueue(429, "{}")
        assertTrue(error(repository.login("u", "p")).contains("Too many"))
        assertEquals(1, server.requestCount)
    }
    @Test fun redirectDoesNotForwardCredentials() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", server.url("/other")))
        assertTrue(error(repository.login("u", "p")).contains("redirected"))
        assertEquals(1, server.requestCount)
    }
    private fun failingRepository(failure: Exception): AcademicRepository {
        val client = OkHttpClient.Builder().addInterceptor { throw failure }.build()
        val service = Retrofit.Builder().baseUrl(server.url("/")).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(UniCcApiService::class.java)
        return AcademicRepository(service)
    }
    @Test fun dnsFailureDoesNotLeakExceptionDetails() = runBlocking {
        val message = error(failingRepository(UnknownHostException("private detail")).login("u", "p"))
        assertTrue(message.contains("DNS"))
        assertFalse(message.contains("private detail"))
    }
    @Test fun timeoutAndTlsHaveDistinctMessages() = runBlocking {
        assertTrue(error(failingRepository(SocketTimeoutException()).login("u", "p")).contains("too long"))
        assertTrue(error(failingRepository(SSLHandshakeException("private detail")).login("u", "p")).contains("secure connection"))
    }

    @Test fun coroutineCancellationIsNotPresentedAsNetworkFailure() = runBlocking {
        val service = java.lang.reflect.Proxy.newProxyInstance(
            UniCcApiService::class.java.classLoader, arrayOf(UniCcApiService::class.java)
        ) { _, _, _ -> throw CancellationException("cancelled") } as UniCcApiService
        try {
            AcademicRepository(service).login("u", "p")
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            // Expected: navigating away must not emit a spurious login error.
        }
    }
}
