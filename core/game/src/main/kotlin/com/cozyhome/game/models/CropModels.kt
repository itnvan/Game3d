package com.cozyhome.game.models

import com.cozyhome.game.math.Rng
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.world.Lot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CropModels {
    private val spots = listOf(-0.42f to -0.42f, 0.42f to -0.42f, -0.42f to 0.42f, 0.42f to 0.42f)

    fun soil(wet: Boolean, planted: Boolean): Mesh = mesh {
        val half = Lot.PLOT_SIZE / 2f
        val base = if (wet) Palette.SOIL_WET else Palette.SOIL
        mat = Mat(roughness = if (wet) 0.45f else 1f)
        block(-half, 0f, -half, half, 0.1f, half, base)
        for (k in 0 until 4) {
            val z = -half + 0.21f + k * 0.42f
            group {
                translate(0f, 0.1f, z)
                rotateZ(90f)
                cylinder(0f, -half + 0.08f, 0f, 0.09f, Lot.PLOT_SIZE - 0.16f, base.lighter(0.08f), sides = 6, caps = true)
            }
        }
        if (planted && !wet) {
            withMat(Mat.MATTE) { for ((x, z) in spots) sphere(x, 0.17f, z, 0.03f, base.darker(0.3f), rings = 3, segments = 6) }
        }
    }

    fun plant(crop: String, stage: Int): Mesh = mesh {
        when (stage) {
            0 -> for ((i, s) in spots.withIndex()) seedling(s.first, s.second, 0.9f, i)
            1 -> for ((i, s) in spots.withIndex()) seedling(s.first, s.second, 1.8f, i)
            2 -> grown(crop, 0.7f)
            else -> grown(crop, 1f, ripe = true)
        }
    }

    private fun MeshBuilder.seedling(x: Float, z: Float, k: Float, seed: Int) {
        mat = Mat(roughness = 0.6f)
        cylinder(x, 0.15f, z, 0.012f, 0.09f * k, Palette.LEAF_DEEP, sides = 5, caps = false)
        for (s in listOf(-1f, 1f)) {
            group {
                translate(x, 0.15f + 0.09f * k, z)
                rotateY(seed * 40f)
                rotateZ(s * 50f)
                ellipsoid(0f, 0.04f * k, 0f, 0.025f * k + 0.01f, 0.05f * k, 0.012f, Palette.LEAF_LIGHT, rings = 4, segments = 6)
            }
        }
    }

    private fun MeshBuilder.leafRosette(x: Float, z: Float, r: Float, h: Float, c: Rgb, count: Int, seed: Int) {
        for (i in 0 until count) {
            val a = i / count.toFloat() * 360f + seed * 17f
            group {
                translate(x, 0.14f, z)
                rotateY(a)
                rotateX(55f - (i % 2) * 20f)
                ellipsoid(0f, h * 0.5f, 0f, r * 0.45f, h * 0.55f, 0.02f, if (i % 2 == 0) c else c.darker(0.12f), rings = 5, segments = 8)
            }
        }
    }

    private fun MeshBuilder.grown(crop: String, k: Float, ripe: Boolean = false) {
        mat = Mat(roughness = 0.6f)
        when (crop) {
            "lettuce" -> for ((i, s) in spots.withIndex()) {
                blob(s.first, 0.2f + 0.08f * k, s.second, 0.2f * k, 0.15f * k, 0.2f * k, rgb(0x9BE36A), 1, 0.16f, 200 + i)
                if (ripe) blob(s.first, 0.3f, s.second, 0.14f, 0.1f, 0.14f, rgb(0xC5F28A), 1, 0.15f, 210 + i)
                leafRosette(s.first, s.second, 0.3f * k, 0.2f * k, rgb(0x7FD957), 5, i)
            }
            "carrot" -> for ((i, s) in spots.withIndex()) {
                for (j in 0 until 5) {
                    val a = j / 5f * 2f * PI.toFloat()
                    group {
                        translate(s.first, 0.14f, s.second)
                        rotateY(a * 57.3f)
                        rotateX(15f + j * 4f)
                        cylinder(0f, 0f, 0f, 0.012f, 0.32f * k, Palette.LEAF_DEEP, sides = 4, caps = false)
                        blob(0f, 0.32f * k, 0f, 0.05f * k, 0.07f * k, 0.03f, Palette.LEAF, 0, 0.2f, 230 + i * 5 + j)
                    }
                }
                if (ripe) withMat(Mat.GLOSSY) { lathe(s.first, s.second, listOf(0.07f to 0.14f, 0.075f to 0.2f, 0.05f to 0.24f, 0f to 0.25f), Palette.ORANGE, 10) }
            }
            "tomato" -> for ((i, s) in spots.withIndex()) {
                cylinder(s.first, 0.1f, s.second, 0.02f, 0.85f * k + 0.1f, Palette.WOOD_LIGHT, sides = 6)
                blob(s.first, 0.45f * k + 0.1f, s.second, 0.22f * k, 0.3f * k, 0.22f * k, Palette.LEAF, 1, 0.18f, 250 + i)
                if (ripe) withMat(Mat.GLOSSY) {
                    val rng = Rng(i.toLong() + 9)
                    repeat(4) {
                        val a = rng.range(0f, 6.28f)
                        sphere(s.first + cos(a) * 0.2f, rng.range(0.3f, 0.75f), s.second + sin(a) * 0.2f, 0.065f, Palette.RED, rings = 6, segments = 10)
                    }
                }
            }
            "corn" -> for ((i, s) in spots.withIndex()) {
                val h = 1.25f * k
                cylinder(s.first, 0.1f, s.second, 0.035f, h, rgb(0x7CC850), sides = 7)
                for (j in 0 until 4) {
                    group {
                        translate(s.first, 0.3f + j * h * 0.2f, s.second)
                        rotateY(j * 95f + i * 30f)
                        rotateX(60f)
                        ellipsoid(0f, 0.18f * k, 0f, 0.05f, 0.24f * k, 0.012f, rgb(0x8EDB5E), rings = 4, segments = 6)
                    }
                }
                if (ripe) withMat(Mat.GLOSSY) {
                    group {
                        translate(s.first + 0.05f, 0.6f, s.second)
                        rotateZ(-20f)
                        capsule(0f, 0f, 0f, 0.05f, 0.24f, Palette.YELLOW, sides = 8, capRings = 3)
                        ellipsoid(-0.02f, 0.1f, 0f, 0.055f, 0.13f, 0.04f, rgb(0x9BE36A), rings = 4, segments = 6)
                    }
                }
                cone(s.first, 0.1f + h, s.second, 0.05f, 0.14f, rgb(0xE8D27A), sides = 5)
            }
            "strawberry" -> for ((i, s) in spots.withIndex()) {
                leafRosette(s.first, s.second, 0.28f * k, 0.16f * k, Palette.LEAF, 6, i + 3)
                blob(s.first, 0.22f, s.second, 0.13f * k, 0.08f * k, 0.13f * k, Palette.LEAF_DEEP, 0, 0.18f, 270 + i)
                if (ripe) withMat(Mat.GLOSSY) {
                    for (j in 0 until 3) {
                        val a = j * 2.1f + i
                        lathe(s.first + cos(a) * 0.17f, s.second + sin(a) * 0.17f, listOf(0f to 0.15f, 0.04f to 0.18f, 0.05f to 0.22f, 0.035f to 0.25f, 0f to 0.255f), Palette.RED, 8)
                    }
                    withMat(Mat.GLOSSY) { sphere(s.first, 0.3f, s.second, 0.035f, Palette.WHITE, rings = 4, segments = 6) }
                }
            }
            "pumpkin" -> {
                leafRosette(0f, 0f, 0.7f * k, 0.32f * k, Palette.LEAF, 7, 5)
                for ((i, s) in spots.withIndex()) leafRosette(s.first * 0.9f, s.second * 0.9f, 0.3f * k, 0.16f * k, Palette.LEAF_DEEP, 4, i)
                if (ripe) withMat(Mat.GLOSSY) {
                    val prof = (0..10).map { j ->
                        val t = j / 10f
                        val a = t * PI.toFloat()
                        (sin(a) * 0.42f + 0.02f) to (0.12f + (1f - cos(a)) * 0.2f)
                    }
                    for (r in 0 until 8) {
                        group {
                            translate(0f, 0f, 0f)
                            rotateY(r * 45f)
                            scale(1f, 1f, 0.55f)
                            group { translate(0.12f, 0f, 0f); lathe(0f, 0f, prof, Palette.ORANGE, 10) }
                        }
                    }
                    cylinder(0f, 0.5f, 0f, 0.035f, 0.14f, Palette.TRUNK, sides = 6)
                }
            }
            else -> for ((i, s) in spots.withIndex()) {
                cylinder(s.first, 0.1f, s.second, 0.022f, 1.1f * k, rgb(0x7CC850), sides = 6)
                for (j in 0 until 3) {
                    group {
                        translate(s.first, 0.3f + j * 0.25f * k, s.second)
                        rotateY(j * 120f + i * 20f)
                        rotateX(55f)
                        ellipsoid(0f, 0.1f * k, 0f, 0.07f, 0.12f * k, 0.012f, Palette.LEAF, rings = 4, segments = 6)
                    }
                }
                if (ripe) {
                    group {
                        translate(s.first, 1.2f, s.second)
                        rotateX(-25f)
                        withMat(Mat.GLOSSY) {
                            for (p in 0 until 10) {
                                val a = p / 10f * 2f * PI.toFloat()
                                ellipsoid(cos(a) * 0.14f, 0f, sin(a) * 0.14f, 0.07f, 0.02f, 0.04f, Palette.YELLOW, rings = 3, segments = 6)
                            }
                            cylinder(0f, -0.02f, 0f, 0.1f, 0.05f, Palette.WOOD_DARK, sides = 12)
                        }
                    }
                }
            }
        }
    }

    fun icon(crop: String): Mesh = mesh {
        when (crop) {
            "lettuce" -> blob(0f, 0.2f, 0f, 0.22f, 0.18f, 0.22f, rgb(0x9BE36A), 1, 0.16f, 3)
            "carrot" -> withMat(Mat.GLOSSY) {
                group { rotateZ(-30f); lathe(0f, 0f, listOf(0f to -0.25f, 0.07f to 0.0f, 0.08f to 0.12f, 0f to 0.13f), Palette.ORANGE, 12) }
                blob(0.06f, 0.2f, 0f, 0.08f, 0.1f, 0.05f, Palette.LEAF, 0, 0.2f, 4)
            }
            "tomato" -> withMat(Mat.GLOSSY) { sphere(0f, 0f, 0f, 0.2f, Palette.RED, rings = 10, segments = 14); blob(0f, 0.19f, 0f, 0.07f, 0.03f, 0.07f, Palette.LEAF, 0, 0.2f, 5) }
            "corn" -> withMat(Mat.GLOSSY) { group { rotateZ(-30f); capsule(0f, -0.25f, 0f, 0.08f, 0.5f, Palette.YELLOW, 10, 3) } }
            "strawberry" -> withMat(Mat.GLOSSY) { lathe(0f, 0f, listOf(0f to -0.2f, 0.13f to -0.05f, 0.15f to 0.06f, 0.1f to 0.14f, 0f to 0.15f), Palette.RED, 12); blob(0f, 0.15f, 0f, 0.08f, 0.03f, 0.08f, Palette.LEAF, 0, 0.2f, 6) }
            "pumpkin" -> withMat(Mat.GLOSSY) { ellipsoid(0f, 0f, 0f, 0.25f, 0.18f, 0.25f, Palette.ORANGE, 10, 14); cylinder(0f, 0.15f, 0f, 0.03f, 0.1f, Palette.TRUNK, 6) }
            else -> withMat(Mat.GLOSSY) {
                for (p in 0 until 10) {
                    val a = p / 10f * 2f * PI.toFloat()
                    group { rotateX(90f); ellipsoid(cos(a) * 0.16f, 0f, sin(a) * 0.16f, 0.09f, 0.02f, 0.05f, Palette.YELLOW, 3, 6) }
                }
                group { rotateX(90f); cylinder(0f, -0.02f, 0f, 0.11f, 0.04f, Palette.WOOD_DARK, sides = 14) }
            }
        }
    }
}
