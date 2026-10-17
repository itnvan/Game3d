package com.cozyhome.app.game

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cozyhome.game.game.Interaction
import com.cozyhome.game.game.Mode
import com.cozyhome.game.game.Msg
import com.cozyhome.game.game.WorldEvent
import com.cozyhome.game.state.PlacedItem
import com.cozyhome.game.state.Zone

enum class Screen { LOADING, CREATE, PLAY }

enum class DecorPanel { CATALOG, PAINT }

enum class HudPanel { QUESTS, ALBUM, LOGIN, EMOTES, COOK }

data class PlotVisual(val crop: String?, val stage: Int, val wet: Boolean, val status: Int)

data class DecorEdit(val item: PlacedItem, val isNew: Boolean, val fromShop: Boolean, val valid: Boolean)

data class ToastItem(val id: Long, val msg: Msg, val arg: String, val bornAt: Long)

data class RewardPop(val id: Long, val coins: Int, val xp: Int, val bornAt: Long)

@Stable
class HudState {
    var mode by mutableStateOf(Mode.FREE)
    var interaction by mutableStateOf<Interaction?>(null)
    var hourQuarter by mutableIntStateOf(36)
    var plotVisuals by mutableStateOf(List(12) { PlotVisual(null, 0, false, 0) })
    var giftReady by mutableStateOf(false)
    var inside by mutableStateOf(false)

    var basketShot by mutableIntStateOf(0)
    var basketScore by mutableIntStateOf(0)
    var basketMade by mutableIntStateOf(0)
    var basketStreak by mutableIntStateOf(0)
    var basketAiming by mutableStateOf(false)
    var basketLastPoints by mutableIntStateOf(0)
    val basketMeter = mutableFloatStateOf(0f)
    val basketZone = mutableFloatStateOf(0.1f)

    var soccerKicks by mutableIntStateOf(0)
    var soccerGoals by mutableIntStateOf(0)
    var soccerResults by mutableStateOf<List<Boolean>>(emptyList())
    var soccerAiming by mutableStateOf(false)

    var jogTenths by mutableIntStateOf(0)
    var jogLap by mutableIntStateOf(1)
    var jogGate by mutableIntStateOf(0)
    var jogCountdown by mutableIntStateOf(0)
    var jogRunning by mutableStateOf(false)
    val jogStamina = mutableFloatStateOf(1f)
    var jogOnTrack by mutableStateOf(true)

    var bounceCombo by mutableIntStateOf(0)
    var result by mutableStateOf<WorldEvent.SportResult?>(null)
    var seedPlot by mutableStateOf<Int?>(null)
    var levelUp by mutableStateOf<Int?>(null)

    var decorZone by mutableStateOf(Zone.HOUSE)
    var decorPanel by mutableStateOf(DecorPanel.CATALOG)
    var decorEdit by mutableStateOf<DecorEdit?>(null)

    var fishPhase by mutableIntStateOf(-1)
    val fishTension = mutableFloatStateOf(0f)
    val fishProgress = mutableFloatStateOf(0f)
    var fishCard by mutableStateOf<WorldEvent.FishCaught?>(null)
    var bugCard by mutableStateOf<WorldEvent.BugCaught?>(null)
    var dishCard by mutableStateOf<WorldEvent.Cooked?>(null)
    var panel by mutableStateOf<HudPanel?>(null)
    var festival by mutableStateOf<com.cozyhome.game.progress.Festival?>(null)
    var festivalNext by mutableStateOf<Pair<com.cozyhome.game.progress.Festival, Int>?>(null)
    var questsReady by mutableIntStateOf(0)
    var loginReady by mutableStateOf(false)
    var albumReady by mutableIntStateOf(0)

    val toasts = mutableStateListOf<ToastItem>()
    val rewards = mutableStateListOf<RewardPop>()
}
