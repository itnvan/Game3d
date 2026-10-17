package com.cozyhome.game.audio

import com.cozyhome.game.activities.FishEvent
import com.cozyhome.game.game.ActionKind
import com.cozyhome.game.game.Msg
import com.cozyhome.game.game.WorldEvent
import com.cozyhome.game.sports.SportEvent
import com.cozyhome.game.sports.SportKind
import com.cozyhome.game.world.Surface
import kotlin.math.min

data class Cue(val sfx: Sfx, val volume: Float = 1f, val pitch: Float = 1f)

object SoundCues {
    private val GATE_STEPS = floatArrayOf(1f, 1.125f, 1.25f, 1.5f, 1.667f, 2f, 2.25f, 2.5f)

    fun collect(events: List<WorldEvent>): List<Cue> {
        if (events.isEmpty()) return emptyList()
        val levelUp = events.any { it is WorldEvent.LevelUp }
        val lap = events.any { it is WorldEvent.Sport && it.event is SportEvent.Lap }
        val out = ArrayList<Cue>()
        for (e in events) {
            if (levelUp && e is WorldEvent.SportResult) continue
            if (lap && e is WorldEvent.Sport && e.event is SportEvent.Gate) continue
            for (cue in of(e)) if (out.none { it.sfx == cue.sfx }) out += cue
        }
        return out
    }

    fun of(e: WorldEvent): List<Cue> = when (e) {
        is WorldEvent.Footstep -> listOf(Cue(step(e.surface), if (e.running) 1f else 0.7f))
        is WorldEvent.Bounced -> listOf(if (e.combo > 0) Cue(Sfx.BOING, 1f, 1f + min(e.combo, 12) * 0.035f) else Cue(Sfx.BOING, 0.35f, 0.92f))
        is WorldEvent.Act -> of(e.kind)
        is WorldEvent.Reward -> when {
            e.coins > 0 -> listOf(Cue(Sfx.COIN))
            e.coins < 0 -> listOf(Cue(Sfx.SPEND))
            else -> emptyList()
        }
        is WorldEvent.LevelUp -> listOf(Cue(Sfx.LEVEL_UP))
        is WorldEvent.SportResult -> if (e.best) listOf(Cue(Sfx.RESULT), Cue(Sfx.SPARKLE, 0.8f)) else listOf(Cue(Sfx.RESULT))
        is WorldEvent.Sport -> of(e.event)
        is WorldEvent.OpenSeeds, is WorldEvent.Toast, WorldEvent.OpenCook -> emptyList()
        is WorldEvent.FishCaught -> if (e.firstTime || e.record) listOf(Cue(Sfx.RESULT), Cue(Sfx.SPARKLE)) else listOf(Cue(Sfx.SPARKLE))
        is WorldEvent.BugCaught -> listOf(Cue(Sfx.SPARKLE, 0.75f, 1.2f))
        is WorldEvent.Cooked -> listOf(Cue(Sfx.HARVEST), Cue(Sfx.SPARKLE, 0.8f))
        is WorldEvent.Fish -> of(e.event)
    }

    fun of(e: FishEvent): List<Cue> = when (e) {
        FishEvent.Splash -> listOf(Cue(Sfx.WATER, 0.7f, 1.35f))
        FishEvent.Nibble -> listOf(Cue(Sfx.TAP, 0.35f, 1.6f))
        FishEvent.Bite -> listOf(Cue(Sfx.BEEP), Cue(Sfx.WATER, 0.5f, 1.6f))
        FishEvent.Hooked -> listOf(Cue(Sfx.WHOOSH))
        is FishEvent.Caught -> listOf(Cue(Sfx.WATER, 0.8f, 0.9f))
        FishEvent.Escaped -> listOf(Cue(Sfx.MISS))
        FishEvent.Snapped -> listOf(Cue(Sfx.BUZZ))
        FishEvent.TooEarly -> listOf(Cue(Sfx.ERROR, 0.5f))
    }

