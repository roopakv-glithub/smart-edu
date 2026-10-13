package com.studysync.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.data.model.Role
import com.studysync.app.data.model.StudyNote
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel
import com.studysync.app.data.mapper.PresentationMappers as Display

@Composable
fun NotesScreen(viewModel: StudySyncViewModel) {
    val context = LocalContext.current
    val notes by viewModel.studyNotes.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val pods by viewModel.studyPods.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val syllabuses by viewModel.subjectSyllabuses.collectAsState()

    var subject by remember { mutableStateOf<String?>(null) }
    var teacher by remember { mutableStateOf<String?>(null) }
    var inSyllabusSection by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    // Upload & Deletion Dialog states
    var upload by remember { mutableStateOf(false) }
    var uploadCommon by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }

    // Syllabus Management states
    var showSyllabusUploadDialog by remember { mutableStateOf(false) }
    var syllabusTextInput by remember { mutableStateOf("") }

    // Admin / Moderator Deletion Dialog (Requirement 4)
    var noteToDelete by remember { mutableStateOf<StudyNote?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val request = pending
        if (uri != null && request != null) {
            viewModel.uploadNote(context.contentResolver, uri, request.first, request.second, request.third)
        }
        pending = null
    }

    fun back() {
        if (inSyllabusSection) inSyllabusSection = false
        else if (teacher != null) teacher = null
        else subject = null
        search = ""
    }

    BackHandler(subject != null) { back() }

    val subjects = courses.groupBy { Display.subjectCode(it.courseCode) }
    val subjectTitle = subjects[subject]?.firstOrNull()?.let { Display.title(it.title, it.courseCode) } ?: subject.orEmpty()
    val subjectNotes = notes.filter { Display.subjectCode(it.category) == subject }
    val sectionNotes = subjectNotes.filter { if (teacher == "__common__") it.isCommon else !it.isCommon && Display.teacher(it.teacher) == teacher }
    val uploadPod = pods.firstOrNull { Display.subjectCode(it.courseCode) == subject && (uploadCommon || Display.teacher(it.teacher) == teacher) }
    val currentSyllabus = subject?.let { syllabuses[it] }

    // Upload Material Dialog
    if (upload) {
        AlertDialog(
            onDismissRequest = { upload = false },
            title = { Text(if (uploadCommon) "Upload common material" else "Upload material") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(subjectTitle, fontWeight = FontWeight.Bold)
                    if (!uploadCommon && !teacher.isNullOrBlank()) Text("Teacher: ${teacher.orEmpty()}")
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 200) title = it },
                        label = { Text("Material title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("PDF, text, JPG, PNG or DOCX · up to 20 MB", style = MaterialTheme.typography.bodySmall, color = PencilGray)
                    if (uploadPod == null) Text("Uploads are available for your own class sections.", color = ErrorRed)
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank() && uploadPod != null,
                    onClick = {
                        pending = Triple(uploadPod!!.id, title, uploadCommon)
                        upload = false
                        picker.launch(arrayOf("application/pdf", "text/plain", "image/jpeg", "image/png", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Choose file")
                }
            },
            dismissButton = {
                TextButton(onClick = { upload = false }) { Text("Cancel") }
            }
        )
    }

    // Syllabus Upload / Replace Dialog (Requirement 5 & 6)
    if (showSyllabusUploadDialog) {
        AlertDialog(
            onDismissRequest = { showSyllabusUploadDialog = false },
            title = { Text(if (currentSyllabus == null) "Upload Subject Syllabus" else "Replace Syllabus") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Subject: $subjectTitle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "Paste curriculum / syllabus text below. AI will analyze the module structure and create a reusable learning roadmap for all students.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray
                    )
                    OutlinedTextField(
                        value = syllabusTextInput,
                        onValueChange = { syllabusTextInput = it },
                        label = { Text("Syllabus Content (Modules, Topics)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = syllabusTextInput.isNotBlank() && subject != null,
                    onClick = {
                        viewModel.uploadSyllabus(subject!!, subjectTitle, syllabusTextInput)
                        showSyllabusUploadDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Save & Generate Roadmap")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyllabusUploadDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Admin & Moderator Deletion Modal (Requirement 4)
    if (showDeleteConfirmDialog && noteToDelete != null) {
        val note = noteToDelete!!
        val isPrivileged = userRole == Role.ADMIN || userRole == Role.MODERATOR

        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                noteToDelete = null
            },
            title = { Text("Delete Material Options", fontWeight = FontWeight.Bold) },
            text = {
                Text("Select whether you want to hide this note only for yourself or remove it for everyone in the class.")
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Option 1: For me (Personal Hiding)
                    Button(
                        onClick = {
                            viewModel.hideNoteForMe(note.id)
                            showDeleteConfirmDialog = false
                            noteToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                    ) {
                        Text("1. For me (Hide from my view)")
                    }

                    // Option 2: For everyone (Global Deletion)
                    if (isPrivileged) {
                        Button(
                            onClick = {
                                viewModel.deleteNoteGlobally(note.id)
                                showDeleteConfirmDialog = false
                                noteToDelete = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("2. For everyone (Global deletion)")
                        }
                    }

                    // Option 3: Cancel
                    OutlinedButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            noteToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3. Cancel")
                    }
                }
            },
            dismissButton = {}
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (subject != null) {
                IconButton(onClick = { back() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        inSyllabusSection -> "Subject Syllabus"
                        teacher != null -> if (teacher == "__common__") "Common Materials" else teacher.orEmpty()
                        subject != null -> subjectTitle
                        else -> "Study Materials"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (subject != null && teacher == null && !inSyllabusSection) {
                    Text("Select teacher notes or canonical syllabus", style = MaterialTheme.typography.bodySmall, color = PencilGray)
                }
            }
        }

        if (subject == null || teacher != null) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (subject == null) {
                // 1. Root Level: Subjects List
                val visible = subjects.entries.filter { (code, rows) ->
                    rows.any { it.title.contains(search, true) || code.contains(search, true) } ||
                    notes.any { Display.subjectCode(it.category) == code && it.title.contains(search, true) }
                }.sortedBy { it.value.first().title }

                if (visible.isEmpty()) item { Text("No matching subjects.") }
                items(visible, key = { it.key }) { (code, rows) ->
                    MaterialSection(
                        label = Display.title(rows.first().title, code),
                        icon = Icons.Default.Folder,
                        onClick = { subject = code; search = "" }
                    )
                }
            } else if (inSyllabusSection) {
                // 2. Syllabus Section View (Requirement 5 & 6)
                item {
                    TactileCard(
                        backgroundColor = PaperWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Canonical Syllabus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(StorybookGreen)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Version ${currentSyllabus?.version ?: 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DarkGreenContainer,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (currentSyllabus != null && currentSyllabus.syllabusText.isNotBlank()) {
                                Text(
                                    text = currentSyllabus.syllabusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceText
                                )
                            } else {
                                Text(
                                    text = "No canonical syllabus uploaded yet for this subject. Upload syllabus text to automatically generate the shared module roadmap.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PencilGray
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        syllabusTextInput = currentSyllabus?.syllabusText.orEmpty()
                                        showSyllabusUploadDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (currentSyllabus == null) "Upload Syllabus" else "Replace Syllabus")
                                }

                                if (currentSyllabus != null) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.loadOrGenerateRoadmap(subject!!, subjectTitle, forceRegenerate = true)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Regenerate Roadmap")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (teacher == null) {
                // 3. Subject Level: Teachers + Canonical Syllabus Section (Requirement 5)
                val teachers = ((subjects[subject].orEmpty().map { Display.teacher(it.faculty) }) +
                        subjectNotes.filter { !it.isCommon }.map { Display.teacher(it.teacher) })
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                // Teacher Sections
                items(teachers, key = { it }) { name ->
                    MaterialSection(
                        label = name,
                        icon = Icons.Default.Person,
                        onClick = { teacher = name; search = "" }
                    )
                }

                // Common Materials
                item {
                    MaterialSection(
                        label = "Common Materials",
                        icon = Icons.Default.FolderShared,
                        onClick = { teacher = "__common__"; search = "" }
                    )
                }

                // Canonical Syllabus Section (Requirement 5)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TactileCard(
                        backgroundColor = StorybookGreen,
                        bottomShadowColor = DarkGreenContainer,
                        onClick = { inSyllabusSection = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Subject Syllabus & Roadmap",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkGreenContainer
                                    )
                                    Text(
                                        text = if (currentSyllabus != null) "View / Replace Syllabus · Regenerate Roadmap" else "Upload canonical syllabus",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkGreenContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = DarkGreenContainer)
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { uploadCommon = true; title = ""; upload = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Common Material")
                    }
                }
            } else {
                // 4. Notes inside Teacher section with Personal Hiding / Deletion
                val visible = sectionNotes.filter { it.title.contains(search, true) }
                if (visible.isEmpty()) {
                    item { Text("No materials in this section yet.") }
                }
                items(visible, key = { it.id }) { note ->
                    TactileCard(
                        backgroundColor = PaperWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.openNote(note.id) { url ->
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    }
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceText
                                )
                                Text(
                                    text = "${note.noteType.uppercase()} • ${note.lastUpdated}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PencilGray
                                )
                            }

                            // Delete Action Button
                            IconButton(
                                onClick = {
                                    noteToDelete = note
                                    showDeleteConfirmDialog = true
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = PencilGray)
                            }
                        }
                    }
                }
            }
        }

        if (teacher != null) {
            Button(
                onClick = { uploadCommon = teacher == "__common__"; title = ""; upload = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload material")
            }
        }
    }
}

@Composable
private fun MaterialSection(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Folder,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PencilGray, modifier = Modifier.size(16.dp))
        }
    }
}
