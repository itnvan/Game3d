package com.cozyhome.game.game

import com.cozyhome.game.anim.Anim
import com.cozyhome.game.anim.Animator
import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.decor.Placement
import com.cozyhome.game.decor.Use
import com.cozyhome.game.garden.Crops
import com.cozyhome.game.garden.Garden
import com.cozyhome.game.garden.PlotStatus
import com.cozyhome.game.math.DEG
import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.damp
import com.cozyhome.game.math.dampAngle
import com.cozyhome.game.math.v3
import com.cozyhome.game.math.yawOf
import com.cozyhome.game.sports.BasketballGame
import com.cozyhome.game.sports.JogRace
import com.cozyhome.game.sports.PenaltyGame
import com.cozyhome.game.sports.SportEvent
import com.cozyhome.game.sports.SportKind
import com.cozyhome.game.state.PlacedItem
import com.cozyhome.game.state.Rules
import com.cozyhome.game.state.SaveData
import com.cozyhome.game.state.Zone
import com.cozyhome.game.world.CameraRig
import com.cozyhome.game.world.CollisionWorld
import com.cozyhome.game.world.DayClock
import com.cozyhome.game.world.FollowCamera
import com.cozyhome.game.world.Lot
import com.cozyhome.game.world.WallSide
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

class GameWorld(
    private val save: SaveAccess,
    private val utcOffsetMs: Long,
    seed: Long = System.nanoTime(),
) {
    private val rng = Rng(seed)
    private val collision = CollisionWorld(Lot.WALK_BOUNDS).apply { setStatic(Lot.staticSolids()) }
    private val walls = Lot.wallSolids()
    private val wallSides = listOf(WallSide.WEST, WallSide.EAST, WallSide.NORTH, WallSide.SOUTH, WallSide.SOUTH)
    private val wallTimers = FloatArray(WallSide.entries.size)
    private var roofTimer = 0f
    private val sportEvents = ArrayList<SportEvent>()

    val animator = Animator()
    val camera = FollowCamera()
    val frame = FrameState().also { it.pose = animator.pose }

    var mode = Mode.FREE
        private set
    var interaction: Interaction? = null
        private set
    var basketball: BasketballGame? = null
        private set
    var soccer: PenaltyGame? = null
        private set
    var jog: JogRace? = null
        private set
    var activeUid = -1L
        private set
    var decorZone = Zone.HOUSE
        private set
    var decorShift = 0f
    private var savedPitch = 38f
    private var savedDistance = 9.5f
    var bounceCombo = 0
        private set

    private var pos = Lot.SPAWN
    private var visualPos = Lot.SPAWN
    private var yaw = Lot.SPAWN_YAW
    private var vel = V2.ZERO
    private var rootY = 0f
    private var actionTime = 0f
    private var actionDuration = 0f
    private var actionAnim = Anim.IDLE
    private var actionDone: (() -> Unit)? = null
    private var placedRef: List<PlacedItem>? = null
    private var sleepTimer = 0f
    private var sleptThisTime = false
    private var bounceH = 0f
    private var bounceV = 0f
    private var bounceTap = 0f
    private var bounceXp = 0
    private var swingAngle = 0f
    private var swingVel = 0f
    private var pendingKick: Pair<V3, Float>? = null
    private var kickDelay = 0f
    private var jogHeading = V2(-1f, 0f)

    init {
        camera.snap(V3(pos.x, 1.1f, pos.z), 0f)
    }

    val playerPosition: V2 get() = pos
    val playerYaw: Float get() = yaw

    fun restore(p: V2, yawDeg: Float, cameraYaw: Float = 0f) {
        pos = collision.resolve(p, RADIUS)
        visualPos = pos
        yaw = yawDeg
        camera.snap(V3(pos.x, 1.1f, pos.z), cameraYaw)
    }

    fun step(input: FrameInput, dt: Float, now: Long, events: MutableList<WorldEvent>) {
        syncSolids()
        frame.clock += dt
        var extra = 0f
        when (mode) {
            Mode.FREE -> moveFree(input, dt, MAX_SPEED)
            Mode.SIT -> if (stickActive(input)) standUp()
            Mode.SLEEP -> {
                sleepTimer += dt
                if (!sleptThisTime && sleepTimer > 1.6f) wakeClock(now, events)
                if (stickActive(input) && sleepTimer > 0.6f) standUp()
            }
            Mode.ACTION -> {
                actionTime += dt
                vel = V2.ZERO
                if (actionTime >= actionDuration) {
                    mode = Mode.FREE
                    val done = actionDone
                    actionDone = null
                    done?.invoke()
                }
            }
            Mode.BASKETBALL -> updateBasket(dt, events)
            Mode.SOCCER -> updateSoccer(dt, events)
            Mode.JOG -> updateJog(input, dt, events)
            Mode.DECOR -> vel = V2.ZERO
            Mode.BOUNCE -> extra = updateBounce(dt, events)
            Mode.SWING -> extra = updateSwing(dt)
        }
        animator.update(dt, vel.length, extra)
        interaction = if (mode == Mode.FREE) findInteraction(now) else null
        updateCamera(input, dt)
        updateWalls(dt)
        writeFrame()
    }

    private fun stickActive(input: FrameInput) = input.stickX * input.stickX + input.stickY * input.stickY > 0.3f

    private fun moveFree(input: FrameInput, dt: Float, maxSpeed: Float) {
        val cy = camera.yaw * DEG
        val fwd = V2(-sin(cy), -cos(cy))
        val right = V2(cos(cy), -sin(cy))
        var desired = right * input.stickX + fwd * input.stickY
        if (desired.length > 1f) desired = desired.normalized()
        desired *= maxSpeed
        val k = if (desired.length > 0.05f) 10f else 14f
        vel = V2(damp(vel.x, desired.x, k, dt), damp(vel.z, desired.z, k, dt))
        pos = collision.move(pos, vel * dt, RADIUS)
        visualPos = pos
        rootY = damp(rootY, 0f, 12f, dt)
        if (desired.length > 0.2f) yaw = dampAngle(yaw, yawOf(desired), 12f, dt)
        animator.play(
            when {
                vel.length > 4.1f -> Anim.RUN
                vel.length > 0.25f -> Anim.WALK
                else -> Anim.IDLE
            }
        )
    }

    private fun syncSolids() {
        val placed = save.current.placed
        if (placed !== placedRef) {
            placedRef = placed
            collision.setDynamic(Placement.solids(placed))
        }
    }

    private fun findInteraction(now: Long): Interaction? {
        val s = save.current
        var best: Interaction? = null
        var bestD = Float.MAX_VALUE
        fun offer(d: Float, radius: Float, make: () -> Interaction?) {
            if (d < radius && d < bestD) {
                val i = make() ?: return
                best = i
                bestD = d
            }
        }
        Lot.PLOTS.forEachIndexed { index, c ->
            offer(pos.distanceTo(c), Lot.PLOT_SIZE * 0.5f + 0.7f) {
                val plot = s.plots.getOrNull(index) ?: return@offer null
                when (val st = Garden.status(plot, now)) {
                    PlotStatus.Empty -> Interaction(Target.PlotSpot(index), ActionKind.PLANT)
                    PlotStatus.Thirsty -> Interaction(Target.PlotSpot(index), ActionKind.WATER, plot.crop ?: "")
                    is PlotStatus.Growing -> Interaction(Target.PlotSpot(index), ActionKind.GROWING, st.secondsLeft.toString())
                    PlotStatus.Ripe -> Interaction(Target.PlotSpot(index), ActionKind.HARVEST, plot.crop ?: "")
                }
            }
        }
        offer(pos.distanceTo(Lot.SHOT_SPOTS[0]), 1.6f) { Interaction(Target.Hoop, ActionKind.BASKETBALL) }
        offer(pos.distanceTo(Lot.KICK_STAND), 1.6f) { Interaction(Target.PenaltySpot, ActionKind.SOCCER) }
        offer(pos.distanceTo(Lot.TRACK_START), 1.8f) { Interaction(Target.TrackStart, ActionKind.JOG) }
        offer(pos.distanceTo(Lot.MAILBOX), 1.7f) { Interaction(Target.Mailbox, ActionKind.GIFT) }
        offer(pos.distanceTo(Lot.GARDEN_TAP), 1.6f) { Interaction(Target.Tap, ActionKind.WATER_ALL) }
        for (item in s.placed) {
            val def = Catalog[item.itemId] ?: continue
            if (def.use == Use.NONE) continue
            val fp = Placement.footprint(def, item.x, item.z, item.rot)
            offer(pos.distanceTo(fp.closest(pos)), 0.8f) {
                val kind = when (def.use) {
                    Use.SIT -> ActionKind.SIT
                    Use.SLEEP -> ActionKind.SLEEP
                    Use.TV -> if (item.on) ActionKind.TV_OFF else ActionKind.TV_ON
                    Use.LAMP -> if (item.on) ActionKind.LAMP_OFF else ActionKind.LAMP_ON
                    Use.BOUNCE -> ActionKind.BOUNCE
                    Use.SWING -> ActionKind.SWING
                    Use.NONE -> return@offer null
                }
                Interaction(Target.Furniture(item.uid), kind, item.itemId)
            }
        }
        return best
    }

    fun primary(now: Long, events: MutableList<WorldEvent>) {
        when (mode) {
            Mode.SIT, Mode.SLEEP -> { standUp(); return }
            Mode.BOUNCE -> { bounceTap = 0.32f; return }
            Mode.SWING -> { swingPush(); return }
            Mode.BASKETBALL -> {
                if (basketball?.release() == true) animator.play(Anim.SHOOT_RELEASE)
                return
            }
            Mode.FREE -> Unit
            else -> return
        }
        val i = interaction ?: return
        when (i.kind) {
            ActionKind.PLANT -> (i.target as? Target.PlotSpot)?.let { events += WorldEvent.OpenSeeds(it.index) }
            ActionKind.WATER -> (i.target as? Target.PlotSpot)?.let { t ->
                face(Lot.PLOTS[t.index])
                startAction(Anim.WATER, 1.1f) {
                    save.update { s -> s.copy(plots = s.plots.mapIndexed { idx, p -> if (idx == t.index) Garden.water(p, now) else p }) }
                    grant(0, 1, v3(Lot.PLOTS[t.index], 1.2f), events)
                }
            }
            ActionKind.HARVEST -> (i.target as? Target.PlotSpot)?.let { t -> harvest(t.index, now, events) }
            ActionKind.GROWING -> events += WorldEvent.Toast(Msg.GROWING, i.detail)
            ActionKind.BASKETBALL -> startBasketball()
            ActionKind.SOCCER -> startSoccer()
            ActionKind.JOG -> startJog()
            ActionKind.GIFT -> gift(now, events)
            ActionKind.WATER_ALL -> waterAll(now, events)
            ActionKind.SIT, ActionKind.SLEEP, ActionKind.BOUNCE, ActionKind.SWING ->
                (i.target as? Target.Furniture)?.let { useFurniture(it.uid, i.kind, now, events) }
            ActionKind.TV_ON, ActionKind.TV_OFF, ActionKind.LAMP_ON, ActionKind.LAMP_OFF ->
                (i.target as? Target.Furniture)?.let { t ->
                    save.update { s -> s.copy(placed = s.placed.map { if (it.uid == t.uid) it.copy(on = !it.on) else it }) }
                }
        }
    }

    fun plant(plotIndex: Int, cropId: String, now: Long, events: MutableList<WorldEvent>) {
        val crop = Crops[cropId] ?: return
        val s = save.current
        if (s.coins < crop.seedPrice) {
            events += WorldEvent.Toast(Msg.NOT_ENOUGH_COINS)
            return
        }
        val plot = s.plots.getOrNull(plotIndex) ?: return
        if (plot.crop != null) return
        face(Lot.PLOTS[plotIndex])
        save.update { cur ->
            cur.copy(
                coins = cur.coins - crop.seedPrice,
                plots = cur.plots.mapIndexed { idx, p -> if (idx == plotIndex) Garden.plant(p, cropId, now) else p },
            )
        }
        events += WorldEvent.Reward(-crop.seedPrice, 0, v3(Lot.PLOTS[plotIndex], 1.2f))
        startAction(Anim.HARVEST, 0.7f) { events += WorldEvent.Toast(Msg.PLANTED, cropId) }
    }

    private fun harvest(index: Int, now: Long, events: MutableList<WorldEvent>) {
        val plot = save.current.plots.getOrNull(index) ?: return
        val crop = Crops[plot.crop] ?: return
        if (Garden.status(plot, now) != PlotStatus.Ripe) return
        face(Lot.PLOTS[index])
        startAction(Anim.HARVEST, 0.75f) {
            save.update { s ->
                s.copy(
                    plots = s.plots.mapIndexed { idx, p -> if (idx == index) Garden.harvest(p) else p },
                    stats = s.stats.copy(harvested = s.stats.harvested + 1),
                )
            }
            grant(crop.sellPrice, crop.xp, v3(Lot.PLOTS[index], 1.3f), events)
            events += WorldEvent.Toast(Msg.HARVESTED, crop.id)
        }
    }

    private fun waterAll(now: Long, events: MutableList<WorldEvent>) {
        val thirsty = save.current.plots.withIndex().filter { Garden.status(it.value, now) == PlotStatus.Thirsty }.map { it.index }
        if (thirsty.isEmpty()) {
            events += WorldEvent.Toast(Msg.NOTHING_TO_WATER)
            return
        }
        face(Lot.GARDEN_TAP)
        startAction(Anim.WATER, 1.6f) {
            save.update { s -> s.copy(plots = s.plots.mapIndexed { idx, p -> if (idx in thirsty) Garden.water(p, now) else p }) }
            grant(0, thirsty.size, v3(Lot.GARDEN_TAP, 1.4f), events)
            events += WorldEvent.Toast(Msg.WATERED_ALL, thirsty.size.toString())
        }
    }

    private fun gift(now: Long, events: MutableList<WorldEvent>) {
        val today = Rules.dayIndex(now, utcOffsetMs)
        if (save.current.lastGiftDay == today) {
            events += WorldEvent.Toast(Msg.GIFT_TAKEN)
            return
        }
        face(Lot.MAILBOX)
        save.update { it.copy(lastGiftDay = today) }
        startAction(Anim.WAVE, 1.0f) {
            val coins = Rules.giftCoins(Rules.level(save.current.xp))
            grant(coins, 10, v3(Lot.MAILBOX, 1.6f), events)
            events += WorldEvent.Toast(Msg.GIFT_RECEIVED, coins.toString())
        }
    }

    fun grant(coins: Int, xp: Int, at: V3?, events: MutableList<WorldEvent>) {
        if (coins == 0 && xp == 0) return
        val before = Rules.level(save.current.xp)
        val after = save.update { it.copy(coins = (it.coins + coins).coerceAtLeast(0), xp = it.xp + xp) }
        events += WorldEvent.Reward(coins, xp, at)
        val level = Rules.level(after.xp)
        if (level > before) events += WorldEvent.LevelUp(level)
    }

    private fun face(target: V2) {
        val d = target - pos
        if (d.length > 0.05f) yaw = yawOf(d)
    }

    private fun startAction(anim: Anim, duration: Float, done: () -> Unit) {
        mode = Mode.ACTION
        actionAnim = anim
        actionTime = 0f
        actionDuration = duration
        actionDone = done
        vel = V2.ZERO
        animator.play(anim)
    }

    private fun useFurniture(uid: Long, kind: ActionKind, now: Long, events: MutableList<WorldEvent>) {
        val item = save.current.placed.firstOrNull { it.uid == uid } ?: return
        val def = Catalog[item.itemId] ?: return
        val seat = def.seat ?: return
        activeUid = uid
        vel = V2.ZERO
        yaw = Placement.yaw(item.rot)
        when (kind) {
            ActionKind.SIT -> {
                val p = Placement.toWorld(item, seat)
                visualPos = p.xz
                rootY = p.y - HIP
                mode = Mode.SIT
                animator.play(Anim.SIT)
            }
            ActionKind.SLEEP -> {
                val foot = Placement.toWorld(item, V3(seat.x, seat.y, def.d * Catalog.CELL / 2f - 0.28f))
                visualPos = foot.xz
                rootY = foot.y + 0.14f
                mode = Mode.SLEEP
                sleepTimer = 0f
                sleptThisTime = !DayClock.isNight(DayClock.hour(now, save.current.clockOffsetMs))
                if (sleptThisTime) events += WorldEvent.Toast(Msg.NAP)
                animator.play(Anim.SLEEP)
            }
            ActionKind.BOUNCE -> {
                val p = Placement.toWorld(item, seat)
                visualPos = p.xz
                rootY = p.y
                bounceH = 0f
                bounceV = 4.2f
                bounceCombo = 0
                bounceXp = 0
                mode = Mode.BOUNCE
                animator.play(Anim.BOUNCE)
            }
            ActionKind.SWING -> {
                swingAngle = 0f
                swingVel = 0.8f
                mode = Mode.SWING
                animator.play(Anim.SWING)
            }
            else -> Unit
        }
    }

    private fun wakeClock(now: Long, events: MutableList<WorldEvent>) {
        sleptThisTime = true
        save.update { it.copy(clockOffsetMs = DayClock.offsetToReach(now, it.clockOffsetMs, DayClock.WAKE_HOUR)) }
        grant(0, 5, null, events)
        events += WorldEvent.Toast(Msg.GOOD_NIGHT)
    }

    fun standUp() {
        val item = save.current.placed.firstOrNull { it.uid == activeUid }
        val def = item?.let { Catalog[it.itemId] }
        if (item != null && def != null) {
            val depth = Placement.cellsD(def, 0) * Catalog.CELL
            val front = Placement.frontOf(item, depth / 2f + 0.45f)
            pos = collision.resolve(front, RADIUS)
        }
        visualPos = pos
        rootY = 0f
        activeUid = -1L
        mode = Mode.FREE
        frame.swingUid = -1L
        frame.bounceUid = -1L
        animator.play(Anim.IDLE)
    }

    private fun updateBounce(dt: Float, events: MutableList<WorldEvent>): Float {
        bounceTap = (bounceTap - dt).coerceAtLeast(0f)
        bounceV -= 13f * dt
        bounceH += bounceV * dt
        frame.bounceSquash = damp(frame.bounceSquash, 0f, 10f, dt)
        if (bounceH <= 0f && bounceV < 0f) {
            bounceH = 0f
            frame.bounceSquash = 1f
            if (bounceTap > 0f) {
                bounceTap = 0f
                bounceCombo++
                bounceV = (abs(bounceV) * 0.95f + 1.4f).coerceAtMost(10.5f)
                if (bounceXp < 20) {
                    bounceXp++
                    grant(0, 1, null, events)
                }
                if (bounceCombo % 5 == 0) events += WorldEvent.Toast(Msg.PERFECT_BOUNCE, bounceCombo.toString())
            } else {
                bounceCombo = 0
                bounceV = max(4.2f, abs(bounceV) * 0.7f)
            }
        }
        frame.bounceUid = activeUid
        return bounceH
    }

    private fun swingPush() {
        swingVel += if (abs(swingVel) < 0.05f) 0.9f else sign(swingVel) * 0.55f
    }

    private fun updateSwing(dt: Float): Float {
        val item = save.current.placed.firstOrNull { it.uid == activeUid } ?: run { standUp(); return 0f }
        val g = 9.81f / SWING_LENGTH
        swingVel += (-g * sin(swingAngle) - 0.18f * swingVel) * dt
        swingAngle = (swingAngle + swingVel * dt).coerceIn(-1.05f, 1.05f)
        val pivot = Placement.toWorld(item, V3(0f, SWING_PIVOT, 0f))
        val fwd = Placement.frontOf(item, 1f) - V2(item.x, item.z)
        val seatOffset = fwd * (sin(swingAngle) * SWING_LENGTH)
        visualPos = pivot.xz + seatOffset
        rootY = pivot.y - cos(swingAngle) * SWING_LENGTH - HIP + 0.06f
        frame.swingUid = activeUid
        frame.swingAngle = swingAngle
        return swingAngle * 57.3f
    }

    private fun startBasketball() {
        val game = BasketballGame(rng)
        basketball = game
        mode = Mode.BASKETBALL
        pos = game.spot
        visualPos = pos
        vel = V2.ZERO
        yaw = yawOf(game.facing)
        animator.play(Anim.SHOOT_AIM)
    }

    private fun updateBasket(dt: Float, events: MutableList<WorldEvent>) {
        val game = basketball ?: return
        sportEvents.clear()
        game.update(dt, sportEvents)
        visualPos = V2(damp(visualPos.x, game.spot.x, 6f, dt), damp(visualPos.z, game.spot.z, 6f, dt))
        pos = visualPos
        yaw = dampAngle(yaw, yawOf(game.facing), 8f, dt)
        animator.play(
            when (game.phase) {
                BasketballGame.Phase.AIM -> Anim.SHOOT_AIM
                BasketballGame.Phase.FLIGHT -> if (game.phaseTime < 0.45f) Anim.SHOOT_RELEASE else Anim.IDLE
                BasketballGame.Phase.RESULT -> if (game.lastScored) Anim.CHEER else Anim.SAD
                BasketballGame.Phase.DONE -> if (game.made >= game.totalShots / 2) Anim.CHEER else Anim.WAVE
            }
        )
        forwardSport(events)
    }

    private fun startSoccer() {
        val game = PenaltyGame(rng)
        soccer = game
        mode = Mode.SOCCER
        pos = Lot.KICK_STAND
        visualPos = pos
        vel = V2.ZERO
        yaw = yawOf(Lot.PENALTY_SPOT - Lot.KICK_STAND)
        pendingKick = null
        animator.play(Anim.IDLE)
    }

    fun kick(target: V3, power: Float) {
        val game = soccer ?: return
        if (mode != Mode.SOCCER || game.phase != PenaltyGame.Phase.AIM || pendingKick != null) return
        pendingKick = target to power
        kickDelay = 0.3f
        animator.play(Anim.KICK)
    }

    private fun updateSoccer(dt: Float, events: MutableList<WorldEvent>) {
        val game = soccer ?: return
        sportEvents.clear()
        pendingKick?.let { (t, p) ->
            kickDelay -= dt
            if (kickDelay <= 0f) {
                pendingKick = null
                if (game.kick(t, p)) sportEvents += SportEvent.Kicked
            }
        }
        game.update(dt, sportEvents)
        if (pendingKick == null) {
            animator.play(
                when (game.phase) {
                    PenaltyGame.Phase.AIM -> Anim.IDLE
                    PenaltyGame.Phase.FLIGHT -> if (game.phaseTime < 0.5f) Anim.KICK else Anim.IDLE
                    PenaltyGame.Phase.RESULT -> if (game.lastGoal) Anim.CHEER else Anim.SAD
                    PenaltyGame.Phase.DONE -> if (game.goals >= 3) Anim.CHEER else Anim.WAVE
                }
            )
        }
        forwardSport(events)
    }

    private fun startJog() {
        val race = JogRace(Lot.TRACK)
        jog = race
        mode = Mode.JOG
        pos = race.startPoint
        visualPos = pos
        vel = V2.ZERO
        jogHeading = race.startDirection
        yaw = yawOf(jogHeading)
        camera.snap(V3(pos.x, 1.1f, pos.z), yawOf(-jogHeading))
        animator.play(Anim.IDLE)
    }

    private fun updateJog(input: FrameInput, dt: Float, events: MutableList<WorldEvent>) {
        val race = jog ?: return
        sportEvents.clear()
        race.update(dt, pos, input.sprint, sportEvents)
        if (race.phase == JogRace.Phase.RUN) {
            moveFree(input, dt, race.speed())
            if (vel.length > 0.5f) jogHeading = vel.normalized()
        } else {
            vel = V2.ZERO
            animator.play(if (race.phase == JogRace.Phase.DONE) (if (race.medal > 0) Anim.CHEER else Anim.WAVE) else Anim.IDLE)
        }
        frame.nextGate = if (race.phase == JogRace.Phase.DONE) null else race.nextGatePosition
        forwardSport(events)
    }

    private fun forwardSport(events: MutableList<WorldEvent>) {
        for (e in sportEvents) {
            events += WorldEvent.Sport(e)
            if (e is SportEvent.Finished) finishSport(e, events)
            if (e is SportEvent.OffTrack) events += WorldEvent.Toast(Msg.OFF_TRACK)
        }
        sportEvents.clear()
    }

    private fun finishSport(e: SportEvent.Finished, events: MutableList<WorldEvent>) {
        val stats = save.current.stats
        val (coins, best, newStats) = when (e.kind) {
            SportKind.BASKETBALL -> Triple(Rules.basketCoins(e.score), e.score > stats.basketBest, stats.copy(basketBest = max(stats.basketBest, e.score)))
            SportKind.SOCCER -> Triple(Rules.soccerCoins(e.score), e.score > stats.soccerBest, stats.copy(soccerBest = max(stats.soccerBest, e.score)))
            SportKind.JOGGING -> {
                val isBest = stats.jogBestMs == 0L || e.millis < stats.jogBestMs
                Triple(Rules.jogCoins(e.score), isBest, stats.copy(jogBestMs = if (isBest) e.millis else stats.jogBestMs))
            }
        }
        save.update { it.copy(stats = newStats.copy(gamesPlayed = newStats.gamesPlayed + 1)) }
        grant(coins, 8 + e.score, null, events)
        events += WorldEvent.SportResult(e.kind, e.score, e.detail, coins, best, e.millis)
    }

    fun startSport(kind: SportKind) {
        quitSport()
        when (kind) {
            SportKind.BASKETBALL -> startBasketball()
            SportKind.SOCCER -> startSoccer()
            SportKind.JOGGING -> startJog()
        }
    }

    fun quitSport() {
        basketball = null
        soccer = null
        jog = null
        pendingKick = null
        frame.nextGate = null
        if (mode == Mode.BASKETBALL || mode == Mode.SOCCER || mode == Mode.JOG) {
            mode = Mode.FREE
            pos = collision.resolve(pos, RADIUS)
            visualPos = pos
            animator.play(Anim.IDLE)
        }
    }

    fun enterDecor(zone: Zone) {
        val wasDecor = mode == Mode.DECOR
        if (mode == Mode.SIT || mode == Mode.SLEEP || mode == Mode.BOUNCE || mode == Mode.SWING) standUp()
        quitSport()
        if (!wasDecor) {
            savedPitch = camera.pitch
            savedDistance = camera.distance
        }
        decorZone = zone
        mode = Mode.DECOR
        camera.setOrbit(58f, if (zone == Zone.HOUSE) 14f else 16f)
        vel = V2.ZERO
        animator.play(Anim.IDLE)
    }

    fun exitDecor() {
        if (mode != Mode.DECOR) return
        mode = Mode.FREE
        camera.setOrbit(savedPitch, savedDistance)
        syncSolids()
        pos = collision.resolve(pos, RADIUS)
        visualPos = pos
    }

    fun nudgeOutOf() {
        pos = collision.resolve(pos, RADIUS)
        visualPos = pos
    }

    private fun updateCamera(input: FrameInput, dt: Float) {
        val look = Triple(input.lookYaw, input.lookPitch, input.zoom)
        input.lookYaw = 0f
        input.lookPitch = 0f
        input.zoom = 1f
        val inside = Lot.isInsideHouse(pos)
        val (focus, rig) = when (mode) {
            Mode.DECOR -> {
                val c = Placement.zoneRect(decorZone).center
                val right = V2(cos(camera.yaw * DEG), -sin(camera.yaw * DEG))
                val toward = right * (-decorShift)
                V3(c.x + toward.x, 0.4f, c.z + toward.z) to CameraRig(minPitch = 40f, maxPitch = 82f, minDistance = 9f, maxDistance = 22f, follow = 5f)
            }
            Mode.BASKETBALL -> {
                val rim = V2(Lot.RIM.x, Lot.RIM.z)
                val mid = (pos + rim) * 0.5f
                V3(mid.x, 1.7f, mid.z) to CameraRig(yaw = yawOf(pos - rim), pitch = 13f, distance = 8.2f, minPitch = 5f, maxPitch = 40f, minDistance = 6f, maxDistance = 11f)
            }
            Mode.SOCCER -> {
                val goal = V2(Lot.GOAL_X, Lot.GOAL_Z)
                val mid = Lot.PENALTY_SPOT * 0.7f + goal * 0.3f
                V3(mid.x, 1.0f, mid.z) to CameraRig(yaw = yawOf(Lot.PENALTY_SPOT - goal), pitch = 17f, distance = 6.4f, minPitch = 4f, maxPitch = 30f, minDistance = 4f, maxDistance = 9f)
            }
            Mode.JOG -> V3(visualPos.x, 1.1f, visualPos.z) to CameraRig(yaw = yawOf(-jogHeading), pitch = 26f, distance = 7.5f, turn = 2.6f, follow = 12f)
            else -> {
                val f = V3(visualPos.x, 1.0f + rootY * 0.5f, visualPos.z)
                f to if (inside) CameraRig(minPitch = 46f, maxPitch = 80f, minDistance = 5f, maxDistance = 11.5f) else CameraRig()
            }
        }
        camera.update(dt, focus, look.first, look.second, look.third, rig)
    }

    private fun updateWalls(dt: Float) {
        val eye = camera.eye.xz
        val focus = if (mode == Mode.DECOR) Placement.zoneRect(decorZone).center else visualPos
        val inside = Lot.isInsideHouse(focus)
        frame.inside = inside
        val cutNow = BooleanArray(WallSide.entries.size)
        walls.forEachIndexed { i, r ->
            val side = wallSides[i]
            val beyond = when (side) {
                WallSide.WEST -> eye.x < Lot.ROOM.minX
                WallSide.EAST -> eye.x > Lot.ROOM.maxX
                WallSide.NORTH -> eye.z < Lot.ROOM.minZ
                WallSide.SOUTH -> eye.z > Lot.ROOM.maxZ
            }
            if (r.segmentHits(eye, focus) || (inside && beyond)) cutNow[side.ordinal] = true
        }
        for (i in wallTimers.indices) {
            wallTimers[i] = if (cutNow[i]) 0.4f else (wallTimers[i] - dt)
            frame.wallCut[i] = wallTimers[i] > 0f
        }
        val roofNow = inside || Lot.ROOM.segmentHits(eye, focus) || eye in Lot.ROOM
        roofTimer = if (roofNow) 0.4f else roofTimer - dt
        frame.roofHidden = roofTimer > 0f
    }

    private fun writeFrame() {
        frame.position = V3(visualPos.x, rootY, visualPos.z)
        frame.yaw = yaw
        frame.eye = camera.eye
        frame.target = camera.target
        frame.showCan = mode == Mode.ACTION && actionAnim == Anim.WATER
        frame.waterTime = if (frame.showCan) actionTime else 0f
        basketball?.let { g ->
            frame.holdBall = g.phase == BasketballGame.Phase.AIM
            frame.basketBall = g.ball.pos
            frame.basketSpin = g.ball.spin
            frame.netWobble = g.netWobble
        } ?: run { frame.holdBall = false }
        soccer?.let { g ->
            frame.soccerBall = g.ball.pos
            frame.soccerSpin = g.ball.spin
            frame.keeperZ = g.keeperZ
            frame.keeperLean = g.keeperLean
            frame.keeperLift = g.keeperLift
            frame.netPush = g.netPush
        }
        if (mode != Mode.SWING) frame.swingUid = -1L
        if (mode != Mode.BOUNCE) frame.bounceUid = -1L
    }

    fun currentSave(): SaveData = save.current

    companion object {
        const val RADIUS = 0.3f
        const val MAX_SPEED = 5.2f
        const val HIP = 0.42f
        const val SWING_PIVOT = 2.25f
        const val SWING_LENGTH = 1.55f
    }
}
