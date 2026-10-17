package com.cozyhome.app.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cozyhome.app.data.SaveStore
import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.decor.ItemDef
import com.cozyhome.game.decor.Layer
import com.cozyhome.game.decor.Placement
import com.cozyhome.game.game.FrameInput
import com.cozyhome.game.game.GameWorld
import com.cozyhome.game.game.Mode
import com.cozyhome.game.game.Msg
import com.cozyhome.game.game.SaveAccess
import com.cozyhome.game.game.WorldEvent
import com.cozyhome.game.garden.Garden
import com.cozyhome.game.garden.PlotStatus
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.sports.BasketballGame
import com.cozyhome.game.sports.JogRace
import com.cozyhome.game.sports.PenaltyGame
import com.cozyhome.game.sports.SportEvent
import com.cozyhome.game.state.Appearance
import com.cozyhome.game.state.PlacedItem
import com.cozyhome.game.state.Rules
import com.cozyhome.game.state.SaveData
import com.cozyhome.game.state.Zone
import com.cozyhome.game.world.DayClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

enum class Haptic { LIGHT, SUCCESS, FAIL }

class GameViewModel(application: Application, private val store: SaveStore) : AndroidViewModel(application) {

    var save by mutableStateOf(SaveData())
        private set
    var screen by mutableStateOf(Screen.LOADING)
        private set
    var editingLook by mutableStateOf(false)
        private set

    val hud = HudState()
    val input = FrameInput()
    private val events = ArrayList<WorldEvent>()
    private val _haptics = MutableSharedFlow<Haptic>(extraBufferCapacity = 16)
    val haptics: SharedFlow<Haptic> = _haptics
    private var saveJob: Job? = null
    private var idSeq = 0L
    private var plotTimer = 0f

    private val access = object : SaveAccess {
        override val current: SaveData get() = save
        override fun update(transform: (SaveData) -> SaveData): SaveData {
            save = transform(save)
            scheduleSave()
            return save
        }
    }

