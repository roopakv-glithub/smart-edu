from pathlib import Path
p=Path('app/src/main/java/com/studysync/app/ui/screens/HomeScreen.kt');s=p.read_text(encoding='utf-8').replace('userName.ifBlank { "Alex Chen" }','userName.ifBlank { "Name was not fetched" }').replace('📍 ${event.location}','${event.location}')
a=s.index('                Row(verticalAlignment = Alignment.CenterVertically) {');b=s.index('\n            }\n        }\n\n        // Task Overview',a);s=s[:a]+s[b:]
s=s.replace('scheduleEvents.take(', 'scheduleEvents.filter { it.date >= java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ENGLISH).format(java.util.Date()) }.sortedBy { it.date }.take(')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/studysync/app/ui/screens/SettingsScreen.kt');s=p.read_text(encoding='utf-8');a=s.index('        // Student Profile Header Card');b=s.index('        // Tactile & AI Preferences',a);s=s[:a]+'''        item {
            TactileCard(modifier = Modifier.fillMaxWidth()) {
                Text(userName.ifBlank { "Name was not fetched" }, style = MaterialTheme.typography.titleLarge)
            }
        }

'''+s[b:];p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/studysync/app/ui/screens/ScheduleScreen.kt');s=p.read_text(encoding='utf-8').replace('Events for $selectedDate','Events').replace('📍 ${event.location}','${event.location}');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/studysync/app/ui/navigation/AppNavigation.kt');s=p.read_text(encoding='utf-8')
s=s.replace('    val drawerState =', '''    fun navigateMain(route: String) {
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
    val drawerState =''')
import re
s=re.sub(r'navController.navigate\(screen.route\) \{\s*popUpTo\(navController.graph.findStartDestination\(\).id\) \{ saveState = true \}\s*launchSingleTop = true\s*restoreState = true\s*\}', 'navigateMain(screen.route)',s)
p.write_text(s,encoding='utf-8')
