package com.studysync.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studysync.app.data.config.AcademicConfig
import com.studysync.app.ui.viewmodel.StudySyncViewModel
import com.studysync.app.ui.viewmodel.LoginUiState
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun AcademicPanel(viewModel: StudySyncViewModel, showCourses: Boolean = true) {
    val courses by viewModel.courses.collectAsState()
    val semester by viewModel.semesterId.collectAsState()
    val loading by viewModel.isSyncing.collectAsState()
    val error by viewModel.syncError.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var connect by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val login by viewModel.loginState.collectAsState()
    LaunchedEffect(login,submitted) {
        if(submitted && login is LoginUiState.Authenticated) { connect=false; submitted=false; password="" }
    }
    if(connect) AlertDialog(
        onDismissRequest={if(login !is LoginUiState.Loading) {connect=false;password=""}},
        title={Text("Import from VTOP")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Connect only to import fresh university data. Your saved app data does not need a VTOP login.")
            OutlinedTextField(value=userId,onValueChange={userId=it},label={Text("VTOP user ID")})
            OutlinedTextField(value=password,onValueChange={password=it},label={Text("VTOP password")},visualTransformation=PasswordVisualTransformation())
            if(submitted && login is LoginUiState.Error) Text((login as LoginUiState.Error).message,color=MaterialTheme.colorScheme.error)
            if(login is LoginUiState.Loading) LinearProgressIndicator()
        }},
        confirmButton={TextButton(enabled=login !is LoginUiState.Loading && userId.isNotBlank() && password.isNotBlank(),onClick={submitted=true;viewModel.authenticate(userId,password);password=""}) {Text("Import")}},
        dismissButton={TextButton(enabled=login !is LoginUiState.Loading,onClick={connect=false;password=""}) {Text("Cancel")}}
    )
    TactileCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("My subjects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }, enabled = !loading) { Text(semester) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AcademicConfig.semesters.forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { expanded = false; viewModel.setSemester(value) })
                        }
                    }
                }
                TextButton(onClick = { viewModel.refreshAcademics() }, enabled = !loading) { Text("Refresh") }
            }
            TextButton(onClick = { if(viewModel.hasVtopSession) viewModel.refreshAcademics(fromVtop=true) else {submitted=false;connect=true} }, enabled = !loading) { Text("Import from VTOP") }
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (!loading && courses.isEmpty() && error == null) Text("Refresh to load your VTOP subjects.")
            if (showCourses) courses.forEach { course ->
                HorizontalDivider()
                Text(course.label, fontWeight = FontWeight.SemiBold)
                Text(course.faculty.ifBlank { "Faculty not provided by VTOP" }, style = MaterialTheme.typography.bodyMedium)
                Text(listOf(course.slots, course.venue).filter { it.isNotBlank() }.distinct().joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                if (course.attendance.isNotBlank()) Text("Attendance: ${course.attendance}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
