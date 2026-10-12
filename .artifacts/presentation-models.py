from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/data/model/Models.kt');s=p.read_text(encoding='utf-8').replace('val isCompleted: Boolean = false\n','val isCompleted: Boolean = false,\n    val dueAt: String = ""\n').replace('val noteType: String\n','val noteType: String,\n    val isCommon: Boolean = false\n').replace('val initials: String\n','val initials: String,\n    val courseCode: String = "",\n    val podIds: List<String> = listOf(id),\n    val kind: String = "THEORY"\n');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/studysync/app/data/repository/SupabaseRepository.kt');s=p.read_text(encoding='utf-8').replace('    val registrationNumber get()', '''    val displayName get() = saved.string("display_name").takeIf { it.isNotBlank() && it != registrationNumber } ?: "Name was not fetched"
    fun saveDisplayName(name:String) { saved.addProperty("display_name",name);store.write(saved) }
    val registrationNumber get()''');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/studysync/app/ui/viewmodel/StudySyncViewModel.kt');s=p.read_text(encoding='utf-8').replace('import com.google.gson.JsonObject','import com.google.gson.JsonObject\nimport com.studysync.app.data.mapper.PresentationMappers')
s=s.replace('MutableStateFlow(cloud.registrationNumber)','MutableStateFlow(cloud.displayName)').replace('_userName.value=result.session.authorizedID','_userName.value=cloud.displayName')
s=s.replace('    private suspend fun loadCloud() {','''    private suspend fun loadCloud() {
        val profile=cloud.rows("profiles",mapOf("id" to "eq.${cloud.userId}")).firstOrNull()
        val name=profile?.string("display_name").orEmpty().takeUnless { it == profile?.string("registration_number") }.orEmpty()
        cloud.saveDisplayName(name);_userName.value=cloud.displayName''')
s=s.replace('_studyPods.value=pods.map { p->StudyPod(', '_studyPods.value=PresentationMappers.groups(pods.map { p->StudyPod(')
s=s.replace('p.string("course_code").take(2)) }','p.string("course_code").take(2),courseCode=p.string("course_code")) })')
s=s.replace('if(t.get("is_personal")?.asBoolean==true) "Personal" else t.string("course_code")','t.string("course_code").ifBlank { "Others" }')
s=s.replace('completed[t.string("id")]==true)', 'completed[t.string("id")]==true,t.string("due_at"))')
s=s.replace('n.string("mime_type").substringAfter(\'/\'))','n.string("mime_type").substringAfter(\'/\'),n.get("is_common")?.asBoolean==true)')
s=s.replace('_scheduleEvents.value=examEvents+events.map', '_scheduleEvents.value=examEvents+_tasks.value.mapNotNull { PresentationMappers.taskEvent(it) }+events.map')
s=s.replace('require(title.isNotBlank()){ "Enter a task title." }','require(title.isNotBlank() && PresentationMappers.wordCount(title)<=25){ "Use a task title of 1 to 25 words." }')
s=s.replace('        require(isPersonal || pod!=null)', '        val personal=isPersonal || category=="OTHERS"\n        require(personal || pod!=null)')
s=s.replace('if(isPersonal) null else pod!!.string("id")','if(personal) null else pod!!.string("id")').replace('if(isPersonal) "" else pod!!.string("course_code")','pod?.string("course_code") ?: "Others"').replace('"is_personal" to isPersonal','"is_personal" to personal')
s=s.replace('podId:String,title:String)=action', 'podId:String,title:String,isCommon:Boolean=false)=action').replace('"faculty" to pod.string("faculty"),"title"','"faculty" to if(isCommon) "" else pod.string("faculty"),"is_common" to isCommon,"title"')
s=s.replace('"pod_id" to "eq.$id","order" to "created_at.desc"','"pod_id" to "in.(${_studyPods.value.firstOrNull { it.id==id }?.podIds?.joinToString(",") ?: id})","order" to "created_at.desc"')
p.write_text(s,encoding='utf-8')
p=Path('supabase/functions/studysync-session/index.ts');s=p.read_text(encoding='utf-8').replace('display_name:authorizedID','display_name:\'\'');p.write_text(s,encoding='utf-8')
