package com.cozyhome.game.models

import com.cozyhome.game.math.Rng
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Rgb
import kotlin.math.cos
import kotlin.math.sin

fun MeshBuilder.roundTree(x: Float, z: Float, s: Float, seed: Int, leaf: Rgb = Palette.LEAF, light: Rgb = Palette.LEAF_LIGHT) {
    val rng = Rng(seed.toLong())
    mat = Mat.MATTE
    frustum(x, 0f, z, 0.24f * s, 0.15f * s, 1.5f * s, Palette.TRUNK, sides = 8)
    group {
        translate(x, 1.25f * s, z)
        rotateZ(28f)
        frustum(0f, 0f, 0f, 0.08f * s, 0.05f * s, 0.55f * s, Palette.TRUNK, sides = 6)
    }
    blob(x, 2.15f * s, z, 1.15f * s, 1.0f * s, 1.15f * s, leaf, 1, 0.13f, seed)
    blob(x + rng.range(-0.5f, 0.5f) * s, 2.75f * s, z + rng.range(-0.4f, 0.4f) * s, 0.75f * s, 0.62f * s, 0.75f * s, light, 1, 0.14f, seed + 1)
    blob(x + rng.range(-0.7f, 0.7f) * s, 1.8f * s, z + rng.range(-0.7f, 0.7f) * s, 0.6f * s, 0.5f * s, 0.6f * s, leaf.mix(Palette.LEAF_DEEP, 0.4f), 1, 0.14f, seed + 2)
}

fun MeshBuilder.pineTree(x: Float, z: Float, s: Float, seed: Int) {
    mat = Mat.MATTE
    cylinder(x, 0f, z, 0.16f * s, 0.8f * s, Palette.TRUNK, sides = 7)
    val tiers = 3
    for (i in 0 until tiers) {
        val k = i / (tiers - 1f)
        val r = (1.15f - k * 0.45f) * s
        val y = (0.6f + i * 0.8f) * s
        val c = Palette.LEAF_DEEP.mix(Palette.LEAF, k * 0.6f)
        group {
            translate(x, y, z)
            rotateY(seed * 37f + i * 20f)
            cone(0f, 0f, 0f, r, 1.35f * s, c, sides = 7, smooth = false)
        }
    }
}

fun MeshBuilder.blossomTree(x: Float, z: Float, s: Float, seed: Int) {
    roundTree(x, z, s, seed, Palette.BLOSSOM, Palette.BLOSSOM.lighter(0.35f))
}

fun MeshBuilder.fruitTree(x: Float, z: Float, s: Float, seed: Int, fruit: Rgb) {
    roundTree(x, z, s, seed)
    val rng = Rng(seed * 7L)
    withMat(Mat.GLOSSY) {
        repeat(9) {
            val a = rng.range(0f, 6.28f)
            val h = rng.range(1.7f, 2.9f) * s
            val r = rng.range(0.85f, 1.05f) * s
            sphere(x + cos(a) * r, h, z + sin(a) * r, 0.1f * s, fruit, rings = 5, segments = 8)
        }
    }
}

fun MeshBuilder.bush(x: Float, z: Float, s: Float, seed: Int, flowers: Rgb? = null) {
    mat = Mat.MATTE
    blob(x, 0.32f * s, z, 0.55f * s, 0.42f * s, 0.55f * s, Palette.LEAF, 1, 0.15f, seed)
    blob(x + 0.3f * s, 0.25f * s, z + 0.1f * s, 0.35f * s, 0.3f * s, 0.35f * s, Palette.LEAF_LIGHT, 0, 0.12f, seed + 3)
    if (flowers != null) {
        val rng = Rng(seed.toLong())
        withMat(Mat.GLOSSY) {
            repeat(6) {
                val a = rng.range(0f, 6.28f)
                sphere(x + cos(a) * 0.42f * s, rng.range(0.35f, 0.6f) * s, z + sin(a) * 0.42f * s, 0.06f * s, flowers, rings = 4, segments = 6)
            }
        }
    }
}

fun MeshBuilder.flower(x: Float, z: Float, h: Float, petal: Rgb, seed: Int) {
    mat = Mat.MATTE
    cylinder(x, 0f, z, 0.012f, h, Palette.LEAF_DEEP, sides = 5, caps = false)
    group {
        translate(x, h * 0.45f, z)
        rotateY(seed * 53f)
        rotateZ(40f)
        ellipsoid(0.05f, 0f, 0f, 0.06f, 0.015f, 0.03f, Palette.LEAF, rings = 3, segments = 6)
    }
    withMat(Mat.GLOSSY) {
        for (k in 0 until 5) {
            val a = k * 1.2566f + seed
            sphere(x + cos(a) * 0.045f, h, z + sin(a) * 0.045f, 0.04f, petal, rings = 4, segments = 6)
        }
        sphere(x, h + 0.01f, z, 0.03f, Palette.YELLOW, rings = 4, segments = 6)
    }
}

fun MeshBuilder.rock(x: Float, z: Float, s: Float, seed: Int) {
    mat = Mat.MATTE
    blob(x, 0.12f * s, z, 0.4f * s, 0.25f * s, 0.32f * s, Palette.STONE.mix(Palette.STONE_DARK, (seed % 3) / 3f), 0, 0.2f, seed)
}

fun MeshBuilder.cloud(x: Float, y: Float, z: Float, s: Float, seed: Int) {
    val rng = Rng(seed.toLong())
    repeat(5) { i ->
        val dx = (i - 2) * 1.1f * s + rng.range(-0.3f, 0.3f) * s
        val r = (1.2f - kotlin.math.abs(i - 2) * 0.25f) * s
        blob(x + dx, y + rng.range(-0.2f, 0.3f) * s, z + rng.range(-0.4f, 0.4f) * s, r * 1.2f, r * 0.85f, r, Rgb.WHITE, 1, 0.08f, seed + i)
    }
}
