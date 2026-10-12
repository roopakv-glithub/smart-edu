from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/ui/screens/TasksScreen.kt');s=p.read_text(encoding='utf-8',errors='replace');s=s.replace('import androidx.compose.foundation.background','import androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.ui.text.style.TextOverflow\nimport com.studysync.app.data.mapper.PresentationMappers\nimport androidx.compose.foundation.background',1)
s=s.replace('Row(verticalAlignment = Alignment.CenterVertically) {\n                        Icon(\n                            imageVector = Icons.Default.Schedule,','Row(modifier = Modifier.fillMaxWidth(0.45f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {\n                        Icon(\n                            imageVector = Icons.Default.Schedule,')
s=s.replace('text = task.dueDate,','text = task.dueDate,\n                            maxLines = 2,\n                            overflow = TextOverflow.Ellipsis,')
a=s.index('@Composable\nfun AddTaskDialog(');s=s[:a]+'''@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    pods: List<com.studysync.app.data.model.StudyPod>,
    onAddTask: (String, String, String, TaskPriority, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var personal by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(java.text.SimpleDateFormat("dd/MM/yy",java.util.Locale.ENGLISH).format(java.util.Date())) }
    var dueTime by remember { mutableStateOf("05:00 PM") }
    var priority by remember { mutableStateOf(TaskPriority.STANDARD) }
    val wordCount=PresentationMappers.wordCount(title)
    val choice=pods.firstOrNull { it.id==category }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add New Task")},text={
        Column(Modifier.fillMaxWidth().heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value=title,onValueChange={if(PresentationMappers.wordCount(it)<=25 && it.length<=2000)title=it},label={Text("Task title")},supportingText={Text("$wordCount / 25 words")},maxLines=3,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=description,onValueChange={description=it},label={Text("Description")},minLines=2,maxLines=4,modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=personal || category=="OTHERS",enabled=category!="OTHERS",onCheckedChange={personal=it})
                Text("Personal task")
            }
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {Text(if(category=="OTHERS") "Others" else choice?.name ?: "Choose subject")}
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    pods.forEach {pod->DropdownMenuItem(text={Column{Text(pod.name);if(pod.teacher.isNotBlank())Text(pod.teacher,style=MaterialTheme.typography.bodySmall)}},onClick={category=pod.id;expanded=false})}
                    DropdownMenuItem(text={Text("Others")},onClick={category="OTHERS";expanded=false})
                }
            }
            if(choice!=null) Text(PresentationMappers.subjectCode(choice.courseCode),style=MaterialTheme.typography.bodySmall)
            if(category=="OTHERS") Text("Others tasks are visible only to you.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=dueDate,onValueChange={dueDate=it},label={Text("Date (dd/mm/yy)")},singleLine=true,modifier=Modifier.weight(0.62f))
                OutlinedTextField(value=dueTime,onValueChange={dueTime=it},label={Text("Time")},supportingText={Text("hh:mm AM/PM")},singleLine=true,modifier=Modifier.weight(0.38f))
            }
            Text("Priority",style=MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                TaskPriority.entries.forEach {p->FilterChip(selected=priority==p,onClick={priority=p},label={Text(p.name.lowercase().replaceFirstChar{it.uppercase()})})}
            }
        }
    },confirmButton={TextButton(enabled=title.isNotBlank() && wordCount<=25 && category.isNotBlank() && dueDate.isNotBlank() && dueTime.isNotBlank(),onClick={onAddTask(title.trim(),description,category,priority,dueDate,dueTime,personal || category=="OTHERS")}) {Text("Save task")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
''';p.write_text(s,encoding='utf-8')