    fun of(e: SportEvent): List<Cue> = when (e) {
        SportEvent.Released -> listOf(Cue(Sfx.THROW))
        SportEvent.RimHit -> listOf(Cue(Sfx.RIM))
        SportEvent.BoardHit -> listOf(Cue(Sfx.BOARD))
        is SportEvent.Basket -> buildList {
            add(Cue(Sfx.SWISH, if (e.swish) 1f else 0.7f))
            add(Cue(Sfx.SCORE, pitch = 1f + (min(e.streak, 7) - 1).coerceAtLeast(0) * 0.06f))
            if (e.streak >= 3) add(Cue(Sfx.SPARKLE, 0.7f))
        }
        SportEvent.BasketMiss, SportEvent.ShotMissed -> listOf(Cue(Sfx.MISS))
        SportEvent.Kicked -> listOf(Cue(Sfx.KICK))
        SportEvent.PostHit -> listOf(Cue(Sfx.POST))
        SportEvent.KeeperSave -> listOf(Cue(Sfx.SAVE), Cue(Sfx.MISS, 0.8f))
        SportEvent.Goal -> listOf(Cue(Sfx.GOAL))
        is SportEvent.Countdown -> listOf(Cue(Sfx.BEEP))
        SportEvent.Go -> listOf(Cue(Sfx.GO))
        is SportEvent.Gate -> listOf(Cue(Sfx.GATE, pitch = GATE_STEPS[e.index.coerceAtLeast(0) % GATE_STEPS.size]))
        is SportEvent.Lap -> listOf(Cue(Sfx.LAP))
        SportEvent.OffTrack -> listOf(Cue(Sfx.BUZZ))
        is SportEvent.Finished -> emptyList()
        is SportEvent.Bounce -> {
            val volume = (e.speed / 5f).coerceIn(0.12f, 1f)
            listOf(Cue(if (e.kind == SportKind.BASKETBALL) Sfx.BALL_BOUNCE else Sfx.BALL_THUD, volume))
        }
    }

    fun of(kind: ActionKind): List<Cue> = when (kind) {
        ActionKind.WATER -> listOf(Cue(Sfx.WATER))
        ActionKind.WATER_ALL -> listOf(Cue(Sfx.HOSE))
        ActionKind.PLANT -> listOf(Cue(Sfx.DIG))
        ActionKind.HARVEST -> listOf(Cue(Sfx.DIG, 0.6f, 1.25f))
        ActionKind.SIT, ActionKind.SLEEP -> listOf(Cue(Sfx.SIT))
        ActionKind.SWING -> listOf(Cue(Sfx.WHOOSH))
        ActionKind.TV_ON, ActionKind.TV_OFF, ActionKind.LAMP_ON, ActionKind.LAMP_OFF -> listOf(Cue(Sfx.SWITCH))
        ActionKind.BASKETBALL, ActionKind.SOCCER -> listOf(Cue(Sfx.WHISTLE))
        ActionKind.FISH -> listOf(Cue(Sfx.WHOOSH, 0.8f, 0.85f))
        ActionKind.CATCH_BUG -> listOf(Cue(Sfx.WHOOSH, 0.7f, 1.3f))
        ActionKind.COOK -> listOf(Cue(Sfx.SWITCH))
        ActionKind.JOG, ActionKind.GIFT, ActionKind.BOUNCE, ActionKind.GROWING -> emptyList()
    }

    fun of(msg: Msg): List<Cue> = when (msg) {
        Msg.NOT_ENOUGH_COINS, Msg.LOCKED, Msg.PLACE_INVALID, Msg.NO_SPACE, Msg.NOTHING_TO_WATER, Msg.GIFT_TAKEN -> listOf(Cue(Sfx.ERROR))
        Msg.HARVESTED -> listOf(Cue(Sfx.HARVEST))
        Msg.GIFT_RECEIVED, Msg.PERFECT_BOUNCE -> listOf(Cue(Sfx.SPARKLE))
        Msg.GOOD_NIGHT -> listOf(Cue(Sfx.LULLABY))
        Msg.BOUGHT -> listOf(Cue(Sfx.PLACE), Cue(Sfx.SPEND))
        Msg.PLACED -> listOf(Cue(Sfx.PLACE))
        Msg.STORED -> listOf(Cue(Sfx.STORE))
        Msg.BUG_MISSED, Msg.MISSING_INGREDIENTS -> listOf(Cue(Sfx.ERROR))
        Msg.QUEST_DONE, Msg.MILESTONE -> listOf(Cue(Sfx.SPARKLE))
        Msg.FESTIVAL_TOKENS -> listOf(Cue(Sfx.COIN, 0.8f, 1.3f))
        Msg.FISH_ESCAPED, Msg.FISH_SNAPPED, Msg.TOO_EARLY, Msg.BUG_CAUGHT, Msg.COOKED -> emptyList()
        Msg.GROWING, Msg.WATERED_ALL, Msg.PLANTED, Msg.NAP, Msg.LEVEL_UP, Msg.OFF_TRACK, Msg.WELCOME -> emptyList()
    }

    fun step(surface: Surface): Sfx = when (surface) {
        Surface.GRASS -> Sfx.STEP_GRASS
        Surface.WOOD -> Sfx.STEP_WOOD
        Surface.HARD -> Sfx.STEP_HARD
    }
}
