package com.cozyhome.game.sports

import com.cozyhome.game.math.V3
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan

const val GRAVITY = 9.81f

class Ball(val radius: Float) {
    var pos = V3.ZERO
    var vel = V3.ZERO
    var spin = 0f
    var bounces = 0
        private set

    fun place(p: V3) {
        pos = p
        vel = V3.ZERO
        spin = 0f
        bounces = 0
    }

    fun launch(v: V3) {
        vel = v
        bounces = 0
    }

    fun integrate(dt: Float, drag: Float, restitution: Float, friction: Float) {
        vel = V3(vel.x * (1f - drag * dt), vel.y - GRAVITY * dt, vel.z * (1f - drag * dt))
        pos += vel * dt
        if (pos.y < radius) {
            pos = V3(pos.x, radius, pos.z)
            if (vel.y < -0.6f) bounces++
            vel = if (vel.y < -0.6f) V3(vel.x * friction, -vel.y * restitution, vel.z * friction)
            else V3(vel.x * (1f - 2.2f * dt), 0f, vel.z * (1f - 2.2f * dt))
        }
        spin += vel.length * dt / radius
    }

    val isResting: Boolean get() = pos.y <= radius + 1e-3f && vel.length < 0.25f
}

fun reflect(v: V3, n: V3, restitution: Float): V3 {
    val vn = v dot n
    if (vn >= 0f) return v
    return v - n * ((1f + restitution) * vn)
}

fun launchVelocity(from: V3, to: V3, angleDeg: Float): V3 {
    val dx = to.x - from.x
    val dz = to.z - from.z
    val d = sqrt(dx * dx + dz * dz).coerceAtLeast(0.01f)
    val h = to.y - from.y
    val a = angleDeg * (Math.PI / 180.0).toFloat()
    val denom = 2f * cos(a) * cos(a) * (d * tan(a) - h)
    val v = if (denom > 1e-4f) sqrt(GRAVITY * d * d / denom) else 10f
    val horizontal = v * cos(a)
    return V3(dx / d * horizontal, v * kotlin.math.sin(a), dz / d * horizontal)
}

fun velocityForTime(from: V3, to: V3, time: Float): V3 {
    val t = time.coerceAtLeast(0.05f)
    return V3((to.x - from.x) / t, (to.y - from.y) / t + 0.5f * GRAVITY * t, (to.z - from.z) / t)
}
