package com.studysync.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.ui.viewmodel.StudySyncViewModel
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(viewModel:StudySyncViewModel) {
    val pods by viewModel.studyPods.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected=pods.firstOrNull{it.id==selectedId}
    var query by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    BackHandler(selected!=null) {selectedId=null}
    LaunchedEffect(selected?.id) {
        viewModel.selectPod(selected?.id)
        while(selected!=null) {viewModel.reloadChat();delay(15000)}
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(selected==null) {
            Text("Chat & Study Pods",style=MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value=query,onValueChange={query=it},placeholder={Text("Search subjects or teachers")},singleLine=true,modifier=Modifier.fillMaxWidth())
            val filtered=pods.filter{it.name.contains(query,true)||it.teacher.contains(query,true)}
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(filtered.isEmpty()) item {Text("No subjects to display.")}
                items(filtered,key={it.id}) { pod ->
                    Surface(shape=RoundedCornerShape(12.dp),tonalElevation=1.dp,modifier=Modifier.fillMaxWidth().clickable{selectedId=pod.id}) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text(when(pod.kind){"LAB"->"</>";"EMBEDDED"->"{ }";else->"≡"},fontSize=20.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.width(34.dp))
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                                Text(pod.name,fontWeight=FontWeight.SemiBold,fontSize=15.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                                if(pod.teacher.isNotBlank()) Text(pod.teacher,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={selectedId=null}){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}
                Column(Modifier.weight(1f)) {
                    Text(selected.name,style=MaterialTheme.typography.titleMedium)
                    if(selected.teacher.isNotBlank()) Text(selected.teacher,style=MaterialTheme.typography.bodySmall)
                }
            }
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(messages.isEmpty()) item {Text("No messages yet. Start the conversation.")}
                items(messages,key={it.id}) { message ->
                    Surface(shape=RoundedCornerShape(12.dp),tonalElevation=if(message.isUser) 2.dp else 0.dp,modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(message.senderName.ifBlank{"Student"},fontWeight=FontWeight.SemiBold,fontSize=12.sp)
                            Text(message.text)
                            Text(message.timestamp,style=MaterialTheme.typography.labelSmall)
                            if(message.isUser && message.text!="Message deleted") TextButton(onClick={viewModel.deleteMessage(message.id)}) {Text("Delete")}
                        }
                    }
                }
            }
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=draft,onValueChange={draft=it},placeholder={Text("Message")},modifier=Modifier.weight(1f),maxLines=4)
                IconButton(enabled=draft.isNotBlank(),onClick={viewModel.sendChatMessage(draft);draft=""}) {Icon(Icons.AutoMirrored.Filled.Send,"Send")}
            }
        }
    }
}
