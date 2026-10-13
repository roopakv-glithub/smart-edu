package com.studysync.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.data.model.SyllabusModule
import com.studysync.app.data.model.SyllabusTopic
import com.studysync.app.ui.theme.*
import kotlin.math.sin

/**
 * Duolingo-style Learning Path Component:
 * - Serpentine winding stepping stones
 * - Tactile 3D beveled circular milestone buttons
 * - Clear module banners, topic labels, and star indicators
 * - Module test treasure chest at the end
 * - Non-linear: users can tap ANY topic or subtopic at any time!
 */
@Composable
fun DuolingoRoadmap(
    modules: List<SyllabusModule>,
    onSelectTopic: (module: SyllabusModule, topic: SyllabusTopic) -> Unit,
    onSelectModuleTest: (module: SyllabusModule) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        modules.forEachIndexed { modIndex, module ->
            // Module Banner Header (Duolingo Unit Card)
            ModuleBanner(
                module = module,
                onTestClick = { onSelectModuleTest(module) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Serpentine Path Nodes
            val allTopics = module.topics
            if (allTopics.isEmpty()) {
                // Fallback default topic nodes for the module if none generated
                val defaultTopics = listOf(
                    SyllabusTopic("d1", "Core Concepts"),
                    SyllabusTopic("d2", "Foundational Principles"),
                    SyllabusTopic("d3", "Applications & Analysis"),
                    SyllabusTopic("d4", "Advanced Problem Solving")
                )
                WindingTopicPath(
                    topics = defaultTopics,
                    module = module,
                    onSelectTopic = onSelectTopic
                )
            } else {
                WindingTopicPath(
                    topics = allTopics,
                    module = module,
                    onSelectTopic = onSelectTopic
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Module Checkpoint Chest / Final Test Node
            ModuleCheckpointNode(
                module = module,
                onClick = { onSelectModuleTest(module) }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ModuleBanner(
    module: SyllabusModule,
    onTestClick: () -> Unit
) {
    TactileCard(
        backgroundColor = PrimaryGreen,
        bottomShadowColor = DarkGreenContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SECTION 1, MODULE ${module.moduleNumber}".uppercase(),
                    color = StorybookGreen,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = module.title,
                    color = PaperWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onTestClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkGreenContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Module Guidebook",
                    tint = PaperWhite
                )
            }
        }
    }
}

@Composable
fun WindingTopicPath(
    topics: List<SyllabusTopic>,
    module: SyllabusModule,
    onSelectTopic: (module: SyllabusModule, topic: SyllabusTopic) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topics.forEachIndexed { index, topic ->
            // Sine wave horizontal offset for winding serpentine path
            val horizontalOffset = (sin(index * 1.3) * 70).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.offset(x = horizontalOffset),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 3D Tactile Stepping Stone Button
                        DuolingoSteppingStone(
                            icon = when {
                                topic.isCompleted -> Icons.Default.Check
                                index % 3 == 0 -> Icons.Default.AutoStories
                                index % 3 == 1 -> Icons.Default.Psychology
                                else -> Icons.Default.Star
                            },
                            isCompleted = topic.isCompleted,
                            onClick = { onSelectTopic(module, topic) }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Star rating indicator underneath
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(3) { starIdx ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (topic.isCompleted || starIdx == 0) GoldAccent else SurfaceContainerHigh,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Topic label pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperWhite)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .shadow(1.dp, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = topic.title.take(24),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3D Tactile Circular Button styled after Duolingo's milestone nodes:
 * Raised dome with a darker bottom shadow bevel.
 */
@Composable
fun DuolingoSteppingStone(
    icon: ImageVector,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topColor = if (isCompleted) Color(0xFF58CC02) else Color(0xFF58CC02)
    val shadowColor = if (isCompleted) Color(0xFF46A302) else Color(0xFF46A302)

    Box(
        modifier = modifier
            .size(72.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Bottom 3D shadow layer
        Box(
            modifier = Modifier
                .size(68.dp)
                .offset(y = 6.dp)
                .clip(CircleShape)
                .background(shadowColor)
        )

        // Main top dome layer
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(topColor),
            contentAlignment = Alignment.Center
        ) {
            // Subtle highlight reflection arc at top
            Box(
                modifier = Modifier
                    .size(54.dp, 24.dp)
                    .offset(y = (-14).dp)
                    .clip(RoundedCornerShape(50))
                    .background(PaperWhite.copy(alpha = 0.25f))
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaperWhite,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Module Checkpoint Treasure Chest / Trophy Node at the end of each module
 */
@Composable
fun ModuleCheckpointNode(
    module: SyllabusModule,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Gold Treasure Chest 3D Button
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center
        ) {
            // Bottom 3D shadow
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .offset(y = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFC78300))
            )

            // Gold main box
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFC800)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Module Mastery Test",
                    tint = PaperWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFFFF3CC))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🏆 Overall Module ${module.moduleNumber} Test",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF8A5A00),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
