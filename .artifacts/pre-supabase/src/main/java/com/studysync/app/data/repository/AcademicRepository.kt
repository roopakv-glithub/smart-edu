package com.studysync.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.studysync.app.data.config.ApiConfig
import com.studysync.app.data.mapper.UniCcMappers
import com.studysync.app.data.network.unicc.*
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

sealed class AuthResult {
    data class Success(val session: UniCcSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AcademicRepository(
    private val apiService: UniCcApiService = createService()
) {
    // VTOP session only, kept in memory. No password cache or HTTP body logging.
    var currentSession: UniCcSession? = null
        private set

    fun logout() { currentSession = null }

    suspend fun login(userId: String, password: String): AuthResult {
        currentSession = null
        if (userId.isBlank() || password.isBlank()) {
            return AuthResult.Error("User ID and password cannot be empty.")
        }
        return try {
            val request = LoginRequest(username = userId.trim(), password = password)
            // Retry only an explicit server-side captcha rejection, once.
            repeat(2) { attempt ->
                val response = apiService.login(request)
                val body = if (response.isSuccessful) response.body() else {
                    response.errorBody()?.use { errorBody ->
                        try { Gson().fromJson(errorBody.string(), LoginResponse::class.java) }
                        catch (_: JsonParseException) { null }
                    }
                }
                val message = body?.message.orEmpty() + " " + body?.error.orEmpty()
                if (response.code() == 401 && message.contains("Invalid Captcha", ignoreCase = true) && attempt == 0) {
                    return@repeat
                }
                if (response.isSuccessful && body?.success == true) {
                    val session = UniCcMappers.mapLoginResponseToSession(body)
                        ?: return AuthResult.Error("UniCC returned incomplete session data. Please try again or update the app.")
                    currentSession = session
                    return AuthResult.Success(session)
                }
                return AuthResult.Error(loginFailure(response.code(), message))
            }
            AuthResult.Error("VTOP captcha verification failed. Please try signing in again.")
        } catch (e: CancellationException) {
            throw e
        } catch (_: UnknownHostException) {
            AuthResult.Error("Cannot find the UniCC server. Check your internet connection or DNS and try again. If this persists, the backend address may have changed.")
        } catch (_: SocketTimeoutException) {
            AuthResult.Error("UniCC or VTOP took too long to respond. Please try again shortly.")
        } catch (_: SSLException) {
            AuthResult.Error("Cannot establish a secure connection to UniCC. Check your device date and network.")
        } catch (_: JsonParseException) {
            AuthResult.Error("UniCC returned an unexpected response. Please try again or update the app.")
        } catch (_: MalformedJsonException) {
            AuthResult.Error("UniCC returned an unexpected response. Please try again or update the app.")
        } catch (_: IOException) {
            AuthResult.Error("Unable to reach UniCC. Check your connection and try again.")
        }
    }

    private fun loginFailure(code: Int, message: String): String = when {
        code == 401 && message.contains("captcha", ignoreCase = true) ->
            "VTOP captcha verification failed. Please try signing in again."
        code == 401 && (message.contains("expired", ignoreCase = true) || message.contains("change your password", ignoreCase = true)) ->
            "Your VTOP password has expired. Change it on the official VTOP website, then sign in again."
        code == 401 -> "Invalid VTOP username or password. Please check your credentials."
        code == 429 -> "Too many sign-in attempts. Please wait and try again."
        code == 403 -> "UniCC refused access. Please try another network or contact the backend maintainer."
        code == 404 -> "The UniCC login endpoint is unavailable. The app's backend address may need updating."
        code >= 500 && message.contains("captcha", ignoreCase = true) ->
            "UniCC could not retrieve the VTOP captcha. Please try again shortly."
        code >= 500 -> "UniCC or VTOP is temporarily unavailable. Please try again shortly."
        code in 300..399 -> "UniCC redirected the login request. Update the backend address before signing in."
        else -> "UniCC could not complete sign-in. Please try again or update the app."
    }

    companion object {
        internal fun createService(baseUrl: String = ApiConfig.UNICC_BASE_URL): UniCcApiService {
            val client = OkHttpClient.Builder()
                // Never forward login credentials to a redirect destination.
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UniCcApiService::class.java)
        }
    }
}
