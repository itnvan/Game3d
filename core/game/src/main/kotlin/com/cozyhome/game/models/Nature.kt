package com.cozyhome.game.models

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.smoothstep
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Paint
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.mesh.smoothPath
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val BARK_COLOR = rgb(0x7A5A43)
private val LEAF_BASE = rgb(0x5E9B3A)
private val LEAF_SUN = rgb(0x9CC95A)
private val LEAF_SHADE = rgb(0x2F5A26)
private val BLADE_ROOT = rgb(0x3E6A2A)
private val BLADE_TIP = rgb(0xA9CB68)

private fun canopyPaint(base: Rgb, sun: Rgb, shade: Rgb, cx: Float, cy: Float, cz: Float, r: Float) = Paint { x, y, z ->
    val dy = (y - cy) / r
    val dx = x - cx
    val dz = z - cz
    val radial = sqrt(dx * dx + dz * dz) / r
    val top = smoothstep(-0.1f, 0.9f, dy)
    val under = smoothstep(0.2f, -0.9f, dy) * (1f - 0.5f * radial)
    base.mix(sun, top * 0.55f).mix(shade, under * 0.7f)
}

private fun MeshBuilder.trunk(x: Float, z: Float, s: Float, height: Float, rng: Rng, branches: Int): List<V3> {
    val lean = V3(rng.range(-0.15f, 0.15f), 0f, rng.range(-0.15f, 0.15f))
    val pts = (0..5).map { k ->
        val t = k / 5f
        V3(x + lean.x * t * t * s, height * t * s, z + lean.z * t * t * s)
    }
    val tips = ArrayList<V3>()
    withMat(Mat.BARK) {
        tube(smoothPath(pts, 3), { t -> (0.2f - 0.09f * t) * s * (1f + 0.35f * smoothstep(0.12f, 0f, t)) }, BARK_COLOR, sides = 10, caps = false)
        puff(x, 0.04f * s, z, 0.3f * s, 0.12f * s, 0.3f * s, BARK_COLOR.darker(0.1f), subdivisions = 1, wobble = 0.2f, seed = rng.int(1000), floorY = 0f)
        repeat(branches) { b ->
            val a = b * 6.283f / branches + rng.range(-0.4f, 0.4f)
            val from = pts[3 + (b % 2)]
            val reach = rng.range(0.7f, 1.1f) * s
            val mid = from + V3(cos(a) * reach * 0.45f, 0.35f * s, sin(a) * reach * 0.45f)
            val tip = from + V3(cos(a) * reach, 0.9f * s, sin(a) * reach)
            tube(smoothPath(listOf(from, mid, tip), 3), { t -> (0.085f - 0.05f * t) * s }, BARK_COLOR, sides = 7, caps = false)
            tips += tip
        }
    }
    tips += pts.last()
    return tips
}

fun MeshBuilder.roundTree(x: Float, z: Float, s: Float, seed: Int, leaf: Rgb = LEAF_BASE, light: Rgb = LEAF_SUN) {
    val rng = Rng(seed.toLong())
    val tips = trunk(x, z, s, 1.7f, rng, 4)
    val cy = 2.45f * s
    val paint = canopyPaint(leaf, light, leaf.mix(LEAF_SHADE, 0.7f), x, cy, z, 1.3f * s)
    withMat(Mat.LEAF) {
        puff(x, cy, z, 1.05f * s, 0.85f * s, 1.05f * s, paint, subdivisions = 2, wobble = 0.12f, seed = seed)
        for ((i, t) in tips.withIndex()) {
            val r = rng.range(0.55f, 0.75f) * s
            puff(t.x, t.y + 0.15f * s, t.z, r, r * 0.82f, r, paint, subdivisions = 2, wobble = 0.16f, seed = seed + 11 + i)
        }
        repeat(3) { i ->
            val a = rng.range(0f, 6.28f)
            val rr = rng.range(0.5f, 0.85f) * s
            val r = rng.range(0.4f, 0.55f) * s
            puff(x + cos(a) * rr, cy + rng.range(0.25f, 0.7f) * s, z + sin(a) * rr, r, r * 0.8f, r, paint, subdivisions = 1, wobble = 0.18f, seed = seed + 31 + i)
        }
    }
}

fun MeshBuilder.pineTree(x: Float, z: Float, s: Float, seed: Int) {
    withMat(Mat.BARK) {
        frustum(x, 0f, z, 0.17f * s, 0.06f * s, 3.4f * s, BARK_COLOR.darker(0.15f), sides = 9)
    }
    val tiers = 5
    withMat(Mat.LEAF) {
        for (i in 0 until tiers) {
            val k = i / (tiers - 1f)
            val r = (1.25f - k * 0.85f) * s
            val y = (0.85f + i * 0.62f) * s
            val paint = canopyPaint(rgb(0x3D6E35), rgb(0x6E9A48), rgb(0x1F3F22), x, y + 0.3f * s, z, r)
            group {
                translate(x, y, z)
                rotateY(seed * 37f + i * 29f)
                warpSphere(paint, 6, 14) { ux, uy, uz ->
                    val h = (1f - uy) * 0.5f
                    val rad = r * h * (1f + 0.12f * sin(kotlin.math.atan2(uz, ux) * 7f + i))
                    val len = sqrt(ux * ux + uz * uz).coerceAtLeast(1e-4f)
                    V3(ux / len * rad, (0.5f - h) * 1.1f * s - smoothstep(0.75f, 1f, h) * 0.18f * s, uz / len * rad)
                }
            }
        }
        val top = (0.85f + tiers * 0.62f) * s
        cone(x, top - 0.1f * s, z, 0.16f * s, 0.45f * s, rgb(0x5E8C45), sides = 8)
    }
}

