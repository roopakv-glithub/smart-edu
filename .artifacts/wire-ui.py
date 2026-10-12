from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/ui/screens/TasksScreen.kt')
s=p.read_text();s=s.replace('    var category by remember { mutableStateOf("Java") }','    var category by remember { mutableStateOf("") }\n    var personal by remember { mutableStateOf(false) }\n    var expanded by remember { mutableStateOf(false) }')
s=s.replace('mutableStateOf("24/10/26")','mutableStateOf(java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.ENGLISH).format(java.util.Date()))')
s=s.replace('    onAddTask: (String, String, String, TaskPriority, String, String) -> Unit','    pods: List<com.studysync.app.data.model.StudyPod>,\n    onAddTask: (String, String, String, TaskPriority, String, String, Boolean) -> Unit')
s=s.replace('onAddTask = { title, desc, cat, priority, dueDate, dueTime ->','pods = viewModel.studyPods.collectAsState().value,\n            onAddTask = { title, desc, cat, priority, dueDate, dueTime, personal ->').replace('viewModel.addTask(title, desc, cat, priority, dueDate, dueTime)','viewModel.addTask(title, desc, cat, priority, dueDate, dueTime, personal)')
a=s.index('                OutlinedTextField(\n                    value = category,');b=s.index('                Row(',a)
s=s[:a]+'''                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = personal, onCheckedChange = { personal = it })
                    Text("Personal task (only me)")
                }
                if (!personal) Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(pods.firstOrNull { it.id == category }?.let { it.name + " · " + it.teacher } ?: "Choose enrolled subject")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        pods.forEach { pod -> DropdownMenuItem(text = { Text(pod.name + " · " + pod.teacher) }, onClick = { category = pod.id; expanded = false }) }
                    }
                }
''' +s[b:]
s=s.replace('if (title.isNotBlank()) {\n                        onAddTask(title, description, category, priority, dueDate, dueTime)','if (title.isNotBlank() && (personal || category.isNotBlank())) {\n                        onAddTask(title, description, category, priority, dueDate, dueTime, personal)')
p.write_text(s)
p=Path('app/src/main/java/com/studysync/app/ui/screens/ChatScreen.kt');s=p.read_text();s=s.replace('    if (selectedPod != null) {','''    LaunchedEffect(selectedPod?.id) {
        viewModel.selectPod(selectedPod?.id)
        while (selectedPod != null) {
            viewModel.reloadChat()
            kotlinx.coroutines.delay(15000)
        }
    }
    if (selectedPod != null) {''',1)
s=s.replace('Text(text = msg.text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceText)','''Text(text = msg.text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceText)
                            if (msg.isUser && msg.text != "Message deleted") TextButton(onClick = { viewModel.deleteMessage(msg.id) }) { Text("Delete") }''')
s=s.replace('IconButton(onClick = {})','IconButton(onClick = { viewModel.refreshCloud() })').replace('Icons.Default.Create','Icons.Default.Refresh').replace('contentDescription = "New Conversation"','contentDescription = "Refresh pods"')
s=s.replace('            items(filteredPods, key', '            if (filteredPods.isEmpty()) item { Text("No verified pods yet. Refresh your subjects on Home and check your semester.") }\n            items(filteredPods, key')
p.write_text(s)
p=Path('app/src/main/java/com/studysync/app/ui/navigation/AppNavigation.kt');s=p.read_text();s=s.replace('    val isLoggedIn by viewModel.isLoggedIn.collectAsState()','''    val cloudError by viewModel.cloudError.collectAsState()
    cloudError?.let { message ->
        AlertDialog(onDismissRequest = { viewModel.clearCloudError() }, title = { Text("StudySync") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { viewModel.clearCloudError() }) { Text("OK") } })
    }
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()''')
p.write_text(s)
