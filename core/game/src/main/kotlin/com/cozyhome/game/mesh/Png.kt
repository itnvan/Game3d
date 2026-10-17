package com.cozyhome.game.mesh

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

object Png {
    fun encode(width: Int, height: Int, rgba: ByteArray): ByteArray {
        val raw = ByteArray((width * 4 + 1) * height)
        for (y in 0 until height) {
            val row = y * (width * 4 + 1)
            raw[row] = 0
            System.arraycopy(rgba, y * width * 4, raw, row + 1, width * 4)
        }
        val deflater = Deflater(6)
        deflater.setInput(raw)
        deflater.finish()
        val zipped = ByteArrayOutputStream()
        val buf = ByteArray(16384)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            zipped.write(buf, 0, n)
        }
        deflater.end()

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        val ihdr = ByteArrayOutputStream().apply {
            writeInt(width); writeInt(height)
            write(8); write(6); write(0); write(0); write(0)
        }.toByteArray()
        out.chunk("IHDR", ihdr)
        out.chunk("IDAT", zipped.toByteArray())
        out.chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.writeInt(v: Int) {
        write((v ushr 24) and 0xFF); write((v ushr 16) and 0xFF); write((v ushr 8) and 0xFF); write(v and 0xFF)
    }

    private fun ByteArrayOutputStream.chunk(type: String, data: ByteArray) {
        writeInt(data.size)
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        write(typeBytes)
        write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        writeInt(crc.value.toInt())
    }
}
