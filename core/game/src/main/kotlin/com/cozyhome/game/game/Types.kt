package com.cozyhome.game.game

import com.cozyhome.game.anim.Pose
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.sports.SportEvent
import com.cozyhome.game.sports.SportKind
import com.cozyhome.game.state.SaveData
import com.cozyhome.game.world.WallSide

enum class Mode { FREE, SIT, SLEEP, ACTION, BASKETBALL, SOCCER, JOG, DECOR, BOUNCE, SWING }

enum class ActionKind {
    PLANT, WATER, HARVEST, GROWING, BASKETBALL, SOCCER, JOG, GIFT, WATER_ALL,
    SIT, SLEEP, TV_ON, TV_OFF, LAMP_ON, LAMP_OFF, BOUNCE, SWING,
}

sealed interface Target {
    data class PlotSpot(val index: Int) : Target
    data object Hoop : Target
    data object PenaltySpot : Target
    data object TrackStart : Target
    data object Mailbox : Target
    data object Tap : Target
    data class Furniture(val uid: Long) : Target
}

data class Interaction(val target: Target, val kind: ActionKind, val detail: String = "")

enum class Msg {
    GROWING, GIFT_TAKEN, GIFT_RECEIVED, NOTHING_TO_WATER, WATERED_ALL, NOT_ENOUGH_COINS, PLANTED,
    HARVESTED, GOOD_NIGHT, NAP, LEVEL_UP, OFF_TRACK, PERFECT_BOUNCE, PLACE_INVALID, STORED, BOUGHT,
    LOCKED, NO_SPACE, PLACED, WELCOME,
}

sealed interface WorldEvent {
    data class OpenSeeds(val plot: Int) : WorldEvent
    data class Toast(val msg: Msg, val arg: String = "") : WorldEvent
    data class Reward(val coins: Int, val xp: Int, val at: V3?) : WorldEvent
    data class Sport(val event: SportEvent) : WorldEvent
    data class SportResult(val kind: SportKind, val score: Int, val detail: Int, val coins: Int, val best: Boolean, val millis: Long) : WorldEvent
    data class LevelUp(val level: Int) : WorldEvent
}

class FrameInput {
    var stickX = 0f
    var stickY = 0f
    var lookYaw = 0f
    var lookPitch = 0f
    var zoom = 1f
    var sprint = false
}

class FrameState {
    var position = V3.ZERO
    var yaw = 0f
    var pose = Pose()
    var showCan = false
    var holdBall = false
    var eye = V3.ZERO
    var target = V3.ZERO
    var inside = false
    val wallCut = BooleanArray(WallSide.entries.size)
    var roofHidden = false
    var basketBall = V3.ZERO
    var basketSpin = 0f
    var netWobble = 0f
    var soccerBall = V3.ZERO
    var soccerSpin = 0f
    var keeperZ = 0f
    var keeperLean = 0f
    var keeperLift = 0f
    var netPush = 0f
    var nextGate: V2? = null
    var swingUid = -1L
    var swingAngle = 0f
    var bounceUid = -1L
    var bounceSquash = 0f
    var waterTime = 0f
    var clock = 0f
}

interface SaveAccess {
    val current: SaveData
    fun update(transform: (SaveData) -> SaveData): SaveData
}
