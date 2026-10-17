package com.cozyhome.game.mesh

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class Tex(val size: Int, val asset: String? = null, val normalScale: Float = 0f) {
    NONE(0), GRASS(256, "grass", 1f), WOOD(256, "wood", 0.6f), ROOF(128, "roof", 1f), PLASTER(128, "plaster", 0.6f),
    FABRIC(128, "linen", 0.8f), PAVER(128, "paver", 1f), SOIL(128, "soil", 1f), WATER(128), LEAF(128), BARK(128, "bark", 1f),
    CARPET(128, "carpet", 0.8f), DOT(64), STONE(128, "stone", 1f), BRICK(128, "brick", 1f), TILE(128, "tile", 0.8f),
    PLANKS(256, "planks", 0.7f), KNIT(128, "knit", 1f), SKY(64), DECK(128, "deck", 0.8f), JERSEY(128, "jersey", 0.7f),
    DENIM(128, "denim", 0.8f), LEATHER(128, "leather", 0.7f), VELVET(128, "velvet", 0.6f), TRACK(128, "track", 0.8f),
    SAND(128, "sand", 0.8f),
}

class TexImage(val size: Int, val rgba: ByteArray)

object TextureGen {
    private val images = HashMap<Tex, TexImage>()
    private val pngs = HashMap<Tex, ByteArray>()

    fun png(tex: Tex): ByteArray = synchronized(pngs) {
        pngs.getOrPut(tex) { image(tex).let { Png.encode(it.size, it.size, it.rgba) } }
    }

    fun image(tex: Tex): TexImage = synchronized(images) {
        images.getOrPut(tex) { build(tex) }
    }

    fun install(tex: Tex, image: TexImage) = synchronized(images) { images[tex] = image }

    private fun hash(x: Int, y: Int, seed: Int): Float {
        var h = x * 374761393 + y * 668265263 + seed * 982451653
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7fffffff) / 2147483647f
    }

    private fun noise(u: Float, v: Float, fx: Int, fy: Int, seed: Int): Float {
        val x = u * fx
        val y = v * fy
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val tx = x - ix
        val ty = y - iy
        val sx = tx * tx * (3 - 2 * tx)
        val sy = ty * ty * (3 - 2 * ty)
        fun h(a: Int, b: Int) = hash(Math.floorMod(a, fx), Math.floorMod(b, fy), seed)
        val a = h(ix, iy); val b = h(ix + 1, iy); val c = h(ix, iy + 1); val d = h(ix + 1, iy + 1)
        return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy
    }

    private fun fbm(u: Float, v: Float, fx: Int, fy: Int, octaves: Int, seed: Int): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var mx = fx
        var my = fy
        repeat(octaves) { o ->
            sum += noise(u, v, mx, my, seed + o * 31) * amp
            norm += amp
            amp *= 0.5f
            mx *= 2
            my *= 2
        }
        return sum / norm
    }

    private fun fbm(u: Float, v: Float, f: Int, octaves: Int, seed: Int) = fbm(u, v, f, f, octaves, seed)

    private class Cell(val d1: Float, val d2: Float, val id: Float)

    private fun cells(u: Float, v: Float, n: Int, seed: Int, jitter: Float = 1f): Cell {
        val x = u * n
        val y = v * n
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        var d1 = 9f
        var d2 = 9f
        var id = 0f
        for (dy in -1..1) for (dx in -1..1) {
            val cx = ix + dx
            val cy = iy + dy
            val wx = Math.floorMod(cx, n)
            val wy = Math.floorMod(cy, n)
            val px = cx + 0.5f + (hash(wx, wy, seed) - 0.5f) * jitter
            val py = cy + 0.5f + (hash(wx, wy, seed + 7) - 0.5f) * jitter
            val d = sqrt((x - px) * (x - px) + (y - py) * (y - py))
            if (d < d1) { d2 = d1; d1 = d; id = hash(wx, wy, seed + 13) } else if (d < d2) d2 = d
        }
        return Cell(d1, d2, id)
    }

    private fun build(tex: Tex): TexImage {
        val s = tex.size
        val out = ByteArray(s * s * 4)
        for (y in 0 until s) for (x in 0 until s) {
            val u = x / s.toFloat()
            val v = y / s.toFloat()
            val c = sample(tex, u, v)
            val i = (y * s + x) * 4
            out[i] = to8(c.r); out[i + 1] = to8(c.g); out[i + 2] = to8(c.b); out[i + 3] = to8(c.a)
        }
        return TexImage(s, out)
    }

    private fun to8(v: Float): Byte = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt().toByte()

    private class C(val r: Float, val g: Float, val b: Float, val a: Float = 1f)

    private fun grey(v: Float) = C(v, v, v)

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    private val TAU = (2 * PI).toFloat()

    private fun sample(tex: Tex, u: Float, v: Float): C = when (tex) {
        Tex.SKY -> {
            val t = smooth(0f, 1f, v)
            C(0.35f + 0.5f * t, 0.6f + 0.33f * t, 0.95f + 0.03f * t)
        }
        Tex.DECK -> sample(Tex.PLANKS, u, v)
        Tex.JERSEY, Tex.DENIM, Tex.VELVET -> sample(Tex.FABRIC, u, v)
        Tex.LEATHER -> sample(Tex.PLASTER, u, v)
        Tex.TRACK -> sample(Tex.STONE, u, v)
        Tex.SAND -> sample(Tex.SOIL, u, v)
        Tex.GRASS -> {
            val patch = fbm(u, v, 4, 3, 1)
            val clump = fbm(u, v, 16, 2, 2)
            var blades = 0f
            for (k in 0 until 3) {
                val bend = noise(u, v, 16, 16, 40 + k) * 0.6f
                val cell = noise(u + bend * 0.02f, v, 96, 24, 50 + k)
                blades += smooth(0.62f, 0.9f, cell) * (0.5f + k * 0.25f)
            }
            val lum = 0.74f + patch * 0.16f + clump * 0.08f + blades * 0.12f
            val warm = smooth(0.55f, 0.8f, patch) * 0.06f
            C(lum * (0.95f + warm), lum, lum * (0.88f - warm))
        }
        Tex.WOOD -> {
            val warp = fbm(u, v, 2, 4, 3, 3)
            val rings = sin((u * 7f + warp * 1.4f) * TAU)
            val streak = fbm(u, v, 32, 4, 3, 5)
            val knot = cells(u, v, 3, 6, 0.8f).d1
            val knotDark = smooth(0.16f, 0.04f, knot) * 0.18f
            val g = 0.8f + rings * 0.05f + (streak - 0.5f) * 0.22f - knotDark
            C(g * 1.03f, g * 0.97f, g * 0.9f)
        }
        Tex.PLANKS -> {
            val boards = 6
            val bx = u * boards
            val board = floor(bx).toInt()
            val fx = bx - board
            val shift = hash(board, 0, 60)
            val by = v * 2f + shift
            val seg = floor(by).toInt()
            val fy = by - seg
            val tone = 0.86f + (hash(board, Math.floorMod(seg, 2), 61) - 0.5f) * 0.16f
            val grain = fbm(u, v, 48, 4, 3, 62)
            val rings = sin((fx * 3f + fbm(u, v, 6, 2, 2, 63) * 1.5f) * TAU) * 0.035f
            val seamX = smooth(0.0f, 0.035f, min(fx, 1f - fx))
            val seamY = smooth(0.0f, 0.012f, min(fy, 1f - fy))
            val seam = 0.62f + 0.38f * min(seamX, seamY)
            val g = (tone + (grain - 0.5f) * 0.18f + rings) * seam
            C(g * 1.03f, g * 0.97f, g * 0.9f)
        }
        Tex.ROOF -> {
            val rows = 8f
            val ry = v * rows
            val row = floor(ry).toInt()
            val fy = ry - row
            val shift = if (row % 2 == 0) 0f else 0.5f
            val rx = u * 8f + shift
            val col = floor(rx).toInt()
            val fx = rx - col
            val edge = min(fx, 1f - fx)
            val bottom = 1f - fy
            val curve = sqrt(max(0f, 1f - (edge * 2f - 1f).let { it * it }))
            val shade = 0.66f + bottom * 0.24f + curve * 0.08f
            val gap = 0.5f + 0.5f * smooth(0.0f, 0.06f, edge) * smooth(0.0f, 0.08f, fy)
            val tone = (hash(Math.floorMod(col, 8), Math.floorMod(row, 8), 7) - 0.5f) * 0.1f
            grey((shade + tone + fbm(u, v, 16, 2, 7) * 0.06f) * gap)
        }
        Tex.PLASTER -> {
            val n = fbm(u, v, 8, 4, 8)
            val pit = smooth(0.78f, 0.86f, noise(u, v, 64, 64, 9)) * 0.05f
            grey(0.9f + n * 0.1f - pit)
        }
        Tex.FABRIC -> {
            val f = 48
            val wx = sin(u * TAU * f) * 0.5f + 0.5f
            val wy = sin(v * TAU * f) * 0.5f + 0.5f
            val over = (floor(u * f) + floor(v * f)).toInt() % 2 == 0
            val weave = if (over) wx else wy
            grey(0.84f + weave * 0.1f + fbm(u, v, 8, 3, 10) * 0.07f)
        }
        Tex.KNIT -> {
            val cols = 16
            val rows = 24
            val cx = u * cols
            val fx = cx - floor(cx)
            val cy = v * rows + (if (fx < 0.5f) fx else 1f - fx) * 0.8f
            val fy = cy - floor(cy)
            val stitch = sin(fy * PI.toFloat()) * (0.6f + 0.4f * sin(fx * PI.toFloat() * 2f).let { it * it })
            grey(0.74f + stitch * 0.22f + fbm(u, v, 16, 2, 64) * 0.05f)
        }
        Tex.CARPET -> {
            val fluff = noise(u, v, 128, 128, 11)
            val tuft = noise(u, v, 32, 32, 13)
            grey(0.8f + fluff * 0.12f + tuft * 0.06f + fbm(u, v, 4, 2, 12) * 0.04f)
        }
        Tex.PAVER -> {
            val c = cells(u, v, 4, 13, 0.7f)
            val grout = c.d2 - c.d1
            val inside = 0.5f + 0.5f * smooth(0.02f, 0.12f, grout)
            val bevel = 0.95f + smooth(0.08f, 0.2f, grout) * 0.06f
            val tone = 0.8f + c.id * 0.14f
            val n = fbm(u, v, 16, 3, 14)
            grey((tone + n * 0.1f) * inside * bevel)
        }
        Tex.STONE -> {
            val n = fbm(u, v, 8, 4, 16)
            val speck = smooth(0.82f, 0.9f, noise(u, v, 96, 96, 17)) * 0.08f
            grey(0.8f + n * 0.2f - speck)
        }
        Tex.BRICK -> {
            val rows = 8
            val ry = v * rows
            val row = floor(ry).toInt()
            val fy = ry - row
            val rx = u * 4f + if (row % 2 == 0) 0f else 0.5f
            val brick = floor(rx).toInt()
            val fx = rx - brick
            val m = min(smooth(0f, 0.08f, fy) * smooth(0f, 0.08f, 1f - fy), smooth(0f, 0.04f, fx) * smooth(0f, 0.04f, 1f - fx))
            val tone = 0.82f + hash(Math.floorMod(brick, 4), Math.floorMod(row, rows), 18) * 0.14f + fbm(u, v, 16, 2, 19) * 0.07f
            val mortar = 0.96f
            C(mortar + (tone - mortar) * m, mortar + (tone * 0.97f - mortar) * m, mortar + (tone * 0.94f - mortar) * m)
        }
        Tex.TILE -> {
            val fx = u * 4f - floor(u * 4f)
            val fy = v * 4f - floor(v * 4f)
            val e = min(min(fx, 1f - fx), min(fy, 1f - fy))
            val grout = smooth(0.015f, 0.035f, e)
            val gloss = smooth(0.035f, 0.08f, e) * 0.03f
            grey(0.8f + grout * (0.15f + fbm(u, v, 16, 2, 20) * 0.04f) + gloss)
        }
        Tex.SOIL -> {
            val n = fbm(u, v, 8, 4, 21)
            val clod = cells(u, v, 8, 22).d1
            val pebble = smooth(0.84f, 0.92f, noise(u, v, 64, 64, 23)) * 0.14f
            val furrow = sin(v * TAU * 4f) * 0.04f
            grey(0.7f + n * 0.26f + (0.25f - min(clod, 0.25f)) * 0.5f + pebble + furrow)
        }
        Tex.WATER -> {
            val c = cells(u, v, 5, 24)
            val caustic = 1f - smooth(0.0f, 0.14f, c.d2 - c.d1)
            val n = fbm(u, v, 8, 3, 25)
            val g = 0.84f + caustic * 0.2f + n * 0.05f
            C(g, g, g)
        }
        Tex.LEAF -> {
            val c = cells(u, v, 10, 26, 0.9f)
            val vein = c.d2 - c.d1
            val shade = 0.8f + c.id * 0.16f
            val rim = 0.86f + 0.14f * smooth(0.02f, 0.12f, vein)
            val center = 1f - smooth(0.0f, 0.5f, c.d1) * 0.1f
            val n = fbm(u, v, 16, 2, 28) * 0.06f
            val g = (shade + n) * rim * center
            C(g * 0.98f, g, g * 0.9f)
        }
        Tex.BARK -> {
            val streak = fbm(u, v, 16, 2, 4, 29)
            val crack = smooth(0.7f, 0.82f, noise(u, v, 24, 4, 30))
            grey(0.72f + streak * 0.26f - crack * 0.2f)
        }
        Tex.DOT -> {
            val dx = u - 0.5f
            val dy = v - 0.5f
            val d = sqrt(dx * dx + dy * dy) * 2f
            val a = (1f - d).coerceIn(0f, 1f)
            C(1f, 1f, 1f, a * a * (3f - 2f * a))
        }
        Tex.NONE -> grey(1f)
    }
}
