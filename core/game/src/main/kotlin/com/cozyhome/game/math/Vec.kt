package com.cozyhome.game.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val DEG = (PI / 180.0).toFloat()

data class V2(val x: Float, val z: Float) {
    operator fun plus(o: V2) = V2(x + o.x, z + o.z)
    operator fun minus(o: V2) = V2(x - o.x, z - o.z)
    operator fun times(s: Float) = V2(x * s, z * s)
    operator fun unaryMinus() = V2(-x, -z)
    infix fun dot(o: V2) = x * o.x + z * o.z
    infix fun cross(o: V2) = x * o.z - z * o.x
    val length: Float get() = sqrt(x * x + z * z)
    val lengthSq: Float get() = x * x + z * z
    fun normalized(): V2 = length.let { if (it < 1e-6f) ZERO else V2(x / it, z / it) }
    fun distanceTo(o: V2) = (this - o).length
    fun rotated(deg: Float): V2 {
        val c = cos(deg * DEG)
        val s = sin(deg * DEG)
        return V2(x * c - z * s, x * s + z * c)
    }

    companion object {
        val ZERO = V2(0f, 0f)
    }
}

data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = V3(x * s, y * s, z * s)
    operator fun unaryMinus() = V3(-x, -y, -z)
    infix fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length: Float get() = sqrt(x * x + y * y + z * z)
    fun normalized(): V3 = length.let { if (it < 1e-8f) UP else V3(x / it, y / it, z / it) }
    val xz: V2 get() = V2(x, z)

    companion object {
        val ZERO = V3(0f, 0f, 0f)
        val UP = V3(0f, 1f, 0f)
    }
}

fun v3(xz: V2, y: Float) = V3(xz.x, y, xz.z)

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

fun lerp(a: V3, b: V3, t: Float) = V3(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t))

fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

fun damp(current: Float, target: Float, sharpness: Float, dt: Float): Float =
    lerp(current, target, 1f - kotlin.math.exp(-sharpness * dt))

fun damp(current: V3, target: V3, sharpness: Float, dt: Float): V3 =
    lerp(current, target, 1f - kotlin.math.exp(-sharpness * dt))

fun wrapDeg(deg: Float): Float {
    var d = deg % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

fun dampAngle(current: Float, target: Float, sharpness: Float, dt: Float): Float =
    current + wrapDeg(target - current) * (1f - kotlin.math.exp(-sharpness * dt))

fun yawOf(dir: V2): Float = atan2(dir.x, dir.z) / DEG

fun dirOfYaw(yawDeg: Float) = V2(sin(yawDeg * DEG), cos(yawDeg * DEG))

fun approxZero(v: Float) = abs(v) < 1e-5f

fun hash01(x: Int, y: Int, seed: Int = 0): Float {
    var h = x * 374761393 + y * 668265263 + seed * 2147483647
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0x7fffffff) / 2147483647f
}

fun valueNoise(x: Float, z: Float, seed: Int = 0): Float {
    val ix = kotlin.math.floor(x).toInt()
    val iz = kotlin.math.floor(z).toInt()
    val fx = x - ix
    val fz = z - iz
    val sx = fx * fx * (3 - 2 * fx)
    val sz = fz * fz * (3 - 2 * fz)
    val a = hash01(ix, iz, seed)
    val b = hash01(ix + 1, iz, seed)
    val c = hash01(ix, iz + 1, seed)
    val d = hash01(ix + 1, iz + 1, seed)
    return lerp(lerp(a, b, sx), lerp(c, d, sx), sz)
}

class Rng(seed: Long) {
    private var state = seed xor 0x5DEECE66DL
    fun nextFloat(): Float {
        state = state * 6364136223846793005L + 1442695040888963407L
        return ((state ushr 40).toInt() and 0xFFFFFF) / 16777216f
    }

    fun range(a: Float, b: Float) = a + (b - a) * nextFloat()
    fun chance(p: Float) = nextFloat() < p
    fun int(bound: Int) = (nextFloat() * bound).toInt().coerceAtMost(bound - 1)
    fun <T> pick(list: List<T>): T = list[(nextFloat() * list.size).toInt().coerceAtMost(list.size - 1)]
}
