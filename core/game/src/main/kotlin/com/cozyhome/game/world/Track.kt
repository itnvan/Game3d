package com.cozyhome.game.world

import com.cozyhome.game.math.V2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Track(val outline: Rect, val radius: Float, val width: Float) {
    val points: List<V2>
    private val cumulative: FloatArray
    val length: Float

    init {
        val pts = ArrayList<V2>()
        val x0 = outline.minX; val x1 = outline.maxX
        val z0 = outline.minZ; val z1 = outline.maxZ
        val r = radius
        val arcSteps = 10
        fun arc(cx: Float, cz: Float, fromDeg: Float, toDeg: Float) {
            for (i in 0..arcSteps) {
                val a = (fromDeg + (toDeg - fromDeg) * i / arcSteps) * (PI / 180.0).toFloat()
                pts += V2(cx + cos(a) * r, cz + sin(a) * r)
            }
        }
        pts += V2((x0 + x1) / 2f, z1)
        arc(x0 + r, z1 - r, 90f, 180f)
        arc(x0 + r, z0 + r, 180f, 270f)
        arc(x1 - r, z0 + r, 270f, 360f)
        arc(x1 - r, z1 - r, 0f, 90f)
        pts += V2((x0 + x1) / 2f, z1)
        points = pts
        cumulative = FloatArray(pts.size)
        for (i in 1 until pts.size) cumulative[i] = cumulative[i - 1] + pts[i].distanceTo(pts[i - 1])
        length = cumulative.last()
    }

    fun pointAt(distance: Float): V2 {
        val s = ((distance % length) + length) % length
        var i = 1
        while (i < cumulative.size - 1 && cumulative[i] < s) i++
        val seg = cumulative[i] - cumulative[i - 1]
        val t = if (seg < 1e-6f) 0f else (s - cumulative[i - 1]) / seg
        return points[i - 1] + (points[i] - points[i - 1]) * t
    }

    fun tangentAt(distance: Float): V2 = (pointAt(distance + 0.5f) - pointAt(distance - 0.5f)).normalized()

    fun distanceFrom(p: V2): Float {
        var best = Float.MAX_VALUE
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val ab = b - a
            val t = (((p - a) dot ab) / ab.lengthSq.coerceAtLeast(1e-6f)).coerceIn(0f, 1f)
            val d = (p - (a + ab * t)).length
            if (d < best) best = d
        }
        return best
    }

    fun isOnTrack(p: V2) = distanceFrom(p) <= width / 2f + 0.15f
}
