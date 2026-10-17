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

    private val GRASS_BASE = rgb(0x789F52)
    private val GRASS_DEEP = rgb(0x5D8443)
    private val GRASS_DRY = rgb(0xA3AE68)
    private val PAVER_COLOR = rgb(0xCFC6B6)
    private val CURB = rgb(0xA8A397)
    private val RUBBER = rgb(0xB85A45)
    private val LINE = rgb(0xF1EEE6)
    private val DECK_WOOD = rgb(0xA8774F)
    private val PICKET = rgb(0xF3F0EA)

    private fun lotHeight(x: Float, z: Float): Float {
        val p = Lot.POND
        val dx = x - p.x
        val dz = z - p.z
        val d = kotlin.math.sqrt(dx * dx + dz * dz)
        return -0.5f * smoothstep(p.r + 0.7f, p.r - 0.2f, d)
    }

    private val grassPaint = Paint { x, y, z ->
        val patch = valueNoise(x * 0.09f, z * 0.09f, 5)
        val fine = valueNoise(x * 0.35f, z * 0.35f, 6)
        val dry = smoothstep(0.62f, 0.85f, valueNoise(x * 0.05f + 7f, z * 0.05f, 8))
        val base = GRASS_BASE.mix(GRASS_DEEP, smoothstep(0.35f, 0.75f, patch) * 0.55f + fine * 0.15f).mix(GRASS_DRY, dry * 0.45f)
        val far = smoothstep(INNER, 90f, max(abs(x), abs(z)))
        base.mix(rgb(0x8DAF5A), far * 0.5f).mix(rgb(0xA9C27A), smoothstep(4f, 12f, y) * 0.35f)
    }

    fun ground(): Mesh = mesh {
        withMat(Mat.GRASS) {
            heightField(-INNER, -INNER, INNER, INNER, 64, 64, grassPaint, ::lotHeight)
            val cells = 18
            heightField(-OUTER, -OUTER, OUTER, -INNER, cells * 2, cells / 2, grassPaint, ::height)
            heightField(-OUTER, INNER, OUTER, OUTER, cells * 2, cells / 2, grassPaint, ::height)
            heightField(-OUTER, -INNER, -INNER, INNER, cells / 2, 8, grassPaint, ::height)
            heightField(INNER, -INNER, OUTER, INNER, cells / 2, 8, grassPaint, ::height)
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
        withMat(Mat.PAVER) {
            slab(-0.85f, -4.85f, 0.85f, -1.9f, 0.03f, PAVER_COLOR)
            slab(-0.85f, 17f, 0.85f, 19.9f, 0.03f, PAVER_COLOR)
            slab(0.85f, -4.4f, 10.6f, -3.4f, 0.03f, PAVER_COLOR)
            slab(9.2f, -6.0f, 10.6f, -3.4f, 0.03f, PAVER_COLOR)
        }
        withMat(Mat.STONE) {
            for (side in listOf(-1f, 1f)) {
                slab(side * 0.85f - 0.06f, -4.85f, side * 0.85f + 0.06f, -1.9f, 0.05f, CURB, 0.06f)
                slab(side * 0.85f - 0.06f, 17f, side * 0.85f + 0.06f, 19.9f, 0.05f, CURB, 0.06f)
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
                    puff(0f, 0.0f, 0f, 0.4f, 0.06f, 0.4f, rgb(0xB9B3A6).mix(rgb(0x9A9488), (i % 3) * 0.3f), subdivisions = 2, wobble = 0.1f, seed = 70 + i, floorY = -0.02f)
                }
            }
        }
    }

    private fun MeshBuilder.track() {
        val t = Lot.TRACK
        val pts = t.points
        val half = t.width / 2f
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
        withMat(Mat.STONE) {
            strip(-half - 0.14f, -half, 0.04f, CURB)
            strip(half, half + 0.14f, 0.04f, CURB)
        }
        withMat(Mat.TRACK) { strip(-half, half, 0.028f, RUBBER) }
        withMat(Mat.MATTE) {
            strip(-0.035f, 0.035f, 0.031f, LINE)
            strip(-half + 0.08f, -half + 0.13f, 0.031f, LINE)
            strip(half - 0.13f, half - 0.08f, 0.031f, LINE)
            val s = Lot.TRACK_START
            for (k in 0 until 8) {
                val z0 = s.z - half + k * (t.width / 8f)
                for (col in 0..1) {
                    val white = (k + col) % 2 == 0
                    block(s.x - 0.2f + col * 0.2f, 0.028f, z0, s.x + col * 0.2f, 0.034f, z0 + t.width / 8f, if (white) LINE else rgb(0x2E2B30))
                }
            }
        }
    }

    private fun MeshBuilder.line(x0: Float, z0: Float, x1: Float, z1: Float, y: Float, w: Float = 0.07f, c: Rgb = LINE) {
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
        withMat(Mat.STONE) { slab(c.minX - 0.2f, c.minZ - 0.2f, c.maxX + 0.2f, c.maxZ + 0.2f, 0.03f, CURB) }
        withMat(Mat.COURT) {
            slab(c.minX, c.minZ, c.maxX, c.maxZ, 0.04f, rgb(0x3E8A6A))
            val rimX = Lot.RIM.x
            slab(rimX - 1.25f, c.minZ, rimX + 1.25f, -11.0f, 0.044f, rgb(0x356FA6))
        }
        withMat(Mat.MATTE) {
            val rimX = Lot.RIM.x
            val y = 0.049f
            line(rimX - 1.25f, -11f, rimX + 1.25f, -11f, y)
            line(rimX - 1.25f, c.minZ, rimX - 1.25f, -11f, y)
            line(rimX + 1.25f, c.minZ, rimX + 1.25f, -11f, y)
            arc(rimX, -11f, 1.2f, -PI.toFloat() / 2f, PI.toFloat() / 2f, y, 12)
            line(c.minX + 0.6f, c.minZ, c.minX + 0.6f, -14.6f, y)
            line(c.maxX - 0.6f, c.minZ, c.maxX - 0.6f, -14.6f, y)
            arc(rimX, Lot.RIM.z, 4.4f, -1.43f, 1.43f, y, 22)
            for (s in Lot.SHOT_SPOTS) annulus(s.x, y + 0.002f, s.z, 0.28f, 0.36f, rgb(0xFFD54F), 16)
        }
    }

    private fun MeshBuilder.pitch() {
        val p = Lot.PITCH
        withMat(Mat.GRASS) {
            var x = p.minX
            var i = 0
            while (x < p.maxX - 1e-3f) {
                val x1 = minOf(x + 1.3f, p.maxX)
                slab(x, p.minZ, x1, p.maxZ, 0.012f, if (i % 2 == 0) rgb(0x77AD4B) else rgb(0x649B3F))
                x = x1
                i++
            }
        }
        withMat(Mat.MATTE) {
            val y = 0.02f
            line(p.minX, p.minZ, p.maxX, p.minZ, y)
            line(p.minX, p.maxZ, p.maxX, p.maxZ, y)
            line(p.maxX, p.minZ, p.maxX, p.maxZ, y)
            line(Lot.GOAL_X, p.minZ, Lot.GOAL_X, p.maxZ, y)
            line(Lot.GOAL_X, 3.6f, -17.6f, 3.6f, y)
            line(Lot.GOAL_X, 10.4f, -17.6f, 10.4f, y)
            line(-17.6f, 3.6f, -17.6f, 10.4f, y)
            arc(-17.6f, 7f, 1.6f, 0.2f, PI.toFloat() - 0.2f, y, 14)
            disc(Lot.PENALTY_SPOT.x, y + 0.003f, Lot.PENALTY_SPOT.z, 0.13f, LINE, 12)
        }
    }

    private fun MeshBuilder.pond() {
        val p = Lot.POND
        withMat(Mat.SAND) {
            lathe(p.x, p.z, listOf((p.r + 0.75f) to 0.0f, (p.r + 0.35f) to 0.035f, (p.r + 0.05f) to 0.0f, (p.r - 0.25f) to -0.22f), rgb(0xD8C49A), sides = 36)
        }
        withMat(Mat.SOIL) { disc(p.x, -0.3f, p.z, p.r + 0.2f, rgb(0x3D3428), 32) }
        withMat(Mat.WATER) { disc(p.x, 0.012f, p.z, p.r + 0.1f, rgb(0x2F7F9C), 36) }
        val rng = Rng(77)
        for (k in 0 until 22) {
            val a = k / 22f * 2f * PI.toFloat() + rng.range(-0.08f, 0.08f)
            val r = p.r + 0.45f + rng.range(-0.1f, 0.25f)
            rock(p.x + cos(a) * r, p.z + sin(a) * r, rng.range(0.55f, 1.0f), 300 + k)
            if (k % 2 == 0) grassTuft(p.x + cos(a + 0.12f) * (r + 0.3f), p.z + sin(a + 0.12f) * (r + 0.3f), 1.3f, 340 + k, 9)
        }
        for (k in 0 until 6) {
            val a = rng.range(0f, 6.28f)
            val r = rng.range(0.6f, p.r - 0.6f)
            val lx = p.x + cos(a) * r
            val lz = p.z + sin(a) * r
            withMat(Mat.LEAF) { annulus(lx, 0.012f, lz, 0.02f, 0.3f, rgb(0x4F8A3A), 14) }
            if (k % 2 == 0) withMat(Mat.SOFT) { sphere(lx + 0.08f, 0.06f, lz, 0.06f, Palette.PINK, rings = 4, segments = 8) }
        }
        withMat(Mat.SOFT) {
            for (k in 0 until 9) {
                val a = 0.4f + k * 0.11f
                val r = p.r - 0.15f
                val h = 0.7f + (k % 3) * 0.12f
                tube(listOf(V3(p.x + cos(a) * r, -0.05f, p.z + sin(a) * r), V3(p.x + cos(a) * r, h, p.z + sin(a) * r)), { 0.012f }, rgb(0x6C8A3C), sides = 4, caps = false)
                ellipsoid(p.x + cos(a) * r, h - 0.08f, p.z + sin(a) * r, 0.022f, 0.08f, 0.022f, rgb(0x6A4A2E), 3, 6)
            }
        }
    }

    private fun MeshBuilder.gardenGround() {
        val g = Lot.GARDEN
        withMat(Mat.GRASS) { slab(g.minX, g.minZ, g.maxX, g.maxZ, 0.01f, rgb(0x6E9E44)) }
        withMat(Mat.PAVER) { slab(g.minX + 0.4f, -8.4f, g.maxX - 0.4f, -6.3f, 0.02f, PAVER_COLOR) }
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
        val petals = listOf(Palette.PINK, Palette.YELLOW, Palette.WHITE, Palette.LILAC, Palette.CORAL, rgb(0xFF8A4C))
        repeat(26) { c ->
            val cx = rng.range(-23f, 23f)
            val cz = rng.range(-19.5f, 19.5f)
            if (!freeForFlower(cx, cz)) return@repeat
            val color = rng.pick(petals)
            repeat(5) {
                val x = cx + rng.range(-0.45f, 0.45f)
                val z = cz + rng.range(-0.45f, 0.45f)
                if (freeForFlower(x, z)) flower(x, z, rng.range(0.16f, 0.27f), color, c * 10 + it)
            }
            grassTuft(cx, cz, 1.1f, 600 + c, 8)
        }
        tufts(rng)
    }

    private fun MeshBuilder.tufts(rng: Rng) {
        val f = Lot.FENCE
        var k = 0
        fun near(x: Float, z: Float, s: Float = 1f) {
            if (freeForFlower(x, z) || (abs(x) > f.maxX - 0.6f || abs(z) > f.maxZ - 0.6f)) grassTuft(x, z, s * rng.range(0.8f, 1.25f), 1000 + k++, 7)
        }
        var x = f.minX + 0.3f
        while (x < f.maxX - 0.3f) {
            near(x + rng.range(-0.2f, 0.2f), f.minZ + rng.range(0.15f, 0.4f))
            near(x + rng.range(-0.2f, 0.2f), f.maxZ - rng.range(0.15f, 0.4f))
            x += rng.range(0.7f, 1.3f)
        }
        var z = f.minZ + 0.3f
        while (z < f.maxZ - 0.3f) {
            near(f.minX + rng.range(0.15f, 0.4f), z + rng.range(-0.2f, 0.2f))
            near(f.maxX - rng.range(0.15f, 0.4f), z + rng.range(-0.2f, 0.2f))
            z += rng.range(0.7f, 1.3f)
        }
        for (t in Lot.TREES) {
            repeat(5) {
                val a = rng.range(0f, 6.28f)
                val r = rng.range(0.4f, 0.9f) * t.scale
                grassTuft(t.pos.x + cos(a) * r, t.pos.z + sin(a) * r, rng.range(0.9f, 1.3f), 1500 + k++, 7)
            }
        }
        repeat(220) {
            val px = rng.range(-23.5f, 23.5f)
            val pz = rng.range(-19.5f, 19.5f)
            if (freeForFlower(px, pz)) grassTuft(px, pz, rng.range(0.7f, 1.15f), 2000 + k++, 6)
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
        mat = Mat.PAINTED_WOOD
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
                block(x - w, 0f, z - w, x + w, h, z + w, PICKET)
                group { translate(x, h, z); rotateY(45f); cone(0f, 0f, 0f, w * 1.3f, w * 1.4f, PICKET, sides = 4, smooth = false) }
            }
            for (rail in listOf(0.3f, 0.62f)) {
                if (abs(az - f.maxZ) < 0.01f && abs(bz - f.maxZ) < 0.01f) {
                    block(minOf(ax, bx), rail, az - 0.03f, Lot.GATE.x - 1.3f, rail + 0.07f, az + 0.03f, PICKET.darker(0.04f))
                    block(Lot.GATE.x + 1.3f, rail, az - 0.03f, maxOf(ax, bx), rail + 0.07f, az + 0.03f, PICKET.darker(0.04f))
                } else if (abs(ax - bx) < 0.01f) {
                    block(ax - 0.03f, rail, minOf(az, bz), ax + 0.03f, rail + 0.07f, maxOf(az, bz), PICKET.darker(0.04f))
                } else {
                    block(minOf(ax, bx), rail, az - 0.03f, maxOf(ax, bx), rail + 0.07f, az + 0.03f, PICKET.darker(0.04f))
                }
            }
        }
        run(f.minX, f.minZ, f.maxX, f.minZ)
        run(f.minX, f.maxZ, f.maxX, f.maxZ)
        run(f.minX, f.minZ, f.minX, f.maxZ)
        run(f.maxX, f.minZ, f.maxX, f.maxZ)
        val g = Lot.GATE
        for (side in listOf(-1f, 1f)) {
            withMat(Mat.DECK) { block(g.x + side * 1.3f - 0.09f, 0f, g.z - 0.09f, g.x + side * 1.3f + 0.09f, 2.5f, g.z + 0.09f, DECK_WOOD) }
        }
        withMat(Mat.DECK) {
            group {
                translate(g.x, 2.5f, g.z)
                rotateX(90f)
                torus(0f, 0f, 0f, 1.3f, 0.07f, DECK_WOOD, 24, 6)
            }
        }
        withMat(Mat.LEAF) {
            for (k in 0 until 11) {
                val a = (k / 10f) * PI.toFloat()
                puff(g.x + cos(a) * 1.3f, 2.5f + sin(a) * 1.3f, g.z, 0.24f, 0.21f, 0.24f, rgb(0x5E9B3A).mix(rgb(0x82B653), (k % 3) * 0.4f), subdivisions = 1, wobble = 0.16f, seed = 500 + k)
            }
        }
        for (k in 0 until 11) {
            if (k % 2 == 1) {
                val a = (k / 10f) * PI.toFloat()
                withMat(Mat.SOFT) { sphere(g.x + cos(a) * 1.3f, 2.5f + sin(a) * 1.3f + 0.12f, g.z + 0.17f, 0.06f, Palette.PINK, rings = 4, segments = 8) }
            }
        }
        for (side in listOf(-1f, 1f)) {
            group {
                translate(g.x + side * 1.22f, 0f, g.z)
                rotateY(if (side < 0) -18f else 18f)
                withMat(Mat.DECK) { block(if (side < 0) 0f else -1.15f, 0.15f, -0.03f, if (side < 0) 1.15f else 0f, 0.95f, 0.03f, DECK_WOOD.lighter(0.1f)) }
            }
        }
    }

    private fun MeshBuilder.gardenStructures() {
        val g = Lot.GARDEN
        mat = Mat.DECK
        fun lowFence(ax: Float, az: Float, bx: Float, bz: Float) {
            val len = V2(bx - ax, bz - az).length
            val steps = (len / 1.2f).toInt().coerceAtLeast(1)
            for (k in 0..steps) {
                val x = ax + (bx - ax) * k / steps
                val z = az + (bz - az) * k / steps
                block(x - 0.06f, 0f, z - 0.06f, x + 0.06f, 0.7f, z + 0.06f, DECK_WOOD.darker(0.08f))
            }
            for (rail in listOf(0.28f, 0.55f)) {
                if (abs(ax - bx) < 0.01f) block(ax - 0.035f, rail, minOf(az, bz), ax + 0.035f, rail + 0.06f, maxOf(az, bz), DECK_WOOD)
                else block(minOf(ax, bx), rail, az - 0.035f, maxOf(ax, bx), rail + 0.06f, az + 0.035f, DECK_WOOD)
            }
        }
        lowFence(g.minX, g.minZ, g.maxX, g.minZ)
        lowFence(g.maxX, g.minZ, g.maxX, g.maxZ)
        lowFence(g.minX, g.minZ, g.minX, g.maxZ)
        lowFence(Lot.GARDEN_GATE.maxX, g.maxZ, g.maxX, g.maxZ)
        val half = Lot.PLOT_SIZE / 2f
        for (c in Lot.PLOTS) {
            val b = 0.09f
            block(c.x - half - b, 0f, c.z - half - b, c.x + half + b, 0.16f, c.z - half, DECK_WOOD)
            block(c.x - half - b, 0f, c.z + half, c.x + half + b, 0.16f, c.z + half + b, DECK_WOOD)
            block(c.x - half - b, 0f, c.z - half, c.x - half, 0.16f, c.z + half, DECK_WOOD)
            block(c.x + half, 0f, c.z - half, c.x + half + b, 0.16f, c.z + half, DECK_WOOD)
        }
        val t = Lot.GARDEN_TAP
        group {
            translate(t.x, 0f, t.z)
            withMat(Mat.STONE) { cylinder(0f, 0f, 0f, 0.42f, 0.32f, rgb(0xB8B2A6), sides = 16) }
            withMat(Mat.WATER) { cylinder(0f, 0.3f, 0f, 0.34f, 0.04f, rgb(0x5AA6C4), sides = 16) }
            withMat(Mat.GLOSSY) {
                cylinder(0f, 0.3f, -0.2f, 0.09f, 0.95f, Palette.SKY, sides = 12)
                group { translate(0f, 1.05f, -0.2f); rotateX(70f); cylinder(0f, 0f, 0f, 0.05f, 0.36f, Palette.SKY.darker(0.15f), sides = 10) }
                group { translate(0f, 1.25f, -0.2f); rotateX(-30f); cylinder(0f, 0f, 0f, 0.025f, 0.5f, Palette.INK, sides = 6) }
            }
            withMat(Mat.GLOSSY) { frustum(0.55f, 0f, 0.25f, 0.15f, 0.19f, 0.26f, Palette.YELLOW, sides = 12, capTop = false) }
        }
        group {
            translate(10.1f, 0f, -15.3f)
            mat = Mat.DECK
            block(-0.45f, 0f, -0.3f, 0.45f, 0.5f, 0.3f, DECK_WOOD)
            block(-0.42f, 0.5f, -0.27f, 0.42f, 0.53f, 0.27f, DECK_WOOD.darker(0.2f))
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
            FurnitureModels.benchInto(this, rgb(0x9C6E4A), rgb(0x2C2C2E))
        }
        for (x in listOf(-2.4f, 2.4f)) lampPost(x, 18.8f)
        lampPost(-1.6f, -2.3f)
        lampPost(1.6f, -2.3f)
    }

    private fun MeshBuilder.lampPost(x: Float, z: Float) {
        val iron = rgb(0x2B2F33)
        withMat(Mat.METAL.copy(roughness = 0.45f, metallic = 0.7f)) {
            lathe(x, z, listOf(0.13f to 0f, 0.13f to 0.08f, 0.09f to 0.12f, 0.06f to 0.3f, 0.045f to 0.34f, 0.045f to 2.2f, 0.07f to 2.25f, 0.07f to 2.28f, 0f to 2.29f), iron, sides = 12)
            lathe(x, z, listOf(0.14f to 2.29f, 0.16f to 2.33f, 0.0f to 2.335f), iron, sides = 12)
            lathe(x, z, listOf(0.22f to 2.6f, 0.22f to 2.63f, 0.1f to 2.72f, 0.03f to 2.83f, 0f to 2.85f), iron, sides = 12)
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2f + PI.toFloat() / 4f
                cylinder(x + cos(a) * 0.17f, 2.33f, z + sin(a) * 0.17f, 0.012f, 0.28f, iron, sides = 5)
            }
        }
        withMat(Mat.glow(rgb(0xFFD590), 3.2f)) { lathe(x, z, listOf(0.15f to 2.335f, 0.185f to 2.6f, 0f to 2.605f), rgb(0xFFF1CF), sides = 12) }
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
            withMat(Mat.BRICK) { roundBox((r.minX + r.maxX) / 2f, 0.11f, (r.minZ + r.maxZ) / 2f, r.maxX - r.minX, 0.22f, r.maxZ - r.minZ, 0.025f, rgb(0xB06A50)) }
            withMat(Mat.SOIL) { block(r.minX + 0.08f, 0.2f, r.minZ + 0.08f, r.maxX - 0.08f, 0.235f, r.maxZ - 0.08f, rgb(0x5B4330)) }
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
        val wall = rgb(0xEFE6D6)
        mat = Mat.PLASTER
        block(-3.5f, 0f, -3f, 3.5f, 3f, 3f, wall)
        tri(V3(-3.5f, 3f, -3f), V3(-3.5f, 3f, 3f), V3(-3.5f, 4.9f, 0f), wall)
        tri(V3(3.5f, 3f, 3f), V3(3.5f, 3f, -3f), V3(3.5f, 4.9f, 0f), wall)
        withMat(Mat.ROOF) {
            quad(V3(-3.9f, 2.9f, 3.4f), V3(3.9f, 2.9f, 3.4f), V3(3.9f, 5f, 0f), V3(-3.9f, 5f, 0f), roof)
            quad(V3(3.9f, 2.9f, -3.4f), V3(-3.9f, 2.9f, -3.4f), V3(-3.9f, 5f, 0f), V3(3.9f, 5f, 0f), roof)
        }
        withMat(Mat.WOOD) { block(-0.6f, 0f, 3f, 0.6f, 2.1f, 3.06f, roof.darker(0.35f)) }
        withMat(Mat.STONE) { block(-3.6f, -0.2f, -3.1f, 3.6f, 0.25f, 3.1f, rgb(0xA9A398)) }
        withMat(Mat.GLASS) {
            block(-2.6f, 1.2f, 3f, -1.3f, 2.2f, 3.05f, Palette.GLASS)
            block(1.3f, 1.2f, 3f, 2.6f, 2.2f, 3.05f, Palette.GLASS)
        }
        bush(-2.2f, 3.8f, 0.7f, seed * 3 + 1, Palette.PINK)
        bush(2.3f, 3.8f, 0.7f, seed * 3 + 2)
    }

    const val SKY_RADIUS = 320f
    private const val SKY_AZIMUTH = -144.6f

    fun skyDome(): Mesh = mesh { withMat(Mat.SKY_DOME) { dome(SKY_RADIUS, 14, 40, 100f, SKY_AZIMUTH, Rgb.WHITE) } }

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
