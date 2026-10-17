package com.cozyhome.game.sports

import com.cozyhome.game.math.V2
import com.cozyhome.game.world.Track

class JogRace(val track: Track, val laps: Int = 3) {
    enum class Phase { COUNTDOWN, RUN, DONE }

    var phase = Phase.COUNTDOWN
        private set
    var countdown = 3f
        private set
    var elapsedMs = 0L
        private set
    var lap = 1
        private set
    var gate = 0
        private set
    var stamina = 1f
        private set
    var sprinting = false
        private set
    var onTrack = true
        private set
    var lapTimes = ArrayList<Long>()
        private set
    var medal = 0
        private set

    private var lastCount = 4
    private var lapStartMs = 0L
    private var offTrackTimer = 0f

    val gatesPerLap = 8
    val gateRadius: Float get() = track.width / 2f + 0.9f
    val startPoint: V2 get() = track.pointAt(0f)
    val startDirection: V2 get() = track.tangentAt(0f)

    fun gatePosition(index: Int): V2 = track.pointAt((index + 1) * track.length / gatesPerLap)

    fun gateDirection(index: Int): V2 = track.tangentAt((index + 1) * track.length / gatesPerLap)

    val nextGatePosition: V2 get() = gatePosition(gate)

    fun speed(): Float = when {
        phase != Phase.RUN -> 0f
        !onTrack -> GRASS_SPEED
        sprinting -> SPRINT_SPEED
        else -> RUN_SPEED
    }

    fun update(dt: Float, player: V2, wantSprint: Boolean, events: MutableList<SportEvent>) {
        when (phase) {
            Phase.COUNTDOWN -> {
                countdown -= dt
                val c = kotlin.math.ceil(countdown).toInt()
                if (c != lastCount && c > 0) {
                    lastCount = c
                    events += SportEvent.Countdown(c)
                }
                if (countdown <= 0f) {
                    phase = Phase.RUN
                    events += SportEvent.Go
                }
            }
            Phase.RUN -> {
                elapsedMs += (dt * 1000f).toLong()
                val was = onTrack
                onTrack = track.isOnTrack(player)
                if (was && !onTrack) offTrackTimer = 0f
                if (!onTrack) {
                    offTrackTimer += dt
                    if (offTrackTimer in 0.5f..(0.5f + dt)) events += SportEvent.OffTrack
                }
                sprinting = wantSprint && stamina > 0.02f && onTrack
                stamina = if (sprinting) (stamina - dt * 0.3f).coerceAtLeast(0f) else (stamina + dt * 0.16f).coerceAtMost(1f)
                if (player.distanceTo(nextGatePosition) <= gateRadius) {
                    events += SportEvent.Gate(lap, gate)
                    gate++
                    if (gate >= gatesPerLap) {
                        val lapMs = elapsedMs - lapStartMs
                        lapTimes += lapMs
                        lapStartMs = elapsedMs
                        events += SportEvent.Lap(lap, lapMs)
                        gate = 0
                        lap++
                        if (lap > laps) {
                            lap = laps
                            phase = Phase.DONE
                            medal = medalFor(elapsedMs)
                            events += SportEvent.Finished(SportKind.JOGGING, medal, laps, elapsedMs)
                        }
                    }
                }
            }
            Phase.DONE -> Unit
        }
    }

    fun medalFor(ms: Long): Int = when {
        ms <= GOLD_MS -> 3
        ms <= SILVER_MS -> 2
        ms <= BRONZE_MS -> 1
        else -> 0
    }

    companion object {
        const val RUN_SPEED = 6.0f
        const val SPRINT_SPEED = 8.2f
        const val GRASS_SPEED = 3.2f
        const val GOLD_MS = 27_000L
        const val SILVER_MS = 31_500L
        const val BRONZE_MS = 38_000L
    }
}
