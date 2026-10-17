package com.cozyhome.app.data

import android.content.Context
import android.util.Log
import com.cozyhome.game.state.SaveData
import kotlinx.serialization.json.Json
import java.io.File

class SaveStore(context: Context) {
    private val dir = context.filesDir
    private val file = File(dir, "save.json")
    private val temp = File(dir, "save.json.tmp")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun load(): SaveData? = runCatching {
        if (!file.exists()) return null
        json.decodeFromString(SaveData.serializer(), file.readText())
    }.onFailure { Log.e("SaveStore", "load failed", it) }.getOrNull()

    @Synchronized
    fun write(data: SaveData) {
        runCatching {
            temp.writeText(json.encodeToString(SaveData.serializer(), data))
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }.onFailure { Log.e("SaveStore", "write failed", it) }
    }

    fun clear() {
        file.delete()
        temp.delete()
    }
}
