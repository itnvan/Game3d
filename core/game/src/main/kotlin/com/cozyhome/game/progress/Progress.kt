package com.cozyhome.game.progress

import com.cozyhome.game.activities.BugDex
import com.cozyhome.game.activities.Cookbook
import com.cozyhome.game.activities.FishDex
import com.cozyhome.game.garden.Crops
import com.cozyhome.game.math.Rng
import kotlinx.serialization.Serializable

data class CivilDate(val year: Int, val month: Int, val day: Int)

object CivilCalendar {
    fun fromEpochDay(epochDay: Long): CivilDate {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return CivilDate((y + if (m <= 2) 1 else 0).toInt(), m.toInt(), d.toInt())
    }

    fun toEpochDay(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val m = month.toLong()
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }
}

enum class Festival(val shopItems: List<String>) {
    LUNAR_NEW_YEAR(listOf("red_lantern", "apricot_tree", "firecracker_string")),
    VALENTINE(listOf("heart_arch", "rose_vase")),
    SPRING_BLOOM(listOf("cherry_tree", "flower_cart")),
    SUMMER_FUN(listOf("ice_cream_cart", "beach_chair")),
    MID_AUTUMN(listOf("star_lantern", "rabbit_lantern", "mooncake_table")),
    HALLOWEEN(listOf("jack_o_lantern", "candy_cauldron", "ghost_lamp")),
    CHRISTMAS(listOf("xmas_tree", "snowman", "gift_pile")),
}

data class FestivalWindow(val festival: Festival, val startDay: Long, val endDay: Long)

object Festivals {
    private val LUNAR = mapOf(2025 to (1 to 29), 2026 to (2 to 17), 2027 to (2 to 6), 2028 to (1 to 26), 2029 to (2 to 13), 2030 to (2 to 3))
    private val MOON = mapOf(2025 to (10 to 6), 2026 to (9 to 25), 2027 to (9 to 15), 2028 to (10 to 3), 2029 to (9 to 22), 2030 to (9 to 12))

    fun windows(year: Int): List<FestivalWindow> {
        fun d(m: Int, day: Int, y: Int = year) = CivilCalendar.toEpochDay(y, m, day)
        val lunar = LUNAR[year] ?: (2 to 5)
        val moon = MOON[year] ?: (9 to 20)
        return listOf(
            FestivalWindow(Festival.LUNAR_NEW_YEAR, d(lunar.first, lunar.second) - 7, d(lunar.first, lunar.second) + 9),
            FestivalWindow(Festival.VALENTINE, d(2, 8), d(2, 15)),
            FestivalWindow(Festival.SPRING_BLOOM, d(3, 20), d(4, 20)),
            FestivalWindow(Festival.SUMMER_FUN, d(6, 20), d(8, 20)),
            FestivalWindow(Festival.MID_AUTUMN, d(moon.first, moon.second) - 10, d(moon.first, moon.second) + 3),
            FestivalWindow(Festival.HALLOWEEN, d(10, 20), d(11, 2)),
            FestivalWindow(Festival.CHRISTMAS, d(12, 12), d(1, 2, year + 1)),
        )
    }

    fun active(epochDay: Long): FestivalWindow? {
        val y = CivilCalendar.fromEpochDay(epochDay).year
        return (windows(y - 1) + windows(y)).firstOrNull { epochDay in it.startDay..it.endDay }
    }

    fun next(epochDay: Long): FestivalWindow {
        val y = CivilCalendar.fromEpochDay(epochDay).year
        return (windows(y) + windows(y + 1)).filter { it.startDay > epochDay }.minBy { it.startDay }
    }

    fun isFestivalItem(id: String): Boolean = Festival.entries.any { id in it.shopItems }
    fun festivalOf(id: String): Festival? = Festival.entries.firstOrNull { id in it.shopItems }
}

enum class QuestKind(val baseTarget: Int, val perLevel: Float, val coins: Int, val xp: Int) {
    HARVEST(3, 0.3f, 60, 14), PLANT(3, 0.3f, 40, 10), WATER(4, 0.4f, 35, 8), FISH(2, 0.2f, 70, 16), BUG(2, 0.2f, 60, 14),
    COOK(1, 0.1f, 90, 20), BASKET(8, 0.6f, 55, 12), SOCCER(3, 0.2f, 55, 12), JOG(1, 0f, 50, 12), DECORATE(1, 0.1f, 45, 10),
    BOUNCE(10, 0.5f, 35, 8), RARE_FISH(1, 0f, 120, 26),
}

@Serializable
data class QuestSlot(
    val kind: String,
    val target: Int,
    val progress: Int = 0,
    val claimed: Boolean = false,
    val coins: Int,
    val xp: Int,
    val tokens: Int = 0,
) {
    val done: Boolean get() = progress >= target
}

@Serializable
data class Daily(
    val day: Long = -1L,
    val quests: List<QuestSlot> = emptyList(),
    val loginDay: Long = -1L,
    val streak: Int = 0,
    val loginClaimed: Boolean = true,
)

@Serializable
data class CatchRecord(val count: Int = 0, val bestCm: Int = 0)

@Serializable
data class Album(
    val fish: Map<String, CatchRecord> = emptyMap(),
    val bugs: Map<String, Int> = emptyMap(),
    val dishes: Map<String, Int> = emptyMap(),
    val crops: Map<String, Int> = emptyMap(),
    val milestones: Set<String> = emptySet(),
)

data class LoginReward(val coins: Int, val xp: Int, val tokens: Int = 0, val item: String? = null)

enum class AlbumBook(val total: () -> Int) {
    FISH({ FishDex.all.size }), BUGS({ BugDex.all.size }), DISHES({ Cookbook.all.size }), CROPS({ Crops.all.size }),
}

data class Milestone(val key: String, val book: AlbumBook, val need: Int, val coins: Int, val item: String?)

object Progression {
    val LOGIN = listOf(
        LoginReward(60, 10), LoginReward(80, 12), LoginReward(100, 15), LoginReward(120, 18),
        LoginReward(150, 20), LoginReward(180, 25), LoginReward(300, 40, tokens = 3, item = "golden_trophy"),
    )

    fun rollQuests(day: Long, level: Int, festival: Boolean): List<QuestSlot> {
        val rng = Rng(day * 7919L + 13)
        val pool = QuestKind.entries.filter { level >= 2 || it != QuestKind.RARE_FISH }.toMutableList()
        val picked = ArrayList<QuestKind>()
        repeat(3) {
            val k = pool[(rng.nextFloat() * pool.size).toInt().coerceAtMost(pool.size - 1)]
            pool.remove(k)
            picked += k
        }
        return picked.map { k ->
            val target = (k.baseTarget + (level - 1) * k.perLevel).toInt().coerceAtLeast(1)
            QuestSlot(k.name, target, coins = k.coins + level * 4, xp = k.xp + level, tokens = if (festival) 2 else 0)
        }
    }

    fun refreshDaily(d: Daily, today: Long, level: Int, festival: Boolean): Daily {
        var out = d
        if (d.day != today) out = out.copy(day = today, quests = rollQuests(today, level, festival))
        if (d.loginDay != today) {
            val streak = if (d.loginDay == today - 1) (d.streak % LOGIN.size) + 1 else 1
            out = out.copy(loginDay = today, streak = streak, loginClaimed = false)
        }
        return out
    }

    fun advance(d: Daily, kind: QuestKind, amount: Int): Daily =
        d.copy(quests = d.quests.map { if (it.kind == kind.name && !it.claimed) it.copy(progress = (it.progress + amount).coerceAtMost(it.target)) else it })

    val MILESTONES = listOf(
        Milestone("fish_5", AlbumBook.FISH, 5, 200, null), Milestone("fish_all", AlbumBook.FISH, FishDex.all.size, 800, "fish_trophy"),
        Milestone("bugs_5", AlbumBook.BUGS, 5, 200, null), Milestone("bugs_all", AlbumBook.BUGS, BugDex.all.size, 800, "bug_case"),
        Milestone("dishes_4", AlbumBook.DISHES, 4, 250, null), Milestone("dishes_all", AlbumBook.DISHES, Cookbook.all.size, 900, "chef_statue"),
        Milestone("crops_all", AlbumBook.CROPS, Crops.all.size, 500, null),
    )

    fun found(a: Album, book: AlbumBook): Int = when (book) {
        AlbumBook.FISH -> a.fish.size
        AlbumBook.BUGS -> a.bugs.size
        AlbumBook.DISHES -> a.dishes.size
        AlbumBook.CROPS -> a.crops.size
    }

    fun unclaimedMilestones(a: Album): List<Milestone> = MILESTONES.filter { it.key !in a.milestones && found(a, it.book) >= it.need }
}
