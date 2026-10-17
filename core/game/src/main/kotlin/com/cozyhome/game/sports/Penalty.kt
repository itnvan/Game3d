package com.cozyhome.game.sports

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.damp
import com.cozyhome.game.math.lerp
import com.cozyhome.game.world.Lot
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

class PenaltyGame(private val rng: Rng, val totalKicks: Int = 5) {
    enum class Phase { AIM, FLIGHT, RESULT, DONE }

    var phase = Phase.AIM
        private set
    var phaseTime = 0f
        private set
    var kicks = 0
        private set
    var goals = 0
        private set
    val results = ArrayList<Boolean>()
    var lastGoal = false
        private set

    val ball = Ball(BALL_RADIUS)
    var keeperZ = Lot.GOAL_Z
        private set
    var keeperLean = 0f
        private set
    var keeperLift = 0f
        private set
    var netPush = 0f
        private set

    private var keeperTargetZ = Lot.GOAL_Z
    private var keeperTargetLean = 0f
    private var keeperTargetLift = 0f
    private var reaction = 0f
    private var diving = false
    private var resolved = false
    private var inNet = false
    private var flightTime = 0f
    private var idleClock = 0f

    val keeperX: Float get() = Lot.GOAL_X + 0.45f

    init {
        resetBall()
    }

    private fun resetBall() {
        ball.place(V3(Lot.PENALTY_SPOT.x, BALL_RADIUS, Lot.PENALTY_SPOT.z))
        keeperZ = Lot.GOAL_Z
        keeperLean = 0f
        keeperLift = 0f
        keeperTargetZ = Lot.GOAL_Z
        keeperTargetLean = 0f
        keeperTargetLift = 0f
        diving = false
        resolved = false
        inNet = false
        flightTime = 0f
        netPush = 0f
    }

    fun kick(target: V3, power: Float): Boolean {
        if (phase != Phase.AIM) return false
        val p = power.coerceIn(0f, 1f)
        val noise = 0.06f + p * 0.22f
        val aim = V3(
            Lot.GOAL_X - 0.2f,
            (target.y + rng.range(-noise, noise)).coerceIn(0.12f, 3.2f),
            target.z + rng.range(-noise, noise),
        )
        val from = ball.pos
        val dist = (aim - from).length
        val speed = lerp(8.5f, 17.5f, p)
        ball.launch(velocityForTime(from, aim, dist / speed))
        val hitZ = aim.z
        val hitY = aim.y
        val guessRight = rng.chance(0.38f + kicks * 0.06f)
        keeperTargetZ = if (guessRight) {
            (hitZ + rng.range(-0.3f, 0.3f)).coerceIn(Lot.GOAL_Z - 1.6f, Lot.GOAL_Z + 1.6f)
        } else {
            Lot.GOAL_Z + (if (rng.chance(0.5f)) 1f else -1f) * rng.range(0.5f, 1.5f)
        }
        val dz = keeperTargetZ - Lot.GOAL_Z
        keeperTargetLean = (dz / 1.4f).coerceIn(-1f, 1f) * (if (abs(dz) > 0.55f) 1f else 0.35f)
        keeperTargetLift = if (guessRight) ((hitY - 0.9f) * 0.5f).coerceIn(0f, 0.55f) else rng.range(0f, 0.3f)
        reaction = rng.range(0.1f, 0.2f) + (1f - p) * 0.08f
        diving = false
        resolved = false
        flightTime = 0f
        setPhase(Phase.FLIGHT)
        return true
    }

    fun update(dt: Float, events: MutableList<SportEvent>) {
        phaseTime += dt
        idleClock += dt
        when (phase) {
            Phase.AIM -> {
                keeperZ = Lot.GOAL_Z + sin(idleClock * 1.7f) * 0.35f
                keeperLean = sin(idleClock * 1.7f) * 0.05f
            }
            Phase.FLIGHT, Phase.RESULT -> {
                if (phase == Phase.FLIGHT) {
                    reaction -= dt
                    if (reaction <= 0f) diving = true
                }
                if (diving) {
                    keeperZ = moveToward(keeperZ, keeperTargetZ, 5.6f * dt)
                    keeperLean = damp(keeperLean, keeperTargetLean, 9f, dt)
                    keeperLift = damp(keeperLift, keeperTargetLift, 8f, dt)
                }
                val steps = 8
                repeat(steps) { stepBall(dt / steps, events) }
                flightTime += dt
                if (phase == Phase.FLIGHT && !resolved && (flightTime > 2.6f || (ball.isResting && flightTime > 0.6f))) {
                    resolve(false, events)
                    events += SportEvent.ShotMissed
                }
                if (phase == Phase.RESULT && phaseTime > 1.4f) {
                    kicks++
                    if (kicks >= totalKicks) {
                        setPhase(Phase.DONE)
                        events += SportEvent.Finished(SportKind.SOCCER, goals, kicks)
                    } else {
                        resetBall()
                        setPhase(Phase.AIM)
                    }
                }
            }
            Phase.DONE -> Unit
        }
        netPush = (netPush - dt * 1.4f).coerceAtLeast(0f)
    }

    private fun stepBall(dt: Float, events: MutableList<SportEvent>) {
        val prev = ball.pos
        val impact = ball.integrate(dt, drag = if (inNet) 3.5f else 0.08f, restitution = 0.5f, friction = 0.82f)
        if (impact > 0f) events += SportEvent.Bounce(SportKind.SOCCER, impact)
        if (!resolved) {
            collidePosts(events)
            collideKeeper(prev, events)
        }
        val c = ball.pos
        if (!resolved && prev.x >= Lot.GOAL_X && c.x < Lot.GOAL_X) {
            val inside = abs(c.z - Lot.GOAL_Z) < Lot.GOAL_HALF - BALL_RADIUS * 0.5f && c.y < Lot.GOAL_HEIGHT - BALL_RADIUS * 0.5f
            if (inside) {
                inNet = true
                netPush = 1f
                resolve(true, events)
                events += SportEvent.Goal
            } else {
                resolve(false, events)
                events += SportEvent.ShotMissed
            }
        }
        if (inNet) {
            val backX = Lot.GOAL_X - Lot.GOAL_DEPTH + BALL_RADIUS
            if (ball.pos.x < backX) {
                ball.pos = V3(backX, ball.pos.y, ball.pos.z)
                ball.vel = V3(-ball.vel.x * 0.15f, ball.vel.y * 0.5f, ball.vel.z * 0.4f)
            }
            val zMin = Lot.GOAL_Z - Lot.GOAL_HALF + BALL_RADIUS
            val zMax = Lot.GOAL_Z + Lot.GOAL_HALF - BALL_RADIUS
            if (ball.pos.z !in zMin..zMax) {
                ball.pos = V3(ball.pos.x, ball.pos.y, ball.pos.z.coerceIn(zMin, zMax))
                ball.vel = V3(ball.vel.x, ball.vel.y, -ball.vel.z * 0.3f)
            }
            if (ball.pos.y > Lot.GOAL_HEIGHT - BALL_RADIUS) {
                ball.pos = V3(ball.pos.x, Lot.GOAL_HEIGHT - BALL_RADIUS, ball.pos.z)
                ball.vel = V3(ball.vel.x, -abs(ball.vel.y) * 0.3f, ball.vel.z)
            }
        }
    }

    private fun collidePosts(events: MutableList<SportEvent>) {
        val c = ball.pos
        for (pz in floatArrayOf(Lot.GOAL_Z - Lot.GOAL_HALF, Lot.GOAL_Z + Lot.GOAL_HALF)) {
            if (c.y > Lot.GOAL_HEIGHT + BALL_RADIUS) continue
            val dx = c.x - Lot.GOAL_X
            val dz = c.z - pz
            val d = sqrt(dx * dx + dz * dz)
            val min = BALL_RADIUS + POST_RADIUS
            if (d < min && d > 1e-5f) {
                val n = V3(dx / d, 0f, dz / d)
                ball.pos = V3(Lot.GOAL_X + n.x * min, c.y, pz + n.z * min)
                ball.vel = reflect(ball.vel, n, 0.6f)
                events += SportEvent.PostHit
            }
        }
        if (abs(c.z - Lot.GOAL_Z) < Lot.GOAL_HALF) {
            val dx = c.x - Lot.GOAL_X
            val dy = c.y - Lot.GOAL_HEIGHT
            val d = sqrt(dx * dx + dy * dy)
            val min = BALL_RADIUS + POST_RADIUS
            if (d < min && d > 1e-5f) {
                val n = V3(dx / d, dy / d, 0f)
                ball.pos = V3(Lot.GOAL_X + n.x * min, Lot.GOAL_HEIGHT + n.y * min, c.z)
                ball.vel = reflect(ball.vel, n, 0.6f)
                events += SportEvent.PostHit
            }
        }
    }

    private fun collideKeeper(prev: V3, events: MutableList<SportEvent>) {
        val (a, b) = keeperSegment()
        val c = ball.pos
        val ab = b - a
        val t = (((c - a) dot ab) / (ab dot ab).coerceAtLeast(1e-6f)).coerceIn(0f, 1f)
        val q = a + ab * t
        val d = c - q
        val dist = d.length
        val min = BALL_RADIUS + KEEPER_RADIUS
        if (dist < min) {
            val n = if (dist > 1e-4f) d * (1f / dist) else V3(1f, 0f, 0f)
            val push = if (n.x < 0.2f) V3(0.6f, n.y, n.z).normalized() else n
            ball.pos = q + push * min
            ball.vel = reflect(ball.vel, push, 0.35f).let { V3(abs(it.x).coerceAtLeast(2f), it.y, it.z) }
            resolve(false, events)
            events += SportEvent.KeeperSave
        }
    }

    fun keeperSegment(): Pair<V3, V3> {
        val lean = keeperLean
        val foot = V3(keeperX, 0.15f + keeperLift * 0.4f, keeperZ - lean * 0.25f)
        val hand = V3(keeperX, 1.55f + keeperLift - abs(lean) * 0.85f, keeperZ + lean * 1.3f)
        return foot to hand
    }

    private fun resolve(goal: Boolean, events: MutableList<SportEvent>) {
        if (resolved) return
        resolved = true
        lastGoal = goal
        results += goal
        if (goal) goals++
        setPhase(Phase.RESULT)
    }

    private fun setPhase(p: Phase) {
        phase = p
        phaseTime = 0f
    }

    private fun moveToward(v: Float, target: Float, maxDelta: Float): Float {
        val d = target - v
        return if (abs(d) <= maxDelta) target else v + sign(d) * maxDelta
    }

    companion object {
        const val BALL_RADIUS = 0.11f
        const val POST_RADIUS = 0.06f
        const val KEEPER_RADIUS = 0.27f
    }
}
