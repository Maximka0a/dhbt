package com.example.dhbt.presentation.task.edit.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dhbt.R

@Composable
fun AdditionalParametersSection(
    duration: Int?,
    estimatedPomodoros: Int?,
    onDurationChanged: (Int?) -> Unit,
    onEstimatedPomodorosChanged: (Int?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.additional_parameters))

        Spacer(modifier = Modifier.height(8.dp))

        // Длительность
        OutlinedTextField(
            value = duration?.toString() ?: "",
            onValueChange = {
                val newDuration = it.toIntOrNull()
                if (it.isEmpty()) {
                    onDurationChanged(null)
                } else if (newDuration != null && newDuration >= 0) {
                    onDurationChanged(newDuration)
                }
            },
            label = { Text(stringResource(R.string.duration_minutes)) },
            leadingIcon = {
                Icon(
                    imageVector = if (duration != null) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Помидоры
        OutlinedTextField(
            value = estimatedPomodoros?.toString() ?: "",
            onValueChange = {
                val newPomodoros = it.toIntOrNull()
                if (it.isEmpty()) {
                    onEstimatedPomodorosChanged(null)
                } else if (newPomodoros != null && newPomodoros >= 0) {
                    onEstimatedPomodorosChanged(newPomodoros)
                }
            },
            label = { Text(stringResource(R.string.estimated_pomodoros)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_pomodoro),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            supportingText = { Text(stringResource(R.string.pomodoro_explanation)) }
        )
    }
}