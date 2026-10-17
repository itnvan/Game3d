package com.cozyhome.game.state

import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.decor.Placement
import com.cozyhome.game.math.V2
import kotlin.math.sqrt

object Rules {
    const val MAX_LEVEL = 20

    fun xpForLevel(level: Int): Int = 30 * (level - 1) * level

    fun level(xp: Int): Int {
        val l = ((1 + sqrt(1 + 4 * xp / 30.0)) / 2).toInt()
        return l.coerceIn(1, MAX_LEVEL)
    }

    fun levelProgress(xp: Int): Float {
        val l = level(xp)
        if (l >= MAX_LEVEL) return 1f
        val a = xpForLevel(l)
        val b = xpForLevel(l + 1)
        return ((xp - a).toFloat() / (b - a)).coerceIn(0f, 1f)
    }

    fun dayIndex(nowMs: Long, utcOffsetMs: Long): Long = (nowMs + utcOffsetMs) / 86_400_000L

    fun giftCoins(level: Int) = 60 + level * 20

    fun basketCoins(score: Int) = score
    fun soccerCoins(goals: Int) = goals * 10
    fun jogCoins(medal: Int) = when (medal) { 3 -> 45; 2 -> 30; 1 -> 18; else -> 6 }

    fun starter(): SaveData {
        val items = listOf(
            Triple("rug_round", V2(-3f, -9.5f), 0) to 1,
            Triple("sofa", V2(-3f, -11f), 0) to 0,
            Triple("coffee_table", V2(-3.25f, -9.5f), 0) to 0,
            Triple("tv", V2(-3.25f, -6.25f), 2) to 0,
            Triple("floor_lamp", V2(-5.25f, -11.25f), 0) to 0,
            Triple("bed_single", V2(4.5f, -15f), 0) to 1,
            Triple("nightstand", V2(3.25f, -15.75f), 0) to 0,
            Triple("fridge", V2(-5.5f, -15.5f), 0) to 0,
            Triple("stove", V2(-4.5f, -15.5f), 0) to 0,
            Triple("counter", V2(-3.5f, -15.5f), 0) to 0,
            Triple("dining_table", V2(1.25f, -13.5f), 0) to 0,
            Triple("chair", V2(0.75f, -12.75f), 2) to 0,
            Triple("chair", V2(1.75f, -12.75f), 2) to 0,
            Triple("monstera", V2(5.75f, -6.25f), 0) to 0,
            Triple("bookshelf", V2(4.75f, -6.25f), 2) to 0,
            Triple("bench", V2(0f, 12.75f), 2) to 0,
            Triple("flower_bed", V2(-3f, 13.25f), 2) to 0,
            Triple("flower_bed", V2(3f, 13.25f), 2) to 2,
        )
        var uid = 1L
        val placed = ArrayList<PlacedItem>()
        for ((t, variant) in items) {
            val (id, want, rot) = t
            val def = Catalog[id] ?: continue
            val zone = Placement.zoneOf(def.zone)
            val pos = Placement.snap(def, rot, zone, want)
            val spot = if (Placement.isValid(def, pos, rot, zone, placed)) pos else Placement.findSpot(def, rot, zone, placed, pos) ?: continue
            placed += PlacedItem(uid++, id, spot.x, spot.z, rot, variant, zone)
        }
        return SaveData(
            placed = placed,
            nextUid = uid,
            inventory = mapOf("teddy" to 1, "cactus" to 1, "gnome" to 1),
        )
    }
}
