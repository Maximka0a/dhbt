package com.example.dhbt.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
        "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙",
        "🏃", "🚶", "💪", "🧠", "❤️", "💤", "🍎", "🍏", "🥦", "🥗",
        "🥛", "💧", "🍵", "☕", "📚", "📝", "✏️", "📖", "🧘", "🏋️",
        "🚴", "🏊", "⚽", "🏀", "🎯", "🎮", "🎨", "🎭", "🎼", "🎧",
        "💻", "📱", "⏰", "🌞", "🌙", "✨", "🔥", "💡", "🌱", "🌿",
        "🍀", "🌺", "🌈", "☀️", "🌤", "⛅", "🌦", "🌧", "⛈", "🌩"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите эмодзи") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}