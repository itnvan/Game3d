package com.cozyhome.game.models

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.smoothstep
import com.cozyhome.game.math.valueNoise
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Paint
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.mottled
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.world.Lot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object WorldModel {
    private const val INNER = 32f
    private const val OUTER = 160f

    fun height(x: Float, z: Float): Float {
        val r = max(abs(x), abs(z))
        val rise = smoothstep(INNER, 70f, r)
        return rise * (2.2f + 7f * valueNoise(x / 34f, z / 34f, 3)) + rise * rise * 4f
    }

    private val grassPaint = Paint { x, y, z ->
        val base = mottled(Palette.GRASS, Palette.GRASS_DARK, 0.11f, 0.85f, 5).at(x, y, z)
        val far = smoothstep(INNER, 90f, max(abs(x), abs(z)))
        base.mix(Palette.GRASS_FAR, far * 0.7f).mix(rgb(0xB9E59A), smoothstep(4f, 12f, y) * 0.5f)
    }

    fun ground(): Mesh = mesh {
        mat = Mat(roughness = 1f)
        terrain(-INNER, -INNER, INNER, INNER, 32, 32, grassPaint)
        val cell = 8f
        val n = ((OUTER * 2) / cell).toInt()
        for (i in 0 until n) for (j in 0 until n) {
            val ax = -OUTER + i * cell
            val az = -OUTER + j * cell
            val bx = ax + cell
            val bz = az + cell
            if (ax >= -INNER && bx <= INNER && az >= -INNER && bz <= INNER) continue
            val p00 = V3(ax, height(ax, az), az)
            val p10 = V3(bx, height(bx, az), az)
            val p01 = V3(ax, height(ax, bz), bz)
            val p11 = V3(bx, height(bx, bz), bz)
            tri(p01, p11, p10, grassPaint)
            tri(p01, p10, p00, grassPaint)
        }
        paths()
        track()
        court()
        pitch()
        pond()
        gardenGround()
    }

    private fun MeshBuilder.slab(x0: Float, z0: Float, x1: Float, z1: Float, y: Float, c: Rgb, thick: Float = 0.03f) =
        block(x0, y - thick, z0, x1, y, z1, c)

    private fun MeshBuilder.paths() {
        mat = Mat(roughness = 0.9f)
        slab(-0.85f, -4.85f, 0.85f, -1.9f, 0.03f, Palette.PATH)
        slab(-0.85f, 17f, 0.85f, 19.9f, 0.03f, Palette.PATH)
        slab(0.85f, -4.4f, 10.6f, -3.4f, 0.03f, Palette.PATH)
        slab(9.2f, -6.0f, 10.6f, -3.4f, 0.03f, Palette.PATH)
        for (side in listOf(-1f, 1f)) {
            slab(side * 0.85f - 0.05f, -4.85f, side * 0.85f + 0.05f, -1.9f, 0.035f, Palette.PATH_EDGE)
            slab(side * 0.85f - 0.05f, 17f, side * 0.85f + 0.05f, 19.9f, 0.035f, Palette.PATH_EDGE)
        }
        val stones = listOf(
            V2(-8.9f, 1.0f), V2(-9.9f, 0.1f), V2(-10.6f, -1.2f), V2(-11.6f, -2.4f), V2(-12.6f, -3.8f), V2(-13.5f, -5.2f), V2(-14.4f, -6.4f),
            V2(-8.9f, 6.2f), V2(-9.9f, 6.6f),
            V2(8.9f, 4.5f), V2(9.9f, 5.0f), V2(10.9f, 5.8f), V2(12.0f, 6.2f),
        )
        for ((i, s) in stones.withIndex()) {
            group {
                translate(s.x, 0f, s.z)
                rotateY(i * 47f)
                scale(1f, 1f, 0.8f)
                cylinder(0f, 0f, 0f, 0.38f, 0.05f, Palette.STONE.mix(Palette.STONE_DARK, (i % 3) * 0.25f), sides = 9)
            }
        }
    }

    private fun MeshBuilder.track() {
        val t = Lot.TRACK
        val pts = t.points
        val half = t.width / 2f
        mat = Mat(roughness = 0.95f)
        fun normalAt(i: Int): V2 {
            val a = pts[(i - 1).coerceAtLeast(0)]
            val b = pts[(i + 1).coerceAtMost(pts.size - 1)]
            val d = (b - a).normalized()
            return V2(-d.z, d.x)
        }
        fun strip(off0: Float, off1: Float, y: Float, c: Rgb) {
            for (i in 1 until pts.size) {
                val na = normalAt(i - 1)
                val nb = normalAt(i)
                val a0 = pts[i - 1] + na * off0
                val a1 = pts[i - 1] + na * off1
                val b0 = pts[i] + nb * off0
                val b1 = pts[i] + nb * off1
                val q = listOf(V3(a0.x, y, a0.z), V3(b0.x, y, b0.z), V3(b1.x, y, b1.z), V3(a1.x, y, a1.z))
                val n = (q[1] - q[0]) cross (q[2] - q[0])
                if (n.y >= 0f) quad(q[0], q[1], q[2], q[3], c) else quad(q[0], q[3], q[2], q[1], c)
            }
        }
        strip(-half - 0.12f, half + 0.12f, 0.022f, Palette.TRACK.darker(0.12f))
        strip(-half, half, 0.028f, Palette.TRACK)
        strip(-0.04f, 0.04f, 0.031f, Palette.TRACK_LINE)
        strip(-half + 0.08f, -half + 0.14f, 0.031f, Palette.TRACK_LINE)
        strip(half - 0.14f, half - 0.08f, 0.031f, Palette.TRACK_LINE)
        val s = Lot.TRACK_START
        for (k in 0 until 8) {
            val z0 = s.z - half + k * (t.width / 8f)
            for (col in 0..1) {
                val white = (k + col) % 2 == 0
                block(s.x - 0.2f + col * 0.2f, 0.028f, z0, s.x + col * 0.2f, 0.034f, z0 + t.width / 8f, if (white) Palette.WHITE else Palette.INK)
            }
        }
    }

    private fun MeshBuilder.line(x0: Float, z0: Float, x1: Float, z1: Float, y: Float, w: Float = 0.07f, c: Rgb = Palette.WHITE) {
        val d = V2(x1 - x0, z1 - z0)
        val len = d.length
        if (len < 1e-4f) return
        val ang = atan2(d.x, d.z) * 180f / PI.toFloat()
        group {
            translate((x0 + x1) / 2f, 0f, (z0 + z1) / 2f)
            rotateY(ang)
            block(-w / 2f, y - 0.004f, -len / 2f, w / 2f, y, len / 2f, c)
        }
    }

    private fun MeshBuilder.arc(cx: Float, cz: Float, r: Float, a0: Float, a1: Float, y: Float, steps: Int = 18) {
        for (i in 0 until steps) {
            val t0 = a0 + (a1 - a0) * i / steps
            val t1 = a0 + (a1 - a0) * (i + 1) / steps
            line(cx + sin(t0) * r, cz + cos(t0) * r, cx + sin(t1) * r, cz + cos(t1) * r, y)
        }
    }

    private fun MeshBuilder.court() {
        val c = Lot.COURT
        mat = Mat(roughness = 0.7f)
        slab(c.minX - 0.15f, c.minZ - 0.15f, c.maxX + 0.15f, c.maxZ + 0.15f, 0.025f, Palette.WHITE)
        slab(c.minX, c.minZ, c.maxX, c.maxZ, 0.035f, Palette.COURT)
        val rimX = Lot.RIM.x
        slab(rimX - 1.25f, c.minZ, rimX + 1.25f, -11.0f, 0.04f, Palette.COURT_KEY)
        val y = 0.046f
        line(rimX - 1.25f, -11f, rimX + 1.25f, -11f, y)
        line(rimX - 1.25f, c.minZ, rimX - 1.25f, -11f, y)
        line(rimX + 1.25f, c.minZ, rimX + 1.25f, -11f, y)
        arc(rimX, -11f, 1.2f, -PI.toFloat() / 2f, PI.toFloat() / 2f, y, 12)
        line(c.minX + 0.6f, c.minZ, c.minX + 0.6f, -14.6f, y)
        line(c.maxX - 0.6f, c.minZ, c.maxX - 0.6f, -14.6f, y)
        arc(rimX, Lot.RIM.z, 4.4f, -1.43f, 1.43f, y, 22)
        for (s in Lot.SHOT_SPOTS) {
            withMat(Mat.GLOSSY) { annulus(s.x, y + 0.002f, s.z, 0.28f, 0.36f, Palette.WHITE, 16) }
        }
    }

    private fun MeshBuilder.pitch() {
        val p = Lot.PITCH
        mat = Mat(roughness = 1f)
        var x = p.minX
        var i = 0
        while (x < p.maxX - 1e-3f) {
            val x1 = minOf(x + 1.3f, p.maxX)
            slab(x, p.minZ, x1, p.maxZ, 0.012f, if (i % 2 == 0) rgb(0x7FD65A) else rgb(0x6CC94D))
            x = x1
            i++
        }
        val y = 0.02f
        line(p.minX, p.minZ, p.maxX, p.minZ, y)
        line(p.minX, p.maxZ, p.maxX, p.maxZ, y)
        line(p.maxX, p.minZ, p.maxX, p.maxZ, y)
        line(Lot.GOAL_X, p.minZ, Lot.GOAL_X, p.maxZ, y)
        line(Lot.GOAL_X, 3.6f, -17.6f, 3.6f, y)
        line(Lot.GOAL_X, 10.4f, -17.6f, 10.4f, y)
        line(-17.6f, 3.6f, -17.6f, 10.4f, y)
        arc(-17.6f, 7f, 1.6f, 0.2f, PI.toFloat() - 0.2f, y, 14)
        withMat(Mat.GLOSSY) { disc(Lot.PENALTY_SPOT.x, y + 0.003f, Lot.PENALTY_SPOT.z, 0.13f, Palette.WHITE, 12) }
    }

    private fun MeshBuilder.pond() {
        val p = Lot.POND
        mat = Mat.MATTE
        cylinder(p.x, -0.3f, p.z, p.r + 0.15f, 0.31f, Palette.SOIL.darker(0.2f), sides = 28)
        cylinder(p.x, -0.29f, p.z, p.r, 0.31f, Palette.WATER_DEEP.darker(0.1f), sides = 28)
        withMat(Mat.WATER) { disc(p.x, 0.04f, p.z, p.r + 0.05f, Palette.WATER, 28) }
        val rng = Rng(77)
        for (k in 0 until 26) {
            val a = k / 26f * 2f * PI.toFloat()
            val r = p.r + 0.18f + rng.range(-0.05f, 0.08f)
            rock(p.x + cos(a) * r, p.z + sin(a) * r, rng.range(0.7f, 1.05f), 300 + k)
        }
        for (k in 0 until 5) {
            val a = rng.range(0f, 6.28f)
            val r = rng.range(0.6f, p.r - 0.6f)
            val lx = p.x + cos(a) * r
            val lz = p.z + sin(a) * r
            withMat(Mat(roughness = 0.5f)) { cylinder(lx, 0.04f, lz, 0.32f, 0.02f, Palette.LEAF.mix(Palette.LEAF_DEEP, 0.3f), sides = 10) }
            if (k % 2 == 0) withMat(Mat.GLOSSY) { sphere(lx + 0.08f, 0.1f, lz, 0.07f, Palette.PINK, rings = 4, segments = 8) }
        }
    }

    private fun MeshBuilder.gardenGround() {
        val g = Lot.GARDEN
        mat = Mat(roughness = 1f)
        slab(g.minX, g.minZ, g.maxX, g.maxZ, 0.01f, rgb(0x8FD66A))
        slab(g.minX + 0.4f, -8.4f, g.maxX - 0.4f, -6.3f, 0.02f, Palette.PATH)
    }

    fun scenery(): Mesh = mesh {
        fence()
        gardenStructures()
        props()
        sportsStatic()
        flowerBeds()
        for (t in Lot.TREES) {
            when (t.kind) {
                1 -> pineTree(t.pos.x, t.pos.z, t.scale, t.seed)
                2 -> blossomTree(t.pos.x, t.pos.z, t.scale, t.seed)
                else -> roundTree(t.pos.x, t.pos.z, t.scale, t.seed)
            }
        }
        bush(-6.9f, -7f, 0.9f, 1, Palette.PINK)
        bush(-6.9f, -14.8f, 1f, 2)
        bush(6.95f, -14.6f, 0.9f, 3, Palette.YELLOW)
        bush(6.95f, -7.4f, 0.85f, 4)
        bush(-2f, -16.9f, 0.8f, 5, Palette.LILAC)
        bush(2.6f, -16.9f, 0.9f, 6)
        bush(13.1f, 9.8f, 0.7f, 7, Palette.CORAL)
        bush(18.8f, 4.6f, 0.8f, 8)
        val rng = Rng(9)
        repeat(60) {
            val x = rng.range(-23f, 23f)
            val z = rng.range(-19.5f, 19.5f)
            if (freeForFlower(x, z)) flower(x, z, rng.range(0.16f, 0.26f), rng.pick(listOf(Palette.PINK, Palette.YELLOW, Palette.WHITE, Palette.LILAC, Palette.CORAL)), it)
        }

    }

    private fun freeForFlower(x: Float, z: Float): Boolean {
        val p = V2(x, z)
        val blocked = listOf(Lot.ROOM.inset(-1.2f), Lot.GARDEN.inset(-0.6f), Lot.COURT.inset(-0.6f), Lot.PITCH.inset(-0.4f), Lot.YARD.inset(-0.3f))
        if (blocked.any { p in it }) return false
        if (Lot.TRACK.distanceFrom(p) < Lot.TRACK.width / 2f + 0.5f) return false
        if (p.distanceTo(Lot.POND.center) < Lot.POND.r + 0.8f) return false
        if (abs(x) < 1.4f) return false
        return Lot.TREES.none { it.pos.distanceTo(p) < 0.9f }
    }

    private fun MeshBuilder.fence() {
        val f = Lot.FENCE
        mat = Mat(roughness = 0.55f)
        fun run(ax: Float, az: Float, bx: Float, bz: Float) {
            val len = V2(bx - ax, bz - az).length
            val dir = V2((bx - ax) / len, (bz - az) / len)
            val nPickets = (len / 0.26f).toInt()
            for (k in 0..nPickets) {
                val t = k * 0.26f
                if (t > len) break
                val x = ax + dir.x * t
                val z = az + dir.z * t
                if (abs(z - f.maxZ) < 0.01f && abs(x - Lot.GATE.x) < 1.3f) continue
                val post = k % 8 == 0
                val w = if (post) 0.1f else 0.06f
                val h = if (post) 1.0f else 0.82f
                block(x - w, 0f, z - w, x + w, h, z + w, Palette.OFFWHITE)
                withMat(Mat.SOFT) { group { translate(x, h, z); rotateY(45f); cone(0f, 0f, 0f, w * 1.3f, w * 1.4f, Palette.OFFWHITE, sides = 4, smooth = false) } }
            }
            for (rail in listOf(0.3f, 0.62f)) {
                if (abs(az - f.maxZ) < 0.01f && abs(bz - f.maxZ) < 0.01f) {
                    block(minOf(ax, bx), rail, az - 0.03f, Lot.GATE.x - 1.3f, rail + 0.07f, az + 0.03f, Palette.WHITE)
                    block(Lot.GATE.x + 1.3f, rail, az - 0.03f, maxOf(ax, bx), rail + 0.07f, az + 0.03f, Palette.WHITE)
                } else if (abs(ax - bx) < 0.01f) {
                    block(ax - 0.03f, rail, minOf(az, bz), ax + 0.03f, rail + 0.07f, maxOf(az, bz), Palette.WHITE)
                } else {
                    block(minOf(ax, bx), rail, az - 0.03f, maxOf(ax, bx), rail + 0.07f, az + 0.03f, Palette.WHITE)
                }
            }
        }
        run(f.minX, f.minZ, f.maxX, f.minZ)
        run(f.minX, f.maxZ, f.maxX, f.maxZ)
        run(f.minX, f.minZ, f.minX, f.maxZ)
        run(f.maxX, f.minZ, f.maxX, f.maxZ)
        val g = Lot.GATE
        for (side in listOf(-1f, 1f)) {
            block(g.x + side * 1.3f - 0.09f, 0f, g.z - 0.09f, g.x + side * 1.3f + 0.09f, 2.5f, g.z + 0.09f, Palette.WOOD_LIGHT)
        }
        group {
            translate(g.x, 2.5f, g.z)
            rotateX(90f)
            torus(0f, 0f, 0f, 1.3f, 0.07f, Palette.WOOD_LIGHT, 24, 6)
        }
        for (k in 0 until 9) {
            val a = (k / 8f) * PI.toFloat()
            blob(g.x + cos(a) * 1.3f, 2.5f + sin(a) * 1.3f, g.z, 0.22f, 0.2f, 0.22f, Palette.LEAF, 0, 0.12f, 500 + k)
            if (k % 2 == 1) withMat(Mat.GLOSSY) { sphere(g.x + cos(a) * 1.3f, 2.5f + sin(a) * 1.3f + 0.1f, g.z + 0.14f, 0.07f, Palette.PINK, rings = 4, segments = 8) }
        }
        for (side in listOf(-1f, 1f)) {
            group {
                translate(g.x + side * 1.22f, 0f, g.z)
                rotateY(if (side < 0) -18f else 18f)
                block(if (side < 0) 0f else -1.15f, 0.15f, -0.03f, if (side < 0) 1.15f else 0f, 0.95f, 0.03f, Palette.WOOD_LIGHT)
            }
        }
    }

    private fun MeshBuilder.gardenStructures() {
        val g = Lot.GARDEN
        mat = Mat(roughness = 0.7f)
        fun lowFence(ax: Float, az: Float, bx: Float, bz: Float) {
            val len = V2(bx - ax, bz - az).length
            val steps = (len / 1.2f).toInt().coerceAtLeast(1)
            for (k in 0..steps) {
                val x = ax + (bx - ax) * k / steps
                val z = az + (bz - az) * k / steps
                block(x - 0.06f, 0f, z - 0.06f, x + 0.06f, 0.7f, z + 0.06f, Palette.WOOD)
            }
            for (rail in listOf(0.28f, 0.55f)) {
                if (abs(ax - bx) < 0.01f) block(ax - 0.035f, rail, minOf(az, bz), ax + 0.035f, rail + 0.06f, maxOf(az, bz), Palette.WOOD_LIGHT)
                else block(minOf(ax, bx), rail, az - 0.035f, maxOf(ax, bx), rail + 0.06f, az + 0.035f, Palette.WOOD_LIGHT)
            }
        }
        lowFence(g.minX, g.minZ, g.maxX, g.minZ)
        lowFence(g.maxX, g.minZ, g.maxX, g.maxZ)
        lowFence(g.minX, g.minZ, g.minX, g.maxZ)
        lowFence(Lot.GARDEN_GATE.maxX, g.maxZ, g.maxX, g.maxZ)
        val half = Lot.PLOT_SIZE / 2f
        for (c in Lot.PLOTS) {
            val b = 0.09f
            block(c.x - half - b, 0f, c.z - half - b, c.x + half + b, 0.16f, c.z - half, Palette.WOOD)
            block(c.x - half - b, 0f, c.z + half, c.x + half + b, 0.16f, c.z + half + b, Palette.WOOD)
            block(c.x - half - b, 0f, c.z - half, c.x - half, 0.16f, c.z + half, Palette.WOOD)
            block(c.x + half, 0f, c.z - half, c.x + half + b, 0.16f, c.z + half, Palette.WOOD)
        }
        val t = Lot.GARDEN_TAP
        group {
            translate(t.x, 0f, t.z)
            cylinder(0f, 0f, 0f, 0.42f, 0.32f, Palette.STONE, sides = 12)
            cylinder(0f, 0.3f, 0f, 0.34f, 0.06f, Palette.WATER, sides = 12)
            withMat(Mat.GLOSSY) {
                cylinder(0f, 0.3f, -0.2f, 0.09f, 0.95f, Palette.SKY, sides = 12)
                group { translate(0f, 1.05f, -0.2f); rotateX(70f); cylinder(0f, 0f, 0f, 0.05f, 0.36f, Palette.SKY.darker(0.15f), sides = 10) }
                group { translate(0f, 1.25f, -0.2f); rotateX(-30f); cylinder(0f, 0f, 0f, 0.025f, 0.5f, Palette.INK, sides = 6) }
            }
            withMat(Mat.GLOSSY) { frustum(0.55f, 0f, 0.25f, 0.15f, 0.19f, 0.26f, Palette.YELLOW, sides = 12, capTop = false) }
        }
        group {
            translate(10.1f, 0f, -15.3f)
            mat = Mat(roughness = 0.7f)
            block(-0.45f, 0f, -0.3f, 0.45f, 0.5f, 0.3f, Palette.WOOD)
            block(-0.42f, 0.5f, -0.27f, 0.42f, 0.53f, 0.27f, Palette.WOOD_DARK)
            for (k in 0 until 3) withMat(Mat.GLOSSY) { sphere(-0.25f + k * 0.25f, 0.6f, 0f, 0.1f, listOf(Palette.ORANGE, Palette.RED, Palette.YELLOW)[k], rings = 5, segments = 8) }
        }
        group {
            translate(18.6f, 0f, -15.2f)
            frustum(0f, 0f, 0f, 0.05f, 0.04f, 1.5f, Palette.WOOD_DARK, sides = 6)
            block(-0.6f, 1.15f, -0.04f, 0.6f, 1.22f, 0.04f, Palette.WOOD_DARK)
            withMat(Mat.SOFT) {
                sphere(0f, 1.55f, 0f, 0.16f, Palette.PEACH, rings = 6, segments = 10)
                ellipsoid(0f, 1.18f, 0f, 0.2f, 0.28f, 0.15f, Palette.CORAL, rings = 6, segments = 10)
                cone(0f, 1.66f, 0f, 0.28f, 0.22f, Palette.YELLOW, sides = 10)
            }
        }
    }

    private fun MeshBuilder.props() {
        val m = Lot.MAILBOX
        group {
            translate(m.x, 0f, m.z)
            rotateY(180f)
            mat = Mat(roughness = 0.6f)
            block(-0.05f, 0f, -0.05f, 0.05f, 1.0f, 0.05f, Palette.WOOD)
            withMat(Mat.GLOSSY) {
                roundBox(0f, 1.12f, 0f, 0.32f, 0.26f, 0.48f, 0.12f, Palette.CORAL)
                block(0.16f, 1.1f, -0.06f, 0.19f, 1.42f, -0.02f, Palette.YELLOW)
                block(0.16f, 1.33f, -0.02f, 0.19f, 1.42f, 0.12f, Palette.YELLOW)
            }
        }
        group {
            translate(Lot.BENCH_BY_POND.x, 0f, Lot.BENCH_BY_POND.z)
            rotateY(180f)
            FurnitureModels.benchInto(this, Palette.WOOD_LIGHT, Palette.MINT)
        }
        for (x in listOf(-2.4f, 2.4f)) lampPost(x, 18.8f)
        lampPost(-1.6f, -2.3f)
        lampPost(1.6f, -2.3f)
    }

    private fun MeshBuilder.lampPost(x: Float, z: Float) {
        withMat(Mat.METAL) {
            cylinder(x, 0f, z, 0.12f, 0.12f, Palette.NAVY, sides = 10)
            cylinder(x, 0f, z, 0.05f, 2.3f, Palette.NAVY, sides = 8)
        }
        withMat(Mat.glow(rgb(0xFFE3A1), 2.4f)) { sphere(x, 2.42f, z, 0.17f, rgb(0xFFF4D6), rings = 6, segments = 10) }
        withMat(Mat.METAL) { cone(x, 2.52f, z, 0.2f, 0.16f, Palette.NAVY, sides = 10) }
    }

    private fun MeshBuilder.sportsStatic() {
        val p = Lot.HOOP_POLE
        withMat(Mat.METAL) {
            cylinder(p.x, 0f, p.z, 0.25f, 0.2f, Palette.NAVY, sides = 12)
            cylinder(p.x, 0f, p.z, 0.09f, 3.4f, Palette.NAVY, sides = 12)
            block(p.x - 0.05f, 3.25f, p.z, p.x + 0.05f, 3.35f, Lot.BACKBOARD_Z - 0.04f, Palette.NAVY)
        }
        mat = Mat.GLOSSY
        block(Lot.RIM.x - 0.9f, 2.72f, Lot.BACKBOARD_Z - 0.06f, Lot.RIM.x + 0.9f, 3.85f, Lot.BACKBOARD_Z, Palette.WHITE)
        block(Lot.RIM.x - 0.92f, 2.7f, Lot.BACKBOARD_Z - 0.07f, Lot.RIM.x + 0.92f, 2.74f, Lot.BACKBOARD_Z + 0.005f, Palette.CORAL)
        block(Lot.RIM.x - 0.92f, 3.83f, Lot.BACKBOARD_Z - 0.07f, Lot.RIM.x + 0.92f, 3.87f, Lot.BACKBOARD_Z + 0.005f, Palette.CORAL)
        block(Lot.RIM.x - 0.31f, 3.08f, Lot.BACKBOARD_Z - 0.001f, Lot.RIM.x + 0.31f, 3.5f, Lot.BACKBOARD_Z + 0.004f, Palette.CORAL)
        block(Lot.RIM.x - 0.26f, 3.13f, Lot.BACKBOARD_Z - 0.001f, Lot.RIM.x + 0.26f, 3.45f, Lot.BACKBOARD_Z + 0.006f, Palette.WHITE)
        withMat(Mat.METAL) {
            torus(Lot.RIM.x, Lot.RIM.y, Lot.RIM.z, Lot.RIM_RADIUS, 0.018f, Palette.ORANGE, 28, 6)
            block(Lot.RIM.x - 0.06f, Lot.RIM.y - 0.03f, Lot.BACKBOARD_Z, Lot.RIM.x + 0.06f, Lot.RIM.y + 0.01f, Lot.RIM.z - Lot.RIM_RADIUS, Palette.ORANGE)
        }
        mat = Mat.GLOSSY
        val gx = Lot.GOAL_X
        val gz = Lot.GOAL_Z
        val hw = Lot.GOAL_HALF
        val h = Lot.GOAL_HEIGHT
        val d = Lot.GOAL_DEPTH
        for (z in listOf(gz - hw, gz + hw)) {
            cylinder(gx, 0f, z, 0.06f, h, Palette.WHITE, sides = 10)
            cylinder(gx - d, 0f, z, 0.04f, h * 0.6f, Palette.WHITE, sides = 8)
            line3(V3(gx, h, z), V3(gx - d, h * 0.6f, z), 0.035f)
            line3(V3(gx - d, 0.02f, z), V3(gx, 0.02f, z), 0.03f)
        }
        group {
            translate(gx, h, gz - hw)
            rotateX(90f)
            cylinder(0f, 0f, 0f, 0.06f, hw * 2f, Palette.WHITE, sides = 10)
        }
        group {
            translate(gx - d, h * 0.6f, gz - hw)
            rotateX(90f)
            cylinder(0f, 0f, 0f, 0.04f, hw * 2f, Palette.WHITE, sides = 8)
        }
        mat = Mat.MATTE
    }

    private fun MeshBuilder.line3(a: V3, b: V3, r: Float) {
        val d = b - a
        val len = d.length
        val yaw = atan2(d.x, d.z) * 180f / PI.toFloat()
        val pitch = atan2(V2(d.x, d.z).length, d.y) * 180f / PI.toFloat()
        group {
            translate(a.x, a.y, a.z)
            rotateY(yaw)
            rotateX(pitch)
            cylinder(0f, 0f, 0f, r, len, Palette.WHITE, sides = 8)
        }
    }

    private fun MeshBuilder.flowerBeds() {
        val colors = listOf(Palette.PINK, Palette.YELLOW, Palette.CORAL, Palette.LILAC, Palette.WHITE)
        for ((bi, r) in Lot.FLOWER_BEDS.withIndex()) {
            mat = Mat(roughness = 0.8f)
            block(r.minX, 0f, r.minZ, r.maxX, 0.22f, r.maxZ, Palette.STONE)
            block(r.minX + 0.08f, 0.2f, r.minZ + 0.08f, r.maxX - 0.08f, 0.25f, r.maxZ - 0.08f, Palette.SOIL)
            var x = r.minX + 0.25f
            var k = 0
            while (x < r.maxX - 0.2f) {
                group {
                    translate(0f, 0.24f, 0f)
                    flower(x, (r.minZ + r.maxZ) / 2f + (if (k % 2 == 0) -0.15f else 0.15f), 0.28f, colors[(k + bi) % colors.size], k + bi * 10)
                }
                x += 0.3f
                k++
            }
        }
    }

    fun outskirts(): Mesh = mesh { outside() }

    private fun MeshBuilder.outside() {
        val houses = listOf(
            Triple(V2(-44f, -6f), 90f, rgb(0x7DB7FF)),
            Triple(V2(43f, -10f), -90f, rgb(0xFFC94D)),
            Triple(V2(-20f, 42f), 180f, rgb(0x7BE0B0)),
            Triple(V2(22f, 43f), 180f, rgb(0xC4A8FF)),
            Triple(V2(6f, -45f), 0f, rgb(0xFF9F7A)),
            Triple(V2(-28f, -42f), 20f, rgb(0x8FD3F4)),
        )
        for ((i, h) in houses.withIndex()) {
            val (p, rot, roof) = h
            group {
                translate(p.x, height(p.x, p.z) - 0.1f, p.z)
                rotateY(rot)
                neighborHouse(roof, i)
            }
        }
        val rng = Rng(42)
        repeat(70) { i ->
            val a = rng.range(0f, 6.28f)
            val r = rng.range(29f, 70f)
            val x = cos(a) * r
            val z = sin(a) * r
            if (houses.any { it.first.distanceTo(V2(x, z)) < 7f }) return@repeat
            group {
                translate(0f, height(x, z) - 0.1f, 0f)
                val s = rng.range(0.9f, 1.6f)
                if (i % 3 == 0) pineTree(x, z, s, 900 + i) else roundTree(x, z, s, 900 + i)
            }
        }
    }

    private fun MeshBuilder.neighborHouse(roof: Rgb, seed: Int) {
        mat = Mat.MATTE
        block(-3.5f, 0f, -3f, 3.5f, 3f, 3f, Palette.OFFWHITE)
        withMat(Mat(roughness = 0.7f)) {
            quad(V3(-3.9f, 2.9f, 3.4f), V3(3.9f, 2.9f, 3.4f), V3(3.9f, 5f, 0f), V3(-3.9f, 5f, 0f), roof)
            quad(V3(3.9f, 2.9f, -3.4f), V3(-3.9f, 2.9f, -3.4f), V3(-3.9f, 5f, 0f), V3(3.9f, 5f, 0f), roof)
        }
        tri(V3(-3.5f, 3f, -3f), V3(-3.5f, 3f, 3f), V3(-3.5f, 4.9f, 0f), Palette.OFFWHITE)
        tri(V3(3.5f, 3f, 3f), V3(3.5f, 3f, -3f), V3(3.5f, 4.9f, 0f), Palette.OFFWHITE)
        block(-0.6f, 0f, 3f, 0.6f, 2.1f, 3.06f, roof.darker(0.25f))
        withMat(Mat.GLASS) {
            block(-2.6f, 1.2f, 3f, -1.3f, 2.2f, 3.05f, Palette.GLASS)
            block(1.3f, 1.2f, 3f, 2.6f, 2.2f, 3.05f, Palette.GLASS)
        }
        bush(-2.2f, 3.8f, 0.7f, seed * 3 + 1, Palette.PINK)
        bush(2.3f, 3.8f, 0.7f, seed * 3 + 2)
    }

    fun clouds(seed: Int): Mesh = mesh {
        mat = Mat(roughness = 1f)
        val rng = Rng(seed.toLong())
        repeat(9) { i ->
            val a = i / 9f * 6.28f + rng.range(-0.3f, 0.3f)
            val r = rng.range(45f, 85f)
            cloud(cos(a) * r, rng.range(24f, 34f), sin(a) * r, rng.range(1.8f, 3f), seed * 10 + i)
        }
    }

    fun hoopNet(): Mesh = mesh {
        mat = Mat(roughness = 0.9f, doubleSided = true)
        val r0 = Lot.RIM_RADIUS - 0.01f
        val r1 = 0.15f
        val h = 0.42f
        val strands = 14
        for (k in 0 until strands) {
            val a0 = k / strands.toFloat() * 2f * PI.toFloat()
            val a1 = a0 + PI.toFloat() / strands
            val top = V3(cos(a0) * r0, 0f, sin(a0) * r0)
            val bottom = V3(cos(a1) * r1, -h, sin(a1) * r1)
            line3(top, bottom, 0.006f)
        }
        for (ring in 1..3) {
            val t = ring / 4f
            torus(0f, -h * t, 0f, r0 + (r1 - r0) * t, 0.005f, Palette.WHITE, 20, 3)
        }
    }

    fun goalNet(): Mesh = mesh {
        mat = Mat(roughness = 0.9f, alpha = 0.45f, doubleSided = true)
        val gz = Lot.GOAL_Z
        val hw = Lot.GOAL_HALF
        val h = Lot.GOAL_HEIGHT
        val d = Lot.GOAL_DEPTH
        val white = Palette.WHITE
        quad(V3(-d, 0f, gz - hw), V3(-d, 0f, gz + hw), V3(-d, h * 0.6f, gz + hw), V3(-d, h * 0.6f, gz - hw), white)
        quad(V3(0f, h, gz - hw), V3(-d, h * 0.6f, gz - hw), V3(-d, h * 0.6f, gz + hw), V3(0f, h, gz + hw), white)
        for (z in listOf(gz - hw, gz + hw)) {
            quad(V3(0f, 0f, z), V3(-d, 0f, z), V3(-d, h * 0.6f, z), V3(0f, h, z), white)
        }
        mat = Mat(roughness = 0.9f, doubleSided = true)
        val step = 0.3f
        var z = gz - hw
        while (z <= gz + hw + 1e-3f) {
            block(-d - 0.008f, 0f, z - 0.008f, -d + 0.008f, h * 0.6f, z + 0.008f, white)
            z += step
        }
        var y = 0f
        while (y <= h * 0.6f + 1e-3f) {
            block(-d - 0.008f, y - 0.008f, gz - hw, -d + 0.008f, y + 0.008f, gz + hw, white)
            y += step
        }
    }
}
