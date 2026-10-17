package com.cozyhome.game.models

import com.cozyhome.game.activities.BugDex
import com.cozyhome.game.activities.FishDex
import com.cozyhome.game.game.Emote
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object ActivityModels {
    val ROD_HANDLE = V3(0.22f, 1.0f, 0.3f)
    val ROD_TIP = V3(0.22f, 1.95f, 1.55f)

    fun rod(): Mesh = mesh {
        val dir = (ROD_TIP - ROD_HANDLE)
        val len = dir.length
        val n = dir * (1f / len)
        val pts = (0..6).map { k -> ROD_HANDLE + n * (len * k / 6f) + V3(0f, -sin(k / 6f * PI.toFloat()) * 0.04f, 0f) }
        withMat(Mat.WOOD_FINE) { tube(pts.subList(0, 2), { 0.022f }, rgb(0xC9A27B), sides = 8, caps = true) }
        withMat(Mat.GLOSSY.copy(roughness = 0.25f)) { tube(smoothPath(pts.subList(1, pts.size), 2), { t -> 0.012f - 0.008f * t }, rgb(0x2F5F8F), sides = 6, caps = true) }
        withMat(Mat(roughness = 0.3f, metallic = 0.9f)) {
            val r = ROD_HANDLE + n * 0.18f + V3(0f, -0.05f, 0f)
            group { translate(r.x, r.y, r.z); rotateZ(90f); cylinder(0f, -0.03f, 0f, 0.035f, 0.06f, rgb(0xC8CCD0), sides = 12) }
        }
    }

    fun bobber(): Mesh = mesh {
        withMat(Mat.GLOSSY) {
            sphere(0f, 0.03f, 0f, 0.045f, rgb(0xE8433A), 6, 10)
            ellipsoid(0f, 0.0f, 0f, 0.046f, 0.03f, 0.046f, Palette.WHITE, 5, 10)
            cylinder(0f, 0.06f, 0f, 0.006f, 0.06f, rgb(0x2E2A30), sides = 5)
        }
    }

    fun line(): Mesh = mesh { withMat(Mat.UNLIT) { cylinder(0f, 0f, 0f, 0.004f, 1f, rgb(0xEDEDED), sides = 4, caps = false) } }

    fun fish(id: String): Mesh = mesh {
        val sp = FishDex[id] ?: FishDex.all.first()
        when (id) {
            "old_boot" -> boot()
            "treasure" -> chest()
            else -> fishBody(rgb(sp.body), rgb(sp.belly), id == "pufferfish", id == "koi" || id == "golden_koi", id == "catfish")
        }
    }

    private fun MeshBuilder.fishBody(body: Rgb, belly: Rgb, puffer: Boolean, koi: Boolean, whiskers: Boolean) {
        val paint = Paint { x, y, z ->
            val under = smoothstep(0.02f, -0.06f, y)
            var c = body.mix(belly, under)
            if (koi) {
                val spot = sin(z * 30f) * cos(x * 40f + z * 12f)
                if (spot > 0.35f && y > -0.01f) c = belly
            }
            c.mix(body.darker(0.25f), smoothstep(0.05f, 0.09f, y) * 0.5f)
        }
        withMat(Mat.GLOSSY.copy(roughness = 0.22f)) {
            warpSphere(paint, 10, 16) { x, y, z ->
                val taper = if (z < 0f) 1f - 0.7f * smoothstep(0f, 1f, -z) else 1f - 0.25f * smoothstep(0.4f, 1f, z)
                val r = if (puffer) 0.11f else 0.07f
                V3(x * (if (puffer) 0.1f else 0.045f) * taper, y * r * taper, z * 0.17f)
            }
        }
        withMat(Mat.GLOSSY.copy(roughness = 0.3f, doubleSided = true)) {
            fill(V3(0f, 0f, -0.15f), listOf(V3(0f, 0.07f, -0.26f), V3(0f, 0.0f, -0.22f), V3(0f, -0.07f, -0.26f)), body.darker(0.08f))
            fill(V3(0f, 0.05f, 0.03f), listOf(V3(0f, 0.1f, -0.02f), V3(0f, 0.11f, -0.08f), V3(0f, 0.055f, -0.1f)), body.darker(0.12f))
            for (s in listOf(-1f, 1f)) fill(V3(s * 0.04f, -0.02f, 0.06f), listOf(V3(s * 0.08f, -0.05f, 0.02f), V3(s * 0.07f, -0.06f, 0.0f)), belly.darker(0.1f))
        }
        withMat(Mat.EYE) { for (s in listOf(-1f, 1f)) { sphere(s * 0.036f, 0.018f, 0.11f, 0.012f, Palette.WHITE, 4, 8); sphere(s * 0.042f, 0.018f, 0.115f, 0.007f, rgb(0x111111), 3, 6) } }
        if (whiskers) withMat(Mat.SOFT) { for (s in listOf(-1f, 1f)) tube(listOf(V3(s * 0.02f, -0.01f, 0.16f), V3(s * 0.07f, -0.03f, 0.2f), V3(s * 0.11f, -0.06f, 0.18f)), { 0.003f }, body.darker(0.3f), sides = 3, caps = false) }
        if (puffer) withMat(Mat.SOFT) {
            for (k in 0 until 24) {
                val a = k * 2.4f
                val yv = -0.8f + (k % 6) / 3f
                val r = kotlin.math.sqrt((1f - yv * yv).coerceAtLeast(0f))
                val p = V3(cos(a) * r * 0.1f, yv * 0.1f, sin(a) * r * 0.16f)
                cone(p.x, p.y, p.z, 0.008f, 0.02f, body.lighter(0.2f), sides = 4)
            }
        }
    }

    private fun MeshBuilder.fill(center: V3, rim: List<V3>, c: Rgb) {
        for (i in 0 until rim.size - 1) tri(center, rim[i], rim[i + 1], c)
        tri(center, rim.last(), rim.first(), c)
    }

    private fun MeshBuilder.boot() {
        withMat(Mat.LEATHER.copy(roughness = 0.8f)) {
            roundBox(0f, 0.12f, -0.02f, 0.1f, 0.24f, 0.12f, 0.04f, rgb(0x5A4632))
            roundBox(0f, 0.03f, 0.06f, 0.1f, 0.07f, 0.26f, 0.035f, rgb(0x5A4632))
            roundBox(0f, 0.0f, 0.06f, 0.11f, 0.02f, 0.27f, 0.008f, rgb(0x2E241A))
        }
        withMat(Mat.LEAF_THIN) { group { translate(0.02f, 0.24f, -0.02f); rotateX(-30f); leafBlade(0.1f, 0.03f, rgb(0x5E8C45)) } }
    }

    private fun MeshBuilder.chest() {
        withMat(Mat.WOOD) {
            roundBox(0f, 0.07f, 0f, 0.26f, 0.14f, 0.18f, 0.01f, rgb(0x8A5A2E))
            group { translate(0f, 0.14f, 0f); rotateZ(90f); cylinder(0f, -0.13f, 0f, 0.09f, 0.26f, rgb(0x7A4E26), sides = 12) }
        }
        withMat(Mat(roughness = 0.3f, metallic = 0.9f)) {
            for (x in listOf(-0.1f, 0.1f)) roundBox(x, 0.12f, 0f, 0.02f, 0.25f, 0.19f, 0.005f, rgb(0xC9A15A))
            roundBox(0f, 0.12f, 0.092f, 0.04f, 0.05f, 0.01f, 0.005f, rgb(0xE8C35A))
        }
        withMat(Mat.glow(rgb(0xFFD54F), 1.4f)) { for (k in 0 until 5) sphere(-0.06f + k * 0.03f, 0.2f, 0.02f, 0.02f, rgb(0xF2C94C), 4, 6) }
    }

    fun bug(id: String): Mesh = mesh {
        val sp = BugDex[id] ?: BugDex.all.first()
        val wing = rgb(sp.wing)
        val body = rgb(sp.body)
        when (id) {
            "butterfly", "blue_morpho", "moth" -> {
                withMat(Mat.SOFT) { capsule(0f, -0.03f, 0f, 0.008f, 0.06f, body, sides = 6, capRings = 2) }
                val wingPaint = Paint { x, _, z -> wing.mix(body, smoothstep(0.03f, 0.06f, abs(x) + abs(z) * 0.4f) * 0.55f) }
                withMat(Mat(roughness = 0.6f, doubleSided = true)) {
                    for (s in listOf(-1f, 1f)) {
                        ellipseFlat(s * 0.035f, 0f, 0.012f, 0.035f, 0.03f, wingPaint)
                        ellipseFlat(s * 0.028f, 0f, -0.02f, 0.026f, 0.022f, wingPaint)
                    }
                }
                withMat(Mat.SOFT) { for (s in listOf(-1f, 1f)) tube(listOf(V3(s * 0.003f, 0.0f, 0.03f), V3(s * 0.012f, 0.012f, 0.05f)), { 0.0015f }, body, sides = 3, caps = false) }
            }
            "bee" -> {
                val stripes = Paint { _, _, z -> if (((z + 0.1f) * 60f).toInt() % 2 == 0) body else rgb(0x2A2420) }
                withMat(Mat.SOFT) { ellipsoid(0f, 0f, 0f, 0.016f, 0.016f, 0.026f, stripes, 6, 10) }
                withMat(Mat(roughness = 0.2f, alpha = 0.55f, doubleSided = true)) { for (s in listOf(-1f, 1f)) ellipseFlat(s * 0.018f, 0.012f, 0f, 0.016f, 0.01f, wing) }
            }
            "dragonfly" -> {
                withMat(Mat.GLOSSY) { capsule(0f, 0f, -0.05f, 0.006f, 0.11f, body, sides = 6, capRings = 2) }
                withMat(Mat(roughness = 0.2f, alpha = 0.5f, doubleSided = true)) {
                    for (s in listOf(-1f, 1f)) { ellipseFlat(s * 0.045f, 0f, 0.02f, 0.045f, 0.01f, wing); ellipseFlat(s * 0.042f, 0f, 0.0f, 0.042f, 0.01f, wing) }
                }
            }
            "firefly" -> {
                withMat(Mat.SOFT) { ellipsoid(0f, 0f, 0f, 0.008f, 0.008f, 0.016f, body, 4, 8) }
                withMat(Mat.glow(rgb(0xF7F27A), 4f)) { sphere(0f, 0f, -0.014f, 0.01f, rgb(0xF7F2A0), 4, 8) }
            }
            "ladybug" -> {
                val dots = Paint { x, _, z -> if (((x * 90f).toInt() + (z * 70f).toInt()) % 3 == 0) rgb(0x1E1E1E) else wing }
                withMat(Mat.GLOSSY) {
                    warpSphere(dots, 6, 10) { x, y, z -> V3(x * 0.02f, kotlin.math.max(0f, y) * 0.016f, z * 0.024f) }
                    sphere(0f, 0.004f, 0.022f, 0.009f, body, 4, 6)
                }
            }
            "snail" -> {
                withMat(Mat.SOFT) { capsule(0f, 0f, -0.03f, 0.009f, 0.07f, rgb(0xC9B48A), sides = 6, capRings = 2) }
                withMat(Mat.GLOSSY) {
                    for (k in 0 until 3) torus(0f, 0.022f + k * 0.006f, 0f, 0.016f - k * 0.004f, 0.008f, wing.darker(k * 0.12f), 12, 5)
                }
            }
            else -> {
                withMat(Mat.GLOSSY.copy(roughness = 0.15f)) {
                    ellipsoid(0f, 0.006f, 0f, 0.016f, 0.01f, 0.028f, wing, 6, 10)
                    sphere(0f, 0.004f, 0.026f, 0.009f, body, 4, 6)
                }
                if (id == "stag_beetle") withMat(Mat.GLOSSY) { for (s in listOf(-1f, 1f)) tube(listOf(V3(s * 0.004f, 0.006f, 0.032f), V3(s * 0.012f, 0.01f, 0.05f), V3(s * 0.004f, 0.012f, 0.06f)), { 0.003f }, wing, sides = 4, caps = true) }
            }
        }
    }

    private fun MeshBuilder.ellipseFlat(cx: Float, y: Float, cz: Float, rx: Float, rz: Float, paint: Paint) {
        val n = 12
        val c = V3(cx, y, cz)
        for (k in 0 until n) {
            val a0 = k * 2f * PI.toFloat() / n
            val a1 = (k + 1) * 2f * PI.toFloat() / n
            tri(c, V3(cx + cos(a1) * rx, y, cz + sin(a1) * rz), V3(cx + cos(a0) * rx, y, cz + sin(a0) * rz), paint)
        }
    }

    fun net(): Mesh = mesh {
        withMat(Mat.WOOD_FINE) { tube(listOf(V3(0f, -0.36f, 0.06f), V3(0f, -0.2f, 0.5f), V3(0f, -0.05f, 0.95f)), { 0.012f }, rgb(0xC9A27B), sides = 6, caps = true) }
        withMat(Mat(roughness = 0.4f, metallic = 0.7f)) { group { translate(0f, 0.0f, 1.08f); rotateX(70f); torus(0f, 0f, 0f, 0.14f, 0.008f, rgb(0xC8CCD0), 16, 4) } }
        withMat(Mat(roughness = 0.9f, alpha = 0.5f, doubleSided = true)) {
            group { translate(0f, 0.0f, 1.08f); rotateX(70f); lathe(0f, 0f, listOf(0.14f to 0f, 0.1f to -0.12f, 0.03f to -0.22f, 0f to -0.24f), Palette.WHITE, 12) }
        }
    }

    fun emoteBubble(e: Emote): Mesh = mesh {
        withMat(Mat(roughness = 0.4f)) {
            puff(0f, 0f, 0f, 0.16f, 0.14f, 0.05f, Palette.WHITE, subdivisions = 2, wobble = 0.02f, seed = 1)
            cone(0f, -0.2f, 0f, 0.04f, 0.08f, Palette.WHITE, sides = 6)
        }
        val z = 0.05f
        withMat(Mat.glow(when (e) { Emote.HEART -> rgb(0xFF5C7A); Emote.SAD -> rgb(0x5AA7F2); Emote.THINK -> rgb(0x9B7BFF); else -> rgb(0xFFC93C) }, 1.2f)) {
            when (e) {
                Emote.HEART -> { sphere(-0.03f, 0.02f, z, 0.04f, rgb(0xFF5C7A), 5, 8); sphere(0.03f, 0.02f, z, 0.04f, rgb(0xFF5C7A), 5, 8); group { translate(0f, -0.02f, z); rotateZ(45f); roundBox(0f, 0f, 0f, 0.06f, 0.06f, 0.04f, 0.01f, rgb(0xFF5C7A)) } }
                Emote.SAD -> { sphere(0f, -0.01f, z, 0.035f, rgb(0x5AA7F2), 5, 8); cone(0f, 0.02f, z, 0.033f, 0.05f, rgb(0x5AA7F2), sides = 8) }
                Emote.THINK -> { torus(0f, 0.03f, z, 0.03f, 0.012f, rgb(0x9B7BFF), 12, 5); sphere(0f, -0.05f, z, 0.014f, rgb(0x9B7BFF), 4, 6) }
                Emote.DANCE -> { cylinder(-0.02f, -0.04f, z, 0.008f, 0.09f, rgb(0xFFC93C), sides = 5); sphere(-0.035f, -0.045f, z, 0.022f, rgb(0xFFC93C), 4, 8); cylinder(0.04f, -0.02f, z, 0.008f, 0.08f, rgb(0xFFC93C), sides = 5); sphere(0.025f, -0.025f, z, 0.022f, rgb(0xFFC93C), 4, 8) }
                else -> for (k in 0 until 5) { val a = k * 72f * PI.toFloat() / 180f; sphere(cos(a) * 0.045f, sin(a) * 0.045f, z, 0.02f, rgb(0xFFC93C), 4, 6) }
            }
        }
    }

    fun steam(): Mesh = mesh { withMat(Mat(roughness = 1f, alpha = 0.45f)) { puff(0f, 0f, 0f, 0.07f, 0.06f, 0.07f, Palette.WHITE, subdivisions = 1, wobble = 0.15f, seed = 2) } }

    fun pot(): Mesh = mesh {
        withMat(Mat(roughness = 0.25f, metallic = 0.9f)) {
            lathe(0f, 0f, listOf(0f to 0f, 0.1f to 0f, 0.11f to 0.01f, 0.115f to 0.12f, 0.12f to 0.125f, 0.105f to 0.125f, 0f to 0.03f), rgb(0xC8CCD0), 18)
            for (s in listOf(-1f, 1f)) group { translate(s * 0.13f, 0.1f, 0f); rotateZ(90f); torus(0f, 0f, 0f, 0.02f, 0.006f, rgb(0x2C2C2E), 10, 4) }
        }
        withMat(Mat.GLOSSY) { disc(0f, 0.1f, 0f, 0.105f, rgb(0xD96F3A), 16) }
    }
}
