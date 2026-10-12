package com.studysync.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = PaperWhite,
    primaryContainer = EagerGreen,
    onPrimaryContainer = DarkGreenContainer,
    secondary = SecondaryBlue,
    onSecondary = PaperWhite,
    secondaryContainer = SecondaryContainerBlue,
    onSecondaryContainer = OnSecondaryContainerBlue,
    tertiary = TertiaryPurple,
    onTertiary = PaperWhite,
    tertiaryContainer = TertiaryContainerPurple,
    background = SurfaceBg,
    onBackground = OnSurfaceText,
    surface = PaperWhite,
    onSurface = OnSurfaceText,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = PencilGray,
    error = ErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

@Composable
fun StudySyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
