package com.cozyhome.game.activities

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.lerp
import kotlin.math.abs
import kotlin.math.sin

enum class Rarity(val stars: Int, val weight: Float) { COMMON(1, 60f), UNCOMMON(2, 26f), RARE(3, 10f), LEGENDARY(4, 2.2f) }

data class FishSpecies(
    val id: String,
    val rarity: Rarity,
    val minCm: Int,
    val maxCm: Int,
    val price: Int,
    val xp: Int,
    val strength: Float,
    val body: Int,
    val belly: Int,
    val fromHour: Float = 0f,
    val toHour: Float = 24f,
    val junk: Boolean = false,
) {
    fun activeAt(hour: Float): Boolean =
        if (fromHour <= toHour) hour >= fromHour && hour < toHour else hour >= fromHour || hour < toHour
}

object FishDex {
    val all = listOf(
        FishSpecies("minnow", Rarity.COMMON, 4, 9, 6, 2, 0.15f, 0x9AA7B0, 0xE8EEF2),
        FishSpecies("goldfish", Rarity.COMMON, 6, 14, 10, 3, 0.2f, 0xF08A2C, 0xFFD39A),
        FishSpecies("carp", Rarity.COMMON, 25, 60, 18, 4, 0.45f, 0x8C7A4C, 0xD8C99A),
        FishSpecies("bluegill", Rarity.UNCOMMON, 12, 24, 22, 6, 0.35f, 0x4F7FA8, 0xF2C66B, 5f, 19f),
        FishSpecies("bass", Rarity.UNCOMMON, 25, 55, 30, 7, 0.6f, 0x5E7A3A, 0xE6E0B8, 6f, 20f),
        FishSpecies("trout", Rarity.UNCOMMON, 20, 45, 34, 8, 0.55f, 0x7C8E6A, 0xF2B6B0, 5f, 11f),
        FishSpecies("catfish", Rarity.RARE, 40, 110, 60, 12, 0.8f, 0x4B4A44, 0xC9C2B0, 19f, 5f),
        FishSpecies("koi", Rarity.RARE, 30, 70, 75, 14, 0.65f, 0xF4F1EA, 0xE2533D),
        FishSpecies("pufferfish", Rarity.RARE, 10, 25, 55, 12, 0.5f, 0xD9C36A, 0xF6F0D8, 10f, 16f),
        FishSpecies("golden_koi", Rarity.LEGENDARY, 50, 90, 220, 40, 0.95f, 0xF7C531, 0xFFF0B0, 17f, 20f),
        FishSpecies("old_boot", Rarity.COMMON, 25, 30, 1, 1, 0.1f, 0x5A4632, 0x3A2C20, junk = true),
        FishSpecies("treasure", Rarity.RARE, 30, 30, 150, 20, 0.7f, 0x8A5A2E, 0xF2C94C, junk = true),
    )

    operator fun get(id: String?) = all.firstOrNull { it.id == id }

    fun roll(rng: Rng, hour: Float, luck: Float = 0f): FishSpecies {
        val pool = all.filter { it.activeAt(hour) }
        val weights = pool.map { s ->
            val dusk = if (hour in 17f..20f || hour in 5f..7.5f) 1.6f else 1f
            val boost = if (s.rarity >= Rarity.RARE) dusk * (1f + luck) else 1f
            s.rarity.weight * boost * (if (s.junk && s.id == "old_boot") 0.35f else 1f)
        }
        var r = rng.nextFloat() * weights.sum()
        for ((i, w) in weights.withIndex()) {
            r -= w
            if (r <= 0f) return pool[i]
        }
        return pool.last()
    }
}

sealed interface FishEvent {
    data object Splash : FishEvent
    data object Nibble : FishEvent
    data object Bite : FishEvent
    data object Hooked : FishEvent
    data class Caught(val species: FishSpecies, val cm: Int) : FishEvent
    data object Escaped : FishEvent
    data object Snapped : FishEvent
    data object TooEarly : FishEvent
}

class FishingSession(private val rng: Rng, private val hour: Float, val rodTip: V3, val target: V3) {
    enum class Phase { CAST, WAIT, BITE, REEL, CAUGHT, ESCAPED, SNAPPED }

    var phase = Phase.CAST
        private set
    var phaseTime = 0f
        private set
    var bobber = rodTip
        private set
    var dip = 0f
        private set
    var tension = 0f
        private set
    var progress = 0f
        private set
    var species: FishSpecies = FishDex.roll(rng, hour)
        private set
    var sizeCm = 0
        private set
    var pull = 0f
        private set
    val done: Boolean get() = (phase == Phase.CAUGHT || phase == Phase.ESCAPED || phase == Phase.SNAPPED) && phaseTime > RESULT_TIME

    private var waitFor = rng.range(2.2f, 6.5f)
    private var nextNibble = rng.range(0.8f, 2f)
    private var nibble = 0f
    private var pullPhase = rng.range(0f, 6f)
    private var scared = 0

    private fun enter(p: Phase) {
        phase = p
        phaseTime = 0f
    }

    fun update(dt: Float, holding: Boolean, events: MutableList<FishEvent>) {
        phaseTime += dt
        when (phase) {
            Phase.CAST -> {
                val t = (phaseTime / CAST_TIME).coerceIn(0f, 1f)
                val arc = sin(t * Math.PI.toFloat()) * 1.6f
                bobber = V3(lerp(rodTip.x, target.x, t), lerp(rodTip.y, target.y, t) + arc, lerp(rodTip.z, target.z, t))
                if (t >= 1f) {
                    events += FishEvent.Splash
                    enter(Phase.WAIT)
                }
            }
            Phase.WAIT -> {
                nextNibble -= dt
                if (nextNibble <= 0f) {
                    nibble = 0.35f
                    nextNibble = rng.range(0.9f, 2.2f)
                    events += FishEvent.Nibble
                }
                nibble = (nibble - dt).coerceAtLeast(0f)
                dip = if (nibble > 0f) sin(nibble / 0.35f * Math.PI.toFloat()) * 0.05f else 0f
                bobber = V3(target.x, target.y + sin(phaseTime * 2.6f) * 0.015f - dip, target.z)
                if (phaseTime >= waitFor) {
                    events += FishEvent.Bite
                    enter(Phase.BITE)
                }
            }
            Phase.BITE -> {
                dip = 0.12f + sin(phaseTime * 22f) * 0.03f
                bobber = V3(target.x + sin(phaseTime * 17f) * 0.05f, target.y - dip, target.z)
                if (phaseTime > biteWindow()) {
                    events += FishEvent.Escaped
                    enter(Phase.ESCAPED)
                }
            }
            Phase.REEL -> {
                val s = species.strength
                pullPhase += dt * (1.2f + s * 2.4f)
                pull = (0.5f + 0.5f * sin(pullPhase) * sin(pullPhase * 0.37f + 1.1f)).coerceIn(0f, 1f)
                if (holding) {
                    progress += (0.3f - 0.14f * s) * dt
                    tension += (0.35f + 0.85f * s * pull) * dt
                } else {
                    tension -= 0.75f * dt
                    progress -= 0.05f * s * dt
                }
                tension = tension.coerceAtLeast(0f)
                progress = progress.coerceIn(0f, 1f)
                val toward = progress
                bobber = V3(
                    lerp(target.x, rodTip.x, toward * 0.85f) + sin(pullPhase * 2.1f) * 0.25f * (1f - toward),
                    target.y - 0.06f,
                    lerp(target.z, rodTip.z, toward * 0.85f) + sin(pullPhase * 1.6f) * 0.25f * (1f - toward),
                )
                if (tension >= 1f) {
                    events += FishEvent.Snapped
                    enter(Phase.SNAPPED)
                } else if (progress >= 1f) {
                    sizeCm = species.minCm + ((species.maxCm - species.minCm) * rng.nextFloat() * rng.nextFloat().let { 0.4f + 0.6f * it }).toInt()
                    events += FishEvent.Caught(species, sizeCm)
                    enter(Phase.CAUGHT)
                }
            }
            Phase.CAUGHT, Phase.ESCAPED, Phase.SNAPPED -> Unit
        }
    }

    private fun biteWindow() = when (species.rarity) {
        Rarity.LEGENDARY -> 0.65f
        Rarity.RARE -> 0.8f
        else -> 1.0f
    }

    fun tap(events: MutableList<FishEvent>) {
        when (phase) {
            Phase.WAIT -> {
                if (nibble > 0f) {
                    scared++
                    events += FishEvent.TooEarly
                    waitFor = phaseTime + rng.range(2f, 4.5f)
                    if (scared >= 3) species = FishDex.roll(rng, hour)
                }
            }
            Phase.BITE -> {
                tension = 0.15f
                progress = 0.08f
                events += FishEvent.Hooked
                enter(Phase.REEL)
            }
            else -> Unit
        }
    }

    val reelHint: Float get() = abs(pull)

    companion object {
        const val CAST_TIME = 0.85f
        const val RESULT_TIME = 1.6f
    }
}
