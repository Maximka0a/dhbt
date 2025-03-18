package com.example.dhbt.data.local.datastore

import androidx.datastore.core.Serializer
import com.example.dhbt.data.local.data.model.UserPreferences
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        return try {
            val bytes = input.readBytes()
            if (bytes.isEmpty()) defaultValue
            else Json.decodeFromString(String(bytes))
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        output.write(Json.encodeToString(t).toByteArray())
    }
}