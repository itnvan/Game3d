package com.cozyhome.game.mesh

import com.cozyhome.game.math.lerp
import com.cozyhome.game.math.valueNoise
import kotlin.math.pow

fun interface Paint {
    fun at(x: Float, y: Float, z: Float): Rgb
}

data class Rgb(val r: Float, val g: Float, val b: Float) : Paint {
    override fun at(x: Float, y: Float, z: Float) = this

    fun mix(o: Rgb, t: Float) = Rgb(lerp(r, o.r, t), lerp(g, o.g, t), lerp(b, o.b, t))
    fun lighter(t: Float) = mix(WHITE, t)
    fun darker(t: Float) = mix(BLACK, t)

    val argb: Int
        get() = (0xFF shl 24) or
            ((r * 255f + 0.5f).toInt().coerceIn(0, 255) shl 16) or
            ((g * 255f + 0.5f).toInt().coerceIn(0, 255) shl 8) or
            (b * 255f + 0.5f).toInt().coerceIn(0, 255)

    fun linear(): FloatArray = floatArrayOf(toLinear(r), toLinear(g), toLinear(b))

    companion object {
        val WHITE = Rgb(1f, 1f, 1f)
        val BLACK = Rgb(0f, 0f, 0f)

        private fun toLinear(c: Float): Float =
            if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
}

fun rgb(hex: Int) = Rgb(
    ((hex shr 16) and 0xFF) / 255f,
    ((hex shr 8) and 0xFF) / 255f,
    (hex and 0xFF) / 255f,
)

fun verticalGradient(y0: Float, bottom: Rgb, y1: Float, top: Rgb) = Paint { _, y, _ ->
    bottom.mix(top, ((y - y0) / (y1 - y0)).coerceIn(0f, 1f))
}

fun mottled(base: Rgb, other: Rgb, scale: Float, amount: Float = 1f, seed: Int = 0) = Paint { x, _, z ->
    val n = valueNoise(x * scale, z * scale, seed) * 0.65f + valueNoise(x * scale * 3.1f, z * scale * 3.1f, seed + 7) * 0.35f
    base.mix(other, (n * amount).coerceIn(0f, 1f))
}

data class Mat(
    val roughness: Float = 0.85f,
    val metallic: Float = 0f,
    val emissive: Rgb? = null,
    val emissiveStrength: Float = 1f,
    val alpha: Float = 1f,
    val unlit: Boolean = false,
    val doubleSided: Boolean = false,
) {
    companion object {
        val MATTE = Mat()
        val SOFT = Mat(roughness = 1f)
        val GLOSSY = Mat(roughness = 0.35f)
        val METAL = Mat(roughness = 0.3f, metallic = 0.85f)
        val GLASS = Mat(roughness = 0.08f, alpha = 0.32f)
        val WATER = Mat(roughness = 0.05f, alpha = 0.72f)
        val UNLIT = Mat(unlit = true)
        fun glow(color: Rgb, strength: Float = 3f) = Mat(roughness = 0.5f, emissive = color, emissiveStrength = strength)
    }
}
