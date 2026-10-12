package com.studysync.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.data.model.StudyNote
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.components.TactileCard
import com.studysync.app.ui.components.TactileChip
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun NotesScreen(
    viewModel: StudySyncViewModel
) {
    val searchQuery by viewModel.notesSearchQuery.collectAsState()
    val filter by viewModel.notesFilter.collectAsState()
    val notes by viewModel.studyNotes.collectAsState()

    val filteredNotes = notes.filter { note ->
        (searchQuery.isBlank() || note.title.contains(searchQuery, ignoreCase = true) || note.category.contains(searchQuery, ignoreCase = true)) &&
                (filter == "ALL" || (filter == "PDF" && note.noteType == "PDF") || (filter == "CODE" && note.noteType == "CODE"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study Materials",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurfaceText,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "7 enrolled subjects • 32 resources",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray
                    )
                }

                TactileButton(
                    text = "+ Upload",
                    onClick = { },
                    backgroundColor = EagerGreen,
                    bottomShadowColor = DarkGreenContainer,
                    icon = Icons.Default.UploadFile
                )
            }
        }

        // Search Input Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setNotesSearchQuery(it) },
                placeholder = { Text("Search notes, slides, documents...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PencilGray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setNotesSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Clear",
                                tint = PencilGray
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TactileChip(
                    text = "All Files (32)",
                    selected = filter == "ALL",
                    onClick = { viewModel.setNotesFilter("ALL") }
                )
                TactileChip(
                    text = "PDFs (19)",
                    selected = filter == "PDF",
                    onClick = { viewModel.setNotesFilter("PDF") }
                )
                TactileChip(
                    text = "Code & Labs (8)",
                    selected = filter == "CODE",
                    onClick = { viewModel.setNotesFilter("CODE") }
                )
            }
        }

        // Sync Metric Tile
        item {
            TactileCard(
                backgroundColor = Color(0xFFD7FFB8),
                bottomShadowColor = FreshLeaf,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EagerGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = PaperWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Midterm Prep Ready",
                                style = MaterialTheme.typography.labelLarge,
                                color = DarkGreenContainer
                            )
                            Text(
                                text = "All week 1-8 materials synced",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EagerGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "98% Synced",
                            style = MaterialTheme.typography.labelLarge,
                            color = DarkGreenContainer,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Course Notes Cards
        items(filteredNotes, key = { it.id }) { note ->
            var expanded by remember { mutableStateOf(false) }

            TactileCard(
                backgroundColor = PaperWhite,
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (note.noteType == "CODE") Icons.Default.Code else Icons.Default.Assignment,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = note.category,
                                style = MaterialTheme.typography.labelLarge,
                                color = OnSurfaceText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${note.fileCount} documents • ${note.lastUpdated}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PencilGray
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
                        contentDescription = "Expand",
                        tint = PencilGray
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Resource Document:",
                            style = MaterialTheme.typography.labelLarge,
                            color = PencilGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                            ) {
                                Text("View Document", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Generate Quiz", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
