package com.example.dhbt.data.local.datastore

import androidx.datastore.core.Serializer
import com.example.dhbt.data.local.data.model.PomodoroPreferences
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PomodoroPreferencesSerializer : Serializer<PomodoroPreferences> {
    override val defaultValue: PomodoroPreferences = PomodoroPreferences()

    override suspend fun readFrom(input: InputStream): PomodoroPreferences {
        return try {
            val bytes = input.readBytes()
            if (bytes.isEmpty()) defaultValue
            else Json.decodeFromString(String(bytes))
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: PomodoroPreferences, output: OutputStream) {
        output.write(Json.encodeToString(t).toByteArray())
    }
}