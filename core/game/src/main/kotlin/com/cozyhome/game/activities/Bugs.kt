package com.cozyhome.game.activities

import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.world.Lot
import kotlin.math.cos
import kotlin.math.sin

enum class BugHabitat { FLOWERS, POND, TREES, LAMPS, GARDEN }

data class BugSpecies(
    val id: String,
    val rarity: Rarity,
    val habitat: BugHabitat,
    val price: Int,
    val xp: Int,
    val flyer: Boolean,
    val wing: Int,
    val body: Int,
    val fromHour: Float = 6f,
    val toHour: Float = 19f,
    val skittish: Float = 0.2f,
) {
    fun activeAt(hour: Float): Boolean =
        if (fromHour <= toHour) hour >= fromHour && hour < toHour else hour >= fromHour || hour < toHour
}

object BugDex {
    val all = listOf(
        BugSpecies("butterfly", Rarity.COMMON, BugHabitat.FLOWERS, 12, 3, true, 0xF2A33A, 0x3A2C22),
        BugSpecies("blue_morpho", Rarity.RARE, BugHabitat.FLOWERS, 70, 12, true, 0x3E8FE8, 0x232020, 8f, 16f, 0.45f),
        BugSpecies("bee", Rarity.COMMON, BugHabitat.FLOWERS, 10, 3, true, 0xE8F2FA, 0xF2C230, skittish = 0.3f),
        BugSpecies("ladybug", Rarity.COMMON, BugHabitat.GARDEN, 9, 2, false, 0xD8352A, 0x1E1E1E),
        BugSpecies("dragonfly", Rarity.UNCOMMON, BugHabitat.POND, 28, 6, true, 0xCDEBF2, 0x2F8F9A, skittish = 0.4f),
        BugSpecies("snail", Rarity.COMMON, BugHabitat.GARDEN, 8, 2, false, 0xB8875A, 0x8C8A70, 4f, 10f, 0f),
        BugSpecies("stag_beetle", Rarity.RARE, BugHabitat.TREES, 80, 14, false, 0x3A2A1E, 0x5A3A22, 17f, 23f, 0.15f),
        BugSpecies("firefly", Rarity.UNCOMMON, BugHabitat.POND, 30, 7, true, 0xF7F2A0, 0x3A3A2A, 19.5f, 4.5f, 0.25f),
        BugSpecies("moth", Rarity.COMMON, BugHabitat.LAMPS, 12, 3, true, 0xD9CDB4, 0x6E5E4A, 19f, 5f, 0.2f),
        BugSpecies("cicada", Rarity.UNCOMMON, BugHabitat.TREES, 26, 6, false, 0x9EC48A, 0x4A5A3A, 9f, 18f, 0.25f),
    )

    operator fun get(id: String?) = all.firstOrNull { it.id == id }
}

class Bug(val id: Int, val species: BugSpecies, val home: V3) {
    var pos = home
    var heading = 0f
    var flap = 0f
    var phase = 0f
    var fleeing = 0f
}

class BugField(private val rng: Rng) {
    val bugs = ArrayList<Bug>()
    private var nextId = 1
    private var respawn = 0f

    private fun anchors(h: BugHabitat): List<V3> = when (h) {
        BugHabitat.FLOWERS -> Lot.FLOWER_BEDS.map { V3(it.center.x, 0.55f, it.center.z) } + listOf(V3(-9f, 0.5f, 5f), V3(10f, 0.5f, 12f), V3(-14f, 0.5f, -3f), V3(6f, 0.5f, 16f))
        BugHabitat.POND -> (0 until 4).map { k -> val a = k * 1.57f; V3(Lot.POND.x + cos(a) * (Lot.POND.r * 0.7f), 0.6f, Lot.POND.z + sin(a) * (Lot.POND.r * 0.7f)) }
        BugHabitat.TREES -> Lot.TREES.map { V3(it.pos.x, 1.0f * it.scale, it.pos.z) }
        BugHabitat.LAMPS -> listOf(V3(-1.6f, 2.5f, -2.3f), V3(1.6f, 2.5f, -2.3f), V3(-2.4f, 2.5f, 18.8f), V3(2.4f, 2.5f, 18.8f))
        BugHabitat.GARDEN -> Lot.PLOTS.map { V3(it.x, 0.18f, it.z) }
    }

    fun update(dt: Float, hour: Float, player: V2, running: Boolean) {
        bugs.removeAll { !it.species.activeAt(hour) && it.fleeing <= 0f }
        respawn -= dt
        if (bugs.size < MAX && respawn <= 0f) {
            respawn = rng.range(3f, 8f)
            spawn(hour)
        }
        for (b in bugs) {
            b.phase += dt
            b.flap += dt * if (b.species.flyer) 14f else 3f
            val d = V2(b.pos.x - player.x, b.pos.z - player.z)
            if (running && d.length < 2.2f && rng.nextFloat() < b.species.skittish * dt * 4f) b.fleeing = 1.4f
            if (b.fleeing > 0f) {
                b.fleeing -= dt
                val away = if (d.length > 0.01f) d.normalized() else V2(1f, 0f)
                b.pos = V3(b.pos.x + away.x * dt * 3f, (b.pos.y + dt * (if (b.species.flyer) 1.2f else 0f)).coerceAtMost(3f), b.pos.z + away.z * dt * 3f)
                if (b.fleeing <= 0f) b.pos = b.home
                continue
            }
            if (b.species.flyer) {
                val t = b.phase * 0.7f + b.id
                b.pos = V3(
                    b.home.x + sin(t) * 0.9f + sin(t * 2.3f) * 0.3f,
                    b.home.y + sin(t * 1.7f) * 0.25f + 0.15f,
                    b.home.z + cos(t * 0.8f) * 0.9f,
                )
                b.heading = (kotlin.math.atan2(cos(t), -sin(t * 0.8f)) * 57.3f)
            } else {
                val t = b.phase * 0.15f + b.id
                b.pos = V3(b.home.x + sin(t) * 0.3f, b.home.y, b.home.z + cos(t * 1.3f) * 0.3f)
                b.heading = t * 57.3f
            }
        }
    }

    private fun spawn(hour: Float) {
        val options = BugDex.all.filter { it.activeAt(hour) }
        if (options.isEmpty()) return
        val weights = options.map { it.rarity.weight }
        var r = rng.nextFloat() * weights.sum()
        var pick = options.last()
        for ((i, w) in weights.withIndex()) {
            r -= w
            if (r <= 0f) { pick = options[i]; break }
        }
        val spots = anchors(pick.habitat)
        val home = spots[(rng.nextFloat() * spots.size).toInt().coerceAtMost(spots.size - 1)]
        val jitter = V3(home.x + rng.range(-0.6f, 0.6f), home.y, home.z + rng.range(-0.6f, 0.6f))
        bugs += Bug(nextId++, pick, jitter)
    }

    fun nearest(player: V2, radius: Float): Bug? =
        bugs.filter { it.fleeing <= 0f }.minByOrNull { V2(it.pos.x - player.x, it.pos.z - player.z).length }
            ?.takeIf { V2(it.pos.x - player.x, it.pos.z - player.z).length < radius }

    fun tryCatch(bug: Bug): Boolean {
        val chance = when (bug.species.rarity) {
            Rarity.COMMON -> 0.9f
            Rarity.UNCOMMON -> 0.75f
            Rarity.RARE -> 0.55f
            Rarity.LEGENDARY -> 0.35f
        }
        val ok = rng.nextFloat() < chance
        if (ok) bugs.remove(bug) else bug.fleeing = 1.6f
        return ok
    }

    companion object {
        const val MAX = 6
    }
}
