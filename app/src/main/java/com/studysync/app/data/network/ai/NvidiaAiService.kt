package com.studysync.app.data.network.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.studysync.app.data.config.AiConfig
import com.studysync.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AiChatMessage(val role: String, val content: String)

/**
 * AI service powered by NVIDIA Nemotron API for:
 * 1. AI Syllabus Analysis & Roadmap Generation (modules, topics, subtopics)
 * 2. Note-Grounded Quiz Generation
 * 3. Document Extraction Summarization & Core Ideas
 * 4. Jarvis AI Study Co-Pilot
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
     * Requirement 6: AI Syllabus Analysis
     * Accurately analyzes syllabus text, identifies real number of modules, topics, and subtopics,
     * preserving titles, order, and structure without inventing unsupported content.
     */
    suspend fun analyzeSyllabus(
        syllabusText: String,
        courseTitle: String
    ): List<SyllabusModule> = withContext(Dispatchers.IO) {
        val prompt = """You are an expert curriculum analysis AI. Analyze the following actual syllabus for "$courseTitle".
Accurately identify all distinct modules/units, their respective topics, and subtopics.
CRITICAL INSTRUCTIONS:
- Identify the genuine number of modules in the syllabus. If there are 4, 5, 6, or any number, represent exactly that number.
- Do NOT invent fake topics or alter the syllabus ordering.
- Return ONLY a valid JSON array of modules with no markdown, backticks, or other text.

JSON Schema:
[
  {
    "moduleNumber": 1,
    "title": "Module title",
    "topics": [
      {
        "title": "Topic title",
        "subtopics": [
          {
            "title": "Subtopic title",
            "description": "Brief description"
          }
        ]
      }
    ]
  }
]

Actual Syllabus Text:
${syllabusText.take(8000)}"""

        val result = chatCompletion(
            messages = listOf(
                AiChatMessage("system", "You are a precise curriculum parser that outputs ONLY valid JSON arrays conforming strictly to the requested schema."),
                AiChatMessage("user", prompt)
            ),
            temperature = 0.3
        )

        parseSyllabusModules(result)
    }

    /**
     * Requirement 11: Generate Quiz from Study Notes and Syllabus Topics
     * Grounds questions in actual study notes content and syllabus topics.
     */
    suspend fun generateQuiz(
        subject: String,
        questionCount: Int,
        difficulty: String,
        courseContext: String = "",
        studyNotesContent: String = "",
        moduleTitle: String = "",
        topicTitle: String = ""
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val notesContext = if (studyNotesContent.isNotBlank()) {
            "\nRELEVANT STUDY NOTES:\n${studyNotesContent.take(5000)}"
        } else ""

        val scopeContext = StringBuilder()
        if (moduleTitle.isNotBlank()) scopeContext.append("Target Module: $moduleTitle\n")
        if (topicTitle.isNotBlank()) scopeContext.append("Target Topic: $topicTitle\n")
        if (courseContext.isNotBlank()) scopeContext.append("Course Context: $courseContext\n")

        val prompt = """You are an academic test generator. Generate exactly $questionCount multiple-choice questions for subject "$subject" at $difficulty difficulty level.
$scopeContext$notesContext

REQUIREMENTS:
1. Ground the questions firmly in the provided study notes and syllabus topic.
2. Provide exactly 4 options per question.
3. Indicate the zero-based index of the correct answer (0, 1, 2, or 3).
4. Provide a clear pedagogical explanation for the correct answer.
5. Return ONLY a valid JSON array. No markdown, no commentary.

Format:
[
  {
    "question": "Question text here?",
    "options": ["Option A", "Option B", "Option C", "Option D"],
    "correctIndex": 0,
    "explanation": "Why this option is correct."
  }
]"""

        val result = chatCompletion(
            messages = listOf(
                AiChatMessage("system", "You are an academic quiz generator that outputs strictly JSON arrays of questions."),
                AiChatMessage("user", prompt)
            ),
            temperature = 0.6
        )

        parseQuizQuestions(result)
    }

    /**
     * Requirement 16: Comprehensive AI Document Summarization
     * Generates structured study notes with concepts, definitions, formulas, and key takeaways.
     */
    suspend fun summarizeDocument(
        documentText: String,
        title: String
    ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        val prompt = """You are an expert academic summarizer. Create a comprehensive, organized study summary of the document titled "$title".

REQUIREMENTS:
1. Retain important definitions, formulas, technical terminology, and exam-relevant details.
2. Structure with clear Markdown headers (## Key Concepts, ## Explanations, ## Formulas & Equations, ## Exam Highlights).
3. Do not over-summarize into a short blurb; make it truly useful for thorough revision.
4. Also extract 5 to 8 bullet core idea keywords.

Document Content:
${documentText.take(10000)}"""

        val summary = chatCompletion(
            messages = listOf(
                AiChatMessage("system", "You are an academic summarization specialist."),
                AiChatMessage("user", prompt)
            ),
            temperature = 0.5
        )

        val keyConceptsPrompt = """From this study summary, extract a JSON array of 5-8 key concept strings:
$summary

Respond ONLY with JSON: ["concept1", "concept2", ...]"""

        val conceptsJson = try {
            chatCompletion(
                messages = listOf(AiChatMessage("user", keyConceptsPrompt)),
                temperature = 0.2
            )
        } catch (_: Exception) { "[]" }

        val conceptsList = parseKeyConcepts(conceptsJson)
        Pair(summary, conceptsList)
    }

    /**
     * Requirement 18: Jarvis AI Chat Co-Pilot
     */
    suspend fun chat(
        messages: List<AiChatMessage>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val allMessages = mutableListOf(AiChatMessage("system", systemPrompt))
        allMessages.addAll(messages)
        chatCompletion(allMessages, temperature = 0.7)
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
        val jsonStr = extractJsonArray(response)
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
            throw IOException("Could not parse AI quiz questions. Please retry.", e)
        }
    }

    private fun parseSyllabusModules(response: String): List<SyllabusModule> {
        val jsonStr = extractJsonArray(response)
        return try {
            val array = JsonParser.parseString(jsonStr).asJsonArray
            array.mapIndexedNotNull { modIdx, modElem ->
                try {
                    val modObj = modElem.asJsonObject
                    val modNum = modObj.get("moduleNumber")?.asInt ?: (modIdx + 1)
                    val modTitle = modObj.get("title")?.asString ?: "Module $modNum"
                    val topicsArray = modObj.getAsJsonArray("topics") ?: JsonArray()

                    val topics = topicsArray.mapIndexedNotNull { topIdx, topElem ->
                        try {
                            val topObj = topElem.asJsonObject
                            val topTitle = topObj.get("title")?.asString ?: "Topic ${topIdx + 1}"
                            val subArray = topObj.getAsJsonArray("subtopics") ?: JsonArray()

                            val subtopics = subArray.mapIndexedNotNull { subIdx, subElem ->
                                try {
                                    val subObj = subElem.asJsonObject
                                    SyllabusSubtopic(
                                        id = UUID.randomUUID().toString(),
                                        title = subObj.get("title")?.asString ?: "Subtopic ${subIdx + 1}",
                                        description = subObj.get("description")?.asString ?: ""
                                    )
                                } catch (_: Exception) { null }
                            }

                            SyllabusTopic(
                                id = UUID.randomUUID().toString(),
                                title = topTitle,
                                subtopics = subtopics
                            )
                        } catch (_: Exception) { null }
                    }

                    SyllabusModule(
                        id = UUID.randomUUID().toString(),
                        moduleNumber = modNum,
                        title = modTitle,
                        topics = topics
                    )
                } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            throw IOException("Could not parse syllabus roadmap. Please retry.", e)
        }
    }

    private fun parseKeyConcepts(response: String): List<String> {
        return try {
            val jsonStr = extractJsonArray(response)
            val array = JsonParser.parseString(jsonStr).asJsonArray
            array.mapNotNull { it.asString }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractJsonArray(text: String): String {
        val cleaned = text
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        return if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
    }
}
