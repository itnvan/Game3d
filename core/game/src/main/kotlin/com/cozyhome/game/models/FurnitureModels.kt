package com.cozyhome.game.models

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object FurnitureModels {
    private val FABRIC = listOf(rgb(0xFF9AA2), rgb(0x7FE0C0), rgb(0x8ECDF7), rgb(0xFFD27A))
    private val WOODS = listOf(Palette.WOOD_LIGHT, Palette.WOOD_DARK, rgb(0xF4F1EA))
    private val PASTEL = listOf(rgb(0x9EE6CF), rgb(0xFFFFFF), rgb(0xFFB5C5))
    private val FLOWERS = listOf(Palette.PINK, Palette.YELLOW, Palette.CORAL, Palette.LILAC)
    private val GLOW = rgb(0xFFE3A1)

    fun build(id: String, variant: Int, on: Boolean = false): Mesh = mesh {
        val v = variant.coerceAtLeast(0)
        when (id) {
            "sofa" -> sofa(2.0f, FABRIC[v % 4])
            "armchair" -> sofa(1.0f, FABRIC[v % 4])
            "beanbag" -> beanbag(FABRIC[v % 4])
            "chair" -> chair(WOODS[v % 3])
            "stool" -> stool(WOODS[v % 3], FABRIC[(v + 1) % 4])
            "coffee_table" -> coffeeTable(WOODS[v % 3])
            "dining_table" -> diningTable(WOODS[v % 3])
            "side_table" -> sideTable(WOODS[v % 3])
            "desk" -> desk(WOODS[v % 3])
            "bed_single" -> bed(1.0f, FABRIC[v % 4], 0)
            "bed_double" -> bed(1.5f, FABRIC[v % 4], 1)
            "nightstand" -> nightstand(WOODS[v % 3])
            "bookshelf" -> bookshelf(WOODS[v % 3])
            "wardrobe" -> wardrobe(WOODS[v % 3])
            "tv" -> tv(if (v % 2 == 0) Palette.WOOD_LIGHT else rgb(0xF4F1EA), on)
            "fridge" -> fridge(PASTEL[v % 3])
            "stove" -> stove(if (v % 2 == 0) rgb(0xFFFFFF) else rgb(0xFFB5C5))
            "counter" -> counter(PASTEL[v % 3], sink = false)
            "sink" -> counter(PASTEL[v % 3], sink = true)
            "floor_lamp" -> floorLamp(FABRIC[(v + 3) % 4], on)
            "globe_lamp" -> globeLamp(FABRIC[(v + 1) % 4], on)
            "monstera" -> monstera(potColor(v))
            "cactus" -> cactus(potColor(v))
            "fiddle_tree" -> fiddleTree(potColor(v))
            "tulips" -> tulips(FLOWERS[v % 4])
            "rug_round" -> rugRound(FABRIC[v % 4])
            "rug_rect" -> rugRect(3.0f, 2.0f, FABRIC[v % 4])
            "rug_runner" -> rugRect(1.0f, 2.5f, FABRIC[(v + 2) % 4])
            "piano" -> piano(if (v % 2 == 0) Palette.INK else Palette.WOOD_DARK)
            "aquarium" -> aquarium(if (v % 2 == 0) Palette.WOOD_LIGHT else Palette.INK)
            "teddy" -> teddy(listOf(rgb(0xC8915F), rgb(0xFFC2D1), rgb(0xFFFFFF))[v % 3])
            "arcade" -> arcade(listOf(rgb(0x8E6CEF), rgb(0xFF6B8B), rgb(0x3BB7F2))[v % 3])
            "bench" -> benchInto(this, WOODS[v % 3], FABRIC[(v + 1) % 4])
            "picnic_table" -> picnicTable(WOODS[v % 3])
            "flower_bed" -> flowerBed(FLOWERS[v % 4])
            "lamp_post" -> lampPost(if (v % 2 == 0) Palette.NAVY else Palette.WOOD_DARK, on)
            "parasol" -> parasol(FABRIC[v % 4])
            "trampoline" -> trampoline(FABRIC[v % 4])
            "swing" -> swingFrame(FABRIC[v % 4])
            "sandbox" -> sandbox(WOODS[v % 2])
            "bird_bath" -> birdBath(if (v % 2 == 0) Palette.STONE else rgb(0xBFE8F5))
            "gnome" -> gnome(listOf(Palette.RED, Palette.SKY, Palette.MINT)[v % 3])
            "hedge" -> hedge(v % 2 == 1)
            "fruit_tree" -> fruitTree(0f, 0f, 0.62f, 31 + v, listOf(Palette.RED, Palette.ORANGE, Palette.YELLOW)[v % 3])
            "bbq" -> bbq(if (v % 2 == 0) Palette.RED else Palette.INK)
            "pool" -> pool(if (v % 2 == 0) Palette.SKY else Palette.PINK)
            else -> roundBox(0f, 0.25f, 0f, 0.5f, 0.5f, 0.5f, 0.08f, Palette.PINK)
        }
    }

    fun swingSeat(variant: Int): Mesh = mesh {
        val c = FABRIC[variant.coerceAtLeast(0) % 4]
        mat = Mat.MATTE
        for (x in listOf(-0.32f, 0.32f)) cylinder(x, -1.55f, 0f, 0.015f, 1.55f, Palette.OFFWHITE, sides = 5, caps = false)
        mat = Mat(roughness = 0.6f)
        roundBox(0f, -1.55f, 0f, 0.8f, 0.07f, 0.36f, 0.03f, c)
    }

    private fun potColor(v: Int) = listOf(rgb(0xF4A988), rgb(0xFFFFFF), rgb(0x9FD9F2))[v % 3]

    private fun MeshBuilder.legs(w: Float, d: Float, h: Float, r: Float, c: Rgb, inset: Float = 0.08f, sides: Int = 8) {
        for (sx in listOf(-1f, 1f)) for (sz in listOf(-1f, 1f)) {
            frustum(sx * (w / 2f - inset), 0f, sz * (d / 2f - inset), r * 0.75f, r, h, c, sides = sides)
        }
    }

    private fun MeshBuilder.sofa(width: Float, c: Rgb) {
        val dark = c.darker(0.08f)
        mat = Mat.SOFT
        roundBox(0f, 0.3f, 0f, width - 0.02f, 0.3f, 0.86f, 0.1f, dark)
        roundBox(0f, 0.66f, -0.33f, width - 0.02f, 0.62f, 0.24f, 0.11f, dark)
        roundBox(-width / 2f + 0.13f, 0.52f, 0.02f, 0.24f, 0.46f, 0.84f, 0.1f, c)
        roundBox(width / 2f - 0.13f, 0.52f, 0.02f, 0.24f, 0.46f, 0.84f, 0.1f, c)
        val seats = if (width > 1.5f) 2 else 1
        val inner = width - 0.48f
        for (k in 0 until seats) {
            val cx = -inner / 2f + inner / seats * (k + 0.5f)
            roundBox(cx, 0.5f, 0.06f, inner / seats - 0.03f, 0.14f, 0.66f, 0.06f, c.lighter(0.18f))
            roundBox(cx, 0.76f, -0.2f, inner / seats - 0.08f, 0.38f, 0.14f, 0.07f, c.lighter(0.1f))
        }
        roundBox(-width / 2f + 0.42f, 0.66f, -0.06f, 0.26f, 0.26f, 0.1f, 0.05f, Palette.YELLOW.lighter(0.3f))
        mat = Mat(roughness = 0.6f)
        legs(width - 0.1f, 0.8f, 0.15f, 0.04f, Palette.WOOD_DARK)
    }

    private fun MeshBuilder.beanbag(c: Rgb) {
        mat = Mat.SOFT
        ellipsoid(0f, 0.24f, 0.02f, 0.47f, 0.25f, 0.45f, c, rings = 12, segments = 18)
        ellipsoid(0f, 0.42f, -0.2f, 0.38f, 0.24f, 0.2f, c.darker(0.06f), rings = 10, segments = 16)
        ellipsoid(0f, 0.37f, 0.08f, 0.28f, 0.08f, 0.24f, c.lighter(0.12f), rings = 8, segments = 14)
    }

    private fun MeshBuilder.chair(wood: Rgb) {
        mat = Mat(roughness = 0.55f)
        legs(0.42f, 0.42f, 0.44f, 0.025f, wood, 0.04f, 6)
        roundBox(0f, 0.46f, 0f, 0.44f, 0.05f, 0.42f, 0.02f, wood)
        for (x in listOf(-0.18f, 0.18f)) block(x - 0.02f, 0.46f, -0.2f, x + 0.02f, 0.9f, -0.16f, wood)
        roundBox(0f, 0.82f, -0.18f, 0.42f, 0.12f, 0.04f, 0.015f, wood)
        roundBox(0f, 0.63f, -0.18f, 0.36f, 0.06f, 0.03f, 0.01f, wood)
        mat = Mat.SOFT
        roundBox(0f, 0.5f, 0.01f, 0.38f, 0.04f, 0.36f, 0.018f, Palette.MINT.lighter(0.3f))
    }

    private fun MeshBuilder.stool(wood: Rgb, cushion: Rgb) {
        mat = Mat(roughness = 0.55f)
        for (k in 0 until 3) {
            val a = k / 3f * 2f * PI.toFloat()
            group {
                translate(cos(a) * 0.12f, 0f, sin(a) * 0.12f)
                rotateZ(cos(a) * -6f)
                rotateX(sin(a) * 6f)
                cylinder(0f, 0f, 0f, 0.025f, 0.46f, wood, sides = 6)
            }
        }
        cylinder(0f, 0.44f, 0f, 0.19f, 0.04f, wood, sides = 16)
        mat = Mat.SOFT
        ellipsoid(0f, 0.49f, 0f, 0.17f, 0.04f, 0.17f, cushion, rings = 5, segments = 14)
    }

    private fun MeshBuilder.coffeeTable(wood: Rgb) {
        mat = Mat(roughness = 0.45f)
        roundBox(0f, 0.42f, 0f, 1.3f, 0.06f, 0.75f, 0.03f, wood)
        roundBox(0f, 0.16f, 0f, 1.15f, 0.03f, 0.6f, 0.015f, wood.darker(0.08f))
        legs(1.25f, 0.7f, 0.4f, 0.03f, wood, 0.06f)
        mat = Mat.GLOSSY
        roundBox(0.3f, 0.47f, 0.05f, 0.28f, 0.04f, 0.2f, 0.01f, Palette.CORAL)
        roundBox(0.3f, 0.51f, 0.05f, 0.26f, 0.04f, 0.19f, 0.01f, Palette.SKY)
        cylinder(-0.35f, 0.45f, -0.05f, 0.05f, 0.1f, Palette.WHITE, sides = 10)
        cylinder(-0.35f, 0.53f, -0.05f, 0.042f, 0.02f, rgb(0x7A4E36), sides = 10)
        mat = Mat.MATTE
        blob(0.0f, 0.52f, -0.1f, 0.1f, 0.08f, 0.1f, Palette.LEAF, 0, 0.1f, 2)
    }

    private fun MeshBuilder.diningTable(wood: Rgb) {
        mat = Mat(roughness = 0.45f)
        roundBox(0f, 0.74f, 0f, 1.4f, 0.06f, 0.9f, 0.03f, wood)
        legs(1.3f, 0.8f, 0.72f, 0.04f, wood, 0.05f)
        mat = Mat.SOFT
        roundBox(0f, 0.775f, 0f, 0.35f, 0.01f, 0.86f, 0.004f, Palette.CORAL.lighter(0.4f))
        mat = Mat.GLOSSY
        lathe(0f, 0f, listOf(0.0f to 0.78f, 0.12f to 0.78f, 0.16f to 0.84f, 0.17f to 0.86f), Palette.WHITE, 16)
        sphere(-0.05f, 0.88f, 0.02f, 0.05f, Palette.RED, rings = 5, segments = 8)
        sphere(0.06f, 0.87f, -0.03f, 0.05f, Palette.ORANGE, rings = 5, segments = 8)
        sphere(0.0f, 0.9f, 0.07f, 0.045f, Palette.YELLOW, rings = 5, segments = 8)
    }

    private fun MeshBuilder.sideTable(wood: Rgb) {
        mat = Mat(roughness = 0.45f)
        cylinder(0f, 0f, 0f, 0.16f, 0.03f, wood, sides = 14)
        cylinder(0f, 0f, 0f, 0.03f, 0.66f, wood, sides = 8)
        cylinder(0f, 0.64f, 0f, 0.22f, 0.04f, wood, sides = 18)
        mat = Mat.GLOSSY
        frustum(0f, 0.68f, 0f, 0.05f, 0.07f, 0.1f, Palette.PEACH, sides = 10)
        mat = Mat.MATTE
        blob(0f, 0.84f, 0f, 0.1f, 0.09f, 0.1f, Palette.LEAF_LIGHT, 0, 0.12f, 9)
    }

    private fun MeshBuilder.desk(wood: Rgb) {
        mat = Mat(roughness = 0.45f)
        roundBox(0f, 0.73f, -0.1f, 1.4f, 0.05f, 0.7f, 0.02f, wood)
        block(0.32f, 0f, -0.42f, 0.68f, 0.71f, 0.22f, wood.darker(0.05f))
        for (k in 0 until 3) block(0.34f, 0.08f + k * 0.21f, 0.22f, 0.66f, 0.25f + k * 0.21f, 0.235f, wood.lighter(0.15f))
        for (k in 0 until 3) withMat(Mat.METAL) { sphere(0.5f, 0.17f + k * 0.21f, 0.25f, 0.018f, Palette.YELLOW, rings = 3, segments = 6) }
        block(-0.68f, 0f, -0.42f, -0.62f, 0.71f, 0.22f, wood.darker(0.05f))
        mat = Mat.GLOSSY
        roundBox(-0.2f, 0.765f, -0.02f, 0.42f, 0.02f, 0.28f, 0.008f, rgb(0xD5DCE6))
        group {
            translate(-0.2f, 0.775f, -0.16f)
            rotateX(-12f)
            roundBox(0f, 0.14f, 0f, 0.42f, 0.28f, 0.015f, 0.008f, rgb(0xD5DCE6))
            withMat(Mat.glow(rgb(0x9FE3FF), 1.2f)) { block(-0.18f, 0.03f, 0.008f, 0.18f, 0.26f, 0.012f, rgb(0xC8F0FF)) }
        }
        withMat(Mat.METAL) {
            cylinder(0.45f, 0.755f, -0.3f, 0.07f, 0.02f, Palette.CORAL, sides = 12)
            group { translate(0.45f, 0.77f, -0.3f); rotateX(-15f); cylinder(0f, 0f, 0f, 0.015f, 0.35f, Palette.CORAL, sides = 6) }
        }
        withMat(Mat.glow(GLOW, 1.2f)) { cone(0.45f, 1.03f, -0.22f, 0.1f, 0.12f, Palette.CORAL.lighter(0.4f), sides = 12) }
    }

    private fun MeshBuilder.bed(width: Float, blanket: Rgb, style: Int) {
        val len = 2.0f
        mat = Mat(roughness = 0.5f)
        roundBox(0f, 0.2f, 0f, width - 0.04f, 0.24f, len - 0.06f, 0.05f, Palette.WOOD_LIGHT)
        legs(width - 0.08f, len - 0.1f, 0.1f, 0.04f, Palette.WOOD_DARK)
        roundBox(0f, 0.62f, -len / 2f + 0.07f, width, 1.0f, 0.1f, 0.05f, if (style == 0) Palette.WOOD_LIGHT else blanket.darker(0.1f))
        roundBox(0f, 0.38f, len / 2f - 0.05f, width, 0.45f, 0.08f, 0.04f, Palette.WOOD_LIGHT)
        mat = Mat.SOFT
        roundBox(0f, 0.4f, 0.02f, width - 0.12f, 0.2f, len - 0.2f, 0.08f, Palette.OFFWHITE)
        roundBox(0f, 0.47f, 0.25f, width - 0.06f, 0.14f, 1.25f, 0.07f, blanket)
        roundBox(0f, 0.49f, -0.37f, width - 0.06f, 0.12f, 0.12f, 0.05f, Palette.OFFWHITE)
        val pillows = if (style == 0) 1 else 2
        for (k in 0 until pillows) {
            val cx = if (pillows == 1) 0f else (k - 0.5f) * (width / 2f)
            roundBox(cx, 0.56f, -0.7f, width / pillows - 0.18f, 0.12f, 0.32f, 0.06f, Palette.WHITE)
        }
        for (k in 0 until 4) {
            val z = -0.15f + k * 0.28f
            roundBox(0f, 0.545f, z, width - 0.1f, 0.012f, 0.05f, 0.005f, blanket.lighter(0.35f))
        }
    }

    private fun MeshBuilder.nightstand(wood: Rgb) {
        mat = Mat(roughness = 0.45f)
        roundBox(0f, 0.27f, 0f, 0.44f, 0.5f, 0.4f, 0.04f, wood)
        block(-0.18f, 0.28f, 0.2f, 0.18f, 0.46f, 0.215f, wood.lighter(0.15f))
        withMat(Mat.METAL) { sphere(0f, 0.37f, 0.225f, 0.02f, Palette.YELLOW, rings = 3, segments = 6) }
        withMat(Mat.GLOSSY) { cylinder(0.08f, 0.52f, -0.04f, 0.06f, 0.12f, Palette.MINT, sides = 12) }
        withMat(Mat.glow(GLOW, 1.0f)) { frustum(0.08f, 0.62f, -0.04f, 0.12f, 0.07f, 0.13f, Palette.OFFWHITE, sides = 14) }
    }

    private fun MeshBuilder.bookshelf(wood: Rgb) {
        val w = 1.4f
        val h = 1.78f
        val d = 0.4f
        mat = Mat(roughness = 0.5f)
        block(-w / 2f, 0f, -d / 2f, -w / 2f + 0.05f, h, d / 2f, wood)
        block(w / 2f - 0.05f, 0f, -d / 2f, w / 2f, h, d / 2f, wood)
        block(-w / 2f, 0f, -d / 2f, w / 2f, h, -d / 2f + 0.03f, wood.darker(0.1f))
        val shelves = 4
        val rng = Rng(5)
        val bookColors = listOf(Palette.CORAL, Palette.SKY, Palette.MINT, Palette.YELLOW, Palette.LILAC, Palette.PEACH, Palette.NAVY)
        for (s in 0..shelves) {
            val y = s * (h - 0.04f) / shelves
            block(-w / 2f, y, -d / 2f, w / 2f, y + 0.04f, d / 2f, wood)
            if (s < shelves) {
                var x = -w / 2f + 0.07f
                while (x < w / 2f - 0.2f) {
                    val bw = rng.range(0.04f, 0.08f)
                    val bh = rng.range(0.22f, 0.32f)
                    if (rng.chance(0.12f)) {
                        x += 0.12f
                        continue
                    }
                    withMat(Mat(roughness = 0.6f)) { block(x, y + 0.04f, -0.14f, x + bw, y + 0.04f + bh, 0.13f, rng.pick(bookColors)) }
                    x += bw + 0.008f
                }
            }
        }
        withMat(Mat.GLOSSY) { sphere(0.5f, (h - 0.04f) / shelves * 2 + 0.14f, 0f, 0.09f, Palette.PINK, rings = 6, segments = 10) }
    }

    private fun MeshBuilder.wardrobe(wood: Rgb) {
        mat = Mat(roughness = 0.45f)
        roundBox(0f, 0.98f, 0f, 1.4f, 1.92f, 0.46f, 0.04f, wood)
        block(-0.68f, 0.1f, 0.23f, -0.01f, 1.88f, 0.245f, wood.lighter(0.12f))
        block(0.01f, 0.1f, 0.23f, 0.68f, 1.88f, 0.245f, wood.lighter(0.12f))
        withMat(Mat.METAL) {
            block(-0.08f, 0.85f, 0.245f, -0.05f, 1.15f, 0.27f, Palette.YELLOW)
            block(0.05f, 0.85f, 0.245f, 0.08f, 1.15f, 0.27f, Palette.YELLOW)
        }
        roundBox(0f, 1.97f, 0f, 1.44f, 0.06f, 0.5f, 0.02f, wood.darker(0.08f))
        legs(1.3f, 0.4f, 0.06f, 0.04f, wood.darker(0.2f))
    }

    private fun MeshBuilder.tv(wood: Rgb, on: Boolean) {
        mat = Mat(roughness = 0.45f)
        roundBox(0f, 0.25f, 0f, 1.4f, 0.4f, 0.44f, 0.04f, wood)
        block(-0.66f, 0.1f, 0.22f, -0.02f, 0.4f, 0.235f, wood.lighter(0.12f))
        block(0.02f, 0.1f, 0.22f, 0.66f, 0.4f, 0.235f, wood.lighter(0.12f))
        legs(1.3f, 0.36f, 0.06f, 0.03f, Palette.WOOD_DARK)
        withMat(Mat.GLOSSY) {
            block(-0.12f, 0.45f, -0.08f, 0.12f, 0.47f, 0.04f, Palette.INK)
            block(-0.03f, 0.47f, -0.04f, 0.03f, 0.6f, 0.0f, Palette.INK)
            roundBox(0f, 0.9f, -0.03f, 1.1f, 0.64f, 0.05f, 0.025f, Palette.INK)
        }
        if (on) {
            withMat(Mat.glow(rgb(0x8FE3FF), 2.2f)) { block(-0.5f, 0.62f, -0.002f, 0.5f, 1.18f, 0.003f, rgb(0x9FE6FF)) }
            withMat(Mat.glow(rgb(0xFFE57A), 2.2f)) { sphere(0.22f, 1.0f, 0.004f, 0.08f, rgb(0xFFF1A8), rings = 4, segments = 8) }
            withMat(Mat.glow(rgb(0x7CF0A0), 2f)) { block(-0.5f, 0.62f, 0.004f, 0.5f, 0.76f, 0.006f, rgb(0x9CF5B8)) }
        } else {
            withMat(Mat(roughness = 0.15f)) { block(-0.5f, 0.62f, -0.002f, 0.5f, 1.18f, 0.003f, rgb(0x2B2F44)) }
        }
        withMat(Mat.GLOSSY) {
            roundBox(-0.45f, 0.5f, 0.05f, 0.14f, 0.08f, 0.1f, 0.03f, Palette.WHITE)
            sphere(0.5f, 0.52f, 0.04f, 0.07f, Palette.LEAF, rings = 5, segments = 8)
        }
    }

    private fun MeshBuilder.fridge(c: Rgb) {
        mat = Mat.GLOSSY
        roundBox(0f, 0.92f, -0.08f, 0.8f, 1.8f, 0.7f, 0.12f, c)
        block(-0.38f, 1.12f, 0.27f, 0.38f, 1.14f, 0.275f, c.darker(0.15f))
        withMat(Mat.METAL) {
            roundBox(0.3f, 1.45f, 0.29f, 0.04f, 0.4f, 0.05f, 0.015f, Palette.METAL)
            roundBox(0.3f, 0.75f, 0.29f, 0.04f, 0.4f, 0.05f, 0.015f, Palette.METAL)
        }
        withMat(Mat.MATTE) {
            roundBox(-0.15f, 1.5f, 0.28f, 0.12f, 0.12f, 0.02f, 0.02f, Palette.YELLOW)
            roundBox(-0.05f, 1.42f, 0.28f, 0.1f, 0.08f, 0.02f, 0.02f, Palette.CORAL)
            roundBox(-0.2f, 0.8f, 0.28f, 0.16f, 0.2f, 0.01f, 0.004f, Palette.WHITE)
        }
        legs(0.7f, 0.55f, 0.04f, 0.03f, Palette.INK)
    }

    private fun MeshBuilder.stove(c: Rgb) {
        mat = Mat.GLOSSY
        roundBox(0f, 0.45f, -0.08f, 0.8f, 0.88f, 0.66f, 0.05f, c)
        withMat(Mat(roughness = 0.2f)) { block(-0.38f, 0.89f, -0.4f, 0.38f, 0.91f, 0.24f, Palette.INK) }
        withMat(Mat.METAL) {
            for (sx in listOf(-0.17f, 0.17f)) for (sz in listOf(-0.22f, 0.06f)) torus(sx, 0.92f, sz, 0.09f, 0.015f, Palette.METAL, 14, 4)
        }
        withMat(Mat(roughness = 0.1f)) { roundBox(0f, 0.42f, 0.25f, 0.6f, 0.38f, 0.02f, 0.03f, rgb(0x343A52)) }
        withMat(Mat.METAL) { block(-0.28f, 0.68f, 0.26f, 0.28f, 0.7f, 0.3f, Palette.METAL) }
        for (k in 0 until 4) withMat(Mat.GLOSSY) { sphere(-0.27f + k * 0.18f, 0.8f, 0.26f, 0.03f, Palette.INK, rings = 4, segments = 8) }
        withMat(Mat.METAL) {
            cylinder(0.17f, 0.93f, -0.22f, 0.11f, 0.1f, Palette.CORAL, sides = 14)
            group { translate(0.29f, 1.0f, -0.22f); rotateZ(-90f); cylinder(0f, 0f, 0f, 0.015f, 0.16f, Palette.INK, sides = 6) }
        }
    }

    private fun MeshBuilder.counter(c: Rgb, sink: Boolean) {
        mat = Mat.GLOSSY
        roundBox(0f, 0.44f, -0.12f, 0.96f, 0.84f, 0.7f, 0.04f, c)
        block(-0.44f, 0.1f, 0.23f, -0.02f, 0.78f, 0.245f, c.lighter(0.2f))
        block(0.02f, 0.1f, 0.23f, 0.44f, 0.78f, 0.245f, c.lighter(0.2f))
        withMat(Mat.METAL) {
            block(-0.09f, 0.6f, 0.245f, -0.06f, 0.72f, 0.265f, Palette.YELLOW)
            block(0.06f, 0.6f, 0.245f, 0.09f, 0.72f, 0.265f, Palette.YELLOW)
        }
        withMat(Mat(roughness = 0.35f)) { roundBox(0f, 0.9f, -0.12f, 1.0f, 0.06f, 0.74f, 0.02f, rgb(0xF6F2EA)) }
        if (sink) {
            withMat(Mat.METAL) {
                block(-0.28f, 0.91f, -0.34f, 0.28f, 0.935f, 0.12f, Palette.METAL)
                block(-0.24f, 0.88f, -0.3f, 0.24f, 0.932f, 0.08f, Palette.METAL.darker(0.25f))
                cylinder(0f, 0.93f, -0.4f, 0.025f, 0.28f, Palette.METAL, sides = 8)
                group { translate(0f, 1.2f, -0.4f); rotateX(90f); cylinder(0f, 0f, 0f, 0.02f, 0.18f, Palette.METAL, sides = 8) }
            }
        } else {
            withMat(Mat(roughness = 0.6f)) { roundBox(-0.15f, 0.945f, -0.05f, 0.42f, 0.03f, 0.28f, 0.01f, Palette.WOOD_LIGHT) }
            withMat(Mat.GLOSSY) {
                sphere(-0.2f, 0.99f, -0.05f, 0.05f, Palette.RED, rings = 5, segments = 8)
                ellipsoid(-0.07f, 0.985f, -0.05f, 0.07f, 0.035f, 0.035f, Palette.YELLOW, rings = 5, segments = 8)
                cylinder(0.28f, 0.93f, -0.3f, 0.07f, 0.2f, Palette.SKY, sides = 12)
            }
        }
    }

    private fun MeshBuilder.floorLamp(shade: Rgb, on: Boolean) {
        withMat(Mat.METAL) {
            cylinder(0f, 0f, 0f, 0.17f, 0.04f, Palette.INK, sides = 16)
            cylinder(0f, 0.04f, 0f, 0.02f, 1.3f, Palette.INK, sides = 8)
        }
        withMat(if (on) Mat.glow(GLOW, 3.2f) else Mat.SOFT) {
            lathe(0f, 0f, listOf(0.24f to 1.22f, 0.14f to 1.58f), shade.lighter(0.4f), 18)
            lathe(0f, 0f, listOf(0.14f to 1.58f, 0.24f to 1.22f), shade.lighter(0.3f), 18)
        }
    }

    private fun MeshBuilder.globeLamp(base: Rgb, on: Boolean) {
        withMat(Mat.GLOSSY) {
            lathe(0f, 0f, listOf(0.12f to 0f, 0.12f to 0.04f, 0.05f to 0.08f, 0.04f to 0.32f, 0f to 0.32f), base, 14)
        }
        withMat(if (on) Mat.glow(GLOW, 3.5f) else Mat(roughness = 0.2f, alpha = 0.85f)) {
            sphere(0f, 0.5f, 0f, 0.2f, rgb(0xFFF6DD), rings = 10, segments = 14)
        }
    }

    private fun MeshBuilder.pot(c: Rgb, r: Float, h: Float) {
        withMat(Mat.GLOSSY) {
            lathe(0f, 0f, listOf(0f to 0f, r * 0.75f to 0f, r to h * 0.85f, r * 1.08f to h, r * 0.95f to h, r * 0.9f to h * 0.92f, 0f to h * 0.92f), c, 16)
        }
        withMat(Mat.MATTE) { cylinder(0f, h * 0.88f, 0f, r * 0.9f, 0.02f, Palette.SOIL, sides = 14) }
    }

    private fun MeshBuilder.monstera(potC: Rgb) {
        pot(potC, 0.17f, 0.32f)
        val rng = Rng(12)
        mat = Mat(roughness = 0.6f)
        for (k in 0 until 7) {
            val a = k / 7f * 360f + rng.range(-15f, 15f)
            val tilt = rng.range(25f, 55f)
            val len = rng.range(0.32f, 0.48f)
            group {
                translate(0f, 0.3f, 0f)
                rotateY(a)
                rotateX(tilt)
                cylinder(0f, 0f, 0f, 0.012f, len * 1.3f, Palette.LEAF_DEEP, sides = 5, caps = false)
                group {
                    translate(0f, len * 1.3f, 0.05f)
                    rotateX(70f)
                    ellipsoid(0f, 0.12f, 0f, 0.2f, 0.22f, 0.02f, if (k % 2 == 0) Palette.LEAF else Palette.LEAF_DEEP, rings = 6, segments = 10)
                }
            }
        }
    }

    private fun MeshBuilder.cactus(potC: Rgb) {
        pot(potC, 0.15f, 0.26f)
        mat = Mat(roughness = 0.7f)
        capsule(0f, 0.2f, 0f, 0.1f, 0.52f, rgb(0x5BC27A), sides = 10, capRings = 4)
        group { translate(0.08f, 0.42f, 0f); rotateZ(-90f); capsule(0f, 0f, 0f, 0.055f, 0.14f, rgb(0x5BC27A), sides = 8, capRings = 3) }
        capsule(0.17f, 0.4f, 0f, 0.055f, 0.2f, rgb(0x5BC27A), sides = 8, capRings = 3)
        group { translate(-0.08f, 0.5f, 0f); rotateZ(90f); capsule(0f, 0f, 0f, 0.05f, 0.12f, rgb(0x5BC27A), sides = 8, capRings = 3) }
        capsule(-0.15f, 0.48f, 0f, 0.05f, 0.16f, rgb(0x5BC27A), sides = 8, capRings = 3)
        withMat(Mat.GLOSSY) { sphere(0f, 0.74f, 0f, 0.05f, Palette.PINK, rings = 4, segments = 8) }
    }

    private fun MeshBuilder.fiddleTree(potC: Rgb) {
        pot(potC, 0.26f, 0.45f)
        mat = Mat.MATTE
        frustum(0f, 0.4f, 0f, 0.04f, 0.025f, 0.9f, Palette.TRUNK, sides = 6)
        blob(0f, 1.45f, 0f, 0.42f, 0.42f, 0.42f, Palette.LEAF, 1, 0.16f, 21)
        blob(0.15f, 1.75f, 0.05f, 0.28f, 0.25f, 0.28f, Palette.LEAF_LIGHT, 1, 0.16f, 22)
        blob(-0.2f, 1.2f, -0.05f, 0.24f, 0.2f, 0.24f, Palette.LEAF_DEEP, 0, 0.16f, 23)
    }

    private fun MeshBuilder.tulips(c: Rgb) {
        withMat(Mat(roughness = 0.1f, alpha = 0.55f)) {
            lathe(0f, 0f, listOf(0f to 0f, 0.09f to 0f, 0.11f to 0.12f, 0.06f to 0.28f, 0.07f to 0.32f, 0f to 0.32f), Palette.GLASS, 14)
        }
        mat = Mat.MATTE
        for (k in 0 until 5) {
            val a = k / 5f * 2f * PI.toFloat()
            val x = cos(a) * 0.06f
            val z = sin(a) * 0.06f
            group {
                translate(x * 0.5f, 0.12f, z * 0.5f)
                rotateZ(-x * 120f)
                rotateX(z * 120f)
                cylinder(0f, 0f, 0f, 0.01f, 0.42f + k * 0.03f, Palette.LEAF_DEEP, sides = 5, caps = false)
                withMat(Mat.GLOSSY) { lathe(0f, 0f, listOf(0f to 0.4f + k * 0.03f, 0.05f to 0.44f + k * 0.03f, 0.05f to 0.5f + k * 0.03f, 0.03f to 0.53f + k * 0.03f), c, 8) }
            }
        }
    }

    private fun MeshBuilder.rugRound(c: Rgb) {
        mat = Mat.SOFT
        cylinder(0f, 0f, 0f, 0.98f, 0.012f, c, sides = 32)
        cylinder(0f, 0.004f, 0f, 0.75f, 0.012f, Palette.OFFWHITE, sides = 32)
        cylinder(0f, 0.008f, 0f, 0.5f, 0.012f, c.lighter(0.3f), sides = 32)
        cylinder(0f, 0.012f, 0f, 0.22f, 0.012f, Palette.YELLOW.lighter(0.3f), sides = 24)
    }

    private fun MeshBuilder.rugRect(w: Float, d: Float, c: Rgb) {
        mat = Mat.SOFT
        roundBox(0f, 0.008f, 0f, w - 0.04f, 0.016f, d - 0.04f, 0.006f, c)
        block(-w / 2f + 0.14f, 0.016f, -d / 2f + 0.14f, w / 2f - 0.14f, 0.02f, d / 2f - 0.14f, Palette.OFFWHITE)
        val stripes = ((d - 0.4f) / 0.3f).toInt()
        for (k in 0 until stripes) {
            val z = -d / 2f + 0.3f + k * 0.3f
            block(-w / 2f + 0.24f, 0.02f, z, w / 2f - 0.24f, 0.024f, z + 0.12f, if (k % 2 == 0) c.lighter(0.25f) else Palette.YELLOW.lighter(0.35f))
        }
    }

    private fun MeshBuilder.piano(body: Rgb) {
        mat = Mat.GLOSSY
        block(-0.7f, 0.05f, -0.45f, 0.7f, 1.25f, -0.05f, body)
        block(-0.7f, 0.68f, -0.05f, 0.7f, 0.76f, 0.2f, body)
        block(-0.66f, 0.76f, -0.05f, 0.66f, 0.8f, 0.17f, Palette.WHITE)
        for (k in 0 until 26) {
            if (k % 7 == 2 || k % 7 == 6) continue
            val x = -0.62f + k * 0.05f
            block(x, 0.8f, -0.05f, x + 0.028f, 0.83f, 0.07f, Palette.INK)
        }
        block(-0.7f, 0f, 0.1f, -0.64f, 0.68f, 0.16f, body)
        block(0.64f, 0f, 0.1f, 0.7f, 0.68f, 0.16f, body)
        withMat(Mat.SOFT) {
            roundBox(0f, 0.46f, 0.38f, 0.7f, 0.08f, 0.26f, 0.03f, Palette.CORAL)
        }
        group { translate(0f, 0f, 0.38f); legs(0.64f, 0.2f, 0.42f, 0.025f, body, 0.04f) }
        withMat(Mat.GLOSSY) { sphere(0.45f, 1.32f, -0.25f, 0.07f, Palette.YELLOW, rings = 5, segments = 8) }
    }

    private fun MeshBuilder.aquarium(stand: Rgb) {
        mat = Mat(roughness = 0.45f)
        roundBox(0f, 0.32f, 0f, 1.4f, 0.64f, 0.44f, 0.03f, stand)
        withMat(Mat.WATER) { block(-0.66f, 0.66f, -0.19f, 0.66f, 1.18f, 0.19f, Palette.WATER) }
        withMat(Mat.GLASS) { block(-0.68f, 0.64f, -0.21f, 0.68f, 1.24f, 0.21f, Palette.GLASS) }
        mat = Mat.MATTE
        block(-0.66f, 0.64f, -0.19f, 0.66f, 0.7f, 0.19f, rgb(0xF2DDB0))
        for (k in 0 until 4) blob(-0.5f + k * 0.33f, 0.82f, -0.08f, 0.05f, 0.16f, 0.05f, Palette.LEAF, 0, 0.2f, 60 + k)
        withMat(Mat.GLOSSY) {
            val fish = listOf(Palette.ORANGE, Palette.YELLOW, Palette.CORAL)
            for (k in 0 until 3) {
                val x = -0.35f + k * 0.35f
                val y = 0.88f + (k % 2) * 0.12f
                ellipsoid(x, y, 0.05f, 0.07f, 0.04f, 0.025f, fish[k], rings = 5, segments = 8)
                cone(x - 0.1f, y - 0.04f, 0.05f, 0.04f, 0.08f, fish[k], sides = 4, smooth = false)
            }
        }
        block(-0.7f, 1.24f, -0.22f, 0.7f, 1.28f, 0.22f, stand)
    }

    private fun MeshBuilder.teddy(fur: Rgb) {
        mat = Mat.SOFT
        ellipsoid(0f, 0.2f, 0f, 0.17f, 0.18f, 0.15f, fur, rings = 8, segments = 12)
        sphere(0f, 0.44f, 0.02f, 0.13f, fur, rings = 8, segments = 12)
        sphere(-0.1f, 0.55f, 0f, 0.045f, fur, rings = 5, segments = 8)
        sphere(0.1f, 0.55f, 0f, 0.045f, fur, rings = 5, segments = 8)
        ellipsoid(0f, 0.41f, 0.12f, 0.06f, 0.045f, 0.04f, fur.lighter(0.4f), rings = 5, segments = 8)
        for (x in listOf(-0.12f, 0.12f)) {
            ellipsoid(x, 0.07f, 0.1f, 0.06f, 0.05f, 0.09f, fur, rings = 5, segments = 8)
            ellipsoid(x * 1.4f, 0.25f, 0.04f, 0.05f, 0.1f, 0.05f, fur, rings = 5, segments = 8)
        }
        withMat(Mat.GLOSSY) {
            sphere(-0.045f, 0.47f, 0.12f, 0.016f, Palette.INK, rings = 3, segments = 6)
            sphere(0.045f, 0.47f, 0.12f, 0.016f, Palette.INK, rings = 3, segments = 6)
            sphere(0f, 0.425f, 0.16f, 0.02f, Palette.INK, rings = 3, segments = 6)
            torus(0f, 0.33f, 0.02f, 0.1f, 0.015f, Palette.CORAL, 14, 4)
        }
    }

    private fun MeshBuilder.arcade(c: Rgb) {
        mat = Mat.GLOSSY
        block(-0.38f, 0f, -0.35f, 0.38f, 1.75f, 0.2f, c)
        block(-0.38f, 0.85f, 0.2f, 0.38f, 0.95f, 0.38f, c.darker(0.15f))
        withMat(Mat.glow(rgb(0xA0F0FF), 2.5f)) { block(-0.3f, 1.05f, 0.2f, 0.3f, 1.5f, 0.205f, rgb(0xB8F4FF)) }
        withMat(Mat.glow(rgb(0xFFE36B), 2.5f)) { block(-0.34f, 1.56f, 0.2f, 0.34f, 1.7f, 0.21f, rgb(0xFFF08A)) }
        cylinder(-0.15f, 0.95f, 0.3f, 0.015f, 0.1f, Palette.INK, sides = 6)
        sphere(-0.15f, 1.06f, 0.3f, 0.035f, Palette.RED, rings = 4, segments = 8)
        for (k in 0 until 3) cylinder(0.05f + k * 0.1f, 0.95f, 0.3f, 0.03f, 0.02f, listOf(Palette.YELLOW, Palette.SKY, Palette.MINT)[k], sides = 10)
    }

    fun benchInto(b: MeshBuilder, wood: Rgb, accent: Rgb) = with(b) {
        withMat(Mat(roughness = 0.6f)) {
            for (k in 0 until 3) roundBox(0f, 0.44f, -0.12f + k * 0.12f, 1.4f, 0.04f, 0.1f, 0.015f, wood)
            for (k in 0 until 2) {
                group {
                    translate(0f, 0.72f + k * 0.14f, -0.2f)
                    rotateX(-12f)
                    roundBox(0f, 0f, 0f, 1.4f, 0.09f, 0.035f, 0.012f, wood)
                }
            }
        }
        withMat(Mat.METAL) {
            for (x in listOf(-0.6f, 0.6f)) {
                block(x - 0.03f, 0f, 0.12f, x + 0.03f, 0.44f, 0.16f, accent.darker(0.3f))
                block(x - 0.03f, 0f, -0.19f, x + 0.03f, 0.92f, -0.15f, accent.darker(0.3f))
                block(x - 0.03f, 0.58f, -0.18f, x + 0.03f, 0.62f, 0.16f, accent.darker(0.3f))
            }
        }
    }

    private fun MeshBuilder.picnicTable(wood: Rgb) {
        mat = Mat(roughness = 0.6f)
        for (k in 0 until 4) roundBox(0f, 0.76f, -0.36f + k * 0.24f, 1.8f, 0.05f, 0.22f, 0.015f, wood)
        for (z in listOf(-0.75f, 0.75f)) {
            for (k in 0 until 2) roundBox(0f, 0.45f, z - 0.1f + k * 0.2f, 1.8f, 0.05f, 0.18f, 0.015f, wood)
        }
        for (x in listOf(-0.7f, 0.7f)) {
            for (s in listOf(-1f, 1f)) {
                group {
                    translate(x, 0.38f, 0f)
                    rotateX(s * 34f)
                    block(-0.04f, -0.42f, -0.04f, 0.04f, 0.42f, 0.04f, wood.darker(0.1f))
                }
            }
            block(x - 0.04f, 0.4f, -0.85f, x + 0.04f, 0.45f, 0.85f, wood.darker(0.1f))
        }
        withMat(Mat.SOFT) {
            block(-0.4f, 0.785f, -0.5f, 0.4f, 0.79f, 0.5f, Palette.CORAL.lighter(0.35f))
        }
        withMat(Mat.GLOSSY) {
            cylinder(0.15f, 0.79f, 0.1f, 0.05f, 0.18f, Palette.YELLOW, sides = 10)
            sphere(-0.15f, 0.84f, -0.1f, 0.06f, Palette.RED, rings = 5, segments = 8)
        }
    }

    private fun MeshBuilder.flowerBed(c: Rgb) {
        mat = Mat(roughness = 0.7f)
        block(-0.48f, 0f, -0.23f, 0.48f, 0.26f, 0.23f, Palette.WOOD)
        block(-0.44f, 0.2f, -0.19f, 0.44f, 0.28f, 0.19f, Palette.SOIL)
        for (k in 0 until 6) {
            group {
                translate(0f, 0.27f, 0f)
                flower(-0.36f + k * 0.145f, if (k % 2 == 0) -0.07f else 0.07f, 0.24f, if (k % 3 == 2) Palette.WHITE else c, k)
            }
        }
    }

    private fun MeshBuilder.lampPost(c: Rgb, on: Boolean) {
        withMat(Mat.METAL) {
            cylinder(0f, 0f, 0f, 0.13f, 0.14f, c, sides = 10)
            cylinder(0f, 0f, 0f, 0.05f, 2.25f, c, sides = 8)
            group { translate(0f, 2.1f, 0f); rotateZ(90f); torus(0f, 0f, 0f, 0.14f, 0.02f, c, 14, 4) }
        }
        withMat(if (on) Mat.glow(GLOW, 3f) else Mat(roughness = 0.3f)) { sphere(0f, 2.4f, 0f, 0.18f, rgb(0xFFF4D6), rings = 8, segments = 12) }
        withMat(Mat.METAL) { cone(0f, 2.5f, 0f, 0.22f, 0.16f, c, sides = 10) }
    }

    private fun MeshBuilder.parasol(c: Rgb) {
        mat = Mat(roughness = 0.45f)
        cylinder(0f, 0f, 0f, 0.2f, 0.03f, Palette.WHITE, sides = 14)
        cylinder(0f, 0f, 0f, 0.03f, 2.2f, Palette.WHITE, sides = 8)
        cylinder(0f, 0.7f, 0f, 0.42f, 0.04f, Palette.WHITE, sides = 18)
        withMat(Mat.SOFT) {
            val segs = 8
            for (k in 0 until segs) {
                val a0 = k / segs.toFloat() * 2f * PI.toFloat()
                val a1 = (k + 1) / segs.toFloat() * 2f * PI.toFloat()
                val r = 1.15f
                val col = if (k % 2 == 0) c else Palette.WHITE
                tri(V3(0f, 2.35f, 0f), V3(cos(a1) * r, 1.95f, sin(a1) * r), V3(cos(a0) * r, 1.95f, sin(a0) * r), col)
                tri(V3(0f, 2.33f, 0f), V3(cos(a0) * r, 1.93f, sin(a0) * r), V3(cos(a1) * r, 1.93f, sin(a1) * r), col.darker(0.1f))
            }
        }
        for (s in listOf(-1f, 1f)) {
            group {
                translate(s * 0.72f, 0f, 0f)
                rotateY(s * 90f)
                withMat(Mat(roughness = 0.5f)) {
                    legs(0.42f, 0.42f, 0.42f, 0.02f, Palette.WHITE, 0.04f, 6)
                    roundBox(0f, 0.44f, 0f, 0.44f, 0.04f, 0.42f, 0.015f, Palette.WHITE)
                    roundBox(0f, 0.68f, -0.2f, 0.44f, 0.4f, 0.03f, 0.015f, Palette.WHITE)
                }
                withMat(Mat.SOFT) { roundBox(0f, 0.48f, 0.01f, 0.38f, 0.05f, 0.36f, 0.02f, c.lighter(0.25f)) }
            }
        }
        withMat(Mat.GLOSSY) {
            cylinder(0.12f, 0.74f, 0.1f, 0.04f, 0.12f, Palette.PEACH, sides = 10)
            sphere(-0.15f, 0.79f, -0.05f, 0.05f, Palette.YELLOW, rings = 5, segments = 8)
        }
    }

    private fun MeshBuilder.trampoline(c: Rgb) {
        withMat(Mat.METAL) {
            for (k in 0 until 6) {
                val a = k / 6f * 2f * PI.toFloat()
                cylinder(cos(a) * 1.1f, 0f, sin(a) * 1.1f, 0.03f, 0.7f, Palette.METAL, sides = 6)
            }
            torus(0f, 0.7f, 0f, 1.12f, 0.035f, Palette.METAL, 32, 6)
        }
        withMat(Mat.SOFT) {
            torus(0f, 0.73f, 0f, 1.0f, 0.13f, c, 32, 8)
        }
        withMat(Mat(roughness = 0.9f)) { cylinder(0f, 0.68f, 0f, 0.92f, 0.03f, Palette.INK.lighter(0.15f), sides = 28) }
        withMat(Mat.GLOSSY) { annulus(0f, 0.712f, 0f, 0.25f, 0.32f, Palette.YELLOW, 20) }
    }

    private fun MeshBuilder.swingFrame(c: Rgb) {
        mat = Mat(roughness = 0.55f)
        val top = 2.25f
        for (sx in listOf(-0.95f, 0.95f)) {
            for (sz in listOf(-1f, 1f)) {
                group {
                    translate(sx, 0f, sz * 0.6f)
                    rotateX(-sz * 15f)
                    block(-0.05f, 0f, -0.05f, 0.05f, 2.33f, 0.05f, Palette.WOOD)
                }
            }
        }
        group {
            translate(-1.0f, top, 0f)
            rotateZ(-90f)
            cylinder(0f, 0f, 0f, 0.07f, 2.0f, c, sides = 10)
        }
        withMat(Mat.METAL) {
            for (x in listOf(-0.32f, 0.32f)) torus(x, top - 0.08f, 0f, 0.05f, 0.012f, Palette.METAL, 10, 4)
        }
        mat = Mat.MATTE
        blob(-0.95f, 0.15f, -0.85f, 0.25f, 0.18f, 0.25f, Palette.LEAF, 0, 0.12f, 71)
    }

    private fun MeshBuilder.sandbox(wood: Rgb) {
        mat = Mat(roughness = 0.7f)
        block(-0.98f, 0f, -0.98f, 0.98f, 0.3f, -0.86f, wood)
        block(-0.98f, 0f, 0.86f, 0.98f, 0.3f, 0.98f, wood)
        block(-0.98f, 0f, -0.86f, -0.86f, 0.3f, 0.86f, wood)
        block(0.86f, 0f, -0.86f, 0.98f, 0.3f, 0.86f, wood)
        for ((x, z) in listOf(-0.92f to -0.92f, 0.92f to -0.92f, -0.92f to 0.92f, 0.92f to 0.92f)) block(x - 0.12f, 0.3f, z - 0.12f, x + 0.12f, 0.34f, z + 0.12f, wood.darker(0.1f))
        mat = Mat(roughness = 1f)
        block(-0.86f, 0f, -0.86f, 0.86f, 0.22f, 0.86f, rgb(0xF7E3A8))
        blob(0.2f, 0.25f, -0.2f, 0.32f, 0.12f, 0.3f, rgb(0xF2D68E), 1, 0.1f, 81)
        lathe(-0.3f, 0.25f, listOf(0.14f to 0.2f, 0.12f to 0.36f, 0f to 0.36f), rgb(0xEDD08A), 8)
        lathe(-0.3f, 0.25f, listOf(0.06f to 0.36f, 0.05f to 0.46f, 0f to 0.5f), rgb(0xEDD08A), 6)
        withMat(Mat.GLOSSY) {
            frustum(0.45f, 0.2f, 0.4f, 0.09f, 0.12f, 0.16f, Palette.RED, sides = 12, capTop = false)
            group { translate(0.55f, 0.24f, 0.0f); rotateZ(-70f); cylinder(0f, 0f, 0f, 0.015f, 0.3f, Palette.YELLOW, sides = 6) }
        }
    }

    private fun MeshBuilder.birdBath(stone: Rgb) {
        mat = Mat(roughness = 0.8f)
        lathe(0f, 0f, listOf(0f to 0f, 0.18f to 0f, 0.16f to 0.08f, 0.07f to 0.14f, 0.06f to 0.6f, 0.08f to 0.64f, 0.3f to 0.72f, 0.32f to 0.78f, 0.27f to 0.78f, 0.1f to 0.72f, 0f to 0.72f), stone, 16)
        withMat(Mat.WATER) { disc(0f, 0.77f, 0f, 0.27f, Palette.WATER, 16) }
        withMat(Mat.SOFT) {
            ellipsoid(0.16f, 0.86f, 0f, 0.07f, 0.06f, 0.05f, Palette.SKY, rings = 5, segments = 8)
            sphere(0.22f, 0.92f, 0f, 0.04f, Palette.SKY, rings = 5, segments = 8)
            cone(0.26f, 0.92f, 0f, 0.012f, 0.03f, Palette.ORANGE, sides = 4)
        }
    }

    private fun MeshBuilder.gnome(hat: Rgb) {
        mat = Mat.SOFT
        lathe(0f, 0f, listOf(0f to 0f, 0.13f to 0f, 0.14f to 0.12f, 0.11f to 0.26f, 0f to 0.3f), Palette.SKY.darker(0.2f), 12)
        sphere(0f, 0.32f, 0.02f, 0.09f, Palette.SKIN[1], rings = 6, segments = 10)
        blob(0f, 0.26f, 0.08f, 0.09f, 0.08f, 0.05f, Palette.WHITE, 1, 0.08f, 3)
        sphere(0f, 0.32f, 0.1f, 0.03f, rgb(0xFF9DAF), rings = 4, segments = 6)
        cone(0f, 0.35f, 0f, 0.12f, 0.28f, hat, sides = 12)
        for (x in listOf(-0.07f, 0.07f)) roundBox(x, 0.03f, 0.04f, 0.07f, 0.05f, 0.11f, 0.02f, Palette.WOOD_DARK)
    }

    private fun MeshBuilder.hedge(withFlowers: Boolean) {
        mat = Mat.MATTE
        roundBox(0f, 0.38f, 0f, 1.42f, 0.74f, 0.44f, 0.18f, Palette.LEAF_DEEP.mix(Palette.LEAF, 0.4f), segments = 3)
        val rng = Rng(14)
        repeat(8) {
            blob(rng.range(-0.6f, 0.6f), rng.range(0.5f, 0.72f), rng.range(-0.15f, 0.15f), 0.14f, 0.1f, 0.14f, Palette.LEAF, 0, 0.12f, 140 + it)
        }
        if (withFlowers) withMat(Mat.GLOSSY) {
            repeat(10) { sphere(rng.range(-0.65f, 0.65f), rng.range(0.3f, 0.75f), 0.22f, 0.045f, rng.pick(listOf(Palette.PINK, Palette.WHITE, Palette.YELLOW)), rings = 4, segments = 6) }
        }
    }

    private fun MeshBuilder.bbq(c: Rgb) {
        withMat(Mat.METAL) {
            for (k in 0 until 3) {
                val a = k / 3f * 2f * PI.toFloat()
                group {
                    translate(cos(a) * 0.12f, 0f, sin(a) * 0.12f)
                    rotateZ(cos(a) * -10f)
                    rotateX(sin(a) * 10f)
                    cylinder(0f, 0f, 0f, 0.018f, 0.62f, Palette.INK, sides = 6)
                }
            }
        }
        withMat(Mat.GLOSSY) {
            lathe(0f, 0f, listOf(0f to 0.55f, 0.22f to 0.6f, 0.3f to 0.75f, 0.31f to 0.8f, 0f to 0.8f), c, 18)
            lathe(0f, 0f, listOf(0.31f to 0.84f, 0.28f to 0.95f, 0.15f to 1.05f, 0f to 1.07f), c, 18)
            sphere(0f, 1.08f, 0f, 0.035f, Palette.INK, rings = 4, segments = 8)
        }
        withMat(Mat.METAL) { torus(0f, 0.81f, 0f, 0.29f, 0.012f, Palette.METAL, 18, 4) }
        withMat(Mat.glow(rgb(0xFF7A3D), 1.5f)) { disc(0f, 0.8f, 0f, 0.26f, rgb(0xFF8A50), 16) }
        withMat(Mat(roughness = 0.6f)) { roundBox(0.38f, 0.7f, 0f, 0.3f, 0.03f, 0.24f, 0.01f, Palette.WOOD) }
    }

    private fun MeshBuilder.pool(c: Rgb) {
        withMat(Mat(roughness = 0.4f)) { block(-1.3f, 0f, -0.8f, 1.3f, 0.05f, 0.8f, Palette.WHITE) }
        withMat(Mat.GLOSSY) {
            roundBox(0f, 0.2f, -0.8f, 2.96f, 0.4f, 0.36f, 0.17f, c, segments = 3)
            roundBox(0f, 0.2f, 0.8f, 2.96f, 0.4f, 0.36f, 0.17f, c, segments = 3)
            roundBox(-1.3f, 0.2f, 0f, 0.36f, 0.4f, 1.6f, 0.17f, c, segments = 3)
            roundBox(1.3f, 0.2f, 0f, 0.36f, 0.4f, 1.6f, 0.17f, c, segments = 3)
            for (z in listOf(-0.8f, 0.8f)) roundBox(0f, 0.405f, z, 2.7f, 0.03f, 0.12f, 0.012f, Palette.WHITE)
            for (x in listOf(-1.3f, 1.3f)) roundBox(x, 0.405f, 0f, 0.12f, 0.03f, 1.4f, 0.012f, Palette.WHITE)
        }
        withMat(Mat.WATER) { block(-1.15f, 0.05f, -0.65f, 1.15f, 0.3f, 0.65f, Palette.WATER) }
        withMat(Mat.GLOSSY) {
            ellipsoid(0.4f, 0.32f, 0.1f, 0.13f, 0.09f, 0.1f, Palette.YELLOW, rings = 6, segments = 10)
            sphere(0.5f, 0.42f, 0.1f, 0.07f, Palette.YELLOW, rings = 5, segments = 8)
            cone(0.57f, 0.42f, 0.1f, 0.03f, 0.05f, Palette.ORANGE, sides = 6)
            torus(-0.55f, 0.31f, -0.15f, 0.22f, 0.07f, Palette.CORAL, 18, 8)
        }
    }
}
