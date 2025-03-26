package com.example.dhbt.presentation.habit.edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalSpacing: Int = 0, // Используем простое целое число для вертикального отступа
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val itemConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(itemConstraints) }

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowCount = 0

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth || currentRowCount >= maxItemsInEachRow) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowCount = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width
            currentRowCount++
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        // Вычисляем полную высоту с фиксированным вертикальным отступом
        val height = rows.sumOf { row ->
            row.maxOfOrNull { it.height } ?: 0
        } + (rows.size - 1) * verticalSpacing

        layout(constraints.maxWidth, height) {
            var y = 0

            rows.forEach { row ->
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                val padding = when (horizontalArrangement) {
                    Arrangement.Start -> 0
                    Arrangement.End -> constraints.maxWidth - row.sumOf { it.width }
                    Arrangement.Center -> (constraints.maxWidth - row.sumOf { it.width }) / 2
                    else -> 0
                }

                var x = padding

                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width
                }

                y += rowHeight + verticalSpacing
            }
        }
    }
}
