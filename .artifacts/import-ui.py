from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/ui/components/AcademicPanel.kt');s=p.read_text().replace('import com.studysync.app.ui.viewmodel.StudySyncViewModel','import com.studysync.app.ui.viewmodel.StudySyncViewModel\nimport com.studysync.app.ui.viewmodel.LoginUiState\nimport androidx.compose.ui.text.input.PasswordVisualTransformation')
s=s.replace('    var expanded by remember { mutableStateOf(false) }','''    var expanded by remember { mutableStateOf(false) }
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
    )''')
s=s.replace('            if (loading) LinearProgressIndicator', '''            TextButton(onClick = { if(viewModel.hasVtopSession) viewModel.refreshAcademics(fromVtop=true) else {submitted=false;connect=true} }, enabled = !loading) { Text("Import from VTOP") }
            if (loading) LinearProgressIndicator''')
p.write_text(s)
p=Path('app/src/main/java/com/studysync/app/ui/viewmodel/StudySyncViewModel.kt');s=p.read_text().replace('    private val cloud=SupabaseRepository','    val hasVtopSession get() = academicRepository.currentSession != null\n    private val cloud=SupabaseRepository');p.write_text(s)
