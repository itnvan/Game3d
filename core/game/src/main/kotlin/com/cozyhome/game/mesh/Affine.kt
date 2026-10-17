package com.cozyhome.game.mesh

import com.cozyhome.game.math.DEG
import com.cozyhome.game.math.V3
import kotlin.math.cos
import kotlin.math.sin

class Affine private constructor(private val m: FloatArray) {

    operator fun times(o: Affine): Affine {
        val a = m
        val b = o.m
        val r = FloatArray(12)
        for (row in 0 until 3) {
            val i = row * 4
            r[i] = a[i] * b[0] + a[i + 1] * b[4] + a[i + 2] * b[8]
            r[i + 1] = a[i] * b[1] + a[i + 1] * b[5] + a[i + 2] * b[9]
            r[i + 2] = a[i] * b[2] + a[i + 1] * b[6] + a[i + 2] * b[10]
            r[i + 3] = a[i] * b[3] + a[i + 1] * b[7] + a[i + 2] * b[11] + a[i + 3]
        }
        return Affine(r)
    }

    fun point(x: Float, y: Float, z: Float) = V3(
        m[0] * x + m[1] * y + m[2] * z + m[3],
        m[4] * x + m[5] * y + m[6] * z + m[7],
        m[8] * x + m[9] * y + m[10] * z + m[11],
    )

    val determinant: Float
        get() = m[0] * (m[5] * m[10] - m[6] * m[9]) -
            m[1] * (m[4] * m[10] - m[6] * m[8]) +
            m[2] * (m[4] * m[9] - m[5] * m[8])

    private val cofactor: FloatArray by lazy {
        val s = if (determinant < 0f) -1f else 1f
        floatArrayOf(
            s * (m[5] * m[10] - m[6] * m[9]), s * -(m[4] * m[10] - m[6] * m[8]), s * (m[4] * m[9] - m[5] * m[8]),
            s * -(m[1] * m[10] - m[2] * m[9]), s * (m[0] * m[10] - m[2] * m[8]), s * -(m[0] * m[9] - m[1] * m[8]),
            s * (m[1] * m[6] - m[2] * m[5]), s * -(m[0] * m[6] - m[2] * m[4]), s * (m[0] * m[5] - m[1] * m[4]),
        )
    }

    fun normal(x: Float, y: Float, z: Float): V3 {
        val c = cofactor
        return V3(
            c[0] * x + c[1] * y + c[2] * z,
            c[3] * x + c[4] * y + c[5] * z,
            c[6] * x + c[7] * y + c[8] * z,
        ).normalized()
    }

    companion object {
        val IDENTITY = Affine(floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f))

        fun translate(x: Float, y: Float, z: Float) =
            Affine(floatArrayOf(1f, 0f, 0f, x, 0f, 1f, 0f, y, 0f, 0f, 1f, z))

        fun scale(x: Float, y: Float, z: Float) =
            Affine(floatArrayOf(x, 0f, 0f, 0f, 0f, y, 0f, 0f, 0f, 0f, z, 0f))

        fun rotateX(deg: Float): Affine {
            val c = cos(deg * DEG)
            val s = sin(deg * DEG)
            return Affine(floatArrayOf(1f, 0f, 0f, 0f, 0f, c, -s, 0f, 0f, s, c, 0f))
        }

        fun rotateY(deg: Float): Affine {
            val c = cos(deg * DEG)
            val s = sin(deg * DEG)
            return Affine(floatArrayOf(c, 0f, s, 0f, 0f, 1f, 0f, 0f, -s, 0f, c, 0f))
        }

        fun rotateZ(deg: Float): Affine {
            val c = cos(deg * DEG)
            val s = sin(deg * DEG)
            return Affine(floatArrayOf(c, -s, 0f, 0f, s, c, 0f, 0f, 0f, 0f, 1f, 0f))
        }
    }
}
