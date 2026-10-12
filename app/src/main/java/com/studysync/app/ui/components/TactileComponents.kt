package com.studysync.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysync.app.ui.theme.*

@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = EagerGreen,
    contentColor: Color = PaperWhite,
    bottomShadowColor: Color = DarkGreenContainer,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        label = "btnOffset"
    )
    val bottomBorderPadding by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        label = "borderPad"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bottomShadowColor)
            .padding(bottom = bottomBorderPadding)
            .offset(y = offsetY)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) backgroundColor else SurfaceContainer)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = if (enabled) contentColor else PencilGray,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) contentColor else PencilGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PaperWhite,
    bottomShadowColor: Color = SurfaceContainerHigh,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 2.dp else 0.dp,
        label = "cardOffset"
    )
    val bottomPadding by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 1.dp else 4.dp,
        label = "cardBorderPad"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bottomShadowColor)
            .padding(bottom = bottomPadding)
            .offset(y = offsetY)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, SurfaceContainerHighest.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(18.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun TactileChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (selected) EagerGreen else SurfaceContainerLow
            )
            .border(1.dp, if (selected) DarkGreenContainer.copy(alpha = 0.3f) else SurfaceContainerHighest, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) PaperWhite else OnSurfaceText,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TactileStatCard(
    number: String,
    label: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color,
    numberColor: Color = OnSurfaceText,
    modifier: Modifier = Modifier
) {
    TactileCard(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.displayLarge,
                color = numberColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = PencilGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeColor)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TopNavBar(
    title: String,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(PaperWhite)
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Drawer",
                    tint = OnSurfaceText
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StorybookGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "StudySync",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurfaceText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = PencilGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainerBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AC",
                    color = OnSecondaryContainerBlue,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
