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
    val tex: Tex = Tex.NONE,
    val blend: Boolean = false,
    val texScale: Float = 1f,
) {
    fun textured(t: Tex, scale: Float) = copy(tex = t, texScale = scale)

    companion object {
        val MATTE = Mat()
        val SOFT = Mat(roughness = 1f)
        val GLOSSY = Mat(roughness = 0.35f)
        val METAL = Mat(roughness = 0.3f, metallic = 0.85f)
        val GLASS = Mat(roughness = 0.05f, alpha = 0.28f)
        val WATER = Mat(roughness = 0.04f, alpha = 0.78f, tex = Tex.WATER, texScale = 1.6f)
        val UNLIT = Mat(unlit = true)
        val WOOD = Mat(roughness = 0.55f, tex = Tex.WOOD, texScale = 1.0f)
        val WOOD_FINE = Mat(roughness = 0.45f, tex = Tex.WOOD, texScale = 0.6f)
        val FABRIC = Mat(roughness = 0.95f, tex = Tex.FABRIC, texScale = 0.35f)
        val VELVET = Mat(roughness = 0.9f, tex = Tex.VELVET, texScale = 0.35f)
        val CARPET = Mat(roughness = 1f, tex = Tex.CARPET, texScale = 0.6f)
        val PLANKS = Mat(roughness = 0.5f, tex = Tex.PLANKS, texScale = 1.6f)
        val DECK = Mat(roughness = 0.75f, tex = Tex.DECK, texScale = 1.5f)
        val KNIT = Mat(roughness = 0.95f, tex = Tex.KNIT, texScale = 0.22f)
        val CLOTH = Mat(roughness = 0.9f, tex = Tex.JERSEY, texScale = 0.22f)
        val DENIM = Mat(roughness = 0.9f, tex = Tex.DENIM, texScale = 0.3f)
        val LEATHER = Mat(roughness = 0.45f, tex = Tex.LEATHER, texScale = 0.5f)
        val PLASTER = Mat(roughness = 0.9f, tex = Tex.PLASTER, texScale = 2.2f)
        val STONE = Mat(roughness = 0.85f, tex = Tex.STONE, texScale = 1.2f)
        val PAVER = Mat(roughness = 0.9f, tex = Tex.PAVER, texScale = 2.0f)
        val BRICK = Mat(roughness = 0.9f, tex = Tex.BRICK, texScale = 1.6f)
        val TILE = Mat(roughness = 0.25f, tex = Tex.TILE, texScale = 1.2f)
        val TRACK = Mat(roughness = 0.9f, tex = Tex.TRACK, texScale = 1.8f)
        val SAND = Mat(roughness = 1f, tex = Tex.SAND, texScale = 1.6f)
        val SKY_DOME = Mat(unlit = true, doubleSided = true, tex = Tex.SKY)
        val BLADE = Mat(roughness = 0.85f, doubleSided = true)
        val LEAF_THIN = Mat(roughness = 0.5f, doubleSided = true, tex = Tex.LEAF, texScale = 0.3f)
        val PAINTED_WOOD = Mat(roughness = 0.5f, tex = Tex.PLASTER, texScale = 3f)
        val COURT = Mat(roughness = 0.55f, tex = Tex.PLASTER, texScale = 3f)
        val CERAMIC = Mat(roughness = 0.18f)
        val PLASTIC = Mat(roughness = 0.4f)
        val ROOF = Mat(roughness = 0.75f, tex = Tex.ROOF, texScale = 1.8f)
        val GRASS = Mat(roughness = 1f, tex = Tex.GRASS, texScale = 2.2f)
        val SOIL = Mat(roughness = 1f, tex = Tex.SOIL, texScale = 1.0f)
        val LEAF = Mat(roughness = 0.8f, tex = Tex.LEAF, texScale = 0.7f)
        val BARK = Mat(roughness = 0.95f, tex = Tex.BARK, texScale = 0.8f)
        val SKIN = Mat(roughness = 0.62f)
        val HAIR = Mat(roughness = 0.42f)
        val EYE = Mat(roughness = 0.06f)
        val SHADOW_BLOB = Mat(unlit = true, blend = true, tex = Tex.DOT, alpha = 0.5f)
        val SOFT_DOT = Mat(roughness = 0.8f, blend = true, tex = Tex.DOT)
        fun glow(color: Rgb, strength: Float = 3f) = Mat(roughness = 0.5f, emissive = color, emissiveStrength = strength)
    }
}
