package com.studysync.app.ui.navigation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studysync.app.ui.components.TopNavBar
import com.studysync.app.ui.screens.*
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.StudySyncViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: StudySyncViewModel
) {
    val cloudError by viewModel.cloudError.collectAsState()
    cloudError?.let { message ->
        AlertDialog(onDismissRequest = { viewModel.clearCloudError() }, title = { Text("StudySync") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { viewModel.clearCloudError() }) { Text("OK") } })
    }
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userName by viewModel.userName.collectAsState()
    key(isLoggedIn) {
    val navController = rememberNavController()
    fun navigateMain(route: String) {
        navController.navigate(route) {
            if(route == Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
                restoreState = false
            } else {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                restoreState = true
            }
            launchSingleTop = true
        }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    if (!isLoggedIn) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {}
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = PaperWhite,
                    drawerContentColor = OnSurfaceText,
                    modifier = Modifier.width(280.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StorybookGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = PrimaryGreen)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "StudySync",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceContainer)

                    drawerScreens.forEach { screen ->
                        NavigationDrawerItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontWeight = FontWeight.Bold) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigateMain(screen.route)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = StorybookGreen,
                                selectedIconColor = DarkGreenContainer,
                                selectedTextColor = DarkGreenContainer,
                                unselectedIconColor = PencilGray,
                                unselectedTextColor = OnSurfaceText
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SecondaryContainerBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userName.take(2), color = OnSecondaryContainerBlue, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(userName, style = MaterialTheme.typography.labelLarge, color = OnSurfaceText)
                            Text("Focus Plan", style = MaterialTheme.typography.bodySmall, color = PencilGray)
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopNavBar(
                        title = currentRoute.replaceFirstChar { it.uppercase() },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                },
                bottomBar = {
                    // Floating Curved Bottom Navigation Bar Widget
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(28.dp))
                                .clip(RoundedCornerShape(28.dp))
                                .background(PaperWhite)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomNavScreens.forEach { screen ->
                                val selected = currentRoute == screen.route
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) StorybookGreen else Color.Transparent)
                                        .clickable {
                                            navigateMain(screen.route)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (selected) DarkGreenContainer else PencilGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) DarkGreenContainer else PencilGray,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                            onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                            onNavigateToJarvis = { navController.navigate(Screen.Jarvis.route) },
                            onNavigateToNotes = { navController.navigate(Screen.Notes.route) }
                        )
                    }
                    composable(Screen.Tasks.route) { TasksScreen(viewModel = viewModel) }
                    composable(Screen.Schedule.route) { ScheduleScreen(viewModel = viewModel) }
                    composable(Screen.Notes.route) { NotesScreen(viewModel = viewModel) }
                    composable(Screen.Quiz.route) { QuizScreen(viewModel = viewModel) }
                    composable(Screen.Chat.route) { ChatScreen(viewModel = viewModel) }
                    composable(Screen.Jarvis.route) { JarvisScreen(viewModel = viewModel) }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onSignOut = {}
                        )
                    }
                }
            }
        }
    }
}
}

