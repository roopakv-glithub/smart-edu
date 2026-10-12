package com.studysync.app.data.network.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.studysync.app.data.config.AiConfig
import com.studysync.app.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AiChatMessage(val role: String, val content: String)

/**
 * AI service for quiz generation and Jarvis chat using NVIDIA Nemotron API.
 * Uses OpenAI-compatible chat completions endpoint.
 */
class NvidiaAiService(
    private val baseUrl: String = AiConfig.NVIDIA_BASE_URL,
    private val apiKey: String = AiConfig.NVIDIA_API_KEY,
    private val model: String = AiConfig.NVIDIA_MODEL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val gson = Gson()

    /**
     * Generate quiz questions for a given subject using AI.
     * Returns a list of QuizQuestion objects.
     */
    suspend fun generateQuiz(
        subject: String,
        questionCount: Int,
        difficulty: String,
        courseContext: String = ""
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val contextInfo = if (courseContext.isNotBlank()) {
            "\nAdditional course context & syllabus details:\n$courseContext"
        } else ""

        val prompt = """You are an expert academic quiz generator. Generate exactly $questionCount multiple-choice questions about "$subject" at $difficulty difficulty level.$contextInfo

IMPORTANT: Respond ONLY with a valid JSON array. Do not include markdown formatting or backticks or explanatory text.
Each question object must contain:
- "question": string
- "options": array of exactly 4 strings
- "correctIndex": integer (0, 1, 2, or 3)
- "explanation": string

Format:
[{"question":"Question text","options":["Opt A","Opt B","Opt C","Opt D"],"correctIndex":0,"explanation":"Explanation..."}]"""

        val result = chatCompletion(
            messages = listOf(
                AiChatMessage("system", "You are a specialized JSON-only academic quiz generator. You output solely a JSON array without markdown formatting."),
                AiChatMessage("user", prompt)
            ),
            temperature = 0.7
        )

        parseQuizQuestions(result)
    }

    /**
     * Send a message to Jarvis AI and get a response.
     */
    suspend fun chat(
        messages: List<AiChatMessage>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val allMessages = mutableListOf(AiChatMessage("system", systemPrompt))
        allMessages.addAll(messages)
        chatCompletion(allMessages, temperature = 0.8)
    }

    private fun chatCompletion(
        messages: List<AiChatMessage>,
        temperature: Double = 0.7
    ): String {
        val messagesArray = JsonArray()
        messages.forEach { msg ->
            val obj = JsonObject()
            obj.addProperty("role", msg.role)
            obj.addProperty("content", msg.content)
            messagesArray.add(obj)
        }

        val body = JsonObject().apply {
            addProperty("model", model)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("top_p", 0.95)
            addProperty("max_tokens", 4096)
            addProperty("stream", false)
        }

        val request = Request.Builder()
            .url("${baseUrl}chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("AI request failed (${response.code}): ${responseBody.take(200)}")
            }
            val json = JsonParser.parseString(responseBody).asJsonObject
            val choices = json.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                throw IOException("AI returned no response.")
            }
            return choices[0].asJsonObject
                .getAsJsonObject("message")
                .get("content").asString.trim()
        }
    }

    private fun parseQuizQuestions(response: String): List<QuizQuestion> {
        val jsonStr = response
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
            .let { text ->
                val start = text.indexOf('[')
                val end = text.lastIndexOf(']')
                if (start >= 0 && end > start) text.substring(start, end + 1) else text
            }

        return try {
            val array = JsonParser.parseString(jsonStr).asJsonArray
            array.mapNotNull { element ->
                try {
                    val obj = element.asJsonObject
                    QuizQuestion(
                        question = obj.get("question")?.asString ?: return@mapNotNull null,
                        options = obj.getAsJsonArray("options")?.map { it.asString }
                            ?.takeIf { it.size == 4 } ?: return@mapNotNull null,
                        correctIndex = obj.get("correctIndex")?.asInt ?: 0,
                        explanation = obj.get("explanation")?.asString ?: ""
                    )
                } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            throw IOException("Could not parse AI quiz response. Please try again.", e)
        }
    }
}
