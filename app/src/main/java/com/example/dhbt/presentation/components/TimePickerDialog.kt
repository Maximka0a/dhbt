package com.example.dhbt.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalTime

@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(initialTime.hour.toString()) }
    var minute by remember { mutableStateOf(initialTime.minute.toString().padStart(2, '0')) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour input
                    OutlinedTextField(
                        value = hour,
                        onValueChange = {
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 0..23)) {
                                hour = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        label = { Text("Hour") },
                        singleLine = true
                    )

                    Text(
                        text = ":",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    // Minute input
                    OutlinedTextField(
                        value = minute,
                        onValueChange = {
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 0..59)) {
                                minute = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        label = { Text("Minute") },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val h = hour.toIntOrNull() ?: 0
                            val m = minute.toIntOrNull() ?: 0
                            onTimeSelected(LocalTime.of(h, m))
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}