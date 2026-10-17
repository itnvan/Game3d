package com.cozyhome.game

import com.cozyhome.game.activities.BugField
import com.cozyhome.game.activities.Cookbook
import com.cozyhome.game.activities.FishDex
import com.cozyhome.game.activities.FishEvent
import com.cozyhome.game.activities.FishingSession
import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.progress.CivilCalendar
import com.cozyhome.game.progress.Daily
import com.cozyhome.game.progress.Festival
import com.cozyhome.game.progress.Festivals
import com.cozyhome.game.progress.Progression
import com.cozyhome.game.progress.QuestKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivitiesTest {

    private fun play(seed: Long, hour: Float, careful: Boolean): FishingSession.Phase {
        val s = FishingSession(Rng(seed), hour, V3(0f, 2f, 0f), V3(0f, 0f, 3f))
        val events = ArrayList<FishEvent>()
        var t = 0f
        while (t < 60f && !s.done) {
            val hold = if (careful) s.tension < 0.7f else true
            s.update(1f / 60f, hold, events)
            if (s.phase == FishingSession.Phase.BITE && s.phaseTime > 0.2f) s.tap(events)
            t += 1f / 60f
        }
        return s.phase
    }

    @Test
    fun carefulReelingLandsMostFish() {
        val results = (0 until 60).map { play(it.toLong(), 12f, careful = true) }
        val caught = results.count { it == FishingSession.Phase.CAUGHT }
        assertTrue("caught $caught/60", caught >= 50)
    }

    @Test
    fun holdingForeverSnapsStrongFish() {
        val results = (0 until 60).map { play(it.toLong() + 1000, 21f, careful = false) }
        assertTrue(results.any { it == FishingSession.Phase.SNAPPED })
    }

    @Test
    fun nightPoolHasCatfishAndNoTrout() {
        val rng = Rng(3)
        val seen = (0 until 2000).map { FishDex.roll(rng, 23f).id }.toSet()
        assertTrue("catfish" in seen)
        assertTrue("trout" !in seen)
    }

    @Test
    fun calendarRoundTrips() {
        for (day in listOf(0L, 18_000L, 20_744L, 21_000L, -400L)) {
            val d = CivilCalendar.fromEpochDay(day)
            assertEquals(day, CivilCalendar.toEpochDay(d.year, d.month, d.day))
        }
        val oct18 = CivilCalendar.toEpochDay(2026, 10, 18)
        assertEquals(CivilCalendar.fromEpochDay(oct18).month, 10)
    }

    @Test
    fun festivalsFollowTheCalendar() {
        val oct18 = CivilCalendar.toEpochDay(2026, 10, 18)
        assertNull(Festivals.active(oct18))
        assertEquals(Festival.HALLOWEEN, Festivals.next(oct18).festival)
        assertEquals(Festival.HALLOWEEN, Festivals.active(CivilCalendar.toEpochDay(2026, 10, 31))?.festival)
        assertEquals(Festival.CHRISTMAS, Festivals.active(CivilCalendar.toEpochDay(2027, 1, 1))?.festival)
        assertEquals(Festival.LUNAR_NEW_YEAR, Festivals.active(CivilCalendar.toEpochDay(2027, 2, 6))?.festival)
        assertEquals(Festival.MID_AUTUMN, Festivals.active(CivilCalendar.toEpochDay(2026, 9, 25))?.festival)
    }

    @Test
    fun loginStreakAdvancesAndResets() {
        var d = Progression.refreshDaily(Daily(), 100, 1, false)
        assertEquals(1, d.streak)
        assertEquals(3, d.quests.size)
        d = Progression.refreshDaily(d.copy(loginClaimed = true), 101, 1, false)
        assertEquals(2, d.streak)
        assertTrue(!d.loginClaimed)
        d = Progression.refreshDaily(d, 105, 1, false)
        assertEquals(1, d.streak)
        var week = Daily()
        for (k in 0 until 8) week = Progression.refreshDaily(week, 200L + k, 1, false)
        assertEquals(1, week.streak)
    }

    @Test
    fun questProgressCapsAtTarget() {
        val d = Daily(day = 1, quests = Progression.rollQuests(1, 1, false))
        val kind = QuestKind.valueOf(d.quests[0].kind)
        val after = Progression.advance(d, kind, 999)
        assertEquals(after.quests[0].target, after.quests[0].progress)
    }

    @Test
    fun cookingConsumesIngredients() {
        val salad = Cookbook["salad"]!!
        val pantry = mapOf("lettuce" to 2, "tomato" to 3)
        assertTrue(Cookbook.canCook(salad, pantry))
        assertEquals(mapOf("tomato" to 2), Cookbook.consume(salad, pantry))
        assertTrue(!Cookbook.canCook(salad, mapOf("lettuce" to 1)))
    }

    @Test
    fun bugsSpawnAndCanBeCaught() {
        val field = BugField(Rng(9))
        repeat(60 * 60) { field.update(1f / 60f, 11f, V2(30f, 30f), false) }
        assertTrue(field.bugs.isNotEmpty())
        val b = field.bugs.first()
        val near = field.nearest(V2(b.pos.x, b.pos.z), 1.5f)
        assertTrue(near != null)
    }
}
