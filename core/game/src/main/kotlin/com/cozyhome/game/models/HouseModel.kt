package com.cozyhome.game.models

import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.world.Lot
import com.cozyhome.game.world.Opening
import com.cozyhome.game.world.WallSide

object HouseModel {
    private val EXTERIOR = rgb(0xFFFDF6)
    private val TRIM = rgb(0xFFFFFF)
    private val BASE = rgb(0xE9D9C4)
    private const val ROOF_Y = Lot.WALL_H
    private const val RIDGE = 2.3f
    private const val OVERHANG = 0.55f
    const val CUT_HEIGHT = 0.55f

    fun floor(style: Int): Mesh = mesh {
        val r = Lot.ROOM
        val (a, b) = Palette.FLOORS[style.mod(Palette.FLOORS.size)]
        mat = Mat.MATTE
        block(r.minX - 0.45f, -0.35f, r.minZ - 0.45f, r.maxX + 0.45f, 0.0f, r.maxZ + 0.45f, Palette.STONE)
        if (style.mod(Palette.FLOORS.size) <= 1) {
            mat = Mat(roughness = 0.6f)
            val plank = 0.3f
            var x = r.minX
            var i = 0
            while (x < r.maxX - 1e-3f) {
                val x1 = minOf(x + plank, r.maxX)
                val c = if (i % 2 == 0) a else b
                val split = r.minZ + ((i * 37) % 7 + 2) * 0.55f
                block(x + 0.006f, 0f, r.minZ, x1 - 0.006f, 0.04f, split - 0.01f, c)
                block(x + 0.006f, 0f, split + 0.01f, x1 - 0.006f, 0.04f, r.maxZ, c.mix(a, 0.3f))
                block(x, 0f, r.minZ, x1, 0.032f, r.maxZ, a.darker(0.18f))
                x = x1
                i++
            }
        } else {
            mat = Mat(roughness = 0.4f)
            block(r.minX, 0f, r.minZ, r.maxX, 0.03f, r.maxZ, b.darker(0.06f))
            val tile = 0.5f
            var ix = 0
            var x = r.minX
            while (x < r.maxX - 1e-3f) {
                var z = r.minZ
                var iz = 0
                while (z < r.maxZ - 1e-3f) {
                    val c = if ((ix + iz) % 2 == 0) a else b
                    block(x + 0.012f, 0.03f, z + 0.012f, x + tile - 0.012f, 0.04f, z + tile - 0.012f, c)
                    z += tile
                    iz++
                }
                x += tile
                ix++
            }
        }
    }

    fun wall(side: WallSide, paint: Int, cut: Boolean): Mesh = mesh {
        val inner = Palette.WALLS[paint.mod(Palette.WALLS.size)]
        val r = Lot.ROOM
        val t = Lot.WALL_T
        val h = if (cut) CUT_HEIGHT else Lot.WALL_H
        val openings = Lot.OPENINGS.filter { it.side == side }.sortedBy { it.center }
        val (start, end) = when (side) {
            WallSide.NORTH, WallSide.SOUTH -> (r.minX - t) to (r.maxX + t)
            WallSide.WEST, WallSide.EAST -> r.minZ to r.maxZ
        }
        val intervals = ArrayList<Triple<Float, Float, Opening?>>()
        var cursor = start
        for (o in openings) {
            val a = o.center - o.width / 2f
            val b = o.center + o.width / 2f
            if (a > cursor) intervals += Triple(cursor, a, null)
            intervals += Triple(a, b, o)
            cursor = b
        }
        if (cursor < end) intervals += Triple(cursor, end, null)
        for ((a, b, o) in intervals) {
            if (o == null) {
                piece(side, a, b, 0f, h, inner)
            } else {
                if (o.bottom > 0f) piece(side, a, b, 0f, minOf(o.bottom, h), inner)
                if (o.top < h) piece(side, a, b, o.top, h, inner)
                if (!cut || o.bottom < h) opening(side, o, cut, h)
            }
        }
        mat = Mat(roughness = 0.6f)
        val solidRuns = ArrayList<Pair<Float, Float>>()
        for ((a, b, o) in intervals) {
            if (o != null && o.bottom <= 0f) continue
            val last = solidRuns.lastOrNull()
            if (last != null && kotlin.math.abs(last.second - a) < 1e-3f) solidRuns[solidRuns.size - 1] = last.first to b
            else solidRuns += a to b
        }
        for ((a, b) in solidRuns) {
            along(side, a, b, 0f, 0.14f, 0.03f, true, Palette.WOOD_LIGHT)
            along(side, a, b, 0f, 0.3f, 0.04f, false, BASE)
            if (cut) along(side, a, b, h - 0.03f, h + 0.03f, t + 0.06f, null, Palette.WOOD)
        }
        if (!cut) along(side, start, end, h - 0.12f, h, 0.03f, true, TRIM)
        if (!cut && side == WallSide.SOUTH) {
            porchLights()
            frontDoor()
        }
    }

    private fun MeshBuilder.piece(side: WallSide, a: Float, b: Float, y0: Float, y1: Float, inner: Rgb) {
        if (y1 - y0 < 1e-3f || b - a < 1e-3f) return
        val r = Lot.ROOM
        val t = Lot.WALL_T
        when (side) {
            WallSide.NORTH -> {
                block(a, y0, r.minZ - t / 2f, b, y1, r.minZ, inner)
                block(a, y0, r.minZ - t, b, y1, r.minZ - t / 2f, EXTERIOR)
            }
            WallSide.SOUTH -> {
                block(a, y0, r.maxZ, b, y1, r.maxZ + t / 2f, inner)
                block(a, y0, r.maxZ + t / 2f, b, y1, r.maxZ + t, EXTERIOR)
            }
            WallSide.WEST -> {
                block(r.minX - t / 2f, y0, a, r.minX, y1, b, inner)
                block(r.minX - t, y0, a, r.minX - t / 2f, y1, b, EXTERIOR)
            }
            WallSide.EAST -> {
                block(r.maxX, y0, a, r.maxX + t / 2f, y1, b, inner)
                block(r.maxX + t / 2f, y0, a, r.maxX + t, y1, b, EXTERIOR)
            }
        }
    }

    private fun MeshBuilder.along(side: WallSide, a: Float, b: Float, y0: Float, y1: Float, thick: Float, inside: Boolean?, c: Rgb) {
        val r = Lot.ROOM
        val t = Lot.WALL_T
        val (lo, hi) = when (inside) {
            true -> -thick to 0f
            false -> t to t + thick
            null -> (-thick + t) / 2f - t / 2f to (t + thick) / 2f
        }
        when (side) {
            WallSide.NORTH -> block(a, y0, r.minZ - hi, b, y1, r.minZ - lo, c)
            WallSide.SOUTH -> block(a, y0, r.maxZ + lo, b, y1, r.maxZ + hi, c)
            WallSide.WEST -> block(r.minX - hi, y0, a, r.minX - lo, y1, b, c)
            WallSide.EAST -> block(r.maxX + lo, y0, a, r.maxX + hi, y1, b, c)
        }
    }

    private fun MeshBuilder.opening(side: WallSide, o: Opening, cut: Boolean, h: Float) {
        val r = Lot.ROOM
        val t = Lot.WALL_T
        val a = o.center - o.width / 2f
        val b = o.center + o.width / 2f
        val isDoor = o.bottom <= 0f
        val top = minOf(o.top, h)
        val f = 0.07f
        fun frame(u0: Float, u1: Float, y0: Float, y1: Float, out: Float) {
            if (y1 <= y0) return
            when (side) {
                WallSide.NORTH -> block(u0, y0, r.minZ - t - out, u1, y1, r.minZ + out, TRIM)
                WallSide.SOUTH -> block(u0, y0, r.maxZ - out, u1, y1, r.maxZ + t + out, TRIM)
                WallSide.WEST -> block(r.minX - t - out, y0, u0, r.minX + out, y1, u1, TRIM)
                WallSide.EAST -> block(r.maxX - out, y0, u0, r.maxX + t + out, y1, u1, TRIM)
            }
        }
        mat = Mat(roughness = 0.5f)
        frame(a - f, a, o.bottom, top, 0.02f)
        frame(b, b + f, o.bottom, top, 0.02f)
        if (!cut && o.top <= h) frame(a - f, b + f, o.top, o.top + f, 0.02f)
        if (!isDoor) {
            frame(a - f * 1.5f, b + f * 1.5f, o.bottom - 0.06f, o.bottom, 0.07f)
            if (!cut) {
                frame((a + b) / 2f - 0.03f, (a + b) / 2f + 0.03f, o.bottom, o.top, 0f)
                frame(a, b, (o.bottom + o.top) / 2f - 0.03f, (o.bottom + o.top) / 2f + 0.03f, 0f)
                withMat(Mat.GLASS) {
                    when (side) {
                        WallSide.NORTH -> block(a, o.bottom, r.minZ - t / 2f - 0.01f, b, o.top, r.minZ - t / 2f + 0.01f, Palette.GLASS)
                        WallSide.SOUTH -> block(a, o.bottom, r.maxZ + t / 2f - 0.01f, b, o.top, r.maxZ + t / 2f + 0.01f, Palette.GLASS)
                        WallSide.WEST -> block(r.minX - t / 2f - 0.01f, o.bottom, a, r.minX - t / 2f + 0.01f, o.top, b, Palette.GLASS)
                        WallSide.EAST -> block(r.maxX + t / 2f - 0.01f, o.bottom, a, r.maxX + t / 2f + 0.01f, o.top, b, Palette.GLASS)
                    }
                }
                flowerBox(side, a, b, o.bottom)
            }
        }
    }

    private fun MeshBuilder.flowerBox(side: WallSide, a: Float, b: Float, y: Float) {
        val r = Lot.ROOM
        val t = Lot.WALL_T
        val colors = listOf(Palette.PINK, Palette.YELLOW, Palette.CORAL, Palette.LILAC)
        group {
            when (side) {
                WallSide.NORTH -> { translate((a + b) / 2f, y - 0.32f, r.minZ - t - 0.16f); rotateY(180f) }
                WallSide.SOUTH -> translate((a + b) / 2f, y - 0.32f, r.maxZ + t + 0.16f)
                WallSide.WEST -> { translate(r.minX - t - 0.16f, y - 0.32f, (a + b) / 2f); rotateY(-90f) }
                WallSide.EAST -> { translate(r.maxX + t + 0.16f, y - 0.32f, (a + b) / 2f); rotateY(90f) }
            }
            val w = (b - a) + 0.1f
            mat = Mat(roughness = 0.7f)
            block(-w / 2f, 0f, -0.14f, w / 2f, 0.24f, 0.14f, Palette.WOOD)
            block(-w / 2f + 0.03f, 0.2f, -0.11f, w / 2f - 0.03f, 0.25f, 0.11f, Palette.SOIL)
            mat = Mat.MATTE
            var x = -w / 2f + 0.14f
            var i = 0
            while (x < w / 2f - 0.1f) {
                blob(x, 0.32f, 0f, 0.1f, 0.08f, 0.1f, Palette.LEAF, 0, 0.1f, i + 40)
                sphere(x + 0.02f, 0.39f, 0.04f, 0.055f, colors[i % colors.size], rings = 4, segments = 8)
                x += 0.24f
                i++
            }
        }
    }

    fun roof(): Mesh = mesh {
        val r = Lot.ROOM
        val t = Lot.WALL_T
        val x0 = r.minX - t - OVERHANG
        val x1 = r.maxX + t + OVERHANG
        val z0 = r.minZ - t - OVERHANG
        val z1 = r.maxZ + t + OVERHANG
        val zm = (r.minZ + r.maxZ) / 2f
        val y0 = ROOF_Y - 0.12f
        val y1 = ROOF_Y + RIDGE
        mat = Mat(roughness = 0.75f)
        val rows = 7
        for (side in 0..1) {
            val ze = if (side == 0) z0 else z1
            for (k in 0 until rows) {
                val t0 = k / rows.toFloat()
                val t1 = (k + 1) / rows.toFloat()
                val za = ze + (zm - ze) * t0
                val zb = ze + (zm - ze) * t1
                val ya = y0 + (y1 - y0) * t0
                val yb = y0 + (y1 - y0) * t1
                val c = if (k % 2 == 0) Palette.ROOF else Palette.ROOF.mix(Palette.ROOF_DARK, 0.45f)
                val lift = 0.05f
                if (side == 1) {
                    quad(V3(x0, ya + lift, za), V3(x1, ya + lift, za), V3(x1, yb + lift, zb), V3(x0, yb + lift, zb), c)
                    quad(V3(x0, ya, za), V3(x0, yb, zb), V3(x1, yb, zb), V3(x1, ya, za), Palette.ROOF_DARK)
                } else {
                    quad(V3(x1, ya + lift, za), V3(x0, ya + lift, za), V3(x0, yb + lift, zb), V3(x1, yb + lift, zb), c)
                    quad(V3(x1, ya, za), V3(x1, yb, zb), V3(x0, yb, zb), V3(x0, ya, za), Palette.ROOF_DARK)
                }
            }
            if (side == 1) {
                quad(V3(x0, y0, z1), V3(x1, y0, z1), V3(x1, y0 + 0.05f, z1), V3(x0, y0 + 0.05f, z1), Palette.ROOF_DARK)
            } else {
                quad(V3(x1, y0, z0), V3(x0, y0, z0), V3(x0, y0 + 0.05f, z0), V3(x1, y0 + 0.05f, z0), Palette.ROOF_DARK)
            }
        }
        block(x0 - 0.02f, y1 - 0.02f, zm - 0.12f, x1 + 0.02f, y1 + 0.12f, zm + 0.12f, Palette.ROOF_DARK)
        mat = Mat.MATTE
        for (end in 0..1) {
            val x = if (end == 0) r.minX - t else r.maxX + t
            val zA = r.minZ - t
            val zB = r.maxZ + t
            val yTop = y1 + 0.04f
            val yBand = ROOF_Y + 0.14f
            if (end == 0) {
                quad(V3(x, ROOF_Y, zA), V3(x, ROOF_Y, zB), V3(x, yBand, zB), V3(x, yBand, zA), EXTERIOR)
                tri(V3(x, yBand, zA), V3(x, yBand, zB), V3(x, yTop, zm), EXTERIOR)
                tri(V3(x + 0.05f, yBand, zB), V3(x + 0.05f, yBand, zA), V3(x + 0.05f, yTop, zm), EXTERIOR)
                group {
                    translate(x - 0.02f, ROOF_Y + 1.0f, zm)
                    cylinder(0f, 0f, 0f, 0.32f, 0.04f, TRIM, sides = 16)
                    group { rotateZ(90f); cylinder(0f, -0.06f, 0f, 0.36f, 0.06f, TRIM, sides = 16, caps = true) }
                    group { rotateZ(90f); cylinder(0f, -0.07f, 0f, 0.27f, 0.02f, Palette.SKY, sides = 16) }
                }
            } else {
                quad(V3(x, ROOF_Y, zB), V3(x, ROOF_Y, zA), V3(x, yBand, zA), V3(x, yBand, zB), EXTERIOR)
                tri(V3(x, yBand, zB), V3(x, yBand, zA), V3(x, yTop, zm), EXTERIOR)
                tri(V3(x - 0.05f, yBand, zA), V3(x - 0.05f, yBand, zB), V3(x - 0.05f, yTop, zm), EXTERIOR)
            }
        }
        block(r.minX - t, ROOF_Y - 0.12f, r.minZ - t, r.maxX + t, ROOF_Y, r.maxZ + t, TRIM)
        mat = Mat(roughness = 0.8f)
        block(3.0f, ROOF_Y + 0.6f, -14.2f, 3.8f, ROOF_Y + RIDGE + 0.8f, -13.4f, rgb(0xE07B6A))
        block(2.9f, ROOF_Y + RIDGE + 0.8f, -14.3f, 3.9f, ROOF_Y + RIDGE + 0.95f, -13.3f, rgb(0xC9675A))
    }

    fun porch(): Mesh = mesh {
        val p = Lot.PORCH
        mat = Mat.MATTE
        block(p.minX, 0f, p.minZ, p.maxX, 0.16f, p.maxZ - 0.35f, Palette.STONE)
        block(p.minX + 0.2f, 0f, p.maxZ - 0.35f, p.maxX - 0.2f, 0.08f, p.maxZ, Palette.STONE_DARK)
        mat = Mat.SOFT
        roundBox(0f, 0.17f, p.minZ + 0.3f, 1.0f, 0.02f, 0.5f, 0.01f, Palette.CORAL)
        mat = Mat(roughness = 0.6f)
    }
}

private fun MeshBuilder.porchLights() {
    for (x in listOf(-1.05f, 1.05f)) {
        withMat(Mat.METAL) { block(x - 0.06f, 1.95f, -5.86f, x + 0.06f, 2.0f, -5.75f, Palette.INK) }
        withMat(Mat.glow(rgb(0xFFE7A3), 3f)) { roundBox(x, 1.84f, -5.72f, 0.14f, 0.2f, 0.14f, 0.04f, rgb(0xFFF1C2)) }
    }
    withMat(Mat.glow(rgb(0xFFD36B), 1.4f)) {
        roundBox(0f, 2.62f, -5.74f, 0.9f, 0.3f, 0.06f, 0.05f, rgb(0xFFE7A8))
    }
}

private fun MeshBuilder.frontDoor() {
    withMat(Mat(roughness = 0.6f)) {
        group {
            translate(-0.75f, 0.04f, -6.04f)
            rotateY(-100f)
            block(0f, 0.02f, -0.03f, 1.48f, 2.24f, 0.03f, rgb(0x7CC4A4))
            block(0.15f, 0.25f, 0.03f, 1.33f, 1.0f, 0.045f, rgb(0x6BB394))
            block(0.15f, 1.2f, 0.03f, 1.33f, 2.05f, 0.045f, rgb(0x6BB394))
            withMat(Mat.METAL) { sphere(1.3f, 1.1f, 0.07f, 0.04f, Palette.YELLOW, rings = 4, segments = 8) }
        }
    }
}
