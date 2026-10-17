package com.cozyhome.game.world

import com.cozyhome.game.math.V2
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

sealed interface Solid

data class Rect(val minX: Float, val minZ: Float, val maxX: Float, val maxZ: Float) : Solid {
    val width: Float get() = maxX - minX
    val depth: Float get() = maxZ - minZ
    val center: V2 get() = V2((minX + maxX) / 2f, (minZ + maxZ) / 2f)

    operator fun contains(p: V2) = p.x in minX..maxX && p.z in minZ..maxZ
    fun inset(d: Float) = Rect(minX + d, minZ + d, maxX - d, maxZ - d)
    fun overlaps(o: Rect, eps: Float = 1e-4f) =
        minX < o.maxX - eps && maxX > o.minX + eps && minZ < o.maxZ - eps && maxZ > o.minZ + eps

    fun closest(p: V2) = V2(p.x.coerceIn(minX, maxX), p.z.coerceIn(minZ, maxZ))

    fun segmentHits(a: V2, b: V2): Boolean {
        var t0 = 0f
        var t1 = 1f
        val dx = b.x - a.x
        val dz = b.z - a.z
        fun clip(p: Float, q: Float): Boolean {
            if (abs(p) < 1e-9f) return q >= 0f
            val r = q / p
            if (p < 0f) {
                if (r > t1) return false
                if (r > t0) t0 = r
            } else {
                if (r < t0) return false
                if (r < t1) t1 = r
            }
            return true
        }
        return clip(-dx, a.x - minX) && clip(dx, maxX - a.x) && clip(-dz, a.z - minZ) && clip(dz, maxZ - a.z)
    }

    companion object {
        fun around(center: V2, w: Float, d: Float) =
            Rect(center.x - w / 2f, center.z - d / 2f, center.x + w / 2f, center.z + d / 2f)
    }
}

data class Circle(val x: Float, val z: Float, val r: Float) : Solid {
    val center: V2 get() = V2(x, z)
}

class CollisionWorld(private val bounds: Rect) {
    private var staticSolids: List<Solid> = emptyList()
    private var dynamicSolids: List<Solid> = emptyList()

    fun setStatic(solids: List<Solid>) {
        staticSolids = solids
    }

    fun setDynamic(solids: List<Solid>) {
        dynamicSolids = solids
    }

    fun move(from: V2, delta: V2, radius: Float): V2 {
        val steps = max(1, ceil(delta.length / (radius * 0.5f)).toInt())
        val step = delta * (1f / steps)
        var p = from
        repeat(steps) { p = resolve(p + step, radius) }
        return p
    }

    fun resolve(start: V2, radius: Float): V2 {
        var p = start
        repeat(4) {
            var moved = false
            for (s in staticSolids) {
                val q = push(p, s, radius)
                if (q !== p) { p = q; moved = true }
            }
            for (s in dynamicSolids) {
                val q = push(p, s, radius)
                if (q !== p) { p = q; moved = true }
            }
            p = V2(p.x.coerceIn(bounds.minX + radius, bounds.maxX - radius), p.z.coerceIn(bounds.minZ + radius, bounds.maxZ - radius))
            if (!moved) return p
        }
        return p
    }

    fun isFree(p: V2, radius: Float): Boolean =
        staticSolids.none { penetrates(p, it, radius) } && dynamicSolids.none { penetrates(p, it, radius) }

    private fun penetrates(p: V2, s: Solid, r: Float): Boolean = when (s) {
        is Rect -> (p - s.closest(p)).length < r - 1e-4f
        is Circle -> (p - s.center).length < s.r + r - 1e-4f
    }

    private fun push(p: V2, s: Solid, r: Float): V2 = when (s) {
        is Rect -> {
            val q = s.closest(p)
            val d = p - q
            val len = d.length
            when {
                len >= r -> p
                len > 1e-5f -> q + d * (r / len)
                else -> {
                    val left = p.x - s.minX
                    val right = s.maxX - p.x
                    val top = p.z - s.minZ
                    val bottom = s.maxZ - p.z
                    val m = min(min(left, right), min(top, bottom))
                    when (m) {
                        left -> V2(s.minX - r, p.z)
                        right -> V2(s.maxX + r, p.z)
                        top -> V2(p.x, s.minZ - r)
                        else -> V2(p.x, s.maxZ + r)
                    }
                }
            }
        }
        is Circle -> {
            val d = p - s.center
            val len = d.length
            val target = s.r + r
            when {
                len >= target -> p
                len > 1e-5f -> s.center + d * (target / len)
                else -> V2(s.x + target, s.z)
            }
        }
    }
}
