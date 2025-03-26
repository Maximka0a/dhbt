package com.example.dhbt.presentation.shared

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dhbt.presentation.theme.DHbtTheme
import com.example.dhbt.presentation.util.toColor

@Composable
fun EmojiIcon(
    emoji: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    emojiColor: Color = MaterialTheme.colorScheme.primary,
    emojiSize: TextUnit = TextUnit.Unspecified,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(iconSize)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (emoji.isNullOrEmpty()) {
            // Fallback icon if emoji is not specified
            Text(
                text = "✓",
                color = emojiColor,
                fontWeight = FontWeight.Bold,
                fontSize = emojiSize
            )
        } else {
            Text(
                text = emoji,
                fontSize = emojiSize
            )
        }
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