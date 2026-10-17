package com.cozyhome.game.models

import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import kotlin.math.PI
import kotlin.math.sin
import com.cozyhome.game.world.Lot
import com.cozyhome.game.world.Opening
import com.cozyhome.game.world.WallSide

object HouseModel {
    private val EXTERIOR = rgb(0xF6EFE3)
    private val TRIM = rgb(0xFBF8F2)
    private val BASE = rgb(0xB9B1A3)
    private val ROOF_TILE = rgb(0xC0613F)
    private val SOFFIT = rgb(0x9A7356)
    private const val ROOF_Y = Lot.WALL_H
    private const val RIDGE = 2.3f
    private const val OVERHANG = 0.55f
    const val CUT_HEIGHT = 0.55f

    fun floor(style: Int): Mesh = mesh {
        val r = Lot.ROOM
        val (a, b) = Palette.FLOORS[style.mod(Palette.FLOORS.size)]
        withMat(Mat.STONE) { block(r.minX - 0.45f, -0.35f, r.minZ - 0.45f, r.maxX + 0.45f, 0.0f, r.maxZ + 0.45f, BASE) }
        if (style.mod(Palette.FLOORS.size) <= 1) {
            withMat(Mat.PLANKS) { block(r.minX, 0f, r.minZ, r.maxX, 0.04f, r.maxZ, a.mix(b, 0.3f)) }
        } else {
            withMat(Mat.TILE) { block(r.minX, 0f, r.minZ, r.maxX, 0.04f, r.maxZ, a) }
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
        mat = Mat.PAINTED_WOOD
        val solidRuns = ArrayList<Pair<Float, Float>>()
        for ((a, b, o) in intervals) {
            if (o != null && o.bottom <= 0f) continue
            val last = solidRuns.lastOrNull()
            if (last != null && kotlin.math.abs(last.second - a) < 1e-3f) solidRuns[solidRuns.size - 1] = last.first to b
            else solidRuns += a to b
        }
        for ((a, b) in solidRuns) {
            withMat(Mat.WOOD) { along(side, a, b, 0f, 0.14f, 0.03f, true, Palette.WOOD_LIGHT.darker(0.1f)) }
            withMat(Mat.STONE) { along(side, a, b, 0f, 0.38f, 0.05f, false, BASE) }
            if (cut) withMat(Mat.WOOD) { along(side, a, b, h - 0.03f, h + 0.03f, t + 0.06f, null, Palette.WOOD) }
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
                withMat(Mat.PLASTER) { block(a, y0, r.minZ - t / 2f, b, y1, r.minZ, inner) }
                withMat(Mat.PLASTER) { block(a, y0, r.minZ - t, b, y1, r.minZ - t / 2f, EXTERIOR) }
            }
            WallSide.SOUTH -> {
                withMat(Mat.PLASTER) { block(a, y0, r.maxZ, b, y1, r.maxZ + t / 2f, inner) }
                withMat(Mat.PLASTER) { block(a, y0, r.maxZ + t / 2f, b, y1, r.maxZ + t, EXTERIOR) }
            }
            WallSide.WEST -> {
                withMat(Mat.PLASTER) { block(r.minX - t / 2f, y0, a, r.minX, y1, b, inner) }
                withMat(Mat.PLASTER) { block(r.minX - t, y0, a, r.minX - t / 2f, y1, b, EXTERIOR) }
            }
            WallSide.EAST -> {
                withMat(Mat.PLASTER) { block(r.maxX, y0, a, r.maxX + t / 2f, y1, b, inner) }
                withMat(Mat.PLASTER) { block(r.maxX + t / 2f, y0, a, r.maxX + t, y1, b, EXTERIOR) }
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
        mat = Mat.PAINTED_WOOD
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
                curtains(side, a, b, o.bottom, o.top)
            }
        }
    }

    private fun place(side: WallSide, along: Float, y: Float, inward: Float): V3 {
        val r = Lot.ROOM
        return when (side) {
            WallSide.NORTH -> V3(along, y, r.minZ + inward)
            WallSide.SOUTH -> V3(along, y, r.maxZ - inward)
            WallSide.WEST -> V3(r.minX + inward, y, along)
            WallSide.EAST -> V3(r.maxX - inward, y, along)
        }
    }

