package com.studysync.app.data.repository

import com.google.gson.*
import com.studysync.app.data.session.*
import com.studysync.app.data.config.AcademicConfig
import com.studysync.app.data.config.SupabaseConfig
import com.studysync.app.data.model.*
import com.studysync.app.data.network.unicc.UniCcSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudException(message: String, val status: Int = 0): IOException(message)
fun JsonObject.string(key: String): String = get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
class SupabaseRepository(private val store: SessionStore = MemorySessionStore(), private val baseUrl: String = SupabaseConfig.SUPABASE_URL) {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(110, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS).followRedirects(false).retryOnConnectionFailure(false).build()
    private val gson = Gson()
    private var accessToken = ""
    private var refreshToken = ""
    private var expiresAt = 0L
    var userId = ""; private set
    private val mutex = Mutex()
    private var generation = 0
    private var saved = store.read() ?: JsonObject()
    val hasSession get() = accessToken.isNotBlank() && refreshToken.isNotBlank() && userId.isNotBlank()
    val displayName get() = saved.string("display_name").takeIf { it.isNotBlank() && it != registrationNumber } ?: "Name was not fetched"
    fun saveDisplayName(name:String) { saved.addProperty("display_name",name);store.write(saved) }
    val registrationNumber get() = saved.string("registration_number")
    var selectedSemester: String
        get() = saved.string("semester_id").ifBlank { AcademicConfig.DEFAULT_SEMESTER }
        set(value) { saved.addProperty("semester_id",value); store.write(saved) }
    init {
        if(saved.string("refresh_token").isNotBlank()) {
            accessToken=saved.string("access_token"); refreshToken=saved.string("refresh_token"); userId=saved.string("user_id")
            expiresAt=saved.get("expires_at")?.takeIf { !it.isJsonNull }?.asLong ?: 0
        }
    }
    fun logout() { generation++; accessToken=""; refreshToken=""; userId=""; expiresAt=0; saved=JsonObject(); store.clear() }
    fun cachedSnapshot(semester: String): AcademicSnapshot? = saved.getAsJsonObject("snapshots")?.getAsJsonObject(semester)?.let { gson.fromJson(it,AcademicSnapshot::class.java) }
    suspend fun fetchSnapshot(semester: String): AcademicSnapshot? {
        val row=rows("academic_snapshots",mapOf("user_id" to "eq.$userId","semester_id" to "eq.$semester")).firstOrNull() ?: return null
        val snapshot=AcademicSnapshot(semester,row.getAsJsonArray("courses").map { gson.fromJson(it,AcademicCourse::class.java) },row.getAsJsonArray("exams").map { gson.fromJson(it,ScheduleEvent::class.java) })
        val snapshots=saved.getAsJsonObject("snapshots") ?: JsonObject().also { saved.add("snapshots",it) }
        snapshots.add(semester,gson.toJsonTree(snapshot)); store.write(saved)
        return snapshot
    }
    private fun session(body: JsonObject) {
        accessToken=body.string("access_token"); refreshToken=body.string("refresh_token")
        expiresAt=body.get("expires_at")?.takeIf { !it.isJsonNull }?.asLong ?: (System.currentTimeMillis()/1000+(body.get("expires_in")?.asLong ?: 3500))
        userId=body.string("user_id").ifBlank { body.getAsJsonObject("user")?.string("id").orEmpty() }
        if(accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) throw CloudException("Could not connect your account. Please retry.")
        listOf("access_token","refresh_token").forEach { saved.add(it,body.get(it)) }
        saved.addProperty("expires_at",expiresAt)
        saved.addProperty("user_id",userId)
        if(body.string("registration_number").isNotBlank()) saved.addProperty("registration_number",body.string("registration_number"))
        store.write(saved)
    }
    private suspend fun token(rejected: String? = null): String = mutex.withLock {
        if(accessToken.isBlank()) throw CloudException("Sign in to sync your data.")
        if(expiresAt < System.currentTimeMillis()/1000+60 || (rejected != null && rejected == accessToken)) {
            val epoch=generation
            val data=raw("auth/v1/token?grant_type=refresh_token","POST",gson.toJson(mapOf("refresh_token" to refreshToken)),"").asJsonObject
            if(epoch!=generation) throw CloudException("Signed out")
            session(data)
        }
        accessToken
    }
    private suspend fun raw(path: String, method: String="GET", json: String?=null, bearer: String="", bytes: ByteArray?=null, mime: String="application/json", prefer: String="return=representation"): JsonElement = withContext(Dispatchers.IO) {
        val builder=Request.Builder().url(baseUrl+path).header("apikey",SupabaseConfig.SUPABASE_KEY).header("Prefer",prefer)
        if(bearer.isNotBlank()) builder.header("Authorization","Bearer $bearer")
        val body=bytes?.toRequestBody(mime.toMediaType()) ?: json?.toRequestBody("application/json".toMediaType())
        builder.method(method,body)
        client.newCall(builder.build()).execute().use { response ->
            val result=response.body?.string().orEmpty()
            if(!response.isSuccessful) {
                val message=when(response.code) {
                    401 -> if(path.startsWith("functions/")) "VTOP could not confirm this connection. Your saved app data remains available." else "Could not renew your app connection. Please reconnect your account."
                    403 -> "Your verified class membership does not allow this action."
                    429 -> "Too many requests. Try again in a minute."
                    400 -> "Check the selected subject, date and required fields."
                    else -> "Cloud request failed (${response.code}). Retry; your changes were not confirmed."
                }
                throw CloudException(message,response.code)
            }
            if(result.isBlank()) JsonNull.INSTANCE else JsonParser.parseString(result)
        }
    }
    suspend fun sync(vtop: UniCcSession, semester: String): AcademicSnapshot {
        val epoch=generation
        val body=raw("functions/v1/studysync-session","POST",gson.toJson(mapOf("cookies" to vtop.cookies,"semesterId" to semester))).asJsonObject
        if(epoch!=generation) throw CloudException("Signed out")
        session(body)
        // Read back the persisted snapshot so the displayed data matches storage.
        selectedSemester=semester
        return (fetchSnapshot(semester) ?: throw CloudException("No saved subjects returned.")).copy(warnings=body.getAsJsonObject("snapshot").getAsJsonArray("warnings").map { it.asString })
    }
    private suspend fun authorized(path: String, method: String="GET", json: String?=null, bytes: ByteArray?=null, mime: String="application/json", prefer: String="return=representation"): JsonElement {
        val bearer=token()
        return try { raw(path,method,json,bearer,bytes,mime,prefer) }
        catch(e:CloudException) {
            if(e.status!=401) throw e
            raw(path,method,json,token(rejected=bearer),bytes,mime,prefer)
        }
    }
    suspend fun rows(table: String, query: Map<String,String> = emptyMap()): List<JsonObject> {
        val url=HttpUrl.Builder().scheme("https").host("placeholder.invalid").addPathSegments("rest/v1/$table").addQueryParameter("select","*")
        query.forEach { (k,v)->url.addQueryParameter(k,v) }
        val path=url.build().toString().substringAfter("placeholder.invalid/")
        return authorized(path).asJsonArray.map { it.asJsonObject }
    }
    suspend fun insert(table: String, values: Map<String,Any?>, upsert: Boolean=false) {
        authorized("rest/v1/$table","POST",gson.toJson(values),prefer=if(upsert) "resolution=merge-duplicates,return=representation" else "return=representation")
    }
    suspend fun patch(table: String, id: String, values: Map<String,Any?>) {
        authorized("rest/v1/$table?id=eq.$id","PATCH",gson.toJson(values))
    }
    suspend fun upload(path: String, bytes: ByteArray, mime: String) {
        authorized("storage/v1/object/study-notes/$path","POST",bytes=bytes,mime=mime)
    }
    suspend fun noteUrl(path: String): String {
        val response=authorized("storage/v1/object/sign/study-notes/$path","POST","{\"expiresIn\":300}").asJsonObject
        return SupabaseConfig.SUPABASE_URL.trimEnd('/')+"/storage/v1"+response.string("signedURL")
    }
}
