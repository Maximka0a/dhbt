package com.example.dhbt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dhbt.presentation.theme.DHbtTheme
import com.example.dhbt.presentation.util.toColor

@Composable
fun EmojiIcon(
    emoji: String?,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    defaultEmoji: String = "📋"
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji ?: defaultEmoji,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun ColorIndicator(
    colorString: String?,
    modifier: Modifier = Modifier,
    defaultColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(colorString.toColor(defaultColor))
    )
}

@Preview(showBackground = true)
@Composable
fun EmojiIconPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmojiIcon(
                emoji = "🏃",
                backgroundColor = Color(0xFF6200EE),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorIndicatorPreview() {
    DHbtTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ColorIndicator(
                colorString = "#FF5722",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}