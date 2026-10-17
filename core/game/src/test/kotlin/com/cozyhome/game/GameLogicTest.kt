package com.cozyhome.game

import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.decor.Placement
import com.cozyhome.game.game.FrameInput
import com.cozyhome.game.game.GameWorld
import com.cozyhome.game.game.SaveAccess
import com.cozyhome.game.game.WorldEvent
import com.cozyhome.game.garden.Crops
import com.cozyhome.game.garden.Garden
import com.cozyhome.game.garden.PlotStatus
import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.sports.BasketballGame
import com.cozyhome.game.sports.JogRace
import com.cozyhome.game.sports.PenaltyGame
import com.cozyhome.game.sports.SportEvent
import com.cozyhome.game.state.Plot
import com.cozyhome.game.state.Rules
import com.cozyhome.game.state.SaveData
import com.cozyhome.game.state.Zone
import com.cozyhome.game.world.CollisionWorld
import com.cozyhome.game.world.Lot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    @Test
    fun starterLayoutPlacesEveryItemValidly() {
        val s = Rules.starter()
        assertEquals(18, s.placed.size)
        for (item in s.placed) {
            val def = Catalog[item.itemId]!!
            val others = s.placed.filter { it.uid != item.uid }
            assertTrue("${item.itemId} invalid", Placement.isValid(def, V2(item.x, item.z), item.rot, item.zone, others))
        }
    }

    @Test
    fun snapKeepsFootprintOnGridInsideZone() {
        val def = Catalog["sofa"]!!
        for (rot in 0..3) {
            val p = Placement.snap(def, rot, Zone.HOUSE, V2(100f, -100f))
            val fp = Placement.footprint(def, p.x, p.z, rot)
            assertTrue(fp.minX >= Lot.ROOM.minX - 1e-3f && fp.maxX <= Lot.ROOM.maxX + 1e-3f)
            assertTrue(fp.minZ >= Lot.ROOM.minZ - 1e-3f && fp.maxZ <= Lot.ROOM.maxZ + 1e-3f)
            val k = (fp.minX - Lot.ROOM.minX) / Catalog.CELL
            assertEquals(Math.round(k).toFloat(), k, 1e-3f)
        }
    }

    @Test
    fun cropNeedsWaterEachStage() {
        val crop = Crops["lettuce"]!!
        val stage = Garden.stageMillis(crop)
        var p = Garden.plant(Plot(), crop.id, 0L)
        assertEquals(PlotStatus.Thirsty, Garden.status(p, 10L))
        p = Garden.water(p, 1000L)
        assertTrue(Garden.status(p, 1000L + stage / 2) is PlotStatus.Growing)
        assertEquals(PlotStatus.Thirsty, Garden.status(p, 1000L + stage + 1))
        p = Garden.settle(p, 1000L + stage + 1)
        assertEquals(1, p.stage)
        p = Garden.water(p, 5_000_000L)
        p = Garden.settle(p, 5_000_000L + stage)
        p = Garden.water(p, 9_000_000L)
        assertEquals(PlotStatus.Ripe, Garden.status(p, 9_000_000L + stage))
    }

    @Test
    fun perfectBasketballShotScores() {
        var made = 0
        repeat(30) { seed ->
            val g = BasketballGame(Rng(seed.toLong()), totalShots = 1)
            val events = ArrayList<SportEvent>()
            var guard = 0
            while (kotlin.math.abs(g.meter - BasketballGame.ZONE_CENTER) > 0.01f && guard++ < 5000) g.update(1 / 600f, events)
            g.release()
            repeat(600) { g.update(1 / 60f, events) }
            if (events.any { it is SportEvent.Basket }) made++
        }
        assertTrue("only $made/30 perfect shots scored", made >= 27)
    }

    @Test
    fun wildBasketballShotMisses() {
        var made = 0
        repeat(20) { seed ->
            val g = BasketballGame(Rng(seed.toLong()), totalShots = 1)
            val events = ArrayList<SportEvent>()
            g.update(0.001f, events)
            g.release()
            repeat(600) { g.update(1 / 60f, events) }
            if (events.any { it is SportEvent.Basket }) made++
        }
        assertTrue("$made/20 bottom-of-meter shots scored", made <= 2)
    }

    @Test
    fun penaltySessionFinishes() {
        val g = PenaltyGame(Rng(7))
        val events = ArrayList<SportEvent>()
        var guard = 0
        while (g.phase != PenaltyGame.Phase.DONE && guard++ < 10_000) {
            if (g.phase == PenaltyGame.Phase.AIM) g.kick(V3(Lot.GOAL_X, 1.4f, Lot.GOAL_Z + 1.5f), 0.9f)
            g.update(1 / 60f, events)
        }
        assertEquals(PenaltyGame.Phase.DONE, g.phase)
        assertEquals(5, g.kicks)
        assertTrue(events.any { it is SportEvent.Finished })
    }

    @Test
    fun cornerShotsBeatKeeperOften() {
        var goals = 0
        repeat(40) { seed ->
            val g = PenaltyGame(Rng(seed.toLong()), totalKicks = 1)
            val events = ArrayList<SportEvent>()
            g.kick(V3(Lot.GOAL_X, 1.3f, Lot.GOAL_Z + 1.45f), 0.85f)
            repeat(400) { g.update(1 / 60f, events) }
            goals += g.goals
        }
        assertTrue("corner goals $goals/40", goals in 16..38)
    }

    @Test
    fun trackIsLoopAndAboutFiftyEightMeters() {
        val t = Lot.TRACK
        assertEquals(58f, t.length, 1.5f)
        assertTrue(t.isOnTrack(Lot.TRACK_START))
        assertFalse(t.isOnTrack(V2(0f, 7f)))
        val race = JogRace(t)
        val events = ArrayList<SportEvent>()
        var s = 0f
        repeat(4 * 60) { race.update(1 / 60f, t.pointAt(0f), false, events) }
        assertEquals(JogRace.Phase.RUN, race.phase)
        while (race.phase != JogRace.Phase.DONE && s < t.length * 4) {
            s += 0.1f
            race.update(0.1f / JogRace.RUN_SPEED, t.pointAt(s), false, events)
        }
        assertEquals(JogRace.Phase.DONE, race.phase)
        assertTrue(race.elapsedMs in 26_000L..31_000L)
    }

    @Test
    fun collisionKeepsPlayerOutOfWalls() {
        val c = CollisionWorld(Lot.WALK_BOUNDS).apply { setStatic(Lot.staticSolids()) }
        var p = V2(-3f, -3f)
        repeat(200) { p = c.move(p, V2(0f, -0.05f), 0.3f) }
        assertTrue("walked through south wall: $p", p.z > -5.8f + 0.29f)
        var q = V2(0f, -3f)
        repeat(200) { q = c.move(q, V2(0f, -0.05f), 0.3f) }
        assertTrue("door should be passable: $q", q.z < -10f)
    }

    @Test
    fun levelCurveIsMonotonic() {
        assertEquals(1, Rules.level(0))
        assertEquals(2, Rules.level(60))
        assertEquals(3, Rules.level(180))
        assertEquals(2, Rules.level(179))
        var last = 1
        for (xp in 0..20000 step 7) {
            val l = Rules.level(xp)
            assertTrue(l >= last)
            last = l
        }
    }

    @Test
    fun worldWalksToHoopAndStartsBasketball() {
        var data = Rules.starter()
        val access = object : SaveAccess {
            override val current: SaveData get() = data
            override fun update(transform: (SaveData) -> SaveData): SaveData { data = transform(data); return data }
        }
        val world = GameWorld(access, 0L, seed = 1)
        world.restore(V2(-15f, -9.5f), 180f)
        val input = FrameInput()
        val events = ArrayList<WorldEvent>()
        repeat(30) { world.step(input, 1 / 60f, 1000L, events) }
        assertNotNull(world.interaction)
        world.primary(1000L, events)
        assertEquals(com.cozyhome.game.game.Mode.BASKETBALL, world.mode)
    }
}
