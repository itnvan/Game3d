package com.cozyhome.game

import com.cozyhome.game.activities.FishingSession
import com.cozyhome.game.game.ActionKind
import com.cozyhome.game.game.Emote
import com.cozyhome.game.game.FrameInput
import com.cozyhome.game.game.GameWorld
import com.cozyhome.game.game.Mode
import com.cozyhome.game.game.SaveAccess
import com.cozyhome.game.game.WorldEvent
import com.cozyhome.game.math.V2
import com.cozyhome.game.progress.Progression
import com.cozyhome.game.progress.QuestKind
import com.cozyhome.game.progress.QuestSlot
import com.cozyhome.game.state.Rules
import com.cozyhome.game.state.SaveData
import com.cozyhome.game.world.Lot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldActivitiesTest {
    private class Store(var data: SaveData) : SaveAccess {
        override val current: SaveData get() = data
        override fun update(transform: (SaveData) -> SaveData): SaveData {
            data = transform(data)
            return data
        }
    }

    private val now = 1_790_000_000_000L

    private fun world(save: SaveData = Rules.starter().copy(created = true)): Pair<GameWorld, Store> {
        val store = Store(save)
        return GameWorld(store, 0L, seed = 7) to store
    }

    private fun run(w: GameWorld, input: FrameInput, seconds: Float, events: MutableList<WorldEvent>, each: () -> Unit = {}) {
        var t = 0f
        while (t < seconds) {
            w.step(input, 1f / 60f, now, events)
            each()
            t += 1f / 60f
        }
    }

    @Test
    fun fishingAtThePondFillsTheAlbumAndQuests() {
        val (w, store) = world()
        store.data = store.data.copy(daily = store.data.daily.copy(day = Rules.dayIndex(now, 0L), quests = listOf(QuestSlot(QuestKind.FISH.name, 1, coins = 10, xp = 1))))
        val c = Lot.POND.center
        w.restore(V2(c.x - Lot.POND.r - 0.8f, c.z), 90f)
        val input = FrameInput()
        val events = ArrayList<WorldEvent>()
        run(w, input, 0.2f, events)
        assertEquals(ActionKind.FISH, w.interaction?.kind)
        var caught = false
        repeat(6) {
            if (caught) return@repeat
            w.primary(now, events)
            assertEquals(Mode.FISH, w.mode)
            run(w, input, 40f, events) {
                val s = w.fishing
                if (s != null && s.phase == FishingSession.Phase.BITE && s.phaseTime > 0.15f) w.primary(now, events)
                input.hold = (w.fishing?.tension ?: 0f) < 0.65f
                if (events.any { it is WorldEvent.FishCaught }) caught = true
            }
            input.hold = false
            run(w, input, 0.3f, events)
        }
        assertTrue("never caught a fish", caught)
        assertEquals(Mode.FREE, w.mode)
        assertTrue(store.data.album.fish.isNotEmpty())
        val fish = events.filterIsInstance<WorldEvent.FishCaught>().first()
        if (!com.cozyhome.game.activities.FishDex[fish.species]!!.junk) assertTrue(store.data.daily.quests.first().done)
    }

    @Test
    fun cookingNeedsPantryAndRecordsDish() {
        val (w, store) = world()
        val events = ArrayList<WorldEvent>()
        w.cook("carrot_soup", events)
        assertTrue(events.any { it is WorldEvent.Toast })
        store.data = store.data.copy(pantry = mapOf("carrot" to 3))
        events.clear()
        w.cook("carrot_soup", events)
        assertEquals(Mode.ACTION, w.mode)
        run(w, FrameInput(), 3f, events)
        assertEquals(mapOf("carrot" to 1), store.data.pantry)
        assertTrue("carrot_soup" in store.data.album.dishes)
        assertTrue(events.any { it is WorldEvent.Cooked })
    }

    @Test
    fun harvestAddsProduceToThePantry() {
        val (w, store) = world()
        val ripe = store.data.plots.mapIndexed { i, p -> if (i == 0) p.copy(crop = "lettuce", stage = 3, watered = true, stageStartedAt = 0L) else p }
        store.data = store.data.copy(plots = ripe)
        w.restore(Lot.PLOTS[0] + V2(0f, 1.2f), 180f)
        val events = ArrayList<WorldEvent>()
        run(w, FrameInput(), 0.2f, events)
        assertEquals(ActionKind.HARVEST, w.interaction?.kind)
        w.primary(now, events)
        run(w, FrameInput(), 1.5f, events)
        assertEquals(2, store.data.pantry["lettuce"])
        assertEquals(1, store.data.album.crops["lettuce"])
    }

    @Test
    fun bugsAppearDuringTheDayAndCanBeNetted() {
        val (w, store) = world()
        val events = ArrayList<WorldEvent>()
        w.restore(V2(30f, 30f), 0f)
        run(w, FrameInput(), 90f, events)
        val bug = w.bugs.bugs.firstOrNull()
        assertNotNull(bug)
        var caughtOrMissed = false
        repeat(8) {
            val b = w.bugs.bugs.firstOrNull() ?: return@repeat
            w.restore(V2(b.pos.x, b.pos.z + 0.5f), 180f)
            run(w, FrameInput(), 1f / 60f, events)
            if (w.interaction?.kind == ActionKind.CATCH_BUG) {
                w.primary(now, events)
                run(w, FrameInput(), 1f, events)
                caughtOrMissed = true
            }
        }
        assertTrue(caughtOrMissed)
        assertTrue(store.data.album.bugs.isNotEmpty() || events.any { it is WorldEvent.Toast })
    }

    @Test
    fun emotePlaysThenReturnsToFree() {
        val (w, _) = world()
        val events = ArrayList<WorldEvent>()
        run(w, FrameInput(), 0.1f, events)
        w.emote(Emote.DANCE)
        assertEquals(Mode.ACTION, w.mode)
        run(w, FrameInput(), 1f / 60f, events)
        assertEquals(Emote.DANCE, w.frame.emote)
        run(w, FrameInput(), 4f, events)
        assertEquals(Mode.FREE, w.mode)
    }

    @Test
    fun dailyRefreshGivesThreeQuestsAndLogin() {
        val d = Progression.refreshDaily(Rules.starter().daily, 500, 3, festival = true)
        assertEquals(3, d.quests.size)
        assertTrue(d.quests.all { it.tokens > 0 })
        assertTrue(!d.loginClaimed)
    }
}