fun MeshBuilder.blossomTree(x: Float, z: Float, s: Float, seed: Int) {
    roundTree(x, z, s, seed, rgb(0xF4A6C1), rgb(0xFFD6E6))
    val rng = Rng(seed * 13L)
    withMat(Mat.SOFT) {
        repeat(18) {
            val a = rng.range(0f, 6.28f)
            val r = rng.range(0.3f, 1.5f) * s
            disc(x + cos(a) * r, 0.015f, z + sin(a) * r, 0.03f * s, rgb(0xFFC4DA), sides = 6)
        }
    }
}

fun MeshBuilder.fruitTree(x: Float, z: Float, s: Float, seed: Int, fruit: Rgb) {
    roundTree(x, z, s, seed)
    val rng = Rng(seed * 7L)
    withMat(Mat.GLOSSY) {
        repeat(12) {
            val a = rng.range(0f, 6.28f)
            val h = rng.range(1.9f, 3.0f) * s
            val r = rng.range(0.95f, 1.15f) * s
            sphere(x + cos(a) * r, h, z + sin(a) * r, 0.09f * s, fruit, rings = 5, segments = 8)
        }
    }
}

fun MeshBuilder.bush(x: Float, z: Float, s: Float, seed: Int, flowers: Rgb? = null) {
    val rng = Rng(seed.toLong())
    val cy = 0.35f * s
    val paint = canopyPaint(LEAF_BASE.mix(LEAF_SHADE, 0.15f), LEAF_SUN, LEAF_SHADE, x, cy, z, 0.6f * s)
    withMat(Mat.LEAF) {
        puff(x, cy, z, 0.6f * s, 0.45f * s, 0.6f * s, paint, subdivisions = 2, wobble = 0.15f, seed = seed, floorY = 0f)
        repeat(3) { i ->
            val a = i * 2.1f + rng.range(-0.4f, 0.4f)
            val r = rng.range(0.3f, 0.42f) * s
            puff(x + cos(a) * 0.38f * s, r * 0.8f, z + sin(a) * 0.38f * s, r, r * 0.85f, r, paint, subdivisions = 1, wobble = 0.18f, seed = seed + 5 + i, floorY = 0f)
        }
    }
    if (flowers != null) {
        repeat(9) {
            val a = rng.range(0f, 6.28f)
            val up = rng.range(0.2f, 0.9f)
            val px = x + cos(a) * 0.55f * s * (1f - up * 0.4f)
            val pz = z + sin(a) * 0.55f * s * (1f - up * 0.4f)
            blossom(px, cy + up * 0.35f * s, pz, 0.05f * s, flowers, it + seed)
        }
    }
}

private fun MeshBuilder.blossom(x: Float, y: Float, z: Float, r: Float, petal: Rgb, seed: Int) {
    withMat(Mat.SOFT) {
        group {
            translate(x, y, z)
            rotateY(seed * 41f)
            for (k in 0 until 5) {
                group {
                    rotateY(k * 72f)
                    rotateX(-20f)
                    ellipsoid(0f, 0f, r * 0.9f, r * 0.55f, r * 0.18f, r * 0.9f, petal.mix(Rgb.WHITE, 0.15f * (k % 2)), 3, 6)
                }
            }
            sphere(0f, r * 0.15f, 0f, r * 0.35f, Palette.YELLOW, 3, 6)
        }
    }
}

fun MeshBuilder.flower(x: Float, z: Float, h: Float, petal: Rgb, seed: Int) {
    val rng = Rng(seed.toLong() * 31)
    val bend = V3(rng.range(-0.04f, 0.04f), 0f, rng.range(-0.04f, 0.04f))
    val head = V3(x + bend.x, h, z + bend.z)
    withMat(Mat.SOFT) {
        tube(smoothPath(listOf(V3(x, 0f, z), V3(x + bend.x * 0.3f, h * 0.5f, z + bend.z * 0.3f), head), 3), { 0.008f }, rgb(0x4E7F35), sides = 5, caps = false)
        for (k in 0 until 2) {
            group {
                translate(x, h * (0.25f + k * 0.2f), z)
                rotateY(seed * 53f + k * 160f)
                rotateZ(-50f)
                ellipsoid(0f, 0.035f, 0f, 0.016f, 0.045f, 0.006f, rgb(0x5C9440), 3, 6)
            }
        }
        group {
            translate(head.x, head.y, head.z)
            rotateY(seed * 23f)
            rotateX(-15f)
            val n = 6
            for (k in 0 until n) {
                group {
                    rotateY(k * 360f / n)
                    rotateX(-28f)
                    ellipsoid(0f, 0f, 0.035f, 0.022f, 0.006f, 0.035f, petal.mix(Rgb.WHITE, if (k % 2 == 0) 0f else 0.12f), 3, 7)
                }
            }
            sphere(0f, 0.006f, 0f, 0.017f, Palette.YELLOW.darker(0.05f), 3, 6)
        }
    }
}

