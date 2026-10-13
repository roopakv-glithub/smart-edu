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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.ui.components.TactileButton
import com.studysync.app.ui.theme.*
import com.studysync.app.ui.viewmodel.LoginUiState
import com.studysync.app.ui.viewmodel.StudySyncViewModel

enum class AuthMode { STUDENT, ADMIN }

@Composable
fun LoginScreen(
    viewModel: StudySyncViewModel,
    onLoginSuccess: () -> Unit,
    onAdminLoginSuccess: () -> Unit = onLoginSuccess
) {
    var authMode by remember { mutableStateOf(AuthMode.STUDENT) }

    // Student fields
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Admin fields
    var adminId by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }
    var adminErrorMessage by remember { mutableStateOf<String?>(null) }

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Authenticated && authMode == AuthMode.STUDENT) {
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
        Spacer(modifier = Modifier.height(10.dp))

        // Status Error Banner
        val displayedError = if (authMode == AuthMode.ADMIN) adminErrorMessage else (loginState as? LoginUiState.Error)?.message
        if (displayedError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorContainerRed)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayedError,
                    color = OnErrorContainerRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Brand Emblem
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (authMode == AuthMode.ADMIN) TertiaryContainerPurple else SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (authMode == AuthMode.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.School,
                contentDescription = "Logo",
                tint = if (authMode == AuthMode.ADMIN) Color(0xFF6B21A8) else PrimaryGreen,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (authMode == AuthMode.ADMIN) "Admin Portal" else "StudySync",
            style = MaterialTheme.typography.headlineLarge,
            color = OnSurfaceText,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (authMode == AuthMode.ADMIN)
                "Administrator authentication to manage moderators, roles, and content restrictions."
            else
                "Sign in with your university credentials. Real-time sync, AI quizzes, and study collaboration.",
            style = MaterialTheme.typography.bodyMedium,
            color = PencilGray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Switch: Student Login vs Administrator Login (Requirement 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainerLow)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (authMode == AuthMode.STUDENT) PaperWhite else Color.Transparent)
                    .clickable { authMode = AuthMode.STUDENT }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (authMode == AuthMode.STUDENT) PrimaryGreen else PencilGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Student Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (authMode == AuthMode.STUDENT) FontWeight.Bold else FontWeight.Normal,
                        color = if (authMode == AuthMode.STUDENT) OnSurfaceText else PencilGray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (authMode == AuthMode.ADMIN) PaperWhite else Color.Transparent)
                    .clickable { authMode = AuthMode.ADMIN }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (authMode == AuthMode.ADMIN) Color(0xFF6B21A8) else PencilGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Admin Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (authMode == AuthMode.ADMIN) FontWeight.Bold else FontWeight.Normal,
                        color = if (authMode == AuthMode.ADMIN) Color(0xFF6B21A8) else PencilGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (authMode == AuthMode.STUDENT) {
            // Student Login Form
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("VTOP User ID (Username)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PencilGray) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("VTOP Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PencilGray) },
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

            Text("Session stays active until you sign out. Your password is never saved.", style = MaterialTheme.typography.bodySmall, color = PencilGray)

            Spacer(modifier = Modifier.height(20.dp))

            if (loginState is LoginUiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EagerGreen)
                }
            } else {
                TactileButton(
                    text = "Sign In as Student",
                    onClick = { viewModel.authenticate(userId.trim(), password) },
                    enabled = userId.isNotBlank() && password.isNotBlank(),
                    backgroundColor = EagerGreen,
                    bottomShadowColor = DarkGreenContainer,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        } else {
            // Administrator Login Form (Requirement 1)
            OutlinedTextField(
                value = adminId,
                onValueChange = { adminId = it; adminErrorMessage = null },
                label = { Text("Admin ID / Email") },
                placeholder = { Text("admin@studysync.app or Admin ID") },
                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF6B21A8)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = adminPassword,
                onValueChange = { adminPassword = it; adminErrorMessage = null },
                label = { Text("Admin Password") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF6B21A8)) },
                trailingIcon = {
                    IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                        Icon(
                            imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password",
                            tint = PencilGray
                        )
                    }
                },
                visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Admin credentials are authenticated securely via Supabase. Roles are verified on the backend.", style = MaterialTheme.typography.bodySmall, color = PencilGray)

            Spacer(modifier = Modifier.height(20.dp))

            if (loginState is LoginUiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6B21A8))
                }
            } else {
                TactileButton(
                    text = "Sign In as Administrator",
                    onClick = {
                        viewModel.adminLogin(
                            adminId = adminId.trim(),
                            adminPass = adminPassword,
                            onSuccess = onAdminLoginSuccess,
                            onError = { adminErrorMessage = it }
                        )
                    },
                    enabled = adminId.isNotBlank() && adminPassword.isNotBlank(),
                    backgroundColor = Color(0xFF7C3AED),
                    bottomShadowColor = Color(0xFF5B21B6),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security Assurance Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Encrypted Connection Policy",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurfaceText,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Authentication tokens are encrypted using Android Keystore. Roles are strictly verified on Supabase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PencilGray
                    )
                }
            }
        }
    }
}