    private fun MeshBuilder.curtains(side: WallSide, a: Float, b: Float, bottom: Float, top: Float) {
        val linen = rgb(0xEEE7DB)
        val y0 = maxOf(0.12f, bottom - 0.25f)
        val y1 = top + 0.14f
        val inward = when (side) {
            WallSide.NORTH -> V3(0f, 0f, 1f)
            WallSide.SOUTH -> V3(0f, 0f, -1f)
            WallSide.WEST -> V3(1f, 0f, 0f)
            WallSide.EAST -> V3(-1f, 0f, 0f)
        }
        withMat(Mat.FABRIC.copy(doubleSided = true)) {
            for (s in listOf(-1f, 1f)) {
                val c0 = if (s < 0) a - 0.22f else b - 0.12f
                val w = 0.34f
                val cols = 14
                val rows = 3
                val ids = Array(rows + 1) { IntArray(cols + 1) }
                for (j in 0..rows) {
                    val y = y1 - (y1 - y0) * j / rows
                    val gather = 1f - 0.25f * (j / rows.toFloat()) * (1f - j / rows.toFloat()) * 4f * 0.5f
                    for (i in 0..cols) {
                        val u = i / cols.toFloat()
                        val along = c0 + u * w * gather + (if (s < 0) 0f else (1f - gather) * w)
                        val fold = 0.035f * sin(u * PI.toFloat() * 7f)
                        val p = place(side, along, y, 0.09f + fold)
                        ids[j][i] = vertexPublic(p, inward, linen.darker(0.06f * (0.5f + 0.5f * sin(u * PI.toFloat() * 7f + 1.2f))))
                    }
                }
                for (j in 0 until rows) for (i in 0 until cols) {
                    triPublic(ids[j][i], ids[j + 1][i], ids[j + 1][i + 1])
                    triPublic(ids[j][i], ids[j + 1][i + 1], ids[j][i + 1])
                }
            }
        }
        withMat(Mat(roughness = 0.35f, metallic = 0.9f)) {
            val p0 = place(side, a - 0.3f, y1 + 0.03f, 0.1f)
            val p1 = place(side, b + 0.3f, y1 + 0.03f, 0.1f)
            tube(listOf(p0, p1), { 0.012f }, rgb(0xC9A15A), sides = 8, caps = true)
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
            withMat(Mat.DECK) { block(-w / 2f, 0f, -0.14f, w / 2f, 0.24f, 0.14f, rgb(0x9C6E4A)) }
            withMat(Mat.SOIL) { block(-w / 2f + 0.03f, 0.2f, -0.11f, w / 2f - 0.03f, 0.25f, 0.11f, rgb(0x5B4330)) }
            var x = -w / 2f + 0.14f
            var i = 0
            while (x < w / 2f - 0.1f) {
                withMat(Mat.LEAF) { puff(x, 0.3f, 0f, 0.11f, 0.08f, 0.1f, rgb(0x5E9B3A), subdivisions = 1, wobble = 0.15f, seed = i + 40) }
                withMat(Mat.SOFT) { sphere(x + 0.02f, 0.37f, 0.05f, 0.05f, colors[i % colors.size], rings = 4, segments = 8) }
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
        val roofPaint = com.cozyhome.game.mesh.Paint { _, y, _ -> ROOF_TILE.mix(ROOF_TILE.darker(0.25f), 1f - ((y - y0) / (y1 - y0)).coerceIn(0f, 1f) * 0.6f) }
        for (side in 0..1) {
            val ze = if (side == 0) z0 else z1
            val lift = 0.05f
            withMat(Mat.ROOF) {
                if (side == 1) quad(V3(x0, y0 + lift, ze), V3(x1, y0 + lift, ze), V3(x1, y1 + lift, zm), V3(x0, y1 + lift, zm), roofPaint)
                else quad(V3(x1, y0 + lift, ze), V3(x0, y0 + lift, ze), V3(x0, y1 + lift, zm), V3(x1, y1 + lift, zm), roofPaint)
            }
            withMat(Mat.WOOD) {
                if (side == 1) {
                    quad(V3(x0, y0, ze), V3(x0, y1, zm), V3(x1, y1, zm), V3(x1, y0, ze), SOFFIT)
                    quad(V3(x0, y0, z1), V3(x1, y0, z1), V3(x1, y0 + 0.07f, z1), V3(x0, y0 + 0.07f, z1), SOFFIT.darker(0.1f))
                } else {
                    quad(V3(x1, y0, ze), V3(x1, y1, zm), V3(x0, y1, zm), V3(x0, y0, ze), SOFFIT)
                    quad(V3(x1, y0, z0), V3(x0, y0, z0), V3(x0, y0 + 0.07f, z0), V3(x1, y0 + 0.07f, z0), SOFFIT.darker(0.1f))
                }
            }
        }
        withMat(Mat.ROOF.copy(texScale = 0.6f)) {
            group {
                translate(0f, y1 + 0.06f, zm)
                rotateZ(90f)
                cylinder(0f, x0 - 0.04f, 0f, 0.13f, x1 - x0 + 0.08f, ROOF_TILE.darker(0.12f), sides = 10)
            }
        }
        mat = Mat.PLASTER
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
                withMat(Mat.PAINTED_WOOD) {
                    group {
                        translate(x - 0.02f, ROOF_Y + 1.0f, zm)
                        group { rotateZ(90f); cylinder(0f, -0.06f, 0f, 0.36f, 0.06f, TRIM, sides = 20, caps = true) }
                    }
                }
                withMat(Mat.GLASS) {
                    group {
                        translate(x - 0.02f, ROOF_Y + 1.0f, zm)
                        group { rotateZ(90f); cylinder(0f, -0.075f, 0f, 0.28f, 0.02f, Palette.GLASS, sides = 20) }
                    }
                }
            } else {
                quad(V3(x, ROOF_Y, zB), V3(x, ROOF_Y, zA), V3(x, yBand, zA), V3(x, yBand, zB), EXTERIOR)
                tri(V3(x, yBand, zB), V3(x, yBand, zA), V3(x, yTop, zm), EXTERIOR)
                tri(V3(x - 0.05f, yBand, zA), V3(x - 0.05f, yBand, zB), V3(x - 0.05f, yTop, zm), EXTERIOR)
            }
        }
        withMat(Mat.PAINTED_WOOD) { block(r.minX - t, ROOF_Y - 0.12f, r.minZ - t, r.maxX + t, ROOF_Y, r.maxZ + t, TRIM) }
        withMat(Mat.BRICK) {
            block(3.0f, ROOF_Y + 0.6f, -14.2f, 3.8f, ROOF_Y + RIDGE + 0.8f, -13.4f, rgb(0xA9583F))
        }
        withMat(Mat.STONE) {
            block(2.9f, ROOF_Y + RIDGE + 0.8f, -14.3f, 3.9f, ROOF_Y + RIDGE + 0.95f, -13.3f, rgb(0x8E887E))
        }
    }

    fun porch(): Mesh = mesh {
        val p = Lot.PORCH
        withMat(Mat.STONE) {
            block(p.minX, 0f, p.minZ, p.maxX, 0.16f, p.maxZ - 0.35f, rgb(0xC9C3B7))
            block(p.minX + 0.2f, 0f, p.maxZ - 0.35f, p.maxX - 0.2f, 0.08f, p.maxZ, rgb(0xB3AC9F))
        }
        withMat(Mat.CARPET) { roundBox(0f, 0.17f, p.minZ + 0.3f, 1.0f, 0.02f, 0.5f, 0.01f, rgb(0xB5654C)) }
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
    withMat(Mat.PAINTED_WOOD) {
        group {
            translate(-0.75f, 0.04f, -6.04f)
            rotateY(-100f)
            block(0f, 0.02f, -0.03f, 1.48f, 2.24f, 0.03f, rgb(0x6FA88E))
            block(0.15f, 0.25f, 0.03f, 1.33f, 1.0f, 0.045f, rgb(0x64997F))
            block(0.15f, 1.2f, 0.03f, 1.33f, 2.05f, 0.045f, rgb(0x64997F))
            withMat(Mat.METAL) { sphere(1.3f, 1.1f, 0.07f, 0.04f, Palette.YELLOW, rings = 4, segments = 8) }
        }
    }
}
