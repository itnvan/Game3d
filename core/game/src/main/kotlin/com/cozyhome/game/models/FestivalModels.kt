package com.cozyhome.game.models

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Paint
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.mesh.smoothPath
import com.cozyhome.game.progress.Festival
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object FestivalModels {
    private val GOLD = rgb(0xE8B93E)
    private val GOLD_MAT = Mat(roughness = 0.3f, metallic = 0.95f)
    private val LANTERN_RED = rgb(0xD8352A)
    private val PUMPKIN = rgb(0xE07A23)

    fun item(id: String, on: Boolean = true): Mesh? = when (id) {
        "jack_o_lantern" -> mesh { jackOLantern(0f, 0f, 1f) }
        "candy_cauldron" -> mesh { cauldron() }
        "ghost_lamp" -> mesh { ghost(0f, 0f, 1f) }
        "xmas_tree" -> mesh { xmasTree(0f, 0f, 1f) }
        "snowman" -> mesh { snowman(0f, 0f, 1f) }
        "gift_pile" -> mesh { gifts(0f, 0f) }
        "red_lantern" -> mesh { lanternPole(0f, 0f) }
        "apricot_tree" -> mesh { apricot(0f, 0f, 0.7f) }
        "firecracker_string" -> mesh { firecrackers(0f, 1.8f, 0f) ; withMat(Mat.WOOD) { cylinder(0f, 0f, 0f, 0.03f, 1.9f, rgb(0x8E6A4E), sides = 8) } }
        "star_lantern" -> mesh { starLanternPole(0f, 0f, rgb(0xF2C94C)) }
        "rabbit_lantern" -> mesh { rabbitLantern(0f, 0.3f, 0f) }
        "mooncake_table" -> mesh { mooncakeTable() }
        "heart_arch" -> mesh { heartArch() }
        "rose_vase" -> mesh { roseVase() }
        "cherry_tree" -> mesh { blossomTree(0f, 0f, 0.65f, 77) }
        "flower_cart" -> mesh { flowerCart() }
        "ice_cream_cart" -> mesh { iceCreamCart() }
        "beach_chair" -> mesh { beachChair() }
        "golden_trophy" -> mesh { trophy(GOLD) }
        "fish_trophy" -> mesh { fishPlaque() }
        "bug_case" -> mesh { bugCase() }
        "chef_statue" -> mesh { chefStatue() }
        else -> null
    }

    fun decor(f: Festival): Mesh = mesh {
        when (f) {
            Festival.HALLOWEEN -> {
                for (z in listOf(-4.4f, -3.2f, -2.2f)) for (s in listOf(-1f, 1f)) jackOLantern(s * 1.15f, z, 0.6f)
                jackOLantern(-1.1f, -5.2f, 0.8f); jackOLantern(1.1f, -5.2f, 0.7f)
                for (x in listOf(-9f, 9f)) { hay(x, -3.2f); jackOLantern(x + 0.3f, -3.2f, 0.55f) }
                stringLights(V3(-23.5f, 0.9f, 19.9f), V3(23.5f, 0.9f, 19.9f), 40, listOf(rgb(0xFF8A2A), rgb(0x9B5CFF)))
                ghost(4.6f, 2.5f, 0.8f)
                ghost(-6f, 9f, 0.7f)
            }
            Festival.CHRISTMAS -> {
                xmasTree(7.5f, 1.5f, 1.4f)
                snowman(-2.8f, -0.6f, 0.9f)
                gifts(6.4f, 2.6f)
                roofLights()
                wreath(0f, 2.2f, -5.62f)
                snowPatches()
            }
            Festival.LUNAR_NEW_YEAR -> {
                for (p in listOf(V3(-1.6f, 0f, -2.3f), V3(1.6f, 0f, -2.3f), V3(-2.4f, 0f, 18.8f), V3(2.4f, 0f, 18.8f))) lantern(p.x + 0.28f, 2.05f, p.z, 0.7f)
                lantern(-1.05f, 2.2f, -5.55f, 0.8f); lantern(1.05f, 2.2f, -5.55f, 0.8f)
                apricot(2.6f, -3.0f, 0.75f)
                couplets()
                firecrackers(-1.3f, 2.3f, -5.6f)
                stringLights(V3(-23.5f, 0.9f, 19.9f), V3(23.5f, 0.9f, 19.9f), 34, listOf(LANTERN_RED, GOLD))
            }
            Festival.MID_AUTUMN -> {
                for (z in listOf(-4.4f, -3.0f)) for (s in listOf(-1f, 1f)) starLanternPole(s * 1.3f, z, if (s < 0) rgb(0xF2C94C) else rgb(0xE8433A))
                for (k in 0 until 6) paperLantern(-6f + k * 2.4f, 2.2f, 17.5f, listOf(rgb(0xE8433A), rgb(0xF2C94C), rgb(0xF29BB2))[k % 3])
                rabbitLantern(3.2f, 0.25f, -3.2f)
                stringLights(V3(-23.5f, 0.9f, 19.9f), V3(23.5f, 0.9f, 19.9f), 34, listOf(rgb(0xF2C94C), rgb(0xFFE7A3)))
            }
            Festival.VALENTINE -> {
                heartArchInto(0f, 16.5f)
                for (k in 0 until 6) balloon(-3f + k * 1.2f, -3.2f + (k % 2) * 0.4f)
            }
            Festival.SPRING_BLOOM -> {
                blossomTree(-8.5f, 3f, 0.9f, 81)
                blossomTree(9f, 13f, 0.8f, 82)
                for (k in 0 until 10) flower(-1.4f + (k % 2) * 2.8f, -4.6f + k * 0.28f, 0.24f, listOf(rgb(0xF29BB2), rgb(0xF6D35B), rgb(0xB9A3E6))[k % 3], 300 + k)
            }
            Festival.SUMMER_FUN -> {
                group { translate(12.5f, 0f, 9.5f); rotateY(-30f); beachChair() }
                group { translate(18.2f, 0f, 8.5f); rotateY(30f); iceCreamCart() }
            }
        }
    }

    private fun MeshBuilder.jackOLantern(x: Float, z: Float, s: Float) {
        group {
            translate(x, 0f, z)
            scale(s)
            val ribs = Paint { px, _, pz -> PUMPKIN.mix(PUMPKIN.darker(0.2f), 0.5f + 0.5f * cos(atan2(pz, px) * 10f)) }
            withMat(Mat(roughness = 0.5f)) {
                warpSphere(ribs, 12, 24) { ux, uy, uz ->
                    val rib = 1f - 0.07f * (0.5f - 0.5f * cos(atan2(uz, ux) * 10f))
                    V3(ux * 0.26f * rib, 0.2f + uy * 0.2f, uz * 0.26f * rib)
                }
            }
            withMat(Mat.WOOD) { tube(listOf(V3(0f, 0.38f, 0f), V3(0.02f, 0.45f, 0.01f), V3(0.05f, 0.47f, 0.02f)), { t -> 0.028f - 0.01f * t }, rgb(0x6E5A34), sides = 6, caps = true) }
            withMat(Mat.glow(rgb(0xFFB040), 3.5f)) {
                for (sx in listOf(-1f, 1f)) tri(V3(sx * 0.13f, 0.24f, 0.245f), V3(sx * 0.05f, 0.24f, 0.262f), V3(sx * 0.09f, 0.31f, 0.252f), rgb(0xFFD27A))
                tri(V3(-0.02f, 0.2f, 0.263f), V3(0.02f, 0.2f, 0.263f), V3(0f, 0.23f, 0.262f), rgb(0xFFD27A))
                for (k in 0 until 5) {
                    val a = -0.14f + k * 0.07f
                    tri(V3(a, 0.14f, 0.255f), V3(a + 0.07f, 0.14f, 0.255f), V3(a + 0.035f, if (k % 2 == 0) 0.1f else 0.17f, 0.258f), rgb(0xFFD27A))
                }
            }
        }
    }

    private fun MeshBuilder.hay(x: Float, z: Float) {
        withMat(Mat.FABRIC.copy(texScale = 0.12f)) { roundBox(x, 0.22f, z, 0.9f, 0.44f, 0.5f, 0.06f, rgb(0xD8B860)) }
        withMat(Mat.SOFT) { for (k in 0 until 2) roundBox(x - 0.25f + k * 0.5f, 0.22f, z, 0.03f, 0.46f, 0.52f, 0.01f, rgb(0x8E6A3A)) }
    }

    private fun MeshBuilder.ghost(x: Float, z: Float, s: Float) {
        group {
            translate(x, 0f, z)
            scale(s)
            withMat(Mat.glow(rgb(0xF2F4FF), 0.6f).copy(alpha = 0.85f)) {
                lathe(0f, 0f, listOf(0.28f to 0.25f, 0.24f to 0.5f, 0.2f to 0.8f, 0.12f to 0.95f, 0f to 0.98f), rgb(0xF6F7FF), 18)
                for (k in 0 until 6) {
                    val a = k * PI.toFloat() / 3f
                    sphere(cos(a) * 0.24f, 0.24f, sin(a) * 0.24f, 0.07f, rgb(0xF6F7FF), 4, 8)
                }
            }
            withMat(Mat.EYE) { for (sx in listOf(-1f, 1f)) ellipsoid(sx * 0.07f, 0.78f, 0.17f, 0.03f, 0.045f, 0.02f, rgb(0x1E1E26), 4, 8) }
        }
    }

    private fun MeshBuilder.cauldron() {
        withMat(Mat(roughness = 0.45f, metallic = 0.6f)) {
            lathe(0f, 0f, listOf(0f to 0.05f, 0.22f to 0.08f, 0.3f to 0.25f, 0.28f to 0.42f, 0.31f to 0.45f, 0.26f to 0.45f, 0f to 0.36f), rgb(0x232326), 22)
            for (k in 0 until 3) { val a = k * 2.09f; cylinder(cos(a) * 0.16f, 0f, sin(a) * 0.16f, 0.025f, 0.1f, rgb(0x232326), sides = 6) }
        }
        val rng = Rng(5)
        withMat(Mat.GLOSSY) {
            repeat(14) {
                val a = rng.range(0f, 6.28f)
                val r = rng.range(0f, 0.2f)
                val c = rng.pick(listOf(rgb(0xF2C94C), rgb(0xE8433A), rgb(0x9B5CFF), rgb(0x6FCF97), rgb(0xF29BB2)))
                group { translate(cos(a) * r, 0.42f + rng.range(0f, 0.08f), sin(a) * r); rotateY(rng.range(0f, 360f)); ellipsoid(0f, 0f, 0f, 0.035f, 0.025f, 0.025f, c, 4, 8) }
            }
        }
    }

    private fun MeshBuilder.xmasTree(x: Float, z: Float, s: Float) {
        group {
            translate(x, 0f, z)
            scale(s)
            withMat(Mat.BARK) { cylinder(0f, 0f, 0f, 0.09f, 0.4f, rgb(0x6B5040), sides = 8) }
            for (i in 0 until 4) {
                val k = i / 3f
                val r = 0.85f - k * 0.55f
                withMat(Mat.LEAF) { cone(0f, 0.35f + i * 0.45f, 0f, r, 0.75f, rgb(0x2E5E32).mix(rgb(0x3F7A3E), k), sides = 14) }
            }
            val rng = Rng(9)
            withMat(Mat.GLOSSY.copy(roughness = 0.15f, metallic = 0.4f)) {
                repeat(22) {
                    val h = rng.range(0.45f, 1.9f)
                    val r = (0.85f - (h - 0.35f) / 1.8f * 0.65f).coerceAtLeast(0.15f) * 0.85f
                    val a = rng.range(0f, 6.28f)
                    sphere(cos(a) * r, h, sin(a) * r, 0.05f, rng.pick(listOf(rgb(0xD8352A), GOLD, rgb(0x3E8FE8), rgb(0xF4F1EA))), 4, 8)
                }
            }
            withMat(Mat.glow(rgb(0xFFE07A), 4f)) {
                for (k in 0 until 10) {
                    val a = -PI.toFloat() / 2f + k * PI.toFloat() / 5f
                    val r = if (k % 2 == 0) 0.16f else 0.07f
                    val b = (k + 1) * PI.toFloat() / 5f - PI.toFloat() / 2f
                    val r2 = if ((k + 1) % 2 == 0) 0.16f else 0.07f
                    tri(V3(0f, 2.35f, 0.02f), V3(cos(a) * r, 2.35f + sin(a) * r, 0.02f), V3(cos(b) * r2, 2.35f + sin(b) * r2, 0.02f), rgb(0xFFE07A))
                    tri(V3(0f, 2.35f, -0.02f), V3(cos(b) * r2, 2.35f + sin(b) * r2, -0.02f), V3(cos(a) * r, 2.35f + sin(a) * r, -0.02f), rgb(0xFFE07A))
                }
                repeat(26) {
                    val t = it / 26f
                    val h = 0.45f + t * 1.6f
                    val r = (0.82f - t * 0.6f)
                    val a = t * 6.28f * 3f
                    sphere(cos(a) * r, h, sin(a) * r, 0.022f, if (it % 2 == 0) rgb(0xFFE7A3) else rgb(0xFF9A6B), 3, 6)
                }
            }
        }
    }

    private fun MeshBuilder.snowman(x: Float, z: Float, s: Float) {
        group {
            translate(x, 0f, z)
            scale(s)
            withMat(Mat.CARPET.copy(texScale = 0.2f)) {
                puff(0f, 0.32f, 0f, 0.34f, 0.32f, 0.34f, rgb(0xF4F7FA), subdivisions = 2, wobble = 0.03f, seed = 1, floorY = 0f)
                puff(0f, 0.82f, 0f, 0.24f, 0.23f, 0.24f, rgb(0xF4F7FA), subdivisions = 2, wobble = 0.03f, seed = 2)
                puff(0f, 1.18f, 0f, 0.17f, 0.17f, 0.17f, rgb(0xF4F7FA), subdivisions = 2, wobble = 0.02f, seed = 3)
            }
            withMat(Mat(roughness = 0.5f)) { group { translate(0f, 1.18f, 0.16f); rotateX(90f); cone(0f, 0f, 0f, 0.03f, 0.14f, rgb(0xE8782C), sides = 8) } }
            withMat(Mat.EYE) {
                for (sx in listOf(-1f, 1f)) sphere(sx * 0.06f, 1.24f, 0.15f, 0.018f, rgb(0x1E1E1E), 3, 6)
                for (k in 0 until 3) sphere(0f, 0.72f + k * 0.11f, 0.23f - abs(k - 1) * 0.01f, 0.02f, rgb(0x1E1E1E), 3, 6)
            }
            withMat(Mat.KNIT) { torus(0f, 1.03f, 0f, 0.16f, 0.045f, rgb(0xD8352A), 18, 6); roundBox(0.1f, 0.92f, 0.12f, 0.08f, 0.2f, 0.03f, 0.015f, rgb(0xD8352A)) }
            withMat(Mat.WOOD) { for (sx in listOf(-1f, 1f)) tube(listOf(V3(sx * 0.2f, 0.85f, 0f), V3(sx * 0.4f, 0.98f, 0f), V3(sx * 0.48f, 1.1f, 0f)), { 0.012f }, rgb(0x6B5040), sides = 4, caps = true) }
        }
    }

    private fun MeshBuilder.gifts(x: Float, z: Float) {
        val boxes = listOf(Triple(V3(0f, 0.16f, 0f), 0.32f, rgb(0xD8352A)), Triple(V3(0.32f, 0.12f, 0.08f), 0.24f, rgb(0x3E8FE8)), Triple(V3(0.08f, 0.42f, 0.02f), 0.2f, rgb(0x4E9A6A)), Triple(V3(-0.3f, 0.1f, 0.14f), 0.2f, GOLD))
        group {
            translate(x, 0f, z)
            for ((p, sz, c) in boxes) {
                withMat(Mat.GLOSSY.copy(roughness = 0.35f)) { roundBox(p.x, p.y, p.z, sz, sz, sz, 0.015f, c) }
                withMat(Mat.FABRIC) {
                    roundBox(p.x, p.y, p.z, sz + 0.006f, sz + 0.006f, sz * 0.16f, 0.004f, rgb(0xF4F1EA))
                    roundBox(p.x, p.y, p.z, sz * 0.16f, sz + 0.006f, sz + 0.006f, 0.004f, rgb(0xF4F1EA))
                    for (sx in listOf(-1f, 1f)) group { translate(p.x + sx * 0.04f, p.y + sz / 2f + 0.02f, p.z); rotateZ(sx * 35f); ellipsoid(0f, 0f, 0f, 0.045f, 0.02f, 0.02f, rgb(0xF4F1EA), 4, 8) }
                }
            }
        }
    }

    private fun MeshBuilder.lantern(x: Float, y: Float, z: Float, s: Float) {
        group {
            translate(x, y, z)
            scale(s)
            withMat(Mat.glow(rgb(0xFF5A3C), 2.4f)) {
                warpSphere(Paint { px, _, pz -> LANTERN_RED.mix(LANTERN_RED.darker(0.25f), 0.5f + 0.5f * cos(atan2(pz, px) * 8f)) }, 10, 16) { ux, uy, uz -> V3(ux * 0.22f, -0.2f + uy * 0.18f, uz * 0.22f) }
            }
            withMat(GOLD_MAT) {
                cylinder(0f, -0.03f, 0f, 0.1f, 0.03f, GOLD, sides = 14)
                cylinder(0f, -0.4f, 0f, 0.1f, 0.03f, GOLD, sides = 14)
                cylinder(0f, 0f, 0f, 0.006f, 0.2f, GOLD, sides = 4)
            }
            withMat(Mat.FABRIC) { for (k in 0 until 5) cylinder((k - 2) * 0.012f, -0.62f, 0f, 0.005f, 0.22f, LANTERN_RED, sides = 4, caps = false) }
        }
    }

    private fun MeshBuilder.lanternPole(x: Float, z: Float) {
        withMat(Mat.WOOD) {
            cylinder(x, 0f, z, 0.03f, 1.9f, rgb(0x8E6A4E), sides = 8)
            group { translate(x, 1.85f, z); rotateZ(90f); cylinder(0f, -0.02f, 0f, 0.02f, 0.4f, rgb(0x8E6A4E), sides = 6) }
        }
        lantern(x + 0.35f, 1.83f, z, 0.75f)
    }

    private fun MeshBuilder.apricot(x: Float, z: Float, s: Float) {
        roundTree(x, z, s, 61, rgb(0x6E9A48), rgb(0x9CC95A))
        val rng = Rng(62)
        withMat(Mat.glow(rgb(0xFFD54F), 0.4f)) {
            repeat(60) {
                val a = rng.range(0f, 6.28f)
                val h = rng.range(1.5f, 3.2f) * s
                val r = rng.range(0.6f, 1.15f) * s
                for (p in 0 until 5) {
                    val pa = p * 1.2566f
                    sphere(x + cos(a) * r + cos(pa) * 0.03f * s, h, z + sin(a) * r + sin(pa) * 0.03f * s, 0.022f * s, rgb(0xF6C531), 3, 5)
                }
            }
        }
    }

    private fun MeshBuilder.firecrackers(x: Float, y: Float, z: Float) {
        withMat(Mat.FABRIC) { cylinder(x, y - 1.2f, z, 0.008f, 1.2f, GOLD, sides = 4, caps = false) }
        withMat(Mat.GLOSSY) {
            for (k in 0 until 14) {
                val yy = y - 0.1f - k * 0.08f
                for (s in listOf(-1f, 1f)) group { translate(x + s * 0.04f, yy, z); rotateZ(s * 50f); cylinder(0f, 0f, 0f, 0.016f, 0.07f, LANTERN_RED, sides = 8) }
            }
        }
    }

    private fun MeshBuilder.couplets() {
        withMat(Mat.FABRIC) {
            for (sx in listOf(-1f, 1f)) roundBox(sx * 0.95f, 1.15f, -5.62f, 0.2f, 1.2f, 0.01f, 0.004f, LANTERN_RED)
            roundBox(0f, 2.48f, -5.62f, 1.3f, 0.2f, 0.01f, 0.004f, LANTERN_RED)
        }
        withMat(GOLD_MAT) {
            for (sx in listOf(-1f, 1f)) for (k in 0 until 4) roundBox(sx * 0.95f, 0.7f + k * 0.3f, -5.612f, 0.09f, 0.09f, 0.004f, 0.01f, GOLD)
            for (k in 0 until 4) roundBox(-0.42f + k * 0.28f, 2.48f, -5.612f, 0.09f, 0.09f, 0.004f, 0.01f, GOLD)
        }
    }

    private fun MeshBuilder.starShape(cx: Float, cy: Float, cz: Float, r: Float, depth: Float, c: Rgb) {
        for (k in 0 until 10) {
            val a = -PI.toFloat() / 2f + k * PI.toFloat() / 5f
            val b = a + PI.toFloat() / 5f
            val ra = if (k % 2 == 0) r else r * 0.45f
            val rb = if ((k + 1) % 2 == 0) r else r * 0.45f
            val pa = V3(cx + cos(a) * ra, cy - sin(a) * ra, cz)
            val pb = V3(cx + cos(b) * rb, cy - sin(b) * rb, cz)
            tri(V3(cx, cy, cz + depth), pa, pb, c)
            tri(V3(cx, cy, cz - depth), pb, pa, c)
        }
    }

    private fun MeshBuilder.starLanternPole(x: Float, z: Float, c: Rgb) {
        withMat(Mat.WOOD) { tube(smoothPath(listOf(V3(x, 0f, z), V3(x, 1.2f, z), V3(x + 0.2f, 1.75f, z), V3(x + 0.45f, 1.8f, z)), 3), { 0.02f }, rgb(0x8E6A4E), sides = 6, caps = true) }
        withMat(Mat.glow(c, 2.6f)) { starShape(x + 0.45f, 1.45f, z, 0.24f, 0.08f, c) }
        withMat(Mat.FABRIC) { cylinder(x + 0.45f, 1.67f, z, 0.004f, 0.13f, rgb(0xEDE6DA), sides = 3, caps = false) }
    }

    private fun MeshBuilder.paperLantern(x: Float, y: Float, z: Float, c: Rgb) {
        group {
            translate(x, 0f, z)
            withMat(Mat.glow(c, 2.0f)) { warpSphere(c, 8, 12) { ux, uy, uz -> V3(ux * 0.14f, y + uy * 0.17f, uz * 0.14f) } }
            withMat(Mat.FABRIC) { cylinder(0f, y + 0.17f, 0f, 0.004f, 0.4f, rgb(0x5A4632), sides = 3, caps = false) }
        }
    }

    private fun MeshBuilder.rabbitLantern(x: Float, y: Float, z: Float) {
        group {
            translate(x, y, z)
            withMat(Mat.glow(rgb(0xFFF1E6), 1.8f)) {
                ellipsoid(0f, 0.2f, 0f, 0.28f, 0.2f, 0.2f, rgb(0xFFF6EE), 10, 16)
                sphere(0.24f, 0.32f, 0f, 0.13f, rgb(0xFFF6EE), 8, 12)
                for (s in listOf(-1f, 1f)) group { translate(0.26f, 0.45f, s * 0.05f); rotateX(s * 12f); ellipsoid(0f, 0.1f, 0f, 0.035f, 0.12f, 0.02f, rgb(0xFFF6EE), 6, 8) }
            }
            withMat(Mat.EYE) { sphere(0.34f, 0.35f, 0.07f, 0.016f, rgb(0xD8352A), 3, 6) }
            withMat(Mat.WOOD) { cylinder(-0.25f, -0.25f, 0f, 0.012f, 0.3f, rgb(0x8E6A4E), sides = 5) }
        }
    }

    private fun MeshBuilder.mooncakeTable() {
        withMat(Mat.WOOD_FINE) {
            cylinder(0f, 0.38f, 0f, 0.42f, 0.04f, rgb(0x6F4B35), sides = 24)
            cylinder(0f, 0f, 0f, 0.05f, 0.38f, rgb(0x6F4B35), sides = 10)
            cylinder(0f, 0f, 0f, 0.22f, 0.03f, rgb(0x6F4B35), sides = 16)
        }
        withMat(Mat.CERAMIC) { cylinder(0f, 0.42f, 0f, 0.2f, 0.015f, rgb(0xF4F1EA), sides = 20) }
        withMat(Mat(roughness = 0.6f)) {
            for (k in 0 until 4) {
                val a = k * 1.57f + 0.3f
                cylinder(cos(a) * 0.1f, 0.435f, sin(a) * 0.1f, 0.06f, 0.035f, rgb(0xC9853E), sides = 14)
            }
        }
        withMat(Mat.CERAMIC) { lathe(0.25f, -0.15f, listOf(0f to 0.42f, 0.07f to 0.42f, 0.08f to 0.5f, 0.05f to 0.55f, 0.02f to 0.57f, 0f to 0.57f), rgb(0x8FB3A8), 14) }
    }

    private fun MeshBuilder.heartShape(cx: Float, cy: Float, cz: Float, r: Float, c: Rgb) {
        sphere(cx - r * 0.5f, cy + r * 0.25f, cz, r * 0.55f, c, 5, 8)
        sphere(cx + r * 0.5f, cy + r * 0.25f, cz, r * 0.55f, c, 5, 8)
        group { translate(cx, cy - r * 0.15f, cz); rotateZ(45f); roundBox(0f, 0f, 0f, r * 0.95f, r * 0.95f, r * 0.7f, r * 0.15f, c) }
    }

    private fun MeshBuilder.heartArchInto(x: Float, z: Float) {
        group { translate(x, 0f, z); heartArch() }
    }

    private fun MeshBuilder.heartArch() {
        withMat(Mat(roughness = 0.4f, metallic = 0.6f)) {
            arc(0f, 1.3f, 0f, 1.0f, 0.03f, 0f, 180f, rgb(0xF4F1EA), segments = 18, sides = 6)
            for (s in listOf(-1f, 1f)) cylinder(s * 1.0f, 0f, 0f, 0.03f, 1.3f, rgb(0xF4F1EA), sides = 6)
        }
        withMat(Mat.GLOSSY) {
            for (k in 0..8) {
                val a = k / 8f * PI.toFloat()
                heartShape(cos(a) * 1.0f, 1.3f + sin(a) * 1.0f, 0.04f, 0.09f, if (k % 2 == 0) rgb(0xE8435A) else rgb(0xF29BB2))
            }
        }
        for (s in listOf(-1f, 1f)) bush(s * 1.0f, 0.2f, 0.5f, 90 + s.toInt(), rgb(0xE8435A))
    }

    private fun MeshBuilder.roseVase() {
        withMat(Mat(roughness = 0.05f, alpha = 0.45f)) { lathe(0f, 0f, listOf(0f to 0f, 0.07f to 0f, 0.09f to 0.12f, 0.05f to 0.28f, 0.06f to 0.32f, 0f to 0.32f), Palette.GLASS, 18) }
        for (k in 0 until 6) {
            val a = k * 1.05f
            val top = V3(cos(a) * 0.08f, 0.5f + (k % 3) * 0.05f, sin(a) * 0.08f)
            withMat(Mat.SOFT) { tube(smoothPath(listOf(V3(0f, 0.04f, 0f), V3(cos(a) * 0.03f, 0.3f, sin(a) * 0.03f), top), 2), { 0.005f }, rgb(0x4E7F35), sides = 4, caps = false) }
            withMat(Mat.VELVET) {
                for (p in 0 until 4) group { translate(top.x, top.y, top.z); rotateY(p * 90f + k * 20f); rotateX(-20f + p * 4f); ellipsoid(0f, 0.012f, 0.012f, 0.026f, 0.03f, 0.012f, rgb(0xC8203A).mix(rgb(0xE8435A), p / 4f), 4, 8) }
                sphere(top.x, top.y + 0.02f, top.z, 0.018f, rgb(0xA81830), 4, 8)
            }
        }
    }

    private fun MeshBuilder.flowerCart() {
        withMat(Mat.DECK) {
            roundBox(0f, 0.55f, 0f, 1.1f, 0.3f, 0.6f, 0.02f, rgb(0x9C6E4A))
            for (s in listOf(-1f, 1f)) tube(listOf(V3(0.55f, 0.6f, s * 0.25f), V3(0.95f, 0.75f, s * 0.25f)), { 0.02f }, rgb(0x8E6A4E), sides = 6, caps = true)
        }
        withMat(IRON) { for (s in listOf(-1f, 1f)) group { translate(-0.2f, 0.32f, s * 0.33f); rotateX(90f); torus(0f, 0f, 0f, 0.28f, 0.025f, rgb(0x2C2C2E), 20, 5) } }
        withMat(Mat.SOIL) { block(-0.52f, 0.68f, -0.27f, 0.52f, 0.72f, 0.27f, rgb(0x4A3626)) }
        for (k in 0 until 12) group { translate(0f, 0.7f, 0f); flower(-0.45f + (k % 6) * 0.18f, if (k < 6) -0.12f else 0.12f, 0.24f, listOf(rgb(0xF29BB2), rgb(0xF6D35B), rgb(0xB9A3E6), rgb(0xF2875E))[k % 4], 400 + k) }
    }

    private val IRON = Mat(roughness = 0.5f, metallic = 0.6f)

    private fun MeshBuilder.iceCreamCart() {
        withMat(Mat.PAINTED_WOOD) { roundBox(0f, 0.6f, 0f, 1.2f, 0.7f, 0.6f, 0.04f, rgb(0xF4F1EA)) }
        withMat(Mat.GLOSSY) { roundBox(0f, 0.78f, 0.305f, 1.1f, 0.18f, 0.01f, 0.01f, rgb(0x7FC3DD)) }
        withMat(IRON) { for (s in listOf(-1f, 1f)) group { translate(-0.35f, 0.25f, s * 0.32f); rotateX(90f); torus(0f, 0f, 0f, 0.22f, 0.025f, rgb(0x2C2C2E), 18, 5) } }
        withMat(Mat.WOOD) { cylinder(0.45f, 0.95f, 0f, 0.02f, 1.2f, rgb(0xC9A27B), sides = 8) }
        withMat(Mat.FABRIC.copy(doubleSided = true)) {
            for (k in 0 until 8) {
                val a0 = k * PI.toFloat() / 4f
                val a1 = (k + 1) * PI.toFloat() / 4f
                tri(V3(0.45f, 2.25f, 0f), V3(0.45f + cos(a1) * 0.8f, 1.95f, sin(a1) * 0.8f), V3(0.45f + cos(a0) * 0.8f, 1.95f, sin(a0) * 0.8f), if (k % 2 == 0) rgb(0xF29BB2) else rgb(0xF4F1EA))
            }
        }
        for (k in 0 until 3) {
            withMat(Mat.WOOD) { cone(-0.35f + k * 0.2f, 0.95f, 0f, 0.04f, -0.12f, rgb(0xD9A15A), sides = 8) }
            withMat(Mat.SOFT) { sphere(-0.35f + k * 0.2f, 1.0f, 0f, 0.05f, listOf(rgb(0xF29BB2), rgb(0xF6EBD8), rgb(0x8B5A3C))[k], 5, 8) }
        }
    }

    private fun MeshBuilder.beachChair() {
        val stripes = Paint { x, _, _ -> if (((x + 1f) * 8f).toInt() % 2 == 0) rgb(0x3E8FE8) else rgb(0xF4F1EA) }
        withMat(Mat.FABRIC.copy(doubleSided = true)) { group { translate(0f, 0.32f, 0f); rotateX(-35f); block(-0.3f, 0f, -0.45f, 0.3f, 0.01f, 0.45f, stripes) } }
        withMat(Mat.WOOD) {
            for (s in listOf(-1f, 1f)) {
                tube(listOf(V3(s * 0.31f, 0f, 0.4f), V3(s * 0.31f, 0.6f, -0.3f)), { 0.02f }, rgb(0xC9A27B), sides = 6, caps = true)
                tube(listOf(V3(s * 0.31f, 0f, -0.35f), V3(s * 0.31f, 0.35f, 0.3f)), { 0.02f }, rgb(0xC9A27B), sides = 6, caps = true)
            }
        }
    }

    private fun MeshBuilder.trophy(c: Rgb) {
        withMat(Mat.WOOD_FINE) { roundBox(0f, 0.08f, 0f, 0.3f, 0.16f, 0.3f, 0.01f, rgb(0x3A2A20)) }
        withMat(GOLD_MAT) {
            lathe(0f, 0f, listOf(0.1f to 0.16f, 0.1f to 0.19f, 0.04f to 0.22f, 0.03f to 0.32f, 0.06f to 0.36f, 0.15f to 0.48f, 0.16f to 0.62f, 0.14f to 0.62f, 0f to 0.5f), c, 22)
            for (s in listOf(-1f, 1f)) group { translate(s * 0.16f, 0.53f, 0f); rotateY(if (s < 0) 180f else 0f); arc(0f, 0f, 0f, 0.06f, 0.012f, -90f, 90f, c, segments = 8, sides = 5) }
        }
    }

    private fun MeshBuilder.fishPlaque() {
        withMat(Mat.WOOD_FINE) { roundBox(0f, 0.45f, 0f, 0.7f, 0.4f, 0.05f, 0.02f, rgb(0x5A3A27)) }
        withMat(Mat.WOOD_FINE) { block(-0.04f, 0f, -0.04f, 0.04f, 0.3f, 0.04f, rgb(0x5A3A27)) }
        group { translate(0f, 0.45f, 0.05f); rotateY(90f); scale(1.6f); include(ActivityModels.fish("golden_koi")) }
    }

    private fun MeshBuilder.bugCase() {
        withMat(Mat.WOOD_FINE) {
            roundBox(0f, 0.04f, 0f, 0.8f, 0.08f, 0.5f, 0.01f, rgb(0x5A3A27))
            roundBox(0f, 0.5f, 0f, 0.8f, 0.04f, 0.5f, 0.01f, rgb(0x5A3A27))
        }
        withMat(Mat.FABRIC) { block(-0.38f, 0.08f, -0.23f, 0.38f, 0.1f, 0.23f, rgb(0xEDE6DA)) }
        listOf("butterfly", "blue_morpho", "stag_beetle", "ladybug", "dragonfly").forEachIndexed { i, id ->
            group { translate(-0.28f + i * 0.14f, 0.12f, (i % 2) * 0.1f - 0.05f); scale(1.6f); include(ActivityModels.bug(id)) }
        }
        withMat(Mat.GLASS) { block(-0.4f, 0.08f, -0.25f, 0.4f, 0.48f, 0.25f, Palette.GLASS) }
    }

    private fun MeshBuilder.chefStatue() {
        withMat(Mat.STONE) { roundBox(0f, 0.15f, 0f, 0.4f, 0.3f, 0.4f, 0.02f, rgb(0xB8B2A6)) }
        withMat(GOLD_MAT) {
            puff(0f, 0.55f, 0f, 0.16f, 0.1f, 0.16f, GOLD, subdivisions = 2, wobble = 0.12f, seed = 4)
            cylinder(0f, 0.3f, 0f, 0.13f, 0.18f, GOLD, sides = 18)
            lathe(0.18f, 0f, listOf(0f to 0.3f, 0.02f to 0.3f, 0.02f to 0.55f, 0.06f to 0.62f, 0f to 0.64f), GOLD, 10)
        }
    }

    private fun MeshBuilder.stringLights(a: V3, b: V3, n: Int, colors: List<Rgb>) {
        val pts = (0..n).map { k ->
            val t = k / n.toFloat()
            val sag = sin((t * 8f % 1f) * PI.toFloat()) * 0.18f
            V3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t - sag, a.z + (b.z - a.z) * t)
        }
        withMat(Mat.MATTE) { tube(pts, { 0.006f }, rgb(0x2C2C2E), sides = 3, caps = false) }
        pts.forEachIndexed { i, p -> withMat(Mat.glow(colors[i % colors.size], 3f)) { sphere(p.x, p.y - 0.04f, p.z, 0.035f, colors[i % colors.size].lighter(0.3f), 3, 6) } }
    }

    private fun MeshBuilder.roofLights() {
        val y = 2.86f
        val colors = listOf(rgb(0xFFE7A3), rgb(0xFF6B6B), rgb(0x6FCF97), rgb(0x56CCF2))
        stringLights(V3(-6.8f, y, -5.35f), V3(6.8f, y, -5.35f), 30, colors)
        stringLights(V3(-6.8f, y, -16.65f), V3(6.8f, y, -16.65f), 30, colors)
    }

    private fun MeshBuilder.wreath(x: Float, y: Float, z: Float) {
        withMat(Mat.LEAF) { group { translate(x, y, z); rotateX(90f); torus(0f, 0f, 0f, 0.22f, 0.07f, rgb(0x2E5E32), 20, 8) } }
        withMat(Mat.GLOSSY) { for (k in 0 until 8) { val a = k * 0.785f; sphere(x + cos(a) * 0.22f, y + sin(a) * 0.22f, z + 0.06f, 0.025f, rgb(0xD8352A), 3, 6) } }
        withMat(Mat.VELVET) { ellipsoid(x, y - 0.22f, z + 0.07f, 0.08f, 0.04f, 0.02f, rgb(0xD8352A), 4, 8) }
    }

    private fun MeshBuilder.snowPatches() {
        val rng = Rng(77)
        withMat(Mat.CARPET.copy(texScale = 0.5f)) {
            repeat(40) {
                val x = rng.range(-22f, 22f)
                val z = rng.range(-18f, 18f)
                if (abs(x) < 7f && z < -4.5f && z > -17f) return@repeat
                puff(x, 0f, z, rng.range(0.4f, 1.1f), 0.05f, rng.range(0.3f, 0.9f), rgb(0xF4F7FA), subdivisions = 1, wobble = 0.2f, seed = it, floorY = 0f)
            }
        }
    }

    private fun MeshBuilder.balloon(x: Float, z: Float) {
        withMat(Mat.GLOSSY.copy(roughness = 0.15f)) { heartShape(x, 1.7f, z, 0.14f, rgb(0xE8435A)) }
        withMat(Mat.FABRIC) { cylinder(x, 0.0f, z, 0.003f, 1.6f, Palette.WHITE, sides = 3, caps = false) }
    }

    fun firework(color: Rgb, seed: Int): Mesh = mesh {
        val rng = Rng(seed.toLong())
        val spark = color.lighter(0.12f)
        withMat(Mat.glow(color, 1.8f)) {
            repeat(38) {
                val u = rng.range(-1f, 1f)
                val a = rng.range(0f, 6.28f)
                val r = kotlin.math.sqrt(1f - u * u)
                val d = V3(cos(a) * r, u, sin(a) * r)
                val reach = rng.range(0.8f, 1f)
                val up = if (abs(d.y) > 0.9f) V3(1f, 0f, 0f) else V3(0f, 1f, 0f)
                tube(listOf(d * (reach * 0.42f), d * reach), { t -> 0.003f + 0.013f * t }, color, sides = 4, up = up, caps = false)
                sphere(d.x * reach, d.y * reach, d.z * reach, 0.024f, spark, 2, 4)
            }
        }
    }
}