fun MeshBuilder.rock(x: Float, z: Float, s: Float, seed: Int) {
    val c = rgb(0x9C978C).mix(rgb(0x7E7A70), (seed % 3) / 3f)
    withMat(Mat.STONE) {
        puff(x, 0.08f * s, z, 0.4f * s, 0.26f * s, 0.32f * s, c, subdivisions = 2, wobble = 0.2f, seed = seed, floorY = -0.02f)
    }
}

fun MeshBuilder.grassTuft(x: Float, z: Float, s: Float, seed: Int, blades: Int = 7) {
    val rng = Rng(seed.toLong() * 17 + 3)
    val paint = Paint { _, y, _ -> BLADE_ROOT.mix(BLADE_TIP, smoothstep(0f, 0.3f * s, y)) }
    withMat(Mat.BLADE) {
        repeat(blades) {
            val a = rng.range(0f, 6.28f)
            val h = rng.range(0.16f, 0.3f) * s
            val w = rng.range(0.018f, 0.028f) * s
            val lean = rng.range(0.05f, 0.14f) * s
            val bx = x + cos(a) * rng.range(0f, 0.06f) * s
            val bz = z + sin(a) * rng.range(0f, 0.06f) * s
            val dx = cos(a)
            val dz = sin(a)
            val px = -dz * w
            val pz = dx * w
            val mid = V3(bx + dx * lean * 0.4f, h * 0.55f, bz + dz * lean * 0.4f)
            val tip = V3(bx + dx * lean, h, bz + dz * lean)
            quad(V3(bx - px, 0f, bz - pz), V3(bx + px, 0f, bz + pz), V3(mid.x + px * 0.6f, mid.y, mid.z + pz * 0.6f), V3(mid.x - px * 0.6f, mid.y, mid.z - pz * 0.6f), paint)
            tri(V3(mid.x - px * 0.6f, mid.y, mid.z - pz * 0.6f), V3(mid.x + px * 0.6f, mid.y, mid.z + pz * 0.6f), tip, paint)
        }
    }
}

fun MeshBuilder.cloud(x: Float, y: Float, z: Float, s: Float, seed: Int) {
    val rng = Rng(seed.toLong())
    repeat(5) { i ->
        val dx = (i - 2) * 1.1f * s + rng.range(-0.3f, 0.3f) * s
        val r = (1.2f - kotlin.math.abs(i - 2) * 0.25f) * s
        puff(x + dx, y + rng.range(-0.2f, 0.3f) * s, z + rng.range(-0.4f, 0.4f) * s, r * 1.2f, r * 0.85f, r, Rgb.WHITE, subdivisions = 1, wobble = 0.08f, seed = seed + i)
    }
}

fun MeshBuilder.leafBlade(
    length: Float, width: Float, paint: Paint,
    curl: Float = 0.25f, droop: Float = 0.2f, splits: Int = 0, segments: Int = 8, across: Int = 3,
    shape: (Float) -> Float = { v -> kotlin.math.sin(Math.PI.toFloat() * v).coerceAtLeast(0f).let { s -> kotlin.math.sqrt(s) } * (1f - 0.25f * v) },
) {
    fun point(u: Float, v: Float): V3 {
        val w = width * 0.5f * shape(v) * u
        return V3(w, -curl * width * u * u * shape(v) - droop * length * v * v, length * v)
    }
    val cols = across * 2
    val ids = Array(segments + 1) { IntArray(cols + 1) }
    val e = 0.01f
    for (j in 0..segments) {
        val v = j / segments.toFloat()
        for (i in 0..cols) {
            val u = i / across.toFloat() - 1f
            val p = point(u, v)
            val du = point(u + e, v) - point(u - e, v)
            val dv = point(u, (v + e).coerceAtMost(1f)) - point(u, (v - e).coerceAtLeast(0f))
            var n = (dv cross du).normalized()
            if (n.y < 0f) n = n * -1f
            ids[j][i] = vertexPublic(p, n, paint)
        }
    }
    val splitEvery = if (splits > 0) (segments / (splits + 1)).coerceAtLeast(1) else 0
    for (j in 0 until segments) for (i in 0 until cols) {
        val edge = i == 0 || i == cols - 1
        if (splitEvery > 0 && edge && j > 0 && j % splitEvery == 0) continue
        triPublic(ids[j][i], ids[j + 1][i], ids[j + 1][i + 1])
        triPublic(ids[j][i], ids[j + 1][i + 1], ids[j][i + 1])
    }
}
