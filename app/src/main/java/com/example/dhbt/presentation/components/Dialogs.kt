package com.example.dhbt.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dhbt.R
import com.example.dhbt.presentation.theme.DHbtTheme

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
    confirmText: String = stringResource(id = R.string.confirm),
    dismissText: String = stringResource(id = R.string.cancel)
) {
    Dialog(onDismissRequest = onDismissClick) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissClick) {
                        Text(dismissText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onConfirmClick) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingDialog(
    isLoading: Boolean,
    message: String = stringResource(id = R.string.loading),
    properties: DialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    )
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Dialog(
            onDismissRequest = { },
            properties = properties
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.error_occurred),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (onRetry != null) Arrangement.SpaceBetween
                    else Arrangement.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.ok))
                    }

                    if (onRetry != null) {
                        Button(onClick = onRetry) {
                            Text(stringResource(id = R.string.try_again))
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ConfirmationDialogPreview() {
    DHbtTheme {
        ConfirmationDialog(
            title = "Удалить задачу?",
            message = "Вы уверены, что хотите удалить эту задачу? Это действие нельзя отменить.",
            onConfirmClick = {},
            onDismissClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingDialogPreview() {
    DHbtTheme {
        LoadingDialog(
            isLoading = true,
            message = "Загрузка данных..."
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorDialogPreview() {
    DHbtTheme {
        ErrorDialog(
            errorMessage = "Не удалось загрузить данные. Проверьте подключение к интернету.",
            onDismiss = {},
            onRetry = {}
        )
    }
}