package com.cozyhome.game.world

import com.cozyhome.game.math.V3
import com.cozyhome.game.math.smoothstep
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.rgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class SkyState(
    val hour: Float,
    val sky: Rgb,
    val sunColor: Rgb,
    val sunIntensity: Float,
    val sunDirection: V3,
    val ambient: Float,
    val night: Float,
)

object DayClock {
    const val GAME_HOUR_MS = 50_000L
    const val DAY_MS = GAME_HOUR_MS * 24
    const val WAKE_HOUR = 7f

    fun hour(nowMs: Long, offsetMs: Long): Float {
        val t = ((nowMs + offsetMs) % DAY_MS + DAY_MS) % DAY_MS
        val h = t.toFloat() / GAME_HOUR_MS + START_HOUR
        return (h % 24f + 24f) % 24f
    }

    fun offsetToReach(nowMs: Long, offsetMs: Long, targetHour: Float): Long {
        val current = hour(nowMs, offsetMs)
        var delta = targetHour - current
        if (delta < 0f) delta += 24f
        return offsetMs + (delta * GAME_HOUR_MS).toLong()
    }

    fun isNight(hour: Float) = hour >= 19.5f || hour < 5.5f

    private val DAY_SKY = rgb(0x9FDCF7)
    private val DAWN_SKY = rgb(0xFFC9A8)
    private val DUSK_SKY = rgb(0xF9A8C0)
    private val NIGHT_SKY = rgb(0x5D72B8)

    fun sky(hour: Float): SkyState {
        val dayness = smoothstep(5f, 7.5f, hour) * (1f - smoothstep(17.5f, 20f, hour))
        val dawn = bump(hour, 6.2f, 1.3f)
        val dusk = bump(hour, 18.6f, 1.4f)
        var sky = NIGHT_SKY.mix(DAY_SKY, dayness)
        sky = sky.mix(DAWN_SKY, dawn * 0.75f).mix(DUSK_SKY, dusk * 0.8f)
        val sunAngle = ((hour - 6f) / 12f).coerceIn(0f, 1f) * PI.toFloat()
        val elevation = max(0.25f, sin(sunAngle))
        val azimuth = cos(sunAngle)
        val dir = V3(-azimuth * 0.75f - 0.2f, -elevation, -0.55f).normalized()
        val warm = max(dawn, dusk)
        val sunColor = Rgb(1f, 0.96f, 0.88f).mix(rgb(0xFFB37A), warm * 0.8f).mix(rgb(0x9DB4FF), (1f - dayness) * 0.8f)
        val intensity = 2_600f + 40_000f * dayness * (1f - warm * 0.35f)
        val ambient = 0.14f + 0.86f * dayness
        return SkyState(hour, sky, sunColor, intensity, dir, ambient, 1f - dayness)
    }

    private fun bump(x: Float, center: Float, width: Float): Float {
        val d = (x - center) / width
        return max(0f, 1f - d * d)
    }

    private const val START_HOUR = 9f
}
