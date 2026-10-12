package com.studysync.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.LoginUiState
import com.studysync.app.ui.viewmodel.StudySyncViewModel

@Composable
fun LoginScreen(
    viewModel: StudySyncViewModel,
    onLoginSuccess: () -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Authenticated) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Status Error Banner (Network / Auth Error)
        if (loginState is LoginUiState.Error) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorContainerRed)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (loginState as LoginUiState.Error).message,
                        color = OnErrorContainerRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Brand Emblem & Identity Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "StudySync Logo",
                tint = PrimaryGreen,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "StudySync",
            style = MaterialTheme.typography.headlineLarge,
            color = OnSurfaceText,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Sign in with your VTOP credentials through UniCC. Academic sync and shared study pods are still under development.",
            style = MaterialTheme.typography.bodyMedium,
            color = PencilGray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // VTOP User ID Field
        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("VTOP User ID (Username)") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PencilGray
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // VTOP Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("VTOP Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PencilGray
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = PencilGray
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Session stays active until you sign out or close the app. Your password is never saved.", style = MaterialTheme.typography.bodySmall, color = PencilGray)

        Spacer(modifier = Modifier.height(20.dp))

        // Sign In Tactile Button or Loading Indicator
        if (loginState is LoginUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else {
            TactileButton(
                text = "Sign In with VTOP",
                onClick = {
                    viewModel.authenticate(userId, password)
                    password = ""
                },
                backgroundColor = EagerGreen,
                bottomShadowColor = DarkGreenContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

        }

        Spacer(modifier = Modifier.height(20.dp))

        // Delight Study Insight Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StorybookGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Secure Credential Policy",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurfaceText,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Your password is sent to UniCC over HTTPS. StudySync does not save or log your password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray
                    )
                }
            }
        }
    }
}