    val world = GameWorld(access, TimeZone.getDefault().rawOffset.toLong())

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { store.load() }
            save = loaded ?: Rules.starter()
            screen = if (save.created) Screen.PLAY else Screen.CREATE
            refreshPlots(System.currentTimeMillis())
        }
    }

    val level: Int get() = Rules.level(save.xp)

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(700)
            val snapshot = save
            withContext(Dispatchers.IO) { store.write(snapshot) }
        }
    }

    fun flush() {
        saveJob?.cancel()
        val snapshot = save
        viewModelScope.launch(Dispatchers.IO) { store.write(snapshot) }
    }

    fun tick(dt: Float, now: Long) {
        events.clear()
        world.step(input, dt, now, events)
        handleEvents(now)
        syncHud(dt, now)
    }

    private fun syncHud(dt: Float, now: Long) {
        hud.mode = world.mode
        hud.interaction = world.interaction
        hud.inside = world.frame.inside
        hud.hourQuarter = (DayClock.hour(now, save.clockOffsetMs) * 4f).toInt()
        plotTimer -= dt
        if (plotTimer <= 0f) {
            plotTimer = 0.5f
            refreshPlots(now)
            hud.giftReady = save.lastGiftDay != Rules.dayIndex(now, TimeZone.getDefault().rawOffset.toLong())
        }
        world.basketball?.let { g ->
            hud.basketShot = g.shot
            hud.basketScore = g.score
            hud.basketMade = g.made
            hud.basketStreak = g.streak
            hud.basketAiming = g.phase == BasketballGame.Phase.AIM
            hud.basketMeter.floatValue = g.meter
            hud.basketZone.floatValue = g.zoneHalf
        }
        world.soccer?.let { g ->
            hud.soccerKicks = g.kicks
            hud.soccerGoals = g.goals
            if (hud.soccerResults.size != g.results.size) hud.soccerResults = g.results.toList()
            hud.soccerAiming = g.phase == PenaltyGame.Phase.AIM
        }
        world.jog?.let { r ->
            hud.jogTenths = (r.elapsedMs / 100).toInt()
            hud.jogLap = r.lap
            hud.jogGate = r.gate
            hud.jogCountdown = if (r.phase == JogRace.Phase.COUNTDOWN) kotlin.math.ceil(r.countdown).toInt() else 0
            hud.jogRunning = r.phase == JogRace.Phase.RUN
            hud.jogStamina.floatValue = r.stamina
            hud.jogOnTrack = r.onTrack
        }
        hud.bounceCombo = world.bounceCombo
        world.decorShift = if (hud.decorEdit == null) 3.4f else 0f
        if (hud.toasts.isNotEmpty() && now - hud.toasts.first().bornAt > 2600) hud.toasts.removeAt(0)
        if (hud.rewards.isNotEmpty() && now - hud.rewards.first().bornAt > 1500) hud.rewards.removeAt(0)
    }

    private fun refreshPlots(now: Long) {
        val visuals = save.plots.map { p ->
            val settled = Garden.settle(p, now)
            val status = when (Garden.status(p, now)) {
                PlotStatus.Empty -> 0
                PlotStatus.Thirsty -> 1
                is PlotStatus.Growing -> 2
                PlotStatus.Ripe -> 3
            }
            PlotVisual(settled.crop, settled.stage, settled.watered, status)
        }
        if (visuals != hud.plotVisuals) hud.plotVisuals = visuals
    }

    private fun handleEvents(now: Long) {
        for (e in events) {
            when (e) {
                is WorldEvent.OpenSeeds -> hud.seedPlot = e.plot
                is WorldEvent.Toast -> toast(e.msg, e.arg, now)
                is WorldEvent.Reward -> if (e.coins != 0 || e.xp != 0) hud.rewards += RewardPop(idSeq++, e.coins, e.xp, now)
                is WorldEvent.LevelUp -> {
                    hud.levelUp = e.level
                    _haptics.tryEmit(Haptic.SUCCESS)
                }
                is WorldEvent.SportResult -> {
                    hud.result = e
                    _haptics.tryEmit(Haptic.SUCCESS)
                }
                is WorldEvent.Sport -> when (e.event) {
                    is SportEvent.Basket, SportEvent.Goal -> _haptics.tryEmit(Haptic.SUCCESS)
                    SportEvent.RimHit, SportEvent.PostHit, SportEvent.KeeperSave, is SportEvent.Gate, SportEvent.Go -> _haptics.tryEmit(Haptic.LIGHT)
                    SportEvent.BasketMiss, SportEvent.ShotMissed -> _haptics.tryEmit(Haptic.FAIL)
                    else -> Unit
                }
            }
        }
        if (events.any { it is WorldEvent.Reward && it.coins > 0 }) _haptics.tryEmit(Haptic.LIGHT)
    }

    private fun toast(msg: Msg, arg: String = "", now: Long = System.currentTimeMillis()) {
        if (hud.toasts.lastOrNull()?.let { it.msg == msg && it.arg == arg && now - it.bornAt < 1200 } == true) return
        hud.toasts += ToastItem(idSeq++, msg, arg, now)
        while (hud.toasts.size > 3) hud.toasts.removeAt(0)
    }

    fun primary() {
        world.primary(System.currentTimeMillis(), events.also { it.clear() })
        handleEvents(System.currentTimeMillis())
    }

    fun plant(cropId: String) {
        val plot = hud.seedPlot ?: return
        hud.seedPlot = null
        events.clear()
        world.plant(plot, cropId, System.currentTimeMillis(), events)
        handleEvents(System.currentTimeMillis())
    }

    fun dismissSeeds() {
        hud.seedPlot = null
    }

    fun kick(target: V3, power: Float) = world.kick(target, power)

    fun quitSport() {
        hud.result = null
        world.quitSport()
    }

    fun replaySport() {
        val kind = hud.result?.kind ?: return
        hud.result = null
        world.startSport(kind)
    }

    fun dismissLevelUp() {
        hud.levelUp = null
    }

    fun createCharacter(name: String, look: Appearance) {
        val base = if (save.created) save else save
        save = base.copy(created = true, name = name.trim().ifBlank { "Bạn" }.take(16), look = look)
        scheduleSave()
        if (!editingLook) toast(Msg.WELCOME, save.name)
        editingLook = false
        screen = Screen.PLAY
    }

    fun editLook() {
        editingLook = true
        world.quitSport()
        screen = Screen.CREATE
    }

    fun cancelEditLook() {
        editingLook = false
        screen = Screen.PLAY
    }

    fun resetGame() {
        saveJob?.cancel()
        store.clear()
        save = Rules.starter()
        world.restore(com.cozyhome.game.world.Lot.SPAWN, com.cozyhome.game.world.Lot.SPAWN_YAW)
        hud.decorEdit = null
        editingLook = false
        screen = Screen.CREATE
    }

    fun enterDecor() {
        val zone = if (world.frame.inside) Zone.HOUSE else if (Placement.zoneRect(Zone.YARD).inset(-4f).contains(world.playerPosition)) Zone.YARD else Zone.HOUSE
        hud.decorZone = zone
        hud.decorPanel = DecorPanel.CATALOG
        hud.decorEdit = null
        world.enterDecor(zone)
    }

    fun switchDecorZone(zone: Zone) {
        commitOrCancelEdit()
        hud.decorZone = zone
        world.enterDecor(zone)
    }

    fun exitDecor() {
        commitOrCancelEdit()
        world.exitDecor()
    }

    private fun commitOrCancelEdit() {
        val e = hud.decorEdit ?: return
        if (e.valid) confirmEdit() else hud.decorEdit = null
    }

    fun pickItem(def: ItemDef) {
        if (level < def.level) {
            toast(Msg.LOCKED, def.level.toString())
            return
        }
        val owned = save.inventory[def.id] ?: 0
        if (owned <= 0 && save.coins < def.price) {
            toast(Msg.NOT_ENOUGH_COINS)
            return
        }
        commitOrCancelEdit()
        val zone = hud.decorZone
        val near = world.camera.target.xz
        val spot = Placement.findSpot(def, 0, zone, save.placed, near)
        if (spot == null) {
            toast(Msg.NO_SPACE)
            return
        }
        val item = PlacedItem(save.nextUid, def.id, spot.x, spot.z, 0, 0, zone)
        hud.decorEdit = DecorEdit(item, isNew = true, fromShop = owned <= 0, valid = true)
        _haptics.tryEmit(Haptic.LIGHT)
    }

    private fun withEdit(transform: (PlacedItem, ItemDef) -> PlacedItem) {
        val e = hud.decorEdit ?: return
        val def = Catalog[e.item.itemId] ?: return
        val next = transform(e.item, def)
        hud.decorEdit = e.copy(item = next, valid = Placement.isValid(def, V2(next.x, next.z), next.rot, next.zone, save.placed, next.uid))
    }

    fun rotateEdit() = withEdit { item, def ->
        val rot = (item.rot + 1) % 4
        val p = Placement.snap(def, rot, item.zone, V2(item.x, item.z))
        item.copy(rot = rot, x = p.x, z = p.z)
    }

    fun recolorEdit() = withEdit { item, def -> item.copy(variant = (item.variant + 1) % def.variants.coerceAtLeast(1)) }

    fun moveEdit(to: V2) = withEdit { item, def ->
        val p = Placement.snap(def, item.rot, item.zone, to)
        if (p.x == item.x && p.z == item.z) item else item.copy(x = p.x, z = p.z)
    }

    fun confirmEdit() {
        val e = hud.decorEdit ?: return
        if (!e.valid) {
            toast(Msg.PLACE_INVALID)
            _haptics.tryEmit(Haptic.FAIL)
            return
        }
        val def = Catalog[e.item.itemId] ?: return
        access.update { s ->
            if (e.isNew) {
                val inv = s.inventory.toMutableMap()
                var coins = s.coins
                if (e.fromShop) coins -= def.price else inv[def.id] = ((inv[def.id] ?: 1) - 1).coerceAtLeast(0)
                s.copy(placed = s.placed + e.item, nextUid = s.nextUid + 1, coins = coins, inventory = inv.filterValues { it > 0 })
            } else {
                s.copy(placed = s.placed.map { if (it.uid == e.item.uid) e.item else it })
            }
        }
        if (e.isNew && e.fromShop) hud.rewards += RewardPop(idSeq++, -def.price, 0, System.currentTimeMillis())
        hud.decorEdit = null
        toast(if (e.isNew && e.fromShop) Msg.BOUGHT else Msg.PLACED)
        _haptics.tryEmit(Haptic.SUCCESS)
    }

    fun cancelEdit() {
        hud.decorEdit = null
    }

    fun storeEdit() {
        val e = hud.decorEdit ?: return
        if (e.isNew) {
            hud.decorEdit = null
            return
        }
        access.update { s ->
            val inv = s.inventory.toMutableMap()
            inv[e.item.itemId] = (inv[e.item.itemId] ?: 0) + 1
            s.copy(placed = s.placed.filter { it.uid != e.item.uid }, inventory = inv)
        }
        hud.decorEdit = null
        toast(Msg.STORED)
    }

    fun decorTap(origin: V3, dir: V3) {
        val zone = hud.decorZone
        val floor = rayAt(origin, dir, if (zone == Zone.HOUSE) 0.04f else 0f)
        var best: Pair<PlacedItem, ItemDef>? = null
        var bestT = Float.MAX_VALUE
        for (item in save.placed) {
            if (item.zone != zone || item.uid == hud.decorEdit?.item?.uid) continue
            val def = Catalog[item.itemId] ?: continue
            val fp = Placement.footprint(def, item.x, item.z, item.rot)
            val heights = if (def.layer == Layer.FLOOR) listOf(0.02f) else listOf(def.height * 0.85f, def.height * 0.5f, 0.1f)
            for (h in heights) {
                if (kotlin.math.abs(dir.y) < 1e-5f) continue
                val t = (h - origin.y) / dir.y
                if (t <= 0f) continue
                val p = V2(origin.x + dir.x * t, origin.z + dir.z * t)
                if (p in fp) {
                    val bias = if (def.layer == Layer.FLOOR) 1000f else 0f
                    if (t + bias < bestT) {
                        bestT = t + bias
                        best = item to def
                    }
                    break
                }
            }
        }
        val hit = best
        if (hit != null) {
            commitOrCancelEdit()
            val (item, def) = hit
            hud.decorEdit = DecorEdit(item, isNew = false, fromShop = false, valid = Placement.isValid(def, V2(item.x, item.z), item.rot, item.zone, save.placed, item.uid))
            _haptics.tryEmit(Haptic.LIGHT)
        } else if (hud.decorEdit != null && floor != null) {
            moveEdit(floor)
        }
    }

    fun rayAt(origin: V3, dir: V3, y: Float): V2? {
        if (kotlin.math.abs(dir.y) < 1e-5f) return null
        val t = (y - origin.y) / dir.y
        return if (t < 0f) null else V2(origin.x + dir.x * t, origin.z + dir.z * t)
    }

    fun grabsEdit(origin: V3, dir: V3): Boolean {
        val e = hud.decorEdit ?: return false
        val def = Catalog[e.item.itemId] ?: return false
        val fp = Placement.footprint(def, e.item.x, e.item.z, e.item.rot).inset(-0.5f)
        return listOf(0.04f, def.height * 0.5f, def.height).any { h -> rayAt(origin, dir, h)?.let { it in fp } == true }
    }

    fun setWallColor(index: Int) = access.update { it.copy(wallColor = index) }

    fun setFloor(index: Int) = access.update { it.copy(floorStyle = index) }

    fun isDecor() = world.mode == Mode.DECOR

    fun debugTeleport(x: Float, z: Float, yaw: Float) = world.restore(V2(x, z), yaw, yaw + 180f)

    fun debugCoins(coins: Int) = access.update { it.copy(coins = coins) }

    fun debugXp(xp: Int) = access.update { it.copy(xp = xp) }

    fun debugHour(hour: Float) = access.update { it.copy(clockOffsetMs = DayClock.offsetToReach(System.currentTimeMillis(), it.clockOffsetMs, hour)) }

    data class GfxDebug(val shadows: Boolean, val scale: Float, val post: Boolean, val scenery: Boolean)

    var gfxDebug by mutableStateOf<GfxDebug?>(null)

    fun debugGrow() = access.update { s -> s.copy(plots = s.plots.map { if (it.crop != null && it.watered) it.copy(stageStartedAt = 0L) else it }) }
}
