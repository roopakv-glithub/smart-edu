from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/data/repository/SupabaseRepository.kt');s=p.read_text()
s=s.replace('import com.google.gson.*','import com.google.gson.*\nimport com.studysync.app.data.session.*\nimport com.studysync.app.data.config.AcademicConfig')
s=s.replace('class CloudException(message: String): IOException(message)','class CloudException(message: String, val status: Int = 0): IOException(message)')
s=s.replace('class SupabaseRepository {','class SupabaseRepository(private val store: SessionStore = MemorySessionStore(), private val baseUrl: String = SupabaseConfig.SUPABASE_URL) {')
s=s.replace('    fun logout() { generation++; accessToken=""; refreshToken=""; userId=""; expiresAt=0 }','''    private var saved = store.read() ?: JsonObject()
    val hasSession get() = accessToken.isNotBlank() && refreshToken.isNotBlank() && userId.isNotBlank()
    val registrationNumber get() = saved.string("registration_number")
    var selectedSemester: String
        get() = saved.string("semester_id").ifBlank { AcademicConfig.DEFAULT_SEMESTER }
        set(value) { saved.addProperty("semester_id",value); store.write(saved) }
    init {
        if(saved.string("refresh_token").isNotBlank()) {
            accessToken=saved.string("access_token"); refreshToken=saved.string("refresh_token"); userId=saved.string("user_id")
            expiresAt=saved.get("expires_at")?.asLong ?: 0
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
    }''')
s=s.replace('        if(accessToken.isBlank() || userId.isBlank()) throw CloudException("Cloud sign-in failed. Sign in again.")','''        if(accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) throw CloudException("Could not connect your account. Please retry.")
        listOf("access_token","refresh_token","expires_at").forEach { saved.add(it,body.get(it)) }
        saved.addProperty("user_id",userId)
        if(body.string("registration_number").isNotBlank()) saved.addProperty("registration_number",body.string("registration_number"))
        store.write(saved)''')
s=s.replace('private suspend fun token(): String = mutex.withLock','private suspend fun token(rejected: String? = null): String = mutex.withLock').replace('if(expiresAt < System.currentTimeMillis()/1000+60)', 'if(expiresAt < System.currentTimeMillis()/1000+60 || (rejected != null && rejected == accessToken))')
s=s.replace('Request.Builder().url(SupabaseConfig.SUPABASE_URL+path)','Request.Builder().url(baseUrl+path)')
s=s.replace('401 -> "Session expired. Sign out and sign in again."','401 -> if(path.startsWith("functions/")) "VTOP could not confirm this connection. Your saved app data remains available." else "Could not renew your app connection. Please reconnect your account."')
s=s.replace('throw CloudException(message)','throw CloudException(message,response.code)')
s=s.replace('''        val saved=rows("academic_snapshots",mapOf("user_id" to "eq.$userId","semester_id" to "eq.$semester")).single()
        return AcademicSnapshot(semester,saved.getAsJsonArray("courses").map { gson.fromJson(it,AcademicCourse::class.java) },saved.getAsJsonArray("exams").map { gson.fromJson(it,ScheduleEvent::class.java) },body.getAsJsonObject("snapshot").getAsJsonArray("warnings").map { it.asString })''','''        selectedSemester=semester
        return (fetchSnapshot(semester) ?: throw CloudException("No saved subjects returned.")).copy(warnings=body.getAsJsonObject("snapshot").getAsJsonArray("warnings").map { it.asString })''')
pos=s.index('    suspend fun rows(')
s=s[:pos]+'''    private suspend fun authorized(path: String, method: String="GET", json: String?=null, bytes: ByteArray?=null, mime: String="application/json", prefer: String="return=representation"): JsonElement {
        val bearer=token()
        return try { raw(path,method,json,bearer,bytes,mime,prefer) }
        catch(e:CloudException) {
            if(e.status!=401) throw e
            raw(path,method,json,token(rejected=bearer),bytes,mime,prefer)
        }
    }
'''+s[pos:]
s=s.replace('raw(path,bearer=token())','authorized(path)')
s=s.replace('raw("rest/v1/$table","POST",gson.toJson(values),token(),prefer=', 'authorized("rest/v1/$table","POST",gson.toJson(values),prefer=')
s=s.replace('raw("rest/v1/$table?id=eq.$id","PATCH",gson.toJson(values),token())','authorized("rest/v1/$table?id=eq.$id","PATCH",gson.toJson(values))')
s=s.replace('raw("storage/v1/object/study-notes/$path","POST",bearer=token(),bytes=bytes,mime=mime)','authorized("storage/v1/object/study-notes/$path","POST",bytes=bytes,mime=mime)')
s=s.replace('raw("storage/v1/object/sign/study-notes/$path","POST","{\\"expiresIn\\":300}",token())','authorized("storage/v1/object/sign/study-notes/$path","POST","{\\"expiresIn\\":300}")')
p.write_text(s)
