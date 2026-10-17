package com.cozyhome.game.models

import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object PropModels {
    fun basketball(): Mesh = mesh {
        mat = Mat(roughness = 0.65f)
        sphere(0f, 0f, 0f, 0.115f, rgb(0xFF8A3D), rings = 10, segments = 16)
        mat = Mat.MATTE
        torus(0f, 0f, 0f, 0.116f, 0.005f, Palette.INK, 24, 3)
        group { rotateX(90f); torus(0f, 0f, 0f, 0.116f, 0.005f, Palette.INK, 24, 3) }
        group { rotateZ(90f); torus(0f, 0f, 0f, 0.116f, 0.005f, Palette.INK, 24, 3) }
    }

    fun soccerBall(): Mesh = mesh {
        mat = Mat(roughness = 0.5f)
        val (verts, faces) = com.cozyhome.game.mesh.MeshBuilder.icosphere(1)
        for ((i, f) in faces.withIndex()) {
            val a = verts[f[0]] * 0.11f
            var b = verts[f[1]] * 0.11f
            var c = verts[f[2]] * 0.11f
            val n = (b - a) cross (c - a)
            if ((n dot (a + b + c)) < 0f) { val t = b; b = c; c = t }
            tri(a, b, c, if (i % 4 == 0) Palette.INK else Palette.WHITE)
        }
    }

    fun waterDrop(): Mesh = mesh {
        mat = Mat.glow(rgb(0x7FD6FF), 1.6f)
        lathe(0f, 0f, listOf(0f to -0.12f, 0.07f to -0.09f, 0.09f to -0.03f, 0.06f to 0.05f, 0f to 0.14f), rgb(0x8FDCFF), 14)
    }

    fun star(): Mesh = mesh {
        mat = Mat.glow(rgb(0xFFD54F), 2.4f)
        val pts = 5
        for (k in 0 until pts * 2) {
            val a0 = k / (pts * 2f) * 2f * PI.toFloat()
            val a1 = (k + 1) / (pts * 2f) * 2f * PI.toFloat()
            val r0 = if (k % 2 == 0) 0.2f else 0.09f
            val r1 = if (k % 2 == 0) 0.09f else 0.2f
            val p0 = V3(sin(a0) * r0, cos(a0) * r0, 0f)
            val p1 = V3(sin(a1) * r1, cos(a1) * r1, 0f)
            tri(V3(0f, 0f, 0.05f), p0, p1, rgb(0xFFE27A))
            tri(V3(0f, 0f, -0.05f), p1, p0, rgb(0xFFD54F))
        }
    }

    fun ring(radius: Float = 0.7f): Mesh = mesh {
        mat = Mat.glow(rgb(0xFFF1A8), 1.6f)
        annulus(0f, 0.05f, 0f, radius - 0.08f, radius, rgb(0xFFF3B0), 32)
    }

    fun selection(w: Float, d: Float, valid: Boolean): Mesh = mesh {
        val c = if (valid) rgb(0x6EE7B7) else rgb(0xFF6B6B)
        mat = Mat.glow(c, 1.3f)
        val t = 0.06f
        block(-w / 2f, 0.01f, -d / 2f, w / 2f, 0.03f, -d / 2f + t, c)
        block(-w / 2f, 0.01f, d / 2f - t, w / 2f, 0.03f, d / 2f, c)
        block(-w / 2f, 0.01f, -d / 2f, -w / 2f + t, 0.03f, d / 2f, c)
        block(w / 2f - t, 0.01f, -d / 2f, w / 2f, 0.03f, d / 2f, c)
        mat = Mat(roughness = 1f, alpha = 0.28f)
        block(-w / 2f, 0.005f, -d / 2f, w / 2f, 0.012f, d / 2f, c)
        mat = Mat.glow(c, 1.3f)
        group {
            translate(0f, 0.02f, d / 2f + 0.18f)
            tri(V3(-0.14f, 0f, -0.08f), V3(0f, 0f, 0.12f), V3(0.14f, 0f, -0.08f), c)
            tri(V3(-0.14f, 0f, -0.08f), V3(0.14f, 0f, -0.08f), V3(0f, 0f, 0.12f), c)
        }
    }

    fun checkpoint(next: Boolean): Mesh = mesh {
        val c = if (next) rgb(0xFFD54F) else rgb(0xC4B5FD)
        mat = if (next) Mat.glow(c, 1.8f) else Mat.GLOSSY
        for (s in listOf(-1.25f, 1.25f)) cylinder(s, 0f, 0f, 0.07f, 2.2f, c, sides = 10)
        group { translate(-1.25f, 2.2f, 0f); rotateZ(-90f); cylinder(0f, 0f, 0f, 0.07f, 2.5f, c, sides = 10) }
        withMat(Mat.GLOSSY) {
            for (k in 0 until 5) {
                val x = -1.0f + k * 0.5f
                tri(V3(x - 0.18f, 2.13f, 0f), V3(x + 0.18f, 2.13f, 0f), V3(x, 1.85f, 0f), if (k % 2 == 0) Palette.CORAL else Palette.SKY)
                tri(V3(x + 0.18f, 2.13f, 0f), V3(x - 0.18f, 2.13f, 0f), V3(x, 1.85f, 0f), if (k % 2 == 0) Palette.CORAL else Palette.SKY)
            }
        }
    }

    fun zzz(): Mesh = mesh {
        mat = Mat.glow(rgb(0xC4B5FD), 1.4f)
        for (k in 0 until 3) {
            val s = 0.06f + k * 0.03f
            val y = k * 0.22f
            val x = k * 0.12f
            block(x - s, y + s * 0.8f, -0.01f, x + s, y + s, 0.01f, rgb(0xDDD6FE))
            block(x - s, y - s, -0.01f, x + s, y - s * 0.8f, 0.01f, rgb(0xDDD6FE))
            group {
                translate(x, y, 0f)
                rotateZ(-45f)
                block(-s * 1.3f, -0.012f, -0.01f, s * 1.3f, 0.012f, 0.01f, rgb(0xDDD6FE))
            }
        }
    }

    fun coinBurst(): Mesh = mesh {
        mat = Mat.glow(rgb(0xFFD54F), 1.4f)
        group { rotateX(90f); cylinder(0f, -0.02f, 0f, 0.12f, 0.04f, rgb(0xFFD54F), sides = 16) }
    }
}
