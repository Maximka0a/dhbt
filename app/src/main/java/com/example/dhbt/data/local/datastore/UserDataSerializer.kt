package com.example.dhbt.data.local.datastore

import androidx.datastore.core.Serializer
import com.example.dhbt.domain.model.UserData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object UserDataSerializer : Serializer<UserData> {
    override val defaultValue: UserData = UserData(name = "Пользователь") // Явно указываем значение

    override suspend fun readFrom(input: InputStream): UserData {
        return try {
            val bytes = input.readBytes()
            if (bytes.isEmpty()) defaultValue
            else Json.decodeFromString(String(bytes))
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserData, output: OutputStream) {
        output.write(Json.encodeToString(t).toByteArray())
    }
}