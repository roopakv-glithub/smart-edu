package com.studysync.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Sign In", Icons.Default.Lock)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.CheckCircle)
    object Schedule : Screen("schedule", "Schedule", Icons.Default.CalendarToday)
    object Notes : Screen("notes", "Notes", Icons.Default.Description)
    object Quiz : Screen("quiz", "Quiz", Icons.Default.Quiz)
    object Chat : Screen("chat", "Chat", Icons.Default.Forum)
    object Jarvis : Screen("jarvis", "Jarvis AI", Icons.Default.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Tasks,
    Screen.Schedule,
    Screen.Quiz,
    Screen.Jarvis
)

val drawerScreens = listOf(
    Screen.Home,
    Screen.Tasks,
    Screen.Schedule,
    Screen.Notes,
    Screen.Quiz,
    Screen.Chat,
    Screen.Jarvis,
    Screen.Settings
)
