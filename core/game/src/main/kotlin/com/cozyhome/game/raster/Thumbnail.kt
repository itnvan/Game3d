package com.cozyhome.game.raster

import com.cozyhome.game.math.DEG
import com.cozyhome.game.mesh.Mesh
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Thumbnail {

    fun render(mesh: Mesh, size: Int = 160, yawDeg: Float = -32f, pitchDeg: Float = 26f, supersample: Int = 2): IntArray {
        val s = size * supersample
        val color = FloatArray(s * s * 3)
        val alpha = FloatArray(s * s)
        val depth = FloatArray(s * s) { Float.NEGATIVE_INFINITY }
        val cy = cos(yawDeg * DEG); val sy = sin(yawDeg * DEG)
        val cp = cos(pitchDeg * DEG); val sp = sin(pitchDeg * DEG)

        fun rx(x: Float, y: Float, z: Float): Float = x * cy + z * sy
        fun rzTmp(x: Float, z: Float): Float = -x * sy + z * cy
        fun ry(y: Float, z2: Float): Float = y * cp - z2 * sp
        fun rz(y: Float, z2: Float): Float = y * sp + z2 * cp

        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in mesh.parts) {
            var i = 0
            while (i < p.positions.size) {
                val x = p.positions[i]; val y = p.positions[i + 1]; val z = p.positions[i + 2]
                val vx = rx(x, y, z)
                val vy = ry(y, rzTmp(x, z))
                minX = min(minX, vx); maxX = max(maxX, vx)
                minY = min(minY, vy); maxY = max(maxY, vy)
                i += 3
            }
        }
        if (minX > maxX) return IntArray(size * size)
        val span = max(maxX - minX, maxY - minY).coerceAtLeast(1e-3f)
        val scale = s * 0.84f / span
        val ox = s / 2f - (minX + maxX) / 2f * scale
        val oy = s / 2f + (minY + maxY) / 2f * scale

        val lx = -0.45f; val ly = 0.8f; val lz = 0.4f
        val ll = sqrt(lx * lx + ly * ly + lz * lz)
        val light = floatArrayOf(lx / ll, ly / ll, lz / ll)

        for (p in mesh.parts) {
            val n = p.vertexCount
            val sx = FloatArray(n); val syy = FloatArray(n); val sz = FloatArray(n)
            val shade = FloatArray(n * 3)
            val emissive = p.mat.emissive?.linear()
            for (v in 0 until n) {
                val x = p.positions[v * 3]; val y = p.positions[v * 3 + 1]; val z = p.positions[v * 3 + 2]
                val z2 = rzTmp(x, z)
                sx[v] = ox + rx(x, y, z) * scale
                syy[v] = oy - ry(y, z2) * scale
                sz[v] = rz(y, z2)
                val nx = p.normals[v * 3]; val ny = p.normals[v * 3 + 1]; val nz = p.normals[v * 3 + 2]
                val nvx = rx(nx, ny, nz)
                val nz2 = rzTmp(nx, nz)
                val nvy = ry(ny, nz2)
                val nvz = rz(ny, nz2)
                val worldDot = max(0f, nx * light[0] + ny * light[1] + nz * light[2])
                val facing = max(0f, nvz)
                val rim = (1f - facing).pow(3f) * 0.12f
                val lit = 0.42f + 0.68f * worldDot + rim + 0.06f * nvy.coerceAtLeast(0f)
                for (c in 0 until 3) {
                    var value = p.colors[v * 4 + c] * lit
                    if (emissive != null) value = max(value, p.colors[v * 4 + c] * 0.6f + emissive[c] * 0.5f)
                    shade[v * 3 + c] = value
                }
            }
            val a = p.mat.alpha
            var t = 0
            while (t < p.indices.size) {
                val i0 = p.indices[t]; val i1 = p.indices[t + 1]; val i2 = p.indices[t + 2]
                t += 3
                val x0 = sx[i0]; val y0 = syy[i0]
                val x1 = sx[i1]; val y1 = syy[i1]
                val x2 = sx[i2]; val y2 = syy[i2]
                val area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
                if (area >= 0f && !p.mat.doubleSided) continue
                if (kotlin.math.abs(area) < 1e-6f) continue
                val bx0 = max(0, min(x0, min(x1, x2)).toInt())
                val bx1 = min(s - 1, max(x0, max(x1, x2)).toInt() + 1)
                val by0 = max(0, min(y0, min(y1, y2)).toInt())
                val by1 = min(s - 1, max(y0, max(y1, y2)).toInt() + 1)
                val inv = 1f / area
                for (py in by0..by1) {
                    val fy = py + 0.5f
                    for (px in bx0..bx1) {
                        val fx = px + 0.5f
                        val w0 = ((x1 - fx) * (y2 - fy) - (x2 - fx) * (y1 - fy)) * inv
                        val w1 = ((x2 - fx) * (y0 - fy) - (x0 - fx) * (y2 - fy)) * inv
                        val w2 = 1f - w0 - w1
                        if (w0 < 0f || w1 < 0f || w2 < 0f) continue
                        val d = w0 * sz[i0] + w1 * sz[i1] + w2 * sz[i2]
                        val idx = py * s + px
                        if (d <= depth[idx]) continue
                        val r = w0 * shade[i0 * 3] + w1 * shade[i1 * 3] + w2 * shade[i2 * 3]
                        val g = w0 * shade[i0 * 3 + 1] + w1 * shade[i1 * 3 + 1] + w2 * shade[i2 * 3 + 1]
                        val b = w0 * shade[i0 * 3 + 2] + w1 * shade[i1 * 3 + 2] + w2 * shade[i2 * 3 + 2]
                        if (a >= 0.99f) {
                            depth[idx] = d
                            color[idx * 3] = r; color[idx * 3 + 1] = g; color[idx * 3 + 2] = b
                            alpha[idx] = 1f
                        } else {
                            color[idx * 3] = color[idx * 3] * (1f - a) + r * a
                            color[idx * 3 + 1] = color[idx * 3 + 1] * (1f - a) + g * a
                            color[idx * 3 + 2] = color[idx * 3 + 2] * (1f - a) + b * a
                            alpha[idx] = max(alpha[idx], a)
                        }
                    }
                }
            }
        }

        val out = IntArray(size * size)
        val ss = supersample * supersample
        for (y in 0 until size) for (x in 0 until size) {
            var r = 0f; var g = 0f; var b = 0f; var al = 0f
            for (j in 0 until supersample) for (i in 0 until supersample) {
                val idx = (y * supersample + j) * s + (x * supersample + i)
                val aa = alpha[idx]
                r += color[idx * 3] * aa; g += color[idx * 3 + 1] * aa; b += color[idx * 3 + 2] * aa
                al += aa
            }
            if (al <= 0f) continue
            val ar = al / ss
            out[y * size + x] = (to8(ar, false) shl 24) or (to8(r / al) shl 16) or (to8(g / al) shl 8) or to8(b / al)
        }
        return out
    }

    private fun to8(v: Float, gamma: Boolean = true): Int {
        val c = if (gamma) {
            val x = v.coerceIn(0f, 1f)
            if (x <= 0.0031308f) x * 12.92f else 1.055f * x.pow(1f / 2.4f) - 0.055f
        } else v.coerceIn(0f, 1f)
        return (c * 255f + 0.5f).toInt().coerceIn(0, 255)
    }
}
