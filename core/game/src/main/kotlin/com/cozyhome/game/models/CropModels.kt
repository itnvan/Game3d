package com.cozyhome.game.models

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.smoothstep
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Paint
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.mesh.smoothPath
import com.cozyhome.game.world.Lot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object CropModels {
    private val spots = listOf(-0.42f to -0.42f, 0.42f to -0.42f, -0.42f to 0.42f, 0.42f to 0.42f)
    private val DRY = rgb(0x6E4D35)
    private val WET = rgb(0x3E2B20)
    private val STEM = rgb(0x4F7D33)
    private val LEAF_DARK = rgb(0x356B2C)
    private val LEAF_MID = rgb(0x4F8C3A)
    private val LEAF_LIME = rgb(0x8CC152)
    private val TOP = 0.13f

    private val roundLeaf: (Float) -> Float = { v -> sqrt(sin(PI.toFloat() * v.coerceIn(0f, 1f)).coerceAtLeast(0f)) * (1.05f - 0.3f * v) }
    private val narrowLeaf: (Float) -> Float = { v -> sin(PI.toFloat() * v.coerceIn(0f, 1f)).coerceAtLeast(0f) * (1f - 0.5f * v) }

    fun soil(wet: Boolean, planted: Boolean): Mesh = mesh {
        val half = Lot.PLOT_SIZE / 2f
        val base = if (wet) WET else DRY
        withMat(Mat.SOIL.copy(roughness = if (wet) 0.35f else 1f)) {
            block(-half, 0f, -half, half, 0.08f, half, base)
            for (k in 0 until 4) {
                val z = -half + 0.21f + k * 0.42f
                tube(listOf(V3(-half + 0.06f, 0.08f, z), V3(half - 0.06f, 0.08f, z)), { 0.085f }, base.lighter(0.05f), sides = 8, flatten = 0.55f, up = V3(0f, 1f, 0f), caps = false)
            }
        }
        if (planted && !wet) {
            withMat(Mat.SOIL) { for ((x, z) in spots) disc(x, TOP + 0.005f, z, 0.035f, base.darker(0.35f), 8) }
        }
    }

    fun plant(crop: String, stage: Int): Mesh = mesh {
        when (stage) {
            0 -> for ((i, s) in spots.withIndex()) seedling(s.first, s.second, 0.8f, i, true)
            1 -> for ((i, s) in spots.withIndex()) seedling(s.first, s.second, 1.5f, i, false)
            2 -> grown(crop, 0.7f)
            else -> grown(crop, 1f, ripe = true)
        }
    }

    private fun MeshBuilder.blade(len: Float, width: Float, c: Rgb, curl: Float = 0.25f, droop: Float = 0.2f, shape: (Float) -> Float = roundLeaf, segments: Int = 8) {
        val paint = Paint { _, _, z -> c.mix(c.lighter(0.18f), smoothstep(0f, len, z) * 0.6f) }
        withMat(Mat.LEAF_THIN) { leafBlade(len, width, paint, curl = curl, droop = droop, segments = segments, shape = shape) }
    }

    private fun MeshBuilder.seedling(x: Float, z: Float, k: Float, seed: Int, cotyledon: Boolean) {
        val h = 0.06f * k
        withMat(Mat.SOFT) { cylinder(x, TOP - 0.01f, z, 0.007f, h + 0.01f, STEM, sides = 5, caps = false) }
        val n = if (cotyledon) 2 else 4
        for (j in 0 until n) {
            group {
                translate(x, TOP + h, z)
                rotateY(seed * 40f + j * 360f / n)
                rotateX(-(if (cotyledon) 25f else 35f))
                blade(0.05f * k, (if (cotyledon) 0.03f else 0.04f) * k, if (cotyledon) LEAF_LIME else LEAF_MID, droop = 0.15f)
            }
        }
    }

    private fun MeshBuilder.rosette(x: Float, z: Float, len: Float, width: Float, count: Int, inner: Rgb, outer: Rgb, seed: Int, tiltMin: Float = 20f, tiltMax: Float = 70f, curl: Float = 0.3f) {
        val rng = Rng(seed.toLong() * 13 + 7)
        for (i in 0 until count) {
            val t = i / (count - 1f).coerceAtLeast(1f)
            group {
                translate(x, TOP, z)
                rotateY(i * 137.5f + seed * 11f)
                rotateX(-(tiltMax - (tiltMax - tiltMin) * t) + rng.range(-6f, 6f))
                blade(len * (1f - 0.45f * t), width * (1f - 0.35f * t), outer.mix(inner, t), curl = curl, droop = 0.25f - 0.15f * t)
            }
        }
    }

    private fun MeshBuilder.grown(crop: String, k: Float, ripe: Boolean = false) {
        when (crop) {
            "lettuce" -> for ((i, s) in spots.withIndex()) {
                rosette(s.first, s.second, 0.24f * k, 0.2f * k, 11, rgb(0xB7DC74), rgb(0x5E9B3A), 200 + i, 15f, 80f, 0.4f)
                if (ripe) withMat(Mat.LEAF) { puff(s.first, TOP + 0.08f, s.second, 0.09f, 0.08f, 0.09f, rgb(0xB9DD7C), subdivisions = 2, wobble = 0.12f, seed = 210 + i) }
            }
            "carrot" -> for ((i, s) in spots.withIndex()) {
                val rng = Rng(230L + i)
                for (j in 0 until 12) {
                    group {
                        translate(s.first, TOP, s.second)
                        rotateY(j * 30f + i * 20f + rng.range(-8f, 8f))
                        rotateX(-rng.range(10f, 40f))
                        withMat(Mat.SOFT) { cylinder(0f, 0f, 0f, 0.004f, 0.18f * k, STEM, sides = 4, caps = false) }
                        group {
                            translate(0f, 0.18f * k, 0f)
                            rotateX(-35f)
                            blade(0.2f * k, 0.09f * k, LEAF_MID.mix(LEAF_LIME, rng.nextFloat() * 0.4f), curl = 0.1f, droop = 0.35f, shape = { v -> narrowLeaf(v) * (0.5f + 0.5f * abs(sin(v * 22f))) }, segments = 12)
                        }
                    }
                }
                if (ripe) withMat(Mat(roughness = 0.55f)) { lathe(s.first, s.second, listOf(0.05f to TOP - 0.02f, 0.055f to TOP + 0.02f, 0.04f to TOP + 0.045f, 0f to TOP + 0.05f), rgb(0xE8782C), 14) }
            }
            "tomato" -> for ((i, s) in spots.withIndex()) {
                val h = 0.9f * k + 0.15f
                withMat(Mat.WOOD) { cylinder(s.first + 0.07f, 0.05f, s.second, 0.014f, h + 0.1f, rgb(0xB08A5E), sides = 6) }
                val rng = Rng(250L + i)
                val stem = smoothPath(listOf(V3(s.first, TOP, s.second), V3(s.first + 0.03f, TOP + h * 0.4f, s.second + 0.02f), V3(s.first + 0.05f, TOP + h * 0.8f, s.second - 0.02f), V3(s.first + 0.04f, TOP + h, s.second)), 3)
                withMat(Mat.SOFT) { tube(stem, { t -> 0.014f - 0.006f * t }, STEM, sides = 6, caps = false) }
                for (j in 0 until 16) {
                    val p = stem[(j * stem.size / 16).coerceAtMost(stem.size - 1)]
                    group {
                        translate(p.x, p.y, p.z)
                        rotateY(j * 137.5f + i * 30f)
                        rotateX(-rng.range(45f, 75f))
                        blade(0.2f * k, 0.13f * k, LEAF_DARK.mix(LEAF_MID, rng.nextFloat()), curl = 0.2f, droop = 0.3f, shape = { v -> roundLeaf(v) * (0.7f + 0.3f * abs(sin(v * 9f))) })
                    }
                }
                if (ripe) {
                    repeat(5) { f ->
                        val p = stem[(stem.size * (0.3f + f * 0.12f)).toInt().coerceAtMost(stem.size - 1)]
                        val a = rng.range(0f, 6.28f)
                        val c = if (f == 4) rgb(0x8CB84B) else rgb(0xD8352A)
                        val fx = p.x + cos(a) * 0.08f
                        val fz = p.z + sin(a) * 0.08f
                        withMat(Mat.GLOSSY.copy(roughness = 0.2f)) { puff(fx, p.y - 0.04f, fz, 0.055f, 0.048f, 0.055f, c, subdivisions = 2, wobble = 0.05f, seed = f) }
                        withMat(Mat.SOFT) { for (q in 0 until 5) group { translate(fx, p.y + 0.004f, fz); rotateY(q * 72f); rotateX(-80f); blade(0.025f, 0.01f, LEAF_DARK, droop = 0.4f, shape = narrowLeaf) } }
                    }
                }
            }
            "corn" -> for ((i, s) in spots.withIndex()) {
                val h = 1.3f * k
                withMat(Mat.SOFT) { tube(listOf(V3(s.first, 0.08f, s.second), V3(s.first + 0.02f, 0.1f + h * 0.5f, s.second), V3(s.first, 0.1f + h, s.second + 0.02f)), { t -> 0.028f - 0.014f * t }, rgb(0x6D9B3E), sides = 8, caps = false) }
                for (j in 0 until 6) {
                    group {
                        translate(s.first, 0.25f + j * h * 0.14f, s.second)
                        rotateY(j * 160f + i * 30f)
                        rotateX(-50f + j * 4f)
                        blade(0.55f * k, 0.07f, rgb(0x5E9440).mix(rgb(0x86B556), j / 6f), curl = 0.4f, droop = 0.55f, shape = narrowLeaf, segments = 10)
                    }
                }
                if (ripe) {
                    group {
                        translate(s.first + 0.05f, 0.62f, s.second)
                        rotateZ(-22f)
                        withMat(Mat.GLOSSY.copy(roughness = 0.35f)) { capsule(0f, 0f, 0f, 0.045f, 0.22f, rgb(0xF2C53D), sides = 12, capRings = 3) }
                        withMat(Mat.LEAF_THIN) {
                            for (q in 0 until 3) group { rotateY(q * 120f); rotateX(-8f); translate(0f, 0f, 0.035f); leafBlade(0.24f, 0.07f, rgb(0x9BC26A), curl = 0.6f, droop = -0.05f, shape = narrowLeaf) }
                        }
                    }
                }
                withMat(Mat.SOFT) {
                    for (q in 0 until 5) group { translate(s.first, 0.1f + h, s.second + 0.02f); rotateY(q * 72f); rotateX(-25f); cylinder(0f, 0f, 0f, 0.004f, 0.16f, rgb(0xD9C27A), sides = 3, caps = false) }
                }
            }
            "strawberry" -> for ((i, s) in spots.withIndex()) {
                val rng = Rng(270L + i)
                for (j in 0 until 6) {
                    group {
                        translate(s.first, TOP, s.second)
                        rotateY(j * 60f + i * 17f)
                        rotateX(-rng.range(25f, 45f))
                        withMat(Mat.SOFT) { cylinder(0f, 0f, 0f, 0.005f, 0.13f * k, STEM, sides = 4, caps = false) }
                        for (q in -1..1) group {
                            translate(0f, 0.13f * k, 0f)
                            rotateY(q * 40f)
                            rotateX(-45f)
                            blade(0.08f * k, 0.065f * k, LEAF_MID.mix(LEAF_DARK, rng.nextFloat() * 0.6f), curl = 0.25f, droop = 0.1f, shape = { v -> roundLeaf(v) * (0.85f + 0.15f * abs(sin(v * 14f))) })
                        }
                    }
                }
                if (ripe) {
                    for (j in 0 until 4) {
                        val a = j * 1.6f + i
                        val bx = s.first + cos(a) * 0.16f
                        val bz = s.second + sin(a) * 0.16f
                        withMat(Mat.GLOSSY.copy(roughness = 0.2f)) { lathe(bx, bz, listOf(0f to TOP + 0.01f, 0.028f to TOP + 0.03f, 0.036f to TOP + 0.055f, 0.03f to TOP + 0.07f, 0f to TOP + 0.075f), rgb(0xD62D3A), 12) }
                        withMat(Mat.SOFT) { for (q in 0 until 5) group { translate(bx, TOP + 0.072f, bz); rotateY(q * 72f); rotateX(-70f); blade(0.025f, 0.012f, LEAF_MID, droop = 0.2f, shape = narrowLeaf) } }
                    }
                    withMat(Mat.SOFT) {
                        group {
                            translate(s.first, TOP + 0.16f, s.second)
                            for (q in 0 until 5) group { rotateY(q * 72f); rotateX(-80f); ellipsoid(0f, 0f, 0.016f, 0.012f, 0.003f, 0.016f, rgb(0xF7F4EC), 3, 6) }
                            sphere(0f, 0.004f, 0f, 0.008f, rgb(0xF2C94C), 3, 6)
                        }
                    }
                }
            }
            "pumpkin" -> {
                val rng = Rng(5)
                for (j in 0 until 9) {
                    val a = j * 40f
                    val r = rng.range(0.15f, 0.6f)
                    group {
                        translate(cos(a * PI.toFloat() / 180f) * r, TOP + 0.02f, sin(a * PI.toFloat() / 180f) * r)
                        rotateY(a)
                        rotateX(-30f)
                        withMat(Mat.SOFT) { cylinder(0f, 0f, 0f, 0.008f, 0.16f * k, STEM, sides = 4, caps = false) }
                        group {
                            translate(0f, 0.16f * k, 0f)
                            rotateX(-50f)
                            blade(0.36f * k, 0.38f * k, LEAF_MID.mix(LEAF_DARK, rng.nextFloat() * 0.5f), curl = 0.15f, droop = 0.15f, shape = { v -> roundLeaf(v) * (0.85f + 0.15f * abs(sin(v * 6f))) })
                        }
                    }
                }
                withMat(Mat.SOFT) { tube(smoothPath(listOf(V3(-0.6f, TOP, -0.3f), V3(-0.2f, TOP + 0.02f, 0.1f), V3(0.3f, TOP, 0.0f), V3(0.6f, TOP, 0.4f)), 4), { 0.012f }, STEM, sides = 5, caps = false) }
                if (ripe) {
                    val ribs = Paint { x, _, z -> rgb(0xE07A23).mix(rgb(0xC45E14), 0.5f + 0.5f * cos(atan2(z, x) * 10f)) }
                    withMat(Mat(roughness = 0.45f)) {
                        warpSphere(ribs, 14, 30) { x, y, z ->
                            val a = atan2(z, x)
                            val rib = 1f - 0.07f * (0.5f - 0.5f * cos(a * 10f))
                            val r = 0.36f * rib
                            V3(x * r, TOP + 0.24f + y * 0.24f * (1f - 0.25f * smoothstep(0.6f, 1f, abs(y))), z * r)
                        }
                    }
                    withMat(Mat.WOOD) { tube(listOf(V3(0f, TOP + 0.44f, 0f), V3(0.02f, TOP + 0.52f, 0.01f), V3(0.05f, TOP + 0.55f, 0.02f)), { t -> 0.03f - 0.01f * t }, rgb(0x7A6A3E), sides = 7, caps = true) }
                }
            }
            else -> for ((i, s) in spots.withIndex()) {
                val h = 1.15f * k
                withMat(Mat.SOFT) { tube(listOf(V3(s.first, 0.08f, s.second), V3(s.first + 0.02f, 0.1f + h * 0.55f, s.second), V3(s.first, 0.1f + h, s.second + 0.03f)), { t -> 0.022f - 0.008f * t }, rgb(0x6D9B3E), sides = 7, caps = false) }
                for (j in 0 until 5) {
                    group {
                        translate(s.first, 0.3f + j * 0.18f * k, s.second)
                        rotateY(j * 140f + i * 20f)
                        rotateX(-70f)
                        withMat(Mat.SOFT) { cylinder(0f, 0f, 0f, 0.005f, 0.06f, STEM, sides = 4, caps = false) }
                        translate(0f, 0.06f, 0f)
                        blade(0.26f * k, 0.22f * k, LEAF_MID.mix(LEAF_DARK, j / 5f), curl = 0.2f, droop = 0.35f)
                    }
                }
                if (ripe) {
                    group {
                        translate(s.first, 0.1f + h, s.second + 0.04f)
                        rotateX(65f)
                        withMat(Mat.SOFT) {
                            for (p in 0 until 16) {
                                group { rotateY(p * 22.5f); translate(0f, 0f, 0.11f); ellipsoid(0f, 0f, 0f, 0.025f, 0.006f, 0.06f, rgb(0xF6C531).mix(rgb(0xF2A51E), (p % 3) / 3f), 3, 6) }
                            }
                            cylinder(0f, -0.015f, 0f, 0.1f, 0.035f, rgb(0x4A2F1E), sides = 16)
                        }
                        withMat(Mat.SOIL.copy(texScale = 0.08f)) { disc(0f, 0.022f, 0f, 0.095f, rgb(0x3A2416), 16) }
                    }
                }
            }
        }
    }

    fun icon(crop: String): Mesh = mesh {
        when (crop) {
            "lettuce" -> {
                rosette(0f, 0f, 0.22f, 0.2f, 12, rgb(0xB7DC74), rgb(0x5E9B3A), 3, 20f, 75f, 0.4f)
                withMat(Mat.LEAF) { puff(0f, TOP + 0.08f, 0f, 0.09f, 0.08f, 0.09f, rgb(0xB9DD7C), subdivisions = 2, wobble = 0.12f, seed = 4) }
            }
            "carrot" -> group {
                rotateZ(-30f)
                withMat(Mat(roughness = 0.55f)) { lathe(0f, 0f, listOf(0f to -0.28f, 0.03f to -0.2f, 0.06f to 0.0f, 0.065f to 0.1f, 0.04f to 0.13f, 0f to 0.135f), rgb(0xE8782C), 16) }
                for (j in 0 until 5) group { translate(0f, 0.12f, 0f); rotateY(j * 72f); rotateX(-15f); blade(0.18f, 0.06f, LEAF_MID, droop = 0.2f, shape = narrowLeaf) }
            }
            "tomato" -> {
                withMat(Mat.GLOSSY.copy(roughness = 0.2f)) { puff(0f, 0f, 0f, 0.2f, 0.17f, 0.2f, rgb(0xD8352A), subdivisions = 3, wobble = 0.04f, seed = 5) }
                for (q in 0 until 5) group { translate(0f, 0.16f, 0f); rotateY(q * 72f); rotateX(-80f); blade(0.08f, 0.03f, LEAF_DARK, droop = 0.4f, shape = narrowLeaf) }
            }
            "corn" -> group {
                rotateZ(-30f)
                withMat(Mat.GLOSSY.copy(roughness = 0.35f)) { capsule(0f, -0.25f, 0f, 0.08f, 0.5f, rgb(0xF2C53D), sides = 14, capRings = 4) }
                withMat(Mat.LEAF_THIN) { for (q in 0 until 3) group { translate(0f, -0.25f, 0f); rotateY(q * 120f); rotateX(-6f); translate(0f, 0f, 0.06f); leafBlade(0.4f, 0.12f, rgb(0x9BC26A), curl = 0.6f, droop = -0.05f, shape = narrowLeaf) } }
            }
            "strawberry" -> {
                withMat(Mat.GLOSSY.copy(roughness = 0.2f)) { lathe(0f, 0f, listOf(0f to -0.22f, 0.06f to -0.16f, 0.12f to -0.04f, 0.145f to 0.05f, 0.11f to 0.13f, 0f to 0.15f), rgb(0xD62D3A), 20) }
                withMat(Mat.SOFT) {
                    for (q in 0 until 26) {
                        val a = q * 2.4f
                        val t = (q % 6) / 6f
                        val y = -0.16f + t * 0.26f
                        val r = 0.06f + 0.08f * kotlin.math.sin((t * 0.9f + 0.05f) * PI.toFloat())
                        sphere(cos(a) * r, y, sin(a) * r, 0.007f, rgb(0xF2D35B), 2, 4)
                    }
                }
                for (q in 0 until 6) group { translate(0f, 0.14f, 0f); rotateY(q * 60f); rotateX(-75f); blade(0.08f, 0.035f, LEAF_MID, droop = 0.2f, shape = narrowLeaf) }
            }
            "pumpkin" -> {
                val ribs = Paint { x, _, z -> rgb(0xE07A23).mix(rgb(0xC45E14), 0.5f + 0.5f * cos(atan2(z, x) * 10f)) }
                withMat(Mat(roughness = 0.45f)) {
                    warpSphere(ribs, 14, 30) { x, y, z ->
                        val rib = 1f - 0.07f * (0.5f - 0.5f * cos(atan2(z, x) * 10f))
                        V3(x * 0.26f * rib, y * 0.18f, z * 0.26f * rib)
                    }
                }
                withMat(Mat.WOOD) { tube(listOf(V3(0f, 0.15f, 0f), V3(0.02f, 0.22f, 0.01f), V3(0.05f, 0.25f, 0.02f)), { t -> 0.025f - 0.008f * t }, rgb(0x7A6A3E), sides = 7, caps = true) }
            }
            else -> group {
                rotateX(70f)
                withMat(Mat.SOFT) {
                    for (p in 0 until 16) group { rotateY(p * 22.5f); translate(0f, 0f, 0.15f); ellipsoid(0f, 0f, 0f, 0.035f, 0.008f, 0.08f, rgb(0xF6C531).mix(rgb(0xF2A51E), (p % 3) / 3f), 3, 6) }
                    cylinder(0f, -0.02f, 0f, 0.13f, 0.04f, rgb(0x4A2F1E), sides = 18)
                }
            }
        }
    }
}
