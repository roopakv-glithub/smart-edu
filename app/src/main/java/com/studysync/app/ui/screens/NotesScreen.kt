package com.studysync.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.studysync.app.data.mapper.PresentationMappers as Display
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun NotesScreen(viewModel:StudySyncViewModel) {
    val context=LocalContext.current
    val notes by viewModel.studyNotes.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val pods by viewModel.studyPods.collectAsState()
    var subject by remember { mutableStateOf<String?>(null) }
    var teacher by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var upload by remember { mutableStateOf(false) }
    var uploadCommon by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Triple<String,String,Boolean>?>(null) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val request=pending
        if(uri!=null && request!=null) viewModel.uploadNote(context.contentResolver,uri,request.first,request.second,request.third)
        pending=null
    }
    fun back() { if(teacher!=null) teacher=null else subject=null;search="" }
    BackHandler(subject!=null) {back()}
    val subjects=courses.groupBy{Display.subjectCode(it.courseCode)}
    val subjectTitle=subjects[subject]?.firstOrNull()?.let{Display.title(it.title,it.courseCode)} ?: subject.orEmpty()
    val subjectNotes=notes.filter{Display.subjectCode(it.category)==subject}
    val sectionNotes=subjectNotes.filter{if(teacher=="__common__") it.isCommon else !it.isCommon && Display.teacher(it.teacher)==teacher}
    val uploadPod=pods.firstOrNull{Display.subjectCode(it.courseCode)==subject && (uploadCommon || Display.teacher(it.teacher)==teacher)}
    if(upload) AlertDialog(onDismissRequest={upload=false},title={Text(if(uploadCommon) "Upload common material" else "Upload material")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(subjectTitle)
        if(!uploadCommon && !teacher.isNullOrBlank()) Text(teacher.orEmpty())
        OutlinedTextField(value=title,onValueChange={if(it.length<=200)title=it},label={Text("Material title")},modifier=Modifier.fillMaxWidth())
        Text("PDF, text, JPG, PNG or DOCX · up to 20 MB",style=MaterialTheme.typography.bodySmall)
        if(uploadPod==null) Text("Uploads are available for your own class sections.")
    }},confirmButton={TextButton(enabled=title.isNotBlank() && uploadPod!=null,onClick={pending=Triple(uploadPod!!.id,title,uploadCommon);upload=false;picker.launch(arrayOf("application/pdf","text/plain","image/jpeg","image/png","application/vnd.openxmlformats-officedocument.wordprocessingml.document"))}) {Text("Choose file")}},dismissButton={TextButton(onClick={upload=false}) {Text("Cancel")}})
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            if(subject!=null) IconButton(onClick={back()}) {Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}
            Column(Modifier.weight(1f)) {
                Text(if(subject==null) "Study Materials" else subjectTitle,style=MaterialTheme.typography.headlineSmall)
                if(teacher!=null) Text(if(teacher=="__common__") "Common materials" else teacher.orEmpty(),style=MaterialTheme.typography.bodyMedium)
            }
        }
        if(subject==null || teacher!=null) OutlinedTextField(value=search,onValueChange={search=it},placeholder={Text("Search notes")},singleLine=true,modifier=Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(subject==null) {
                val visible=subjects.entries.filter{(code,rows)->rows.any{it.title.contains(search,true)||code.contains(search,true)} || notes.any{Display.subjectCode(it.category)==code && it.title.contains(search,true)}}.sortedBy{it.value.first().title}
                if(visible.isEmpty()) item {Text("No matching subjects.")}
                items(visible,key={it.key}) { (code,rows) ->
                    MaterialSection(Display.title(rows.first().title,code)) {subject=code;search=""}
                }
            } else if(teacher==null) {
                val teachers=((subjects[subject].orEmpty().map{Display.teacher(it.faculty)})+subjectNotes.filter{!it.isCommon}.map{Display.teacher(it.teacher)}).filter{it.isNotBlank()}.distinct().sorted()
                items(teachers,key={it}) { name -> MaterialSection(name) {teacher=name;search=""} }
                item {MaterialSection("Common materials") {teacher="__common__";search=""}}
                item {OutlinedButton(onClick={uploadCommon=true;title="";upload=true},modifier=Modifier.fillMaxWidth()) {Text("Upload common material")}}
            } else {
                val visible=sectionNotes.filter{it.title.contains(search,true)}
                if(visible.isEmpty()) item {Text("No materials in this section yet.")}
                items(visible,key={it.id}) { note ->
                    MaterialSection(note.title) {viewModel.openNote(note.id){url->context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}}
                }
            }
        }
        if(teacher!=null) Button(onClick={uploadCommon=teacher=="__common__";title="";upload=true},modifier=Modifier.fillMaxWidth()) {Text("Upload material")}
    }
}
@Composable
private fun MaterialSection(label:String,onClick:()->Unit) {
    Surface(shape=RoundedCornerShape(12.dp),tonalElevation=1.dp,modifier=Modifier.fillMaxWidth().clickable(onClick=onClick)) {
        Text(label,modifier=Modifier.padding(16.dp),style=MaterialTheme.typography.titleMedium)
    }
}
