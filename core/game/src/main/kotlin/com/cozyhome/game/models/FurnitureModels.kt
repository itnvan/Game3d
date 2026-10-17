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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object FurnitureModels {
    private val FABRIC = listOf(rgb(0xE3B1A8), rgb(0xA9C2A2), rgb(0x86A3C2), rgb(0xDDAA52))
    private val WOODS = listOf(rgb(0xC9A27B), rgb(0x6F4B35), rgb(0xF2EEE7))
    private val CABINET = listOf(rgb(0xF3F2EE), rgb(0xB8CBB3), rgb(0xE9C9C2))
    private val FLOWERS = listOf(rgb(0xF29BB2), rgb(0xF6D35B), rgb(0xF2875E), rgb(0xB9A3E6))
    private val GLOW = rgb(0xFFE0A6)
    private val BRASS = rgb(0xC9A15A)
    private val IRON = rgb(0x2C2C2E)
    private val LINEN_WHITE = rgb(0xF4F1EA)
    private val STEEL = rgb(0xC8CCD0)
    private val BRASS_MAT = Mat(roughness = 0.35f, metallic = 0.9f)
    private val IRON_MAT = Mat(roughness = 0.5f, metallic = 0.6f)
    private val STEEL_MAT = Mat(roughness = 0.25f, metallic = 0.95f)

    fun build(id: String, variant: Int, on: Boolean = false): Mesh = mesh {
        val v = variant.coerceAtLeast(0)
        when (id) {
            "sofa" -> sofa(2.0f, FABRIC[v % 4], WOODS[(v + 1) % 2])
            "armchair" -> sofa(1.0f, FABRIC[v % 4], WOODS[v % 2])
            "beanbag" -> beanbag(FABRIC[v % 4])
            "chair" -> chair(WOODS[v % 3])
            "stool" -> stool(WOODS[v % 3], FABRIC[(v + 1) % 4])
            "coffee_table" -> coffeeTable(WOODS[v % 3])
            "dining_table" -> diningTable(WOODS[v % 3])
            "side_table" -> sideTable(WOODS[v % 3])
            "desk" -> desk(WOODS[v % 3])
            "bed_single" -> bed(1.0f, FABRIC[v % 4], WOODS[v % 3], 0)
            "bed_double" -> bed(1.5f, FABRIC[v % 4], WOODS[v % 3], 1)
            "nightstand" -> nightstand(WOODS[v % 3])
            "bookshelf" -> bookshelf(WOODS[v % 3])
            "wardrobe" -> wardrobe(WOODS[v % 3])
            "tv" -> tv(WOODS[v % 2], on)
            "fridge" -> fridge(CABINET[v % 3])
            "stove" -> stove(if (v % 2 == 0) rgb(0xF4F3EF) else rgb(0xE9C9C2))
            "counter" -> counter(CABINET[v % 3], sink = false)
            "sink" -> counter(CABINET[v % 3], sink = true)
            "floor_lamp" -> floorLamp(FABRIC[(v + 3) % 4], on)
            "globe_lamp" -> globeLamp(FABRIC[(v + 1) % 4], on)
            "monstera" -> monstera(potColor(v))
            "cactus" -> cactus(potColor(v))
            "fiddle_tree" -> fiddleTree(potColor(v))
            "tulips" -> tulips(FLOWERS[v % 4])
            "rug_round" -> rugRound(FABRIC[v % 4])
            "rug_rect" -> rugRect(3.0f, 2.0f, FABRIC[v % 4])
            "rug_runner" -> rugRunner(1.0f, 2.5f, FABRIC[(v + 2) % 4])
            "piano" -> piano(if (v % 2 == 0) rgb(0x1E1E22) else rgb(0x5A3A27))
            "aquarium" -> aquarium(WOODS[v % 2])
            "teddy" -> teddy(listOf(rgb(0xB98559), rgb(0xF0C2CC), rgb(0xF2EEE6))[v % 3])
            "arcade" -> arcade(listOf(rgb(0x6C55C9), rgb(0xD9506F), rgb(0x2E8FCB))[v % 3])
            "bench" -> benchInto(this, WOODS[v % 2], IRON)
            "picnic_table" -> picnicTable(WOODS[v % 2])
            "flower_bed" -> flowerBed(FLOWERS[v % 4])
            "lamp_post" -> lampPost(if (v % 2 == 0) IRON else rgb(0x3E4A3D), on)
            "parasol" -> parasol(FABRIC[v % 4])
            "trampoline" -> trampoline(FABRIC[v % 4])
            "swing" -> swingFrame(FABRIC[v % 4])
            "sandbox" -> sandbox(WOODS[v % 2])
            "bird_bath" -> birdBath(if (v % 2 == 0) rgb(0xBDB7AC) else rgb(0xC9D9DD))
            "gnome" -> gnome(listOf(rgb(0xD2483F), rgb(0x3D78B8), rgb(0x4E9A6A))[v % 3])
            "hedge" -> hedge(v % 2 == 1)
            "fruit_tree" -> fruitTree(0f, 0f, 0.62f, 31 + v, listOf(rgb(0xD23B30), rgb(0xF0902E), rgb(0xF2CF3E))[v % 3])
            "bbq" -> bbq(if (v % 2 == 0) rgb(0xB8322B) else IRON)
            "pool" -> pool(if (v % 2 == 0) rgb(0x7FC3DD) else rgb(0xE9A9B6))
            else -> FestivalModels.item(id, on)?.let { include(it) } ?: roundBox(0f, 0.25f, 0f, 0.5f, 0.5f, 0.5f, 0.08f, Palette.PINK)
        }
        shadowFor(id)
    }

    private fun MeshBuilder.shadowFor(id: String) {
        val def = com.cozyhome.game.decor.Catalog[id] ?: return
        if (def.layer == com.cozyhome.game.decor.Layer.FLOOR) return
        val w = def.w * com.cozyhome.game.decor.Catalog.CELL
        val d = def.d * com.cozyhome.game.decor.Catalog.CELL
        shadowBlob(0f, 0f, w * 0.62f, d * 0.62f, 0.006f)
    }

    fun swingSeat(variant: Int): Mesh = mesh {
        val c = FABRIC[variant.coerceAtLeast(0) % 4]
        withMat(Mat.FABRIC) {
            for (x in listOf(-0.32f, 0.32f)) cylinder(x, -1.55f, 0f, 0.014f, 1.55f, rgb(0xD8CBB0), sides = 6, caps = false)
        }
        withMat(Mat.WOOD) { roundBox(0f, -1.55f, 0f, 0.8f, 0.06f, 0.34f, 0.025f, WOODS[0]) }
        withMat(Mat.FABRIC) { roundBox(0f, -1.505f, 0f, 0.7f, 0.04f, 0.28f, 0.018f, c, segments = 3) }
    }

    private fun potColor(v: Int) = listOf(rgb(0xC7714E), rgb(0xF1EEE8), rgb(0x5E6E64))[v % 3]

    private fun MeshBuilder.taperLegs(w: Float, d: Float, h: Float, r: Float, c: Rgb, inset: Float = 0.08f, splay: Float = 6f) {
        withMat(Mat.WOOD_FINE) {
            for (sx in listOf(-1f, 1f)) for (sz in listOf(-1f, 1f)) {
                group {
                    translate(sx * (w / 2f - inset), h, sz * (d / 2f - inset))
                    rotateZ(sx * splay)
                    rotateX(-sz * splay)
                    frustum(0f, -h, 0f, r * 0.55f, r, h, c, sides = 10)
                }
            }
        }
    }

    private fun MeshBuilder.cushion(cx: Float, cy: Float, cz: Float, w: Float, h: Float, d: Float, c: Rgb, round: Float = 0.45f) {
        roundBox(cx, cy, cz, w, h, d, minOf(h, minOf(w, d)) * round, c, segments = 3)
    }

    private fun MeshBuilder.sofa(width: Float, c: Rgb, wood: Rgb) {
        val shade = c.darker(0.07f)
        withMat(Mat.FABRIC) {
            cushion(0f, 0.3f, 0.02f, width - 0.02f, 0.22f, 0.86f, shade, 0.3f)
            group {
                translate(0f, 0.66f, -0.34f)
                rotateX(-6f)
                cushion(0f, 0f, 0f, width - 0.06f, 0.6f, 0.2f, shade, 0.35f)
            }
            for (s in listOf(-1f, 1f)) cushion(s * (width / 2f - 0.1f), 0.5f, 0.02f, 0.2f, 0.42f, 0.88f, c, 0.5f)
            val seats = if (width > 1.5f) 2 else 1
            val inner = width - 0.4f
            for (k in 0 until seats) {
                val cx = -inner / 2f + inner / seats * (k + 0.5f)
                cushion(cx, 0.5f, 0.08f, inner / seats - 0.02f, 0.17f, 0.68f, c.lighter(0.08f))
                group {
                    translate(cx, 0.78f, -0.2f)
                    rotateX(-10f)
                    cushion(0f, 0f, 0f, inner / seats - 0.05f, 0.42f, 0.18f, c.lighter(0.05f), 0.5f)
                }
            }
        }
        withMat(Mat.VELVET) {
            group {
                translate(-width / 2f + 0.38f, 0.7f, -0.04f)
                rotateZ(12f)
                rotateX(-14f)
                puff(0f, 0f, 0f, 0.15f, 0.15f, 0.055f, if (c == FABRIC[3]) rgb(0xF0E6D6) else FABRIC[3], subdivisions = 2, wobble = 0.05f, seed = 3)
            }
        }
        taperLegs(width - 0.1f, 0.78f, 0.19f, 0.032f, wood, 0.08f, 8f)
    }

    private fun MeshBuilder.beanbag(c: Rgb) {
        withMat(Mat.VELVET) {
            puff(0f, 0.24f, 0.02f, 0.48f, 0.26f, 0.46f, c, subdivisions = 3, wobble = 0.06f, seed = 5, floorY = 0.0f)
            puff(0f, 0.43f, -0.2f, 0.38f, 0.22f, 0.2f, c.darker(0.05f), subdivisions = 2, wobble = 0.08f, seed = 6)
            puff(0f, 0.4f, 0.08f, 0.3f, 0.07f, 0.25f, c.lighter(0.08f), subdivisions = 2, wobble = 0.05f, seed = 7)
        }
    }

    private fun MeshBuilder.chair(wood: Rgb) {
        withMat(Mat.WOOD_FINE) {
            for (sx in listOf(-1f, 1f)) for (sz in listOf(-1f, 1f)) {
                group {
                    translate(sx * 0.17f, 0.44f, sz * 0.17f)
                    rotateZ(sx * 5f)
                    rotateX(-sz * 5f)
                    frustum(0f, -0.44f, 0f, 0.016f, 0.024f, 0.44f, wood, sides = 8)
                }
            }
            roundBox(0f, 0.455f, 0f, 0.44f, 0.04f, 0.42f, 0.018f, wood)
            for (x in listOf(-0.17f, 0.17f)) {
                group {
                    translate(x, 0.46f, -0.18f)
                    rotateX(-8f)
                    cylinder(0f, 0f, 0f, 0.016f, 0.42f, wood, sides = 8)
                }
            }
            for (k in 0 until 4) {
                group {
                    translate(-0.09f + k * 0.06f, 0.48f, -0.185f)
                    rotateX(-8f)
                    cylinder(0f, 0f, 0f, 0.008f, 0.32f, wood, sides = 6)
                }
            }
            group {
                translate(0f, 0.86f, -0.24f)
                rotateX(-8f)
                roundBox(0f, 0f, 0f, 0.42f, 0.08f, 0.035f, 0.016f, wood)
            }
        }
        withMat(Mat.FABRIC) { cushion(0f, 0.49f, 0.01f, 0.38f, 0.04f, 0.36f, LINEN_WHITE.darker(0.04f)) }
    }

    private fun MeshBuilder.stool(wood: Rgb, cushion: Rgb) {
        withMat(Mat.WOOD_FINE) {
            for (k in 0 until 3) {
                val a = k / 3f * 2f * PI.toFloat()
                group {
                    translate(cos(a) * 0.1f, 0.46f, sin(a) * 0.1f)
                    rotateZ(cos(a) * -8f)
                    rotateX(sin(a) * 8f)
                    frustum(0f, -0.46f, 0f, 0.014f, 0.022f, 0.46f, wood, sides = 7)
                }
            }
            torus(0f, 0.18f, 0f, 0.115f, 0.01f, wood, 16, 5)
            cylinder(0f, 0.44f, 0f, 0.19f, 0.035f, wood, sides = 20)
        }
        withMat(Mat.FABRIC) { puff(0f, 0.49f, 0f, 0.17f, 0.04f, 0.17f, cushion, subdivisions = 2, wobble = 0.02f, seed = 4) }
    }

    private fun MeshBuilder.books(x0: Float, y: Float, z: Float, count: Int, rng: Rng, lying: Boolean = false) {
        val colors = listOf(rgb(0x2F4A6E), rgb(0x8E2F35), rgb(0x3D6B4F), rgb(0xD5A447), rgb(0xE8E1D3), rgb(0x6B6E75), rgb(0xB8664A))
        var x = x0
        var yy = y
        repeat(count) {
            val c = rng.pick(colors)
            if (lying) {
                val t = rng.range(0.025f, 0.045f)
                withMat(Mat.FABRIC) { roundBox(x0, yy + t / 2f, z, rng.range(0.2f, 0.26f), t, rng.range(0.15f, 0.19f), 0.004f, c) }
                withMat(Mat.MATTE) { block(x0 - 0.09f, yy + 0.004f, z - 0.075f, x0 + 0.09f, yy + t - 0.004f, z + 0.078f, rgb(0xF4EEDF)) }
                yy += t
            } else {
                val w = rng.range(0.03f, 0.06f)
                val h = rng.range(0.2f, 0.29f)
                withMat(Mat.FABRIC) { roundBox(x + w / 2f, y + h / 2f, z, w, h, 0.2f, 0.004f, c) }
                x += w + 0.004f
            }
        }
    }

    private fun MeshBuilder.mug(x: Float, y: Float, z: Float, c: Rgb) {
        withMat(Mat.CERAMIC) {
            lathe(x, z, listOf(0f to y, 0.036f to y, 0.042f to y + 0.01f, 0.042f to y + 0.09f, 0.036f to y + 0.09f, 0.034f to y + 0.02f, 0f to y + 0.02f), c, 14)
            group { translate(x + 0.045f, y + 0.05f, z); rotateY(90f); torus(0f, 0f, 0f, 0.022f, 0.006f, c, 10, 4) }
        }
        withMat(Mat.GLOSSY) { disc(x, y + 0.075f, z, 0.034f, rgb(0x4A2C1C), 12) }
    }

    private fun MeshBuilder.potPlant(x: Float, y: Float, z: Float, s: Float, pot: Rgb, seed: Int) {
        group {
            translate(x, y, z)
            scale(s)
            pot(pot, 0.09f, 0.12f)
            val rng = Rng(seed.toLong())
            withMat(Mat.LEAF_THIN) {
                repeat(7) {
                    group {
                        translate(0f, 0.11f, 0f)
                        rotateY(it * 51f + rng.range(-10f, 10f))
                        rotateX(-rng.range(30f, 65f))
                        leafBlade(0.13f, 0.06f, rgb(0x4F8A3C).mix(rgb(0x7DB257), rng.nextFloat()), droop = 0.25f)
                    }
                }
            }
        }
    }

    private fun MeshBuilder.coffeeTable(wood: Rgb) {
        withMat(Mat.WOOD_FINE) {
            roundBox(0f, 0.415f, 0f, 1.3f, 0.05f, 0.72f, 0.025f, wood)
            roundBox(0f, 0.15f, 0f, 1.12f, 0.025f, 0.56f, 0.012f, wood.darker(0.06f))
        }
        taperLegs(1.24f, 0.66f, 0.39f, 0.026f, wood.darker(0.05f), 0.06f, 4f)
        val rng = Rng(8)
        group { translate(0.32f, 0f, 0.06f); rotateY(12f); books(0f, 0.44f, 0f, 3, rng, lying = true) }
        mug(-0.38f, 0.44f, -0.08f, rgb(0xEAE5DC))
        potPlant(-0.05f, 0.44f, -0.12f, 0.9f, rgb(0xC7714E), 2)
        group { translate(0.3f, 0f, 0f); books(-0.2f, 0.165f, 0.05f, 2, rng, lying = true) }
    }

    private fun MeshBuilder.fruit(x: Float, y: Float, z: Float, c: Rgb, r: Float, seed: Int) {
        withMat(Mat.GLOSSY.copy(roughness = 0.3f)) { puff(x, y, z, r, r * 0.92f, r, c, subdivisions = 2, wobble = 0.04f, seed = seed) }
        withMat(Mat.MATTE) { cylinder(x, y + r * 0.85f, z, 0.004f, r * 0.4f, rgb(0x5A3D24), sides = 4, caps = false) }
    }

    private fun MeshBuilder.diningTable(wood: Rgb) {
        withMat(Mat.WOOD_FINE) { roundBox(0f, 0.735f, 0f, 1.4f, 0.055f, 0.9f, 0.022f, wood) }
        taperLegs(1.3f, 0.8f, 0.71f, 0.036f, wood, 0.06f, 3f)
        withMat(Mat.FABRIC) { roundBox(0f, 0.766f, 0f, 0.36f, 0.006f, 0.88f, 0.003f, LINEN_WHITE.darker(0.08f)) }
        withMat(Mat.CERAMIC) {
            lathe(0f, 0f, listOf(0.0f to 0.77f, 0.07f to 0.77f, 0.08f to 0.78f, 0.15f to 0.83f, 0.17f to 0.86f, 0.155f to 0.86f, 0.0f to 0.8f), rgb(0xE9E4DA), 20)
        }
        fruit(-0.05f, 0.85f, 0.02f, rgb(0xC73A2F), 0.045f, 1)
        fruit(0.06f, 0.845f, -0.04f, rgb(0xEE8E2D), 0.045f, 2)
        fruit(0.0f, 0.87f, 0.07f, rgb(0xE9C93E), 0.04f, 3)
        fruit(-0.07f, 0.86f, -0.06f, rgb(0x8DB348), 0.04f, 4)
    }

    private fun MeshBuilder.vaseFlowers(x: Float, y: Float, z: Float, c: Rgb, seed: Int) {
        withMat(Mat.CERAMIC) {
            lathe(x, z, listOf(0f to y, 0.04f to y, 0.06f to y + 0.06f, 0.035f to y + 0.15f, 0.03f to y + 0.17f, 0.036f to y + 0.18f, 0f to y + 0.175f), rgb(0x9FB3B8), 16)
        }
        val rng = Rng(seed.toLong())
        repeat(5) {
            val a = it * 1.3f
            val tip = V3(x + cos(a) * 0.06f, y + rng.range(0.32f, 0.4f), z + sin(a) * 0.06f)
            withMat(Mat.SOFT) { tube(smoothPath(listOf(V3(x, y + 0.15f, z), V3(x + cos(a) * 0.02f, y + 0.26f, z + sin(a) * 0.02f), tip), 2), { 0.004f }, rgb(0x4E7F35), sides = 4, caps = false) }
            withMat(Mat.SOFT) {
                group {
                    translate(tip.x, tip.y, tip.z)
                    for (k in 0 until 5) {
                        group { rotateY(k * 72f); rotateX(-25f); ellipsoid(0f, 0f, 0.022f, 0.014f, 0.004f, 0.022f, c.mix(Rgb.WHITE, 0.08f * (k % 2)), 3, 6) }
                    }
                    sphere(0f, 0.004f, 0f, 0.01f, rgb(0xF2C94C), 3, 6)
                }
            }
        }
    }

    private fun MeshBuilder.sideTable(wood: Rgb) {
        withMat(Mat.WOOD_FINE) {
            for (k in 0 until 3) {
                val a = k / 3f * 2f * PI.toFloat()
                group {
                    translate(cos(a) * 0.07f, 0.62f, sin(a) * 0.07f)
                    rotateZ(cos(a) * -10f)
                    rotateX(sin(a) * 10f)
                    frustum(0f, -0.62f, 0f, 0.012f, 0.02f, 0.62f, wood, sides = 7)
                }
            }
            lathe(0f, 0f, listOf(0.22f to 0.62f, 0.225f to 0.64f, 0.22f to 0.66f, 0f to 0.661f), wood, 24)
        }
        vaseFlowers(0.02f, 0.66f, 0.02f, FLOWERS[0], 11)
    }

    private fun MeshBuilder.desk(wood: Rgb) {
        withMat(Mat.WOOD_FINE) {
            roundBox(0f, 0.735f, -0.1f, 1.4f, 0.04f, 0.68f, 0.012f, wood)
            roundBox(0.48f, 0.37f, -0.1f, 0.38f, 0.7f, 0.64f, 0.01f, wood.darker(0.04f))
            for (k in 0 until 3) roundBox(0.48f, 0.15f + k * 0.21f, 0.225f, 0.35f, 0.19f, 0.02f, 0.006f, wood.lighter(0.05f))
        }
        withMat(BRASS_MAT) { for (k in 0 until 3) roundBox(0.48f, 0.2f + k * 0.21f, 0.24f, 0.12f, 0.012f, 0.012f, 0.005f, BRASS) }
        taperLegs(1.3f, 0.6f, 0.715f, 0.024f, wood, 0.05f, 0f)
        withMat(STEEL_MAT) {
            roundBox(-0.2f, 0.76f, -0.02f, 0.4f, 0.014f, 0.27f, 0.006f, STEEL)
            group {
                translate(-0.2f, 0.767f, -0.155f)
                rotateX(-14f)
                roundBox(0f, 0.13f, 0f, 0.4f, 0.26f, 0.012f, 0.006f, STEEL)
            }
        }
        withMat(Mat.glow(rgb(0xA9D8FF), 1.0f)) {
            group {
                translate(-0.2f, 0.767f, -0.155f)
                rotateX(-14f)
                block(-0.18f, 0.02f, 0.0065f, 0.18f, 0.245f, 0.008f, rgb(0xDDEBFF))
            }
        }
        withMat(IRON_MAT) {
            cylinder(0.42f, 0.755f, -0.3f, 0.06f, 0.015f, IRON, sides = 16)
            tube(smoothPath(listOf(V3(0.42f, 0.77f, -0.3f), V3(0.4f, 1.05f, -0.32f), V3(0.33f, 1.12f, -0.22f)), 3), { 0.009f }, IRON, sides = 6, caps = true)
            group {
                translate(0.33f, 1.1f, -0.2f)
                rotateX(40f)
                frustum(0f, -0.08f, 0f, 0.075f, 0.03f, 0.1f, IRON, sides = 14, capBottom = false)
            }
        }
        withMat(Mat.glow(GLOW, 1.6f)) { group { translate(0.33f, 1.1f, -0.2f); rotateX(40f); disc(0f, -0.075f, 0f, 0.07f, GLOW, 12, up = false) } }
        mug(0.1f, 0.755f, -0.32f, FABRIC[1])
    }

    private fun MeshBuilder.bed(width: Float, blanket: Rgb, wood: Rgb, style: Int) {
        val len = 2.0f
        withMat(Mat.WOOD_FINE) {
            roundBox(0f, 0.24f, 0f, width + 0.04f, 0.16f, len, 0.025f, wood)
            roundBox(0f, 0.36f, len / 2f - 0.03f, width + 0.04f, 0.3f, 0.05f, 0.02f, wood)
        }
        taperLegs(width, len - 0.05f, 0.18f, 0.035f, wood.darker(0.08f), 0.06f, 0f)
        withMat(Mat.FABRIC) {
            roundBox(0f, 0.75f, -len / 2f + 0.06f, width + 0.08f, 0.82f, 0.1f, 0.05f, blanket.darker(0.1f), segments = 3)
            val tufts = if (style == 0) 2 else 3
            for (i in 0 until tufts) for (j in 0 until 2) {
                withMat(Mat.VELVET) { sphere(-width / 2f + (i + 0.5f) * (width / tufts), 0.62f + j * 0.28f, -len / 2f + 0.115f, 0.014f, blanket.darker(0.25f), 3, 6) }
            }
            roundBox(0f, 0.41f, 0.01f, width - 0.06f, 0.18f, len - 0.12f, 0.06f, LINEN_WHITE, segments = 3)
        }
        withMat(Mat.KNIT) {
            group {
                translate(0f, 0.505f, 0.22f)
                warpSphere(blanket, 8, 16) { x, y, z ->
                    val ax = kotlin.math.abs(x)
                    V3(
                        x * (width * 0.5f) * (0.97f + 0.03f * ax),
                        y * 0.055f + (1f - smoothstep(0.75f, 1f, ax)) * 0.01f * cos(z * 9f),
                        z * (len * 0.37f),
                    )
                }
            }
            roundBox(0f, 0.52f, -0.4f, width - 0.02f, 0.06f, 0.16f, 0.03f, blanket.lighter(0.12f), segments = 3)
        }
        withMat(Mat.FABRIC) {
            val pillows = if (style == 0) 1 else 2
            for (k in 0 until pillows) {
                val cx = if (pillows == 1) 0f else (k - 0.5f) * (width / 2f)
                group {
                    translate(cx, 0.58f, -0.68f)
                    rotateX(-14f)
                    puff(0f, 0f, 0f, (width / pillows - 0.22f) / 2f, 0.075f, 0.17f, LINEN_WHITE, subdivisions = 2, wobble = 0.03f, seed = 20 + k)
                }
            }
        }
        withMat(Mat.KNIT) { roundBox(0f, 0.53f, len / 2f - 0.3f, width + 0.02f, 0.045f, 0.36f, 0.02f, rgb(0xEDE3D2), segments = 2) }
    }

    private fun MeshBuilder.nightstand(wood: Rgb) {
        withMat(Mat.WOOD_FINE) {
            roundBox(0f, 0.3f, 0f, 0.44f, 0.4f, 0.4f, 0.015f, wood)
            roundBox(0f, 0.37f, 0.2f, 0.4f, 0.14f, 0.02f, 0.006f, wood.lighter(0.05f))
        }
        taperLegs(0.4f, 0.36f, 0.1f, 0.02f, wood.darker(0.08f), 0.04f, 0f)
        withMat(BRASS_MAT) { sphere(0f, 0.37f, 0.215f, 0.016f, BRASS, 4, 8) }
        withMat(Mat.CERAMIC) { lathe(0.08f, -0.04f, listOf(0f to 0.5f, 0.06f to 0.5f, 0.075f to 0.56f, 0.03f to 0.64f, 0.015f to 0.66f, 0f to 0.66f), rgb(0xE6D9C6), 16) }
        withMat(Mat.glow(GLOW, 1.2f)) {
            lathe(0.08f, -0.04f, listOf(0.12f to 0.66f, 0.12f to 0.67f, 0.075f to 0.8f, 0.0f to 0.8f), LINEN_WHITE, 18)
        }
        withMat(Mat.FABRIC) { group { translate(-0.08f, 0f, 0.02f); books(0f, 0.5f, 0f, 2, Rng(31), lying = true) } }
    }

    private fun MeshBuilder.bookshelf(wood: Rgb) {
        val w = 1.4f
        val h = 1.78f
        val d = 0.36f
        withMat(Mat.WOOD_FINE) {
            block(-w / 2f, 0f, -d / 2f, -w / 2f + 0.035f, h, d / 2f, wood)
            block(w / 2f - 0.035f, 0f, -d / 2f, w / 2f, h, d / 2f, wood)
            block(-w / 2f, 0f, -d / 2f, w / 2f, h, -d / 2f + 0.015f, wood.darker(0.12f))
        }
        val shelves = 4
        val rng = Rng(5)
        for (s in 0..shelves) {
            val y = s * (h - 0.03f) / shelves
            withMat(Mat.WOOD_FINE) { block(-w / 2f, y, -d / 2f, w / 2f, y + 0.03f, d / 2f, wood) }
            if (s < shelves) {
                val mode = (s * 7 + 3) % 4
                books(-w / 2f + 0.06f, y + 0.03f, 0f, 9 + s, rng)
                when (mode) {
                    0 -> potPlant(0.45f, y + 0.03f, 0f, 1.1f, potColor(s), 40 + s)
                    1 -> books(0.42f, y + 0.03f, 0f, 3, rng, lying = true)
                    2 -> vaseFlowers(0.45f, y + 0.03f, 0f, FLOWERS[s % 4], 50 + s)
                    else -> withMat(Mat.CERAMIC) { puff(0.45f, y + 0.12f, 0f, 0.08f, 0.09f, 0.08f, rgb(0xD7C9B8), subdivisions = 2, wobble = 0.02f, seed = s) }
                }
            }
        }
    }

    private fun MeshBuilder.wardrobe(wood: Rgb) {
        withMat(Mat.WOOD_FINE) {
            roundBox(0f, 1.0f, 0f, 1.4f, 1.86f, 0.5f, 0.012f, wood)
            for (s in listOf(-1f, 1f)) {
                roundBox(s * 0.35f, 1.0f, 0.255f, 0.66f, 1.78f, 0.02f, 0.006f, wood.lighter(0.04f))
                roundBox(s * 0.35f, 1.0f, 0.268f, 0.52f, 1.6f, 0.01f, 0.004f, wood.darker(0.03f))
            }
            roundBox(0f, 1.96f, 0f, 1.46f, 0.05f, 0.54f, 0.012f, wood.darker(0.06f))
            roundBox(0f, 0.04f, 0f, 1.36f, 0.08f, 0.46f, 0.01f, wood.darker(0.15f))
        }
        withMat(BRASS_MAT) {
            for (s in listOf(-1f, 1f)) {
                cylinder(s * 0.045f, 0.9f, 0.27f, 0.008f, 0.22f, BRASS, sides = 8)
                for (y in listOf(0.92f, 1.1f)) block(s * 0.045f - 0.006f, y - 0.006f, 0.255f, s * 0.045f + 0.006f, y + 0.006f, 0.275f, BRASS)
            }
        }
    }

    private fun MeshBuilder.tv(wood: Rgb, on: Boolean) {
        withMat(Mat.WOOD_FINE) {
            roundBox(0f, 0.3f, 0f, 1.45f, 0.36f, 0.42f, 0.015f, wood)
            for (k in 0 until 14) {
                val x = -0.66f + k * 0.1f
                block(x - 0.02f, 0.16f, 0.21f, x + 0.02f, 0.44f, 0.225f, wood.darker(0.08f))
            }
        }
        taperLegs(1.35f, 0.34f, 0.12f, 0.022f, wood.darker(0.08f), 0.06f, 5f)
        withMat(Mat.GLOSSY.copy(roughness = 0.25f)) {
            block(-0.14f, 0.48f, -0.1f, 0.14f, 0.495f, 0.04f, IRON)
            block(-0.025f, 0.495f, -0.05f, 0.025f, 0.6f, -0.02f, IRON)
            roundBox(0f, 0.92f, -0.03f, 1.14f, 0.66f, 0.03f, 0.008f, rgb(0x16171A))
        }
        if (on) {
            val screen = Paint { x, y, _ -> rgb(0x7FC3F5).mix(rgb(0xFFD58A), smoothstep(0.65f, 1.15f, y) * 0.0f + smoothstep(-0.5f, 0.5f, x) * 0.35f).mix(rgb(0x5DBB6A), smoothstep(0.8f, 0.62f, y)) }
            withMat(Mat.glow(rgb(0xBFE3FF), 1.8f)) { block(-0.55f, 0.61f, -0.014f, 0.55f, 1.23f, -0.012f, screen) }
        } else {
            withMat(Mat(roughness = 0.08f)) { block(-0.55f, 0.61f, -0.014f, 0.55f, 1.23f, -0.012f, rgb(0x0E1014)) }
        }
        potPlant(0.55f, 0.48f, 0.02f, 1.0f, potColor(1), 7)
        withMat(Mat.FABRIC) { roundBox(-0.55f, 0.58f, 0.0f, 0.12f, 0.2f, 0.12f, 0.03f, rgb(0x3A3D42)) }
    }

    private fun MeshBuilder.fridge(c: Rgb) {
        withMat(Mat.GLOSSY.copy(roughness = 0.22f)) {
            roundBox(0f, 0.93f, -0.06f, 0.74f, 1.78f, 0.66f, 0.1f, c, segments = 3)
            block(-0.36f, 1.18f, 0.26f, 0.36f, 1.195f, 0.275f, c.darker(0.12f))
        }
        withMat(STEEL_MAT) {
            roundBox(0.28f, 1.48f, 0.3f, 0.025f, 0.34f, 0.035f, 0.012f, STEEL)
            roundBox(0.28f, 0.86f, 0.3f, 0.025f, 0.42f, 0.035f, 0.012f, STEEL)
        }
        withMat(Mat.GLOSSY) {
            roundBox(-0.15f, 1.5f, 0.275f, 0.1f, 0.1f, 0.012f, 0.012f, rgb(0xF2C94C))
            roundBox(-0.04f, 1.43f, 0.275f, 0.09f, 0.07f, 0.012f, 0.012f, rgb(0xE07A5F))
        }
        withMat(Mat.FABRIC) { roundBox(-0.18f, 0.9f, 0.272f, 0.14f, 0.18f, 0.004f, 0.002f, rgb(0xF8F6F0)) }
        taperLegs(0.64f, 0.52f, 0.04f, 0.02f, IRON, 0.04f, 0f)
    }

    private fun MeshBuilder.stove(c: Rgb) {
        withMat(Mat.GLOSSY.copy(roughness = 0.25f)) { roundBox(0f, 0.45f, -0.08f, 0.76f, 0.88f, 0.64f, 0.025f, c) }
        withMat(Mat(roughness = 0.08f)) { roundBox(0f, 0.895f, -0.1f, 0.74f, 0.012f, 0.6f, 0.004f, rgb(0x121316)) }
        withMat(STEEL_MAT) {
            for (sx in listOf(-0.17f, 0.17f)) for (sz in listOf(-0.27f, 0.05f)) annulus(sx, 0.903f, sz, 0.07f, 0.095f, rgb(0x3A3C40), 18)
            lathe(0.17f, -0.27f, listOf(0.1f to 0.905f, 0.11f to 0.91f, 0.115f to 1.02f, 0.105f to 1.02f, 0.0f to 0.92f), STEEL, 18)
            group { translate(0.29f, 1.0f, -0.27f); rotateZ(-90f); cylinder(0f, 0f, 0f, 0.012f, 0.14f, IRON, sides = 6) }
            block(-0.3f, 0.71f, 0.235f, 0.3f, 0.725f, 0.26f, STEEL)
        }
        withMat(Mat(roughness = 0.05f, alpha = 0.7f)) { roundBox(0f, 0.42f, 0.235f, 0.56f, 0.34f, 0.01f, 0.02f, rgb(0x1E2024)) }
        for (k in 0 until 4) withMat(STEEL_MAT) { group { translate(-0.27f + k * 0.18f, 0.82f, 0.24f); rotateX(90f); cylinder(0f, 0f, 0f, 0.022f, 0.03f, STEEL, sides = 12) } }
    }

    private fun MeshBuilder.counter(c: Rgb, sink: Boolean) {
        withMat(Mat.PAINTED_WOOD) {
            roundBox(0f, 0.45f, -0.12f, 0.94f, 0.82f, 0.66f, 0.01f, c)
            for (s in listOf(-1f, 1f)) roundBox(s * 0.235f, 0.46f, 0.215f, 0.45f, 0.72f, 0.02f, 0.006f, c.lighter(0.04f))
            roundBox(0f, 0.04f, -0.1f, 0.9f, 0.08f, 0.6f, 0.004f, c.darker(0.2f))
        }
        withMat(BRASS_MAT) {
            for (s in listOf(-1f, 1f)) cylinder(s * 0.04f, 0.66f, 0.232f, 0.007f, 0.14f, BRASS, sides = 8)
        }
        withMat(Mat.STONE.copy(roughness = 0.3f, texScale = 0.9f)) { roundBox(0f, 0.885f, -0.12f, 1.0f, 0.04f, 0.72f, 0.008f, rgb(0xEDEAE4)) }
        if (sink) {
            withMat(STEEL_MAT) {
                block(-0.27f, 0.9f, -0.36f, 0.27f, 0.908f, 0.1f, STEEL)
                block(-0.24f, 0.84f, -0.33f, 0.24f, 0.906f, 0.07f, STEEL.darker(0.3f))
                cylinder(0f, 0.905f, -0.42f, 0.022f, 0.08f, STEEL, sides = 10)
                group {
                    translate(0f, 0.98f, -0.42f)
                    rotateY(90f)
                    arc(0f, 0f, 0f, 0.12f, 0.014f, 0f, 180f, STEEL, segments = 12, sides = 6)
                }
            }
            withMat(Mat.FABRIC) { roundBox(0.38f, 0.92f, -0.1f, 0.14f, 0.03f, 0.1f, 0.01f, rgb(0xF2D35B)) }
        } else {
            withMat(Mat.WOOD) { roundBox(-0.15f, 0.92f, -0.08f, 0.4f, 0.025f, 0.26f, 0.008f, WOODS[0]) }
            fruit(-0.22f, 0.975f, -0.06f, rgb(0xC73A2F), 0.04f, 9)
            withMat(Mat.GLOSSY) { group { translate(-0.08f, 0.955f, -0.08f); rotateY(30f); ellipsoid(0f, 0f, 0f, 0.08f, 0.02f, 0.025f, rgb(0xF0D25A), 4, 10) } }
            withMat(STEEL_MAT) {
                lathe(0.3f, -0.3f, listOf(0f to 0.905f, 0.08f to 0.905f, 0.085f to 0.93f, 0.07f to 1.08f, 0.04f to 1.1f, 0f to 1.11f), STEEL, 16)
            }
            withMat(IRON_MAT) { group { translate(0.38f, 1.02f, -0.3f); rotateZ(90f); arc(0f, 0f, 0f, 0.05f, 0.01f, -80f, 80f, IRON, segments = 8, sides = 5) } }
        }
    }

    private fun MeshBuilder.floorLamp(shade: Rgb, on: Boolean) {
        withMat(Mat.WOOD_FINE) {
            for (k in 0 until 3) {
                val a = k / 3f * 2f * PI.toFloat()
                group {
                    translate(0f, 1.25f, 0f)
                    rotateY(a * 180f / PI.toFloat())
                    rotateX(-12f)
                    frustum(0f, -1.28f, 0f, 0.012f, 0.02f, 1.28f, WOODS[0], sides = 7)
                }
            }
        }
        withMat(BRASS_MAT) { cylinder(0f, 1.2f, 0f, 0.03f, 0.08f, BRASS, sides = 10) }
        val linen = shade.mix(LINEN_WHITE, 0.6f)
        withMat(if (on) Mat.glow(GLOW, 2.6f).copy(doubleSided = true, tex = com.cozyhome.game.mesh.Tex.FABRIC, texScale = 0.35f) else Mat.FABRIC.copy(doubleSided = true)) {
            cylinder(0f, 1.24f, 0f, 0.23f, 0.32f, linen, sides = 24, caps = false)
        }
    }

    private fun MeshBuilder.globeLamp(base: Rgb, on: Boolean) {
        withMat(Mat.CERAMIC) {
            lathe(0f, 0f, listOf(0.1f to 0f, 0.11f to 0.02f, 0.12f to 0.08f, 0.09f to 0.18f, 0.04f to 0.26f, 0.03f to 0.3f, 0f to 0.3f), base.mix(LINEN_WHITE, 0.3f), 18)
        }
        withMat(if (on) Mat.glow(GLOW, 3.2f) else Mat(roughness = 0.1f, alpha = 0.7f)) {
            sphere(0f, 0.5f, 0f, 0.2f, rgb(0xFFF6E3), rings = 12, segments = 18)
        }
    }

    private fun MeshBuilder.pot(c: Rgb, r: Float, h: Float) {
        withMat(Mat.CERAMIC.copy(roughness = 0.45f)) {
            lathe(0f, 0f, listOf(0f to 0f, r * 0.72f to 0f, r * 0.8f to h * 0.06f, r to h * 0.86f, r * 1.07f to h * 0.9f, r * 1.07f to h, r * 0.94f to h, r * 0.92f to h * 0.93f, 0f to h * 0.93f), c, 20)
        }
        withMat(Mat.SOIL) { disc(0f, h * 0.9f, 0f, r * 0.92f, rgb(0x4A3626), 16) }
    }

    private fun MeshBuilder.monstera(potC: Rgb) {
        pot(potC, 0.17f, 0.32f)
        val rng = Rng(12)
        for (k in 0 until 9) {
            val a = k / 9f * 360f + rng.range(-15f, 15f)
            val tilt = rng.range(20f, 55f)
            val len = rng.range(0.32f, 0.5f)
            group {
                translate(0f, 0.28f, 0f)
                rotateY(a)
                rotateX(tilt)
                withMat(Mat.SOFT) { cylinder(0f, 0f, 0f, 0.009f, len, rgb(0x4C7A33), sides = 5, caps = false) }
                group {
                    translate(0f, len, 0f)
                    rotateX(-tilt - rng.range(10f, 40f))
                    withMat(Mat.LEAF_THIN) {
                        leafBlade(0.34f, 0.32f, rgb(0x2F6B2E).mix(rgb(0x4E8F3E), rng.nextFloat()), curl = 0.12f, droop = 0.18f, splits = 3, segments = 12, across = 4) { v ->
                            val s = kotlin.math.sin(PI.toFloat() * v.coerceIn(0f, 1f)).coerceAtLeast(0f)
                            kotlin.math.sqrt(s) * (1.05f - 0.45f * v) + 0.25f * smoothstep(0.15f, 0f, v)
                        }
                    }
                }
            }
        }
    }

    private fun MeshBuilder.cactus(potC: Rgb) {
        pot(potC, 0.15f, 0.26f)
        val ribs = Paint { x, _, z ->
            val a = kotlin.math.atan2(z, x)
            rgb(0x4E8E58).mix(rgb(0x6FAF6A), (0.5f + 0.5f * cos(a * 10f)) * 0.7f)
        }
        withMat(Mat(roughness = 0.6f)) {
            capsule(0f, 0.2f, 0f, 0.1f, 0.54f, ribs, sides = 20, capRings = 5)
            group { translate(0.08f, 0.42f, 0f); rotateZ(-90f); capsule(0f, 0f, 0f, 0.05f, 0.14f, ribs, sides = 14, capRings = 3) }
            capsule(0.17f, 0.4f, 0f, 0.052f, 0.22f, ribs, sides = 14, capRings = 3)
            group { translate(-0.08f, 0.5f, 0f); rotateZ(90f); capsule(0f, 0f, 0f, 0.046f, 0.12f, ribs, sides = 14, capRings = 3) }
            capsule(-0.15f, 0.48f, 0f, 0.046f, 0.17f, ribs, sides = 14, capRings = 3)
        }
        withMat(Mat.SOFT) {
            for (k in 0 until 5) group { translate(0f, 0.74f, 0f); rotateY(k * 72f); rotateX(-35f); ellipsoid(0f, 0.02f, 0.02f, 0.014f, 0.006f, 0.03f, rgb(0xF27BA5), 3, 6) }
        }
    }

    private fun MeshBuilder.fiddleTree(potC: Rgb) {
        pot(potC, 0.24f, 0.42f)
        withMat(Mat.BARK) { tube(smoothPath(listOf(V3(0f, 0.38f, 0f), V3(0.03f, 0.9f, 0.01f), V3(-0.02f, 1.5f, 0f)), 3), { t -> 0.035f - 0.015f * t }, rgb(0x6B5040), sides = 7, caps = false) }
        val rng = Rng(21)
        withMat(Mat.LEAF_THIN) {
            repeat(22) {
                val y = rng.range(0.85f, 1.75f)
                group {
                    translate(rng.range(-0.04f, 0.04f), y, rng.range(-0.04f, 0.04f))
                    rotateY(it * 137.5f)
                    rotateX(-rng.range(10f, 60f))
                    leafBlade(rng.range(0.2f, 0.28f), rng.range(0.15f, 0.2f), rgb(0x2E5E2B).mix(rgb(0x4F8C3C), rng.nextFloat()), curl = 0.2f, droop = 0.1f, segments = 8) { v ->
                        kotlin.math.sqrt(kotlin.math.sin(PI.toFloat() * v.coerceIn(0f, 1f)).coerceAtLeast(0f)) * (0.7f + 0.5f * v) * (1f - 0.6f * smoothstep(0.85f, 1f, v))
                    }
                }
            }
        }
    }

    private fun MeshBuilder.tulips(c: Rgb) {
        withMat(Mat(roughness = 0.05f, alpha = 0.45f)) {
            lathe(0f, 0f, listOf(0f to 0f, 0.08f to 0f, 0.1f to 0.1f, 0.06f to 0.26f, 0.07f to 0.3f, 0f to 0.3f), Palette.GLASS, 18)
        }
        withMat(Mat.WATER) { disc(0f, 0.17f, 0f, 0.085f, rgb(0xBFE4EC), 14) }
        for (k in 0 until 7) {
            val a = k / 7f * 2f * PI.toFloat()
            val top = V3(cos(a) * 0.1f, 0.48f + (k % 3) * 0.04f, sin(a) * 0.1f)
            withMat(Mat.SOFT) { tube(smoothPath(listOf(V3(cos(a) * 0.015f, 0.03f, sin(a) * 0.015f), V3(cos(a) * 0.04f, 0.3f, sin(a) * 0.04f), top), 3), { 0.006f }, rgb(0x5A8F3B), sides = 5, caps = false) }
            withMat(Mat.SOFT) {
                for (p in 0 until 3) {
                    group {
                        translate(top.x, top.y, top.z)
                        rotateY(p * 120f + k * 20f)
                        rotateX(-8f)
                        ellipsoid(0f, 0.035f, 0.016f, 0.026f, 0.045f, 0.014f, c.mix(Rgb.WHITE, 0.1f * p), 4, 8)
                    }
                }
            }
        }
        withMat(Mat.LEAF_THIN) {
            for (k in 0 until 3) group { translate(0f, 0.25f, 0f); rotateY(k * 120f + 30f); rotateX(-55f); leafBlade(0.2f, 0.05f, rgb(0x5A8F3B), droop = 0.25f) }
        }
    }

    private fun MeshBuilder.rugRound(c: Rgb) {
        val rings = listOf(0.97f to c.darker(0.1f), 0.86f to LINEN_WHITE, 0.78f to c, 0.56f to c.mix(rgb(0xD9C7A8), 0.5f), 0.46f to LINEN_WHITE, 0.36f to c, 0.14f to c.mix(LINEN_WHITE, 0.5f))
        withMat(Mat.CARPET) {
            lathe(0f, 0f, listOf(0.98f to 0.0f, 0.985f to 0.012f, 0.97f to 0.022f), rings[0].second, 48)
            for (i in rings.indices) {
                val outer = rings[i].first
                val inner = rings.getOrNull(i + 1)?.first ?: 0f
                if (inner <= 0f) disc(0f, 0.022f, 0f, outer, rings[i].second, 40) else annulus(0f, 0.022f, 0f, inner, outer, rings[i].second, 48)
            }
        }
    }

    private fun MeshBuilder.rugRect(w: Float, d: Float, c: Rgb) {
        val paint = Paint { x, _, z ->
            val bx = w / 2f - kotlin.math.abs(x)
            val bz = d / 2f - kotlin.math.abs(z)
            val edge = minOf(bx, bz)
            when {
                edge < 0.12f -> c.darker(0.12f)
                edge < 0.2f -> LINEN_WHITE
                ((x * 2.2f).toInt() + (z * 2.2f).toInt()) % 2 == 0 -> c.mix(LINEN_WHITE, 0.55f)
                else -> c.mix(LINEN_WHITE, 0.25f)
            }
        }
        withMat(Mat.CARPET) {
            heightField(-w / 2f + 0.02f, -d / 2f + 0.02f, w / 2f - 0.02f, d / 2f - 0.02f, 30, 20, paint) { _, _ -> 0.02f }
            block(-w / 2f + 0.02f, 0f, -d / 2f + 0.02f, w / 2f - 0.02f, 0.017f, d / 2f - 0.02f, c.darker(0.12f))
        }
    }

    private fun MeshBuilder.rugRunner(w: Float, d: Float, c: Rgb) {
        val paint = Paint { _, _, z -> if ((((z + d) * 5f).toInt()) % 2 == 0) c else LINEN_WHITE }
        withMat(Mat.CARPET) {
            heightField(-w / 2f + 0.02f, -d / 2f + 0.02f, w / 2f - 0.02f, d / 2f - 0.02f, 4, 50, paint) { _, _ -> 0.02f }
            block(-w / 2f + 0.02f, 0f, -d / 2f + 0.02f, w / 2f - 0.02f, 0.017f, d / 2f - 0.02f, c.darker(0.1f))
        }
        withMat(Mat.FABRIC) {
            for (s in listOf(-1f, 1f)) for (k in 0 until 12) {
                val x = -w / 2f + 0.06f + k * (w - 0.12f) / 11f
                block(x - 0.008f, 0f, s * (d / 2f) - 0.03f, x + 0.008f, 0.01f, s * (d / 2f) + 0.03f, LINEN_WHITE)
            }
        }
    }

    private fun MeshBuilder.piano(body: Rgb) {
        val lac = Mat(roughness = 0.12f)
        withMat(lac) {
            roundBox(0f, 0.68f, -0.26f, 1.42f, 1.24f, 0.36f, 0.015f, body)
            roundBox(0f, 0.72f, -0.03f, 1.42f, 0.08f, 0.26f, 0.012f, body)
            roundBox(0f, 1.32f, -0.24f, 1.46f, 0.04f, 0.42f, 0.012f, body)
            for (x in listOf(-0.68f, 0.68f)) roundBox(x, 0.36f, 0.08f, 0.06f, 0.72f, 0.06f, 0.01f, body)
        }
        withMat(Mat.CERAMIC) {
            for (k in 0 until 36) {
                val x = -0.63f + k * 0.035f
                block(x + 0.0015f, 0.76f, -0.05f, x + 0.0335f, 0.785f, 0.1f, rgb(0xF7F4EC))
            }
        }
        withMat(lac) {
            for (k in 0 until 36) {
                val n = k % 7
                if (n == 2 || n == 6) continue
                val x = -0.63f + k * 0.035f + 0.024f
                block(x, 0.785f, -0.05f, x + 0.02f, 0.81f, 0.045f, rgb(0x111114))
            }
        }
        withMat(BRASS_MAT) { for (x in listOf(-0.08f, 0f, 0.08f)) roundBox(x, 0.05f, 0.05f, 0.04f, 0.015f, 0.09f, 0.006f, BRASS) }
        group {
            translate(0f, 0f, 0.42f)
            withMat(lac) { roundBox(0f, 0.43f, 0f, 0.7f, 0.05f, 0.3f, 0.012f, body) }
            withMat(Mat.LEATHER) { roundBox(0f, 0.47f, 0f, 0.66f, 0.04f, 0.27f, 0.015f, rgb(0x2F2A2A)) }
            taperLegs(0.64f, 0.24f, 0.41f, 0.022f, body, 0.03f, 0f)
        }
        withMat(Mat.CERAMIC) { lathe(0.48f, -0.24f, listOf(0f to 1.34f, 0.05f to 1.34f, 0.06f to 1.4f, 0.03f to 1.5f, 0f to 1.5f), rgb(0xE8DCC6), 14) }
    }

    private fun MeshBuilder.aquarium(stand: Rgb) {
        withMat(Mat.WOOD_FINE) { roundBox(0f, 0.32f, 0f, 1.4f, 0.64f, 0.44f, 0.015f, stand) }
        withMat(Mat.SAND) { block(-0.66f, 0.64f, -0.19f, 0.66f, 0.7f, 0.19f, rgb(0xE6D3A7)) }
        val rng = Rng(60)
        withMat(Mat.LEAF_THIN) {
            repeat(10) {
                group {
                    translate(rng.range(-0.6f, 0.6f), 0.69f, rng.range(-0.15f, 0.0f))
                    rotateY(rng.range(0f, 360f))
                    rotateX(-rng.range(70f, 85f))
                    leafBlade(rng.range(0.25f, 0.42f), 0.04f, rgb(0x3E8A3A).mix(rgb(0x7CB342), rng.nextFloat()), curl = 0.1f, droop = -0.05f)
                }
            }
        }
        repeat(4) { rock(rng.range(-0.5f, 0.5f), rng.range(-0.1f, 0.1f), 0.18f, 600 + it) }
        withMat(Mat.GLOSSY) {
            val fish = listOf(rgb(0xF08A2C), rgb(0xF2D04B), rgb(0xE85D75), rgb(0x4FB3E8))
            for (k in 0 until 4) {
                val x = -0.4f + k * 0.27f
                val y = 0.88f + (k % 2) * 0.12f
                ellipsoid(x, y, 0.04f, 0.055f, 0.032f, 0.018f, fish[k], rings = 6, segments = 10)
                group { translate(x - 0.07f, y, 0.04f); rotateZ(90f); cone(0f, -0.01f, 0f, 0.025f, 0.04f, fish[k], sides = 4, smooth = false) }
            }
        }
        withMat(Mat.WATER.copy(alpha = 0.35f)) { block(-0.66f, 0.7f, -0.19f, 0.66f, 1.18f, 0.19f, rgb(0xA8DDEB)) }
        withMat(Mat.GLASS) { block(-0.68f, 0.64f, -0.21f, 0.68f, 1.24f, 0.21f, Palette.GLASS) }
        withMat(IRON_MAT) { block(-0.7f, 1.24f, -0.22f, 0.7f, 1.27f, 0.22f, IRON) }
    }

    private fun MeshBuilder.teddy(fur: Rgb) {
        withMat(Mat.CARPET.copy(texScale = 0.12f)) {
            puff(0f, 0.2f, 0f, 0.17f, 0.18f, 0.15f, fur, subdivisions = 2, wobble = 0.03f, seed = 1)
            puff(0f, 0.44f, 0.02f, 0.13f, 0.125f, 0.12f, fur, subdivisions = 2, wobble = 0.02f, seed = 2)
            for (x in listOf(-0.1f, 0.1f)) {
                sphere(x, 0.55f, 0f, 0.045f, fur, rings = 6, segments = 10)
                ellipsoid(x, 0.07f, 0.1f, 0.06f, 0.05f, 0.09f, fur, rings = 6, segments = 10)
                ellipsoid(x * 1.45f, 0.25f, 0.04f, 0.05f, 0.1f, 0.05f, fur, rings = 6, segments = 10)
            }
            ellipsoid(0f, 0.41f, 0.11f, 0.06f, 0.045f, 0.04f, fur.lighter(0.35f), rings = 6, segments = 10)
        }
        withMat(Mat.EYE) {
            sphere(-0.045f, 0.47f, 0.11f, 0.016f, rgb(0x1C1A1A), rings = 4, segments = 8)
            sphere(0.045f, 0.47f, 0.11f, 0.016f, rgb(0x1C1A1A), rings = 4, segments = 8)
            sphere(0f, 0.43f, 0.15f, 0.018f, rgb(0x2A2020), rings = 4, segments = 8)
        }
        withMat(Mat.VELVET) {
            group { translate(0f, 0.34f, 0.07f); rotateX(15f); torus(0f, 0f, 0f, 0.085f, 0.014f, rgb(0xC84B5B), 16, 5) }
            ellipsoid(0.04f, 0.34f, 0.15f, 0.04f, 0.025f, 0.012f, rgb(0xC84B5B), 4, 8)
            ellipsoid(-0.04f, 0.34f, 0.15f, 0.04f, 0.025f, 0.012f, rgb(0xC84B5B), 4, 8)
        }
    }

    private fun MeshBuilder.arcade(c: Rgb) {
        withMat(Mat.GLOSSY.copy(roughness = 0.3f)) {
            roundBox(0f, 0.87f, -0.08f, 0.74f, 1.74f, 0.56f, 0.02f, IRON)
            for (s in listOf(-1f, 1f)) roundBox(s * 0.375f, 0.87f, -0.08f, 0.02f, 1.76f, 0.58f, 0.008f, c)
            group { translate(0f, 0.92f, 0.22f); rotateX(-20f); roundBox(0f, 0f, 0f, 0.7f, 0.04f, 0.24f, 0.01f, c.darker(0.2f)) }
        }
        withMat(Mat.glow(rgb(0xA6E8FF), 2.0f)) {
            group { translate(0f, 1.27f, 0.2f); rotateX(-12f); block(-0.29f, -0.2f, 0f, 0.29f, 0.2f, 0.004f, rgb(0xB9EEFF)) }
        }
        withMat(Mat.glow(rgb(0xFFDD6B), 2.2f)) { block(-0.33f, 1.6f, 0.2f, 0.33f, 1.72f, 0.205f, rgb(0xFFE88A)) }
        withMat(Mat.GLOSSY) {
            group {
                translate(0f, 0.95f, 0.24f)
                rotateX(-20f)
                cylinder(-0.16f, 0.02f, 0f, 0.012f, 0.08f, IRON, sides = 6)
                sphere(-0.16f, 0.11f, 0f, 0.03f, rgb(0xD23B30), rings = 5, segments = 10)
                for (k in 0 until 3) cylinder(0.04f + k * 0.09f, 0.02f, 0f, 0.026f, 0.02f, listOf(rgb(0xF2C94C), rgb(0x4FB3E8), rgb(0x6FCF97))[k], sides = 12)
            }
        }
    }

    fun benchInto(b: MeshBuilder, wood: Rgb, iron: Rgb) = with(b) {
        withMat(Mat.DECK) {
            for (k in 0 until 4) roundBox(0f, 0.44f, -0.15f + k * 0.1f, 1.4f, 0.035f, 0.085f, 0.012f, wood)
            for (k in 0 until 3) {
                group {
                    translate(0f, 0.6f + k * 0.12f, -0.21f - k * 0.02f)
                    rotateX(-14f)
                    roundBox(0f, 0f, 0f, 1.4f, 0.08f, 0.03f, 0.01f, wood)
                }
            }
        }
        withMat(IRON_MAT) {
            for (x in listOf(-0.6f, 0.6f)) {
                tube(smoothPath(listOf(V3(x, 0f, 0.18f), V3(x, 0.3f, 0.14f), V3(x, 0.42f, 0.12f), V3(x, 0.6f, 0.16f), V3(x, 0.64f, 0.08f)), 3), { 0.018f }, iron, sides = 6, caps = true)
                tube(smoothPath(listOf(V3(x, 0f, -0.2f), V3(x, 0.42f, -0.18f), V3(x, 0.95f, -0.28f)), 3), { 0.018f }, iron, sides = 6, caps = true)
                block(x - 0.015f, 0.4f, -0.2f, x + 0.015f, 0.43f, 0.14f, iron)
            }
        }
    }

    private fun MeshBuilder.picnicTable(wood: Rgb) {
        withMat(Mat.DECK) {
            for (k in 0 until 4) roundBox(0f, 0.76f, -0.36f + k * 0.24f, 1.8f, 0.045f, 0.21f, 0.012f, wood)
            for (z in listOf(-0.75f, 0.75f)) for (k in 0 until 2) roundBox(0f, 0.45f, z - 0.1f + k * 0.2f, 1.8f, 0.045f, 0.18f, 0.012f, wood)
            for (x in listOf(-0.7f, 0.7f)) {
                for (s in listOf(-1f, 1f)) {
                    group {
                        translate(x, 0.38f, 0f)
                        rotateX(s * 34f)
                        block(-0.04f, -0.42f, -0.035f, 0.04f, 0.42f, 0.035f, wood.darker(0.1f))
                    }
                }
                block(x - 0.04f, 0.4f, -0.85f, x + 0.04f, 0.45f, 0.85f, wood.darker(0.1f))
            }
        }
        withMat(Mat.FABRIC) {
            val check = Paint { x, _, z -> if (((x * 6f + 10f).toInt() + (z * 6f + 10f).toInt()) % 2 == 0) rgb(0xD6453D) else LINEN_WHITE }
            block(-0.4f, 0.783f, -0.5f, 0.4f, 0.788f, 0.5f, check)
        }
        withMat(Mat.GLASS) { cylinder(0.15f, 0.79f, 0.1f, 0.04f, 0.16f, Palette.GLASS, sides = 12) }
        fruit(-0.15f, 0.83f, -0.1f, rgb(0xC73A2F), 0.045f, 33)
        withMat(Mat.WOOD) { cylinder(0.0f, 0.79f, -0.25f, 0.13f, 0.06f, WOODS[0].darker(0.1f), sides = 16) }
    }

    private fun MeshBuilder.flowerBed(c: Rgb) {
        withMat(Mat.DECK) { block(-0.48f, 0f, -0.23f, 0.48f, 0.26f, 0.23f, rgb(0x9C6E4A)) }
        withMat(Mat.SOIL) { block(-0.44f, 0.2f, -0.19f, 0.44f, 0.27f, 0.19f, rgb(0x4A3626)) }
        for (k in 0 until 7) {
            group {
                translate(0f, 0.26f, 0f)
                flower(-0.38f + k * 0.125f, if (k % 2 == 0) -0.07f else 0.07f, 0.24f, if (k % 3 == 2) rgb(0xF6F2EA) else c, k)
            }
        }
        group { translate(0f, 0.26f, 0f); grassTuft(-0.3f, 0.1f, 0.8f, 3); grassTuft(0.3f, -0.1f, 0.8f, 4) }
    }

    private fun MeshBuilder.lampPost(c: Rgb, on: Boolean) {
        withMat(IRON_MAT) {
            lathe(0f, 0f, listOf(0.13f to 0f, 0.13f to 0.08f, 0.09f to 0.12f, 0.06f to 0.3f, 0.045f to 0.34f, 0.045f to 2.2f, 0.07f to 2.25f, 0.07f to 2.28f, 0f to 2.29f), c, 12)
            lathe(0f, 0f, listOf(0.14f to 2.29f, 0.16f to 2.33f, 0.0f to 2.335f), c, 12)
            lathe(0f, 0f, listOf(0.22f to 2.6f, 0.22f to 2.63f, 0.1f to 2.72f, 0.03f to 2.83f, 0f to 2.85f), c, 12)
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2f + PI.toFloat() / 4f
                cylinder(cos(a) * 0.17f, 2.33f, sin(a) * 0.17f, 0.012f, 0.28f, c, sides = 5)
            }
        }
        withMat(if (on) Mat.glow(GLOW, 3.2f) else Mat(roughness = 0.1f, alpha = 0.6f)) { lathe(0f, 0f, listOf(0.15f to 2.335f, 0.185f to 2.6f, 0f to 2.605f), rgb(0xFFF1CF), 12) }
    }

    private fun MeshBuilder.parasol(c: Rgb) {
        withMat(Mat.WOOD_FINE) {
            cylinder(0f, 0f, 0f, 0.025f, 2.3f, WOODS[0], sides = 10)
            cylinder(0f, 0.72f, 0f, 0.42f, 0.035f, WOODS[0], sides = 24)
        }
        withMat(IRON_MAT) { cylinder(0f, 0f, 0f, 0.2f, 0.05f, IRON, sides = 16) }
        withMat(Mat.FABRIC.copy(doubleSided = true)) {
            val segs = 8
            for (k in 0 until segs) {
                val a0 = k / segs.toFloat() * 2f * PI.toFloat()
                val a1 = (k + 1) / segs.toFloat() * 2f * PI.toFloat()
                val r = 1.18f
                val col = if (k % 2 == 0) c else LINEN_WHITE
                val am = (a0 + a1) / 2f
                tri(V3(0f, 2.36f, 0f), V3(cos(am) * r * 0.96f, 2.0f, sin(am) * r * 0.96f), V3(cos(a0) * r, 1.94f, sin(a0) * r), col)
                tri(V3(0f, 2.36f, 0f), V3(cos(a1) * r, 1.94f, sin(a1) * r), V3(cos(am) * r * 0.96f, 2.0f, sin(am) * r * 0.96f), col)
            }
        }
        for (s in listOf(-1f, 1f)) {
            group {
                translate(s * 0.75f, 0f, 0f)
                rotateY(s * -90f)
                withMat(Mat.WOOD_FINE) {
                    roundBox(0f, 0.42f, 0f, 0.46f, 0.035f, 0.44f, 0.012f, WOODS[0])
                    group { translate(0f, 0.62f, -0.21f); rotateX(-12f); roundBox(0f, 0f, 0f, 0.46f, 0.34f, 0.03f, 0.012f, WOODS[0]) }
                }
                taperLegs(0.42f, 0.4f, 0.4f, 0.02f, WOODS[0], 0.03f, 4f)
                withMat(Mat.FABRIC) { cushion(0f, 0.46f, 0.01f, 0.4f, 0.05f, 0.38f, c.mix(LINEN_WHITE, 0.4f)) }
            }
        }
        withMat(Mat.GLASS) { cylinder(0.12f, 0.755f, 0.1f, 0.035f, 0.13f, Palette.GLASS, sides = 12) }
        withMat(Mat.WATER) { cylinder(0.12f, 0.76f, 0.1f, 0.03f, 0.09f, rgb(0xF2C35B), sides = 12) }
    }

    private fun MeshBuilder.trampoline(c: Rgb) {
        withMat(IRON_MAT) {
            for (k in 0 until 6) {
                val a = k / 6f * 2f * PI.toFloat()
                tube(listOf(V3(cos(a) * 1.0f, 0f, sin(a) * 1.0f), V3(cos(a) * 1.08f, 0.35f, sin(a) * 1.08f), V3(cos(a) * 1.1f, 0.68f, sin(a) * 1.1f)), { 0.025f }, IRON, sides = 6, caps = true)
            }
            torus(0f, 0.69f, 0f, 1.1f, 0.03f, IRON, 36, 6)
        }
        withMat(Mat.FABRIC) { torus(0f, 0.72f, 0f, 1.0f, 0.12f, c, 40, 10) }
        withMat(Mat.FABRIC.copy(roughness = 1f)) { cylinder(0f, 0.68f, 0f, 0.9f, 0.025f, rgb(0x232427), sides = 36) }
        withMat(Mat.FABRIC) { annulus(0f, 0.706f, 0f, 0.26f, 0.31f, rgb(0xF2C94C), 24) }
    }

    private fun MeshBuilder.swingFrame(c: Rgb) {
        val top = 2.25f
        withMat(Mat.DECK) {
            for (sx in listOf(-0.95f, 0.95f)) for (sz in listOf(-1f, 1f)) {
                group {
                    translate(sx, 0f, sz * 0.6f)
                    rotateX(-sz * 15f)
                    roundBox(0f, 1.16f, 0f, 0.1f, 2.33f, 0.1f, 0.02f, WOODS[0].darker(0.1f))
                }
            }
            group {
                translate(-1.05f, top, 0f)
                rotateZ(-90f)
                cylinder(0f, 0f, 0f, 0.07f, 2.1f, WOODS[0].darker(0.15f), sides = 12)
            }
        }
        withMat(STEEL_MAT) { for (x in listOf(-0.32f, 0.32f)) torus(x, top - 0.08f, 0f, 0.04f, 0.01f, STEEL, 12, 4) }
        withMat(Mat.FABRIC) { for (sx in listOf(-0.95f, 0.95f)) roundBox(sx, top + 0.02f, 0f, 0.14f, 0.06f, 0.14f, 0.02f, c) }
        grassTuft(-0.95f, -0.85f, 1f, 71)
        grassTuft(0.95f, 0.85f, 1f, 72)
    }

    private fun MeshBuilder.sandbox(wood: Rgb) {
        withMat(Mat.DECK) {
            block(-0.98f, 0f, -0.98f, 0.98f, 0.3f, -0.86f, wood)
            block(-0.98f, 0f, 0.86f, 0.98f, 0.3f, 0.98f, wood)
            block(-0.98f, 0f, -0.86f, -0.86f, 0.3f, 0.86f, wood)
            block(0.86f, 0f, -0.86f, 0.98f, 0.3f, 0.86f, wood)
            for ((x, z) in listOf(-0.92f to -0.92f, 0.92f to -0.92f, -0.92f to 0.92f, 0.92f to 0.92f)) block(x - 0.12f, 0.3f, z - 0.12f, x + 0.12f, 0.34f, z + 0.12f, wood.darker(0.1f))
        }
        withMat(Mat.SAND) {
            block(-0.86f, 0f, -0.86f, 0.86f, 0.22f, 0.86f, rgb(0xE8D6A8))
            puff(0.2f, 0.22f, -0.2f, 0.32f, 0.1f, 0.3f, rgb(0xDCC795), subdivisions = 2, wobble = 0.1f, seed = 81, floorY = 0.2f)
            lathe(-0.3f, 0.25f, listOf(0.14f to 0.2f, 0.12f to 0.36f, 0f to 0.37f), rgb(0xDCC795), 10)
            lathe(-0.3f, 0.25f, listOf(0.06f to 0.36f, 0.05f to 0.46f, 0f to 0.5f), rgb(0xDCC795), 8)
        }
        withMat(Mat.PLASTIC) {
            frustum(0.45f, 0.2f, 0.4f, 0.09f, 0.12f, 0.16f, rgb(0xD23B30), sides = 14, capTop = false)
            group { translate(0.55f, 0.24f, 0.0f); rotateZ(-70f); cylinder(0f, 0f, 0f, 0.015f, 0.3f, rgb(0xF2C94C), sides = 6) }
        }
    }

    private fun MeshBuilder.birdBath(stone: Rgb) {
        withMat(Mat.STONE) {
            lathe(0f, 0f, listOf(0f to 0f, 0.18f to 0f, 0.16f to 0.08f, 0.07f to 0.14f, 0.06f to 0.6f, 0.08f to 0.64f, 0.3f to 0.72f, 0.32f to 0.78f, 0.27f to 0.78f, 0.1f to 0.72f, 0f to 0.72f), stone, 20)
        }
        withMat(Mat.WATER) { disc(0f, 0.765f, 0f, 0.27f, rgb(0x6FB3C8), 20) }
        withMat(Mat.SOFT) {
            ellipsoid(0.16f, 0.86f, 0f, 0.07f, 0.055f, 0.05f, rgb(0x5C88C4), rings = 6, segments = 10)
            sphere(0.22f, 0.91f, 0f, 0.038f, rgb(0x5C88C4), rings = 6, segments = 10)
            ellipsoid(0.17f, 0.85f, 0f, 0.05f, 0.03f, 0.052f, rgb(0xF2E3C9), rings = 4, segments = 8)
            group { translate(0.258f, 0.91f, 0f); rotateZ(-90f); cone(0f, 0f, 0f, 0.012f, 0.03f, rgb(0xE8A23C), sides = 6) }
        }
        withMat(Mat.EYE) { for (s in listOf(-1f, 1f)) sphere(0.235f, 0.925f, s * 0.026f, 0.007f, rgb(0x111111), 3, 6) }
    }

    private fun MeshBuilder.gnome(hat: Rgb) {
        withMat(Mat.CERAMIC.copy(roughness = 0.3f)) {
            lathe(0f, 0f, listOf(0f to 0f, 0.13f to 0f, 0.14f to 0.12f, 0.11f to 0.26f, 0f to 0.3f), rgb(0x3F6FB0), 16)
            sphere(0f, 0.32f, 0.02f, 0.09f, Palette.SKIN[1], rings = 8, segments = 12)
            puff(0f, 0.25f, 0.08f, 0.09f, 0.08f, 0.05f, rgb(0xF4F1EA), subdivisions = 2, wobble = 0.08f, seed = 3)
            sphere(0f, 0.32f, 0.1f, 0.028f, rgb(0xF0A0A8), rings = 4, segments = 8)
            cone(0f, 0.35f, 0f, 0.115f, 0.3f, hat, sides = 16)
            for (x in listOf(-0.07f, 0.07f)) roundBox(x, 0.03f, 0.04f, 0.07f, 0.05f, 0.11f, 0.02f, rgb(0x4A3426))
        }
    }

    private fun MeshBuilder.hedge(withFlowers: Boolean) {
        val rng = Rng(14)
        withMat(Mat.LEAF) {
            for (k in 0 until 5) {
                val x = -0.56f + k * 0.28f
                puff(x, 0.42f, 0f, 0.26f, 0.38f, 0.24f, rgb(0x456F35).mix(rgb(0x5E8C45), rng.nextFloat()), subdivisions = 2, wobble = 0.14f, seed = 140 + k, floorY = 0f)
            }
        }
        if (withFlowers) {
            repeat(12) {
                val x = rng.range(-0.62f, 0.62f)
                val y = rng.range(0.35f, 0.78f)
                withMat(Mat.SOFT) { sphere(x, y, 0.22f, 0.035f, rng.pick(listOf(rgb(0xF29BB2), rgb(0xF6F2EA), rgb(0xF6D35B))), rings = 4, segments = 8) }
            }
        }
    }

    private fun MeshBuilder.bbq(c: Rgb) {
        withMat(IRON_MAT) {
            for (k in 0 until 3) {
                val a = k / 3f * 2f * PI.toFloat()
                group {
                    translate(cos(a) * 0.12f, 0f, sin(a) * 0.12f)
                    rotateZ(cos(a) * -12f)
                    rotateX(sin(a) * 12f)
                    cylinder(0f, 0f, 0f, 0.016f, 0.62f, IRON, sides = 6)
                }
            }
            for (k in 0 until 2) group { translate(0f, 0.08f, (k - 0.5f) * 0.26f); rotateZ(90f); cylinder(0f, -0.12f, 0f, 0.05f, 0.03f, IRON, sides = 12) }
        }
        withMat(Mat.GLOSSY.copy(roughness = 0.25f)) {
            lathe(0f, 0f, listOf(0f to 0.55f, 0.22f to 0.6f, 0.3f to 0.75f, 0.31f to 0.8f, 0f to 0.8f), c, 22)
            lathe(0f, 0f, listOf(0.31f to 0.84f, 0.28f to 0.95f, 0.15f to 1.05f, 0f to 1.07f), c, 22)
            sphere(0f, 1.08f, 0f, 0.035f, IRON, rings = 5, segments = 8)
        }
        withMat(STEEL_MAT) {
            for (k in -3..3) block(-0.28f, 0.805f, k * 0.07f - 0.004f, 0.28f, 0.812f, k * 0.07f + 0.004f, STEEL)
        }
        withMat(Mat.glow(rgb(0xFF6A2B), 1.6f)) { disc(0f, 0.79f, 0f, 0.26f, rgb(0xFF7A3D), 18) }
        withMat(Mat.WOOD) { roundBox(0.38f, 0.7f, 0f, 0.3f, 0.025f, 0.24f, 0.01f, WOODS[0]) }
    }

    private fun MeshBuilder.pool(c: Rgb) {
        withMat(Mat.STONE) { block(-1.3f, 0f, -0.8f, 1.3f, 0.05f, 0.8f, rgb(0xD9D4CA)) }
        withMat(Mat.PLASTIC) {
            roundBox(0f, 0.2f, -0.8f, 2.96f, 0.4f, 0.36f, 0.17f, c, segments = 3)
            roundBox(0f, 0.2f, 0.8f, 2.96f, 0.4f, 0.36f, 0.17f, c, segments = 3)
            roundBox(-1.3f, 0.2f, 0f, 0.36f, 0.4f, 1.6f, 0.17f, c, segments = 3)
            roundBox(1.3f, 0.2f, 0f, 0.36f, 0.4f, 1.6f, 0.17f, c, segments = 3)
            for (z in listOf(-0.8f, 0.8f)) roundBox(0f, 0.405f, z, 2.7f, 0.03f, 0.12f, 0.012f, Palette.WHITE)
            for (x in listOf(-1.3f, 1.3f)) roundBox(x, 0.405f, 0f, 0.12f, 0.03f, 1.4f, 0.012f, Palette.WHITE)
        }
        withMat(Mat.WATER) { block(-1.15f, 0.05f, -0.65f, 1.15f, 0.3f, 0.65f, rgb(0x5FB7D6)) }
        withMat(Mat.PLASTIC) {
            ellipsoid(0.4f, 0.32f, 0.1f, 0.13f, 0.09f, 0.1f, rgb(0xF2C94C), rings = 8, segments = 12)
            sphere(0.5f, 0.42f, 0.1f, 0.07f, rgb(0xF2C94C), rings = 6, segments = 10)
            group { translate(0.57f, 0.42f, 0.1f); rotateZ(-90f); cone(0f, 0f, 0f, 0.03f, 0.05f, rgb(0xE8873C), sides = 8) }
            torus(-0.55f, 0.31f, -0.15f, 0.22f, 0.07f, rgb(0xE8606D), 22, 10)
        }
    }
}
