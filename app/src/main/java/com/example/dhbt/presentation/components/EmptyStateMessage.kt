package com.example.dhbt.presentation.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Отображает сообщение о пустом состоянии с опциональной иконкой и кнопкой действия
 */
@Composable
fun EmptyStateMessage(
    message: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp)
        )
    },
    actionLabel: String? = null,
    onActionClicked: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                iconContent?.invoke()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (actionLabel != null && onActionClicked != null) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onActionClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = actionLabel)
                    }
                }
            }
        }
    }
}

/**
 * Удобная функция для использования EmptyStateMessage с ImageVector в качестве иконки
 */
@Composable
fun EmptyStateWithIcon(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    actionLabel: String? = null,
    onActionClicked: (() -> Unit)? = null
) {
    EmptyStateMessage(
        message = message,
        modifier = modifier,
        iconContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(40.dp)
            )
        },
        actionLabel = actionLabel,
        onActionClicked = onActionClicked
    )
}

/**
 * Удобная функция для использования EmptyStateMessage без иконки
 */
@Composable
fun EmptyStateWithoutIcon(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClicked: (() -> Unit)? = null
) {
    EmptyStateMessage(
        message = message,
        modifier = modifier,
        iconContent = null,
        actionLabel = actionLabel,
        onActionClicked = onActionClicked
    )
}