package com.cozyhome.game.sports

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.v3
import com.cozyhome.game.world.Lot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class BasketballGame(private val rng: Rng, val totalShots: Int = 10) {
    enum class Phase { AIM, FLIGHT, RESULT, DONE }

    var phase = Phase.AIM
        private set
    var phaseTime = 0f
        private set
    var shot = 0
        private set
    var score = 0
        private set
    var made = 0
        private set
    var streak = 0
        private set
    var meter = 0f
        private set
    var lastScored = false
        private set
    var lastSwish = false
        private set
    var netWobble = 0f
        private set

    val ball = Ball(BALL_RADIUS)
    private var meterDir = 1f
    private var touchedRim = false
    private var touchedBoard = false
    private var scored = false
    private var flightTime = 0f

    val spotIndex: Int get() = shot % Lot.SHOT_SPOTS.size
    val spot: V2 get() = Lot.SHOT_SPOTS[spotIndex]
    val zoneHalf: Float get() = max(0.05f, 0.095f - streak * 0.007f)
    val meterSpeed: Float get() = 1.05f + streak * 0.1f
    val pointsForSpot: Int get() = if (spotIndex == 0) 2 else 3

    val facing: V2 get() = (V2(Lot.RIM.x, Lot.RIM.z) - spot).normalized()

    fun handPosition(): V3 = v3(spot + facing * 0.26f, 1.42f)

    init {
        ball.place(handPosition())
    }

    fun release(): Boolean {
        if (phase != Phase.AIM) return false
        val err = meter - ZONE_CENTER
        val inZone = abs(err) <= zoneHalf
        val speedFactor = 1f + (if (inZone) err * 0.06f else err * 0.22f)
        val lateralDeg = (if (inZone) rng.range(-0.4f, 0.4f) else rng.range(-1f, 1f) * (abs(err) * 9f + 0.6f))
        val from = handPosition()
        val aim = Lot.RIM + v3(facing * 0.05f, 0f)
        val ideal = launchVelocity(from, aim, LAUNCH_ANGLE)
        val h = V2(ideal.x, ideal.z).rotated(lateralDeg)
        val v = V3(h.x * speedFactor, ideal.y * speedFactor, h.z * speedFactor)
        ball.place(from)
        ball.launch(v)
        touchedRim = false
        touchedBoard = false
        scored = false
        flightTime = 0f
        setPhase(Phase.FLIGHT)
        return true
    }

    fun update(dt: Float, events: MutableList<SportEvent>) {
        phaseTime += dt
        netWobble = (netWobble - dt * 1.6f).coerceAtLeast(0f)
        when (phase) {
            Phase.AIM -> {
                meter += meterDir * dt * meterSpeed
                if (meter >= 1f) { meter = 1f; meterDir = -1f }
                if (meter <= 0f) { meter = 0f; meterDir = 1f }
                ball.place(handPosition())
            }
            Phase.FLIGHT -> {
                val steps = 8
                val h = dt / steps
                repeat(steps) { stepFlight(h, events) }
                flightTime += dt
                if ((ball.bounces >= 2 && flightTime > 1.1f) || flightTime > 3.6f) finishShot(events)
            }
            Phase.RESULT -> {
                val steps = 4
                repeat(steps) { stepFlight(dt / steps, events) }
                if (phaseTime > 1.0f) {
                    shot++
                    if (shot >= totalShots) {
                        setPhase(Phase.DONE)
                        events += SportEvent.Finished(SportKind.BASKETBALL, score, made)
                    } else {
                        meter = 0f
                        meterDir = 1f
                        setPhase(Phase.AIM)
                        ball.place(handPosition())
                    }
                }
            }
            Phase.DONE -> Unit
        }
    }

    private fun finishShot(events: MutableList<SportEvent>) {
        lastScored = scored
        lastSwish = scored && !touchedRim && !touchedBoard
        if (!scored) {
            streak = 0
            events += SportEvent.BasketMiss
        }
        setPhase(Phase.RESULT)
    }

    private fun stepFlight(dt: Float, events: MutableList<SportEvent>) {
        val prev = ball.pos
        val impact = ball.integrate(dt, drag = 0.04f, restitution = 0.62f, friction = 0.78f)
        if (impact > 0f) events += SportEvent.Bounce(SportKind.BASKETBALL, impact)
        collideRim(events)
        collideBoard(events)
        if (!scored && prev.y > Lot.RIM.y && ball.pos.y <= Lot.RIM.y) {
            val dx = ball.pos.x - Lot.RIM.x
            val dz = ball.pos.z - Lot.RIM.z
            if (sqrt(dx * dx + dz * dz) < Lot.RIM_RADIUS - BALL_RADIUS * 0.45f) {
                scored = true
                made++
                streak++
                val bonus = if (streak >= 3) 1 else 0
                val points = pointsForSpot + bonus
                score += points
                netWobble = 1f
                ball.vel = V3(ball.vel.x * 0.35f, ball.vel.y * 0.6f, ball.vel.z * 0.35f)
                events += SportEvent.Basket(points, !touchedRim && !touchedBoard, streak)
                if (phase == Phase.FLIGHT) {
                    lastScored = true
                    lastSwish = !touchedRim && !touchedBoard
                    setPhase(Phase.RESULT)
                }
            }
        }
    }

    private fun collideRim(events: MutableList<SportEvent>) {
        val c = ball.pos
        val dx = c.x - Lot.RIM.x
        val dz = c.z - Lot.RIM.z
        val len = sqrt(dx * dx + dz * dz)
        if (len < 1e-4f) return
        val q = V3(Lot.RIM.x + dx / len * Lot.RIM_RADIUS, Lot.RIM.y, Lot.RIM.z + dz / len * Lot.RIM_RADIUS)
        val d = c - q
        val dist = d.length
        val minDist = BALL_RADIUS + RIM_TUBE
        if (dist < minDist && dist > 1e-5f) {
            val n = d * (1f / dist)
            ball.pos = q + n * minDist
            if ((ball.vel dot n) < 0f) {
                ball.vel = reflect(ball.vel, n, 0.55f)
                if (!touchedRim) events += SportEvent.RimHit
                touchedRim = true
            }
        }
    }

    private fun collideBoard(events: MutableList<SportEvent>) {
        val c = ball.pos
        if (c.x < Lot.RIM.x - 0.9f || c.x > Lot.RIM.x + 0.9f || c.y < 2.72f || c.y > 3.85f) return
        if (c.z - BALL_RADIUS < Lot.BACKBOARD_Z && c.z > Lot.BACKBOARD_Z - 0.3f && ball.vel.z < 0f) {
            ball.pos = V3(c.x, c.y, Lot.BACKBOARD_Z + BALL_RADIUS)
            ball.vel = V3(ball.vel.x * 0.9f, ball.vel.y * 0.9f, -ball.vel.z * 0.58f)
            if (!touchedBoard) events += SportEvent.BoardHit
            touchedBoard = true
        }
    }

    private fun setPhase(p: Phase) {
        phase = p
        phaseTime = 0f
    }

    companion object {
        const val BALL_RADIUS = 0.115f
        const val RIM_TUBE = 0.014f
        const val ZONE_CENTER = 0.72f
        const val LAUNCH_ANGLE = 64f
    }
}
