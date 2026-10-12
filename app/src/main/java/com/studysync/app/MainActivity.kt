package com.studysync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.studysync.app.ui.navigation.AppNavigation
import com.studysync.app.ui.theme.StudySyncTheme
import com.studysync.app.ui.viewmodel.StudySyncViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StudySyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudySyncTheme {
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
