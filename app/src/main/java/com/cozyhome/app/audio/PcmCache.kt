package com.cozyhome.app.audio

import android.content.Context
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmCache(context: Context, rate: Int) {
    private val dir: File

    init {
        val stamp = File(context.applicationInfo.sourceDir).lastModified()
        val name = "audio_${stamp}_$rate"
        context.cacheDir.listFiles { f -> f.name.startsWith("audio_") && f.name != name }?.forEach { it.deleteRecursively() }
        dir = File(context.cacheDir, name).apply { mkdirs() }
    }

    fun get(key: String, render: () -> FloatArray): FloatArray {
        val file = File(dir, "$key.pcm")
        read(file)?.let { return it }
        val pcm = render()
        write(file, pcm)
        return pcm
    }

    private fun read(file: File): FloatArray? = runCatching {
        if (!file.exists()) return null
        val bytes = file.readBytes()
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        FloatArray(shorts.size) { shorts[it] * SCALE_IN }
    }.getOrNull()

    private fun write(file: File, pcm: FloatArray) {
        runCatching {
            val shorts = ShortArray(pcm.size) { (pcm[it].coerceIn(-1f, 1f) * 32767f).toInt().toShort() }
            val bytes = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bytes.asShortBuffer().put(shorts)
            val temp = File(dir, file.name + ".tmp")
            temp.writeBytes(bytes.array())
            if (!temp.renameTo(file)) temp.delete()
        }.onFailure { Log.w("Audio", "cache write failed", it) }
    }

    private companion object {
        const val SCALE_IN = 1f / 32767f
    }
}
