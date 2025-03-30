package com.example.dhbt.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SnackbarType {
    SUCCESS, ERROR, INFO, WARNING
}

data class SnackbarData(
    val message: String,
    val type: SnackbarType = SnackbarType.INFO,
    val duration: Long = 3000L
)

@Composable
fun CustomSnackbarHost(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState
) {
    val currentSnackbar by snackbarHostState.currentSnackbar.collectAsState()

    AnimatedVisibility(
        visible = currentSnackbar != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            currentSnackbar?.let { snackbar ->
                SnackbarContent(snackbar = snackbar)
            }
        }
    }
}

@Composable
fun SnackbarContent(snackbar: SnackbarData) {
    val iconAndColor = when (snackbar.type) {
        SnackbarType.SUCCESS -> Icons.Filled.Check to MaterialTheme.colorScheme.primary
        SnackbarType.ERROR -> Icons.Filled.Error to MaterialTheme.colorScheme.error
        SnackbarType.WARNING -> Icons.Filled.Warning to MaterialTheme.colorScheme.errorContainer
        SnackbarType.INFO -> Icons.Filled.Info to MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = iconAndColor.first,
            contentDescription = null,
            tint = iconAndColor.second,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = snackbar.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

class SnackbarHostState {
    private val _currentSnackbar = MutableStateFlow<SnackbarData?>(null)
    val currentSnackbar = _currentSnackbar.asStateFlow()

    private val snackbarQueue = mutableListOf<SnackbarData>()
    private var isShowingSnackbar = false

    suspend fun showSnackbar(
        message: String,
        type: SnackbarType = SnackbarType.INFO,
        duration: Long = 3000L
    ) {
        val snackbar = SnackbarData(message, type, duration)
        snackbarQueue.add(snackbar)

        if (!isShowingSnackbar) {
            processQueue()
        }
    }

    private suspend fun processQueue() {
        if (snackbarQueue.isEmpty()) {
            isShowingSnackbar = false
            return
        }

        isShowingSnackbar = true
        val snackbar = snackbarQueue.removeAt(0)
        _currentSnackbar.value = snackbar

        delay(snackbar.duration)
        _currentSnackbar.value = null

        // Small delay between snackbars
        delay(300)
        processQueue()
    }
}

@Composable
fun rememberSnackbarHostState(): SnackbarHostState {
    return remember { SnackbarHostState() }
}