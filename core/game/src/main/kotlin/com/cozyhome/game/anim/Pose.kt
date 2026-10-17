package com.cozyhome.game.anim

import com.cozyhome.game.math.damp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

enum class Anim { IDLE, WALK, RUN, SIT, SLEEP, WATER, HARVEST, SHOOT_AIM, SHOOT_RELEASE, KICK, CHEER, SAD, WAVE, BOUNCE, SWING, KEEPER }

class Pose {
    var lift = 0f
    var bob = 0f
    var lean = 0f
    var roll = 0f
    var headPitch = 0f
    var headYaw = 0f
    var armLFwd = 0f
    var armLOut = 0f
    var armRFwd = 0f
    var armROut = 0f
    var legLFwd = 0f
    var legRFwd = 0f
    var lie = 0f

    fun set(o: Pose) {
        lift = o.lift; bob = o.bob; lean = o.lean; roll = o.roll
        headPitch = o.headPitch; headYaw = o.headYaw
        armLFwd = o.armLFwd; armLOut = o.armLOut; armRFwd = o.armRFwd; armROut = o.armROut
        legLFwd = o.legLFwd; legRFwd = o.legRFwd; lie = o.lie
    }

    fun clear() = set(ZERO)

    fun dampTo(t: Pose, k: Float, dt: Float) {
        lift = damp(lift, t.lift, k, dt); bob = damp(bob, t.bob, k, dt)
        lean = damp(lean, t.lean, k, dt); roll = damp(roll, t.roll, k, dt)
        headPitch = damp(headPitch, t.headPitch, k, dt); headYaw = damp(headYaw, t.headYaw, k, dt)
        armLFwd = damp(armLFwd, t.armLFwd, k, dt); armLOut = damp(armLOut, t.armLOut, k, dt)
        armRFwd = damp(armRFwd, t.armRFwd, k, dt); armROut = damp(armROut, t.armROut, k, dt)
        legLFwd = damp(legLFwd, t.legLFwd, k, dt); legRFwd = damp(legRFwd, t.legRFwd, k, dt)
        lie = damp(lie, t.lie, k * 0.6f, dt)
    }

    companion object {
        private val ZERO = Pose()
    }
}

class Animator {
    val pose = Pose()
    private val target = Pose()
    private var cycle = 0f
    private var clock = 0f
    private var stateTime = 0f
    var steps = 0
        private set
    var anim = Anim.IDLE
        private set

    fun play(a: Anim) {
        if (a != anim) {
            anim = a
            stateTime = 0f
        }
    }

    fun update(dt: Float, speed: Float, extra: Float = 0f) {
        clock += dt
        stateTime += dt
        target.clear()
        val t = stateTime
        when (anim) {
            Anim.IDLE -> {
                target.bob = sin(clock * 2.2f) * 0.012f
                target.armLFwd = sin(clock * 2.2f) * 3f
                target.armRFwd = -sin(clock * 2.2f) * 3f
                target.armLOut = 6f; target.armROut = 6f
                target.headYaw = sin(clock * 0.45f) * 12f
            }
            Anim.WALK, Anim.RUN -> {
                val run = anim == Anim.RUN
                val stride = floor(cycle + 0.5f)
                cycle += dt * speed * (if (run) 1.55f else 2.3f)
                if (floor(cycle + 0.5f) != stride) steps++
                val s = sin(cycle * PI.toFloat())
                val amp = if (run) 42f else 30f
                target.legLFwd = s * amp
                target.legRFwd = -s * amp
                target.armLFwd = -s * amp * 0.85f
                target.armRFwd = s * amp * 0.85f
                target.armLOut = 8f; target.armROut = 8f
                target.bob = abs(s) * (if (run) 0.07f else 0.04f)
                target.lean = if (run) 10f else 3f
            }
            Anim.SIT -> {
                target.legLFwd = 88f; target.legRFwd = 88f
                target.armLFwd = 28f; target.armRFwd = 28f
                target.armLOut = 10f; target.armROut = 10f
                target.bob = sin(clock * 2f) * 0.006f
                target.headYaw = sin(clock * 0.5f) * 15f
            }
            Anim.SLEEP -> {
                target.lie = 1f
                target.armLOut = 8f; target.armROut = 8f
                target.bob = sin(clock * 1.4f) * 0.01f
                target.headYaw = 12f
            }
            Anim.WATER -> {
                target.armRFwd = 72f + sin(clock * 9f) * 4f
                target.armROut = -6f
                target.armLFwd = 18f
                target.lean = 8f
                target.headPitch = 18f
            }
            Anim.HARVEST -> {
                val k = sin((t * 3.2f).coerceAtMost(PI.toFloat()))
                target.lean = 34f * k
                target.bob = -0.12f * k
                target.armLFwd = 62f * k; target.armRFwd = 62f * k
                target.legLFwd = 18f * k; target.legRFwd = -6f * k
                target.headPitch = 20f * k
            }
            Anim.SHOOT_AIM -> {
                target.armLFwd = 150f; target.armRFwd = 150f
                target.armLOut = -14f; target.armROut = -14f
                target.headPitch = -12f
                target.bob = sin(clock * 3f) * 0.01f - 0.04f
                target.legLFwd = 8f; target.legRFwd = 8f
            }
            Anim.SHOOT_RELEASE -> {
                val k = (t / 0.25f).coerceIn(0f, 1f)
                target.armLFwd = 150f + 25f * k; target.armRFwd = 150f + 25f * k
                target.armLOut = -10f; target.armROut = -10f
                target.lift = if (t < 0.35f) sin(t / 0.35f * PI.toFloat()) * 0.18f else 0f
                target.headPitch = -18f
            }
            Anim.KICK -> {
                val back = (t / 0.18f).coerceIn(0f, 1f)
                val swing = ((t - 0.18f) / 0.16f).coerceIn(0f, 1f)
                target.legRFwd = -40f * back * (1f - swing) + 75f * swing * (1f - ((t - 0.5f) / 0.3f).coerceIn(0f, 1f))
                target.armLFwd = 30f; target.armLOut = 35f
                target.armRFwd = -25f; target.armROut = 30f
                target.lean = -6f * swing
            }
            Anim.CHEER -> {
                val w = sin(clock * 14f)
                target.armLFwd = 165f; target.armRFwd = 165f
                target.armLOut = 22f + w * 10f; target.armROut = 22f - w * 10f
                target.lift = abs(sin(clock * 7f)) * 0.16f
                target.headPitch = -10f
            }
            Anim.SAD -> {
                target.headPitch = 28f
                target.armLFwd = 6f; target.armRFwd = 6f
                target.lean = 6f
                target.bob = -0.02f
            }
            Anim.WAVE -> {
                target.armRFwd = 150f
                target.armROut = 28f + sin(clock * 12f) * 18f
                target.armLFwd = 4f; target.armLOut = 6f
                target.headYaw = 0f
            }
            Anim.BOUNCE -> {
                target.lift = extra
                val up = (extra * 0.6f).coerceIn(0f, 1f)
                target.armLFwd = 40f + up * 120f; target.armRFwd = 40f + up * 120f
                target.armLOut = 30f; target.armROut = 30f
                target.legLFwd = 12f * up; target.legRFwd = -12f * up
            }
            Anim.SWING -> {
                target.legLFwd = 70f + extra * 0.4f; target.legRFwd = 70f + extra * 0.4f
                target.armLFwd = 140f; target.armRFwd = 140f
                target.armLOut = 12f; target.armROut = 12f
                target.lean = -extra * 0.35f
            }
            Anim.KEEPER -> {
                target.armLFwd = 40f; target.armRFwd = 40f
                target.armLOut = 50f; target.armROut = 50f
                target.legLFwd = 12f; target.legRFwd = -12f
                target.bob = -0.08f + sin(clock * 6f) * 0.015f
                target.lean = 12f
            }
        }
        val sharp = when (anim) {
            Anim.WALK, Anim.RUN -> 22f
            Anim.SLEEP -> 6f
            Anim.KICK, Anim.SHOOT_RELEASE -> 30f
            else -> 12f
        }
        pose.dampTo(target, sharp, dt)
    }
}
