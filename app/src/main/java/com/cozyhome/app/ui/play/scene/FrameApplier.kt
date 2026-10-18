package com.cozyhome.app.ui.play.scene

import com.cozyhome.app.game.PlotVisual
import com.cozyhome.game.anim.Pose
import com.cozyhome.game.game.FrameState
import com.cozyhome.game.game.GameWorld
import com.cozyhome.game.game.Mode
import com.cozyhome.game.math.DEG
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.yawOf
import com.cozyhome.game.sports.JogRace
import com.cozyhome.game.world.Lot
import com.cozyhome.game.world.SkyState
import com.cozyhome.game.math.smoothstep
import com.cozyhome.app.render.TextureBank
import com.google.android.filament.View
import io.github.sceneview.RenderInvalidator
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val DAY_SKY_LINEAR = com.cozyhome.game.mesh.rgb(0x9FDCF7).linear()
private const val WINDOW_GLOW_MAX = 0.85f

class FrameApplier(private val refs: SceneRefs) {
    private var clock = 0f
    private val keeperPose = Pose()
    private val keeperAnim = com.cozyhome.game.anim.Animator().also { it.play(com.cozyhome.game.anim.Anim.KEEPER) }
    private var lastEye = V3.ZERO
    private var lastTarget = V3.ZERO

    fun apply(world: GameWorld, camera: CameraNode, plots: List<PlotVisual>, giftReady: Boolean, dt: Float) {
        clock += dt
        val f = world.frame
        refs.player.pose(f.position, f.yaw, f.pose)
        refs.player.prop?.isVisible = f.showCan

        if ((f.eye - lastEye).length > 1e-4f || (f.target - lastTarget).length > 1e-4f) {
            camera.position = Position(f.eye.x, f.eye.y, f.eye.z)
            camera.lookAt(Position(f.target.x, f.target.y, f.target.z))
            lastEye = f.eye
            lastTarget = f.target
        }

        for (i in refs.walls.indices) {
            val cut = f.wallCut[i]
            refs.walls[i]?.let { if (it.isVisible == cut) it.isVisible = !cut }
            refs.cutWalls[i]?.let { if (it.isVisible != cut) it.isVisible = cut }
        }
        refs.roof?.let { if (it.isVisible == f.roofHidden) it.isVisible = !f.roofHidden }

        val basket = world.basketball
        refs.basketBall?.let { b ->
            if (basket != null) {
                b.position = Position(f.basketBall.x, f.basketBall.y, f.basketBall.z)
                b.rotation = Rotation(x = -f.basketSpin / DEG)
            } else if (world.mode != Mode.BASKETBALL) {
                val rest = Lot.SHOT_SPOTS[0]
                b.position = Position(rest.x + 1.2f, 0.115f, rest.z + 0.6f)
            }
        }
        refs.hoopNet?.let { n ->
            val w = f.netWobble
            if (w > 0f) n.scale = Scale(1f + sin(clock * 30f) * 0.08f * w, 1f + 0.35f * w * (0.5f + 0.5f * sin(clock * 18f)), 1f + cos(clock * 30f) * 0.08f * w)
            else if (n.scale.y != 1f) n.scale = Scale(1f)
        }

        val soccer = world.soccer
        refs.soccerBall?.let { b ->
            if (soccer != null) {
                b.position = Position(f.soccerBall.x, f.soccerBall.y, f.soccerBall.z)
                b.rotation = Rotation(z = f.soccerSpin / DEG)
            } else {
                b.position = Position(Lot.PENALTY_SPOT.x, 0.11f, Lot.PENALTY_SPOT.z)
            }
        }
        if (soccer != null) {
            keeperAnim.update(dt, 0f)
            keeperPose.set(keeperAnim.pose)
            keeperPose.armLOut += abs(f.keeperLean) * 60f
            keeperPose.armROut += abs(f.keeperLean) * 60f
            keeperPose.armLFwd += f.keeperLift * 120f
            keeperPose.armRFwd += f.keeperLift * 120f
            refs.keeper.pose(V3(soccer.keeperX, f.keeperLift * 0.6f, f.keeperZ), 90f, keeperPose, roll = f.keeperLean * 78f)
        }
        refs.goalNet?.let { n ->
            val p = f.netPush
            if (p > 0f) n.scale = Scale(1f + 0.4f * p, 1f, 1f) else if (n.scale.x != 1f) n.scale = Scale(1f)
        }

        refs.scenery?.let { if (it.isVisible == refs.sceneryHidden) it.isVisible = !refs.sceneryHidden }

        val jog = world.jog
        if (jog != null) {
            for (i in 0 until jog.gatesPerLap) {
                refs.gates[i]?.let { g ->
                    val p = jog.gatePosition(i)
                    val d = jog.gateDirection(i)
                    g.position = Position(p.x, 0f, p.z)
                    g.rotation = Rotation(y = yawOf(d))
                    g.isVisible = i != jog.gate || jog.phase == JogRace.Phase.DONE
                }
            }
            refs.nextGate?.let { g ->
                val p = jog.gatePosition(jog.gate)
                g.position = Position(p.x, abs(sin(clock * 3f)) * 0.08f, p.z)
                g.rotation = Rotation(y = yawOf(jog.gateDirection(jog.gate)))
                g.isVisible = jog.phase != JogRace.Phase.DONE
            }
        }

        val free = world.mode == Mode.FREE || world.mode == Mode.ACTION
        for (i in 0 until 12) {
            val v = plots.getOrNull(i)
            val bob = sin(clock * 2.6f + i) * 0.08f
            refs.drops[i]?.let { d ->
                val show = free && v?.status == 1
                if (d.isVisible != show) d.isVisible = show
                if (show) {
                    d.position = Position(Lot.PLOTS[i].x, 1.35f + bob, Lot.PLOTS[i].z)
                    d.rotation = Rotation(y = clock * 60f)
                }
            }
            refs.stars[i]?.let { s ->
                val show = free && v?.status == 3
                if (s.isVisible != show) s.isVisible = show
                if (show) {
                    s.position = Position(Lot.PLOTS[i].x, 1.55f + bob, Lot.PLOTS[i].z)
                    s.rotation = Rotation(y = clock * 90f)
                }
            }
        }
        val pulse = 1f + sin(clock * 3.2f) * 0.06f
        refs.rings.forEachIndexed { i, r ->
            r ?: return@forEachIndexed
            val show = free && (i != 3 || giftReady)
            if (r.isVisible != show) r.isVisible = show
            if (show) r.scale = Scale(pulse, 1f, pulse)
        }
        refs.zzz?.let { z ->
            val show = world.mode == Mode.SLEEP
            if (z.isVisible != show) z.isVisible = show
            if (show) {
                val hy = f.yaw * DEG
                z.position = Position(f.position.x - sin(hy) * 1.05f, f.position.y + 0.55f + (clock % 2f) * 0.25f, f.position.z - cos(hy) * 1.05f)
                z.rotation = Rotation(y = world.camera.yaw)
            }
        }
        refs.swingSeats.forEach { (uid, seat) ->
            val angle = if (f.swingUid == uid) -f.swingAngle / DEG else 0f
            seat.rotation = Rotation(x = angle)
        }
        refs.trampolines.forEach { (uid, node) ->
            val s = if (f.bounceUid == uid) 1f - 0.05f * f.bounceSquash else 1f
            if (node.scale.y != s) node.scale = Scale(1f, s, 1f)
        }
        refs.editNode?.let { e ->
            if (world.mode == Mode.DECOR) {
                val p = e.position
                e.position = Position(p.x, (if (Lot.isInsideHouse(com.cozyhome.game.math.V2(p.x, p.z))) 0.04f else 0f) + 0.06f + sin(clock * 5f) * 0.03f, p.z)
            }
        }
        for (k in refs.splashes.indices) {
            val s = refs.splashes[k] ?: continue
            if (!f.showCan) {
                if (s.isVisible) s.isVisible = false
                continue
            }
            if (!s.isVisible) s.isVisible = true
            val t = ((f.waterTime * 2.2f + k / refs.splashes.size.toFloat()) % 1f)
            val yaw = f.yaw * DEG
            val fwd = 0.78f + t * 0.12f
            val side = (k % 3 - 1) * 0.05f
            s.position = Position(
                f.position.x + sin(yaw) * fwd + cos(yaw) * (0.25f + side),
                0.85f - t * 0.75f,
                f.position.z + cos(yaw) * fwd - sin(yaw) * (0.25f + side),
            )
        }
    }

    private var skyNight: Boolean? = null
    private var windowGlow = -1f
    private val glowing = HashSet<com.google.android.filament.MaterialInstance>()

    fun applySky(sky: SkyState, sun: LightNode, fill: LightNode, environment: Environment, view: View, invalidator: RenderInvalidator, baseIbl: Float, bank: TextureBank?) {
        val lin = sky.sky.linear()
        environment.skybox?.setColor(lin[0], lin[1], lin[2], 1f)
        refs.sky?.materialInstances?.firstOrNull()?.let { mi ->
            val night = sky.night > 0.55f
            if (bank != null && bank.ready && skyNight != night) {
                bank.setSky(mi, night)
                skyNight = night
            }
            val day = DAY_SKY_LINEAR
            val k = if (night) 0.85f else 1f - 0.45f * sky.night
            val tr = if (night) 0.75f else (lin[0] / day[0]).coerceIn(0.3f, 1.8f)
            val tg = if (night) 0.85f else (lin[1] / day[1]).coerceIn(0.3f, 1.4f)
            val tb = if (night) 1.1f else (lin[2] / day[2]).coerceIn(0.3f, 1.4f)
            mi.setParameter("baseColorFactor", tr * k, tg * k, tb * k, 1f)
        }
        environment.indirectLight?.intensity = baseIbl * sky.ambient
        val sc = sky.sunColor.linear()
        sun.color = Color(sc[0], sc[1], sc[2], 1f)
        sun.intensity = sky.sunIntensity
        sun.lightDirection = Direction(sky.sunDirection.x, sky.sunDirection.y, sky.sunDirection.z)
        fill.intensity = 600f + 4_400f * (1f - sky.night)
        val lampOn = smoothstep(0.35f, 0.85f, sky.night)
        refs.lamps.forEachIndexed { i, l -> l?.let { val lm = lampOn * LAMP_SPOTS[i].lumens; if (it.intensity != lm) it.intensity = lm } }
        val glow = smoothstep(0.3f, 0.75f, sky.night) * WINDOW_GLOW_MAX
        if (glow != windowGlow || refs.windowGlows.any { it != null && it !in glowing }) {
            windowGlow = glow
            glowing.clear()
            for (mi in refs.windowGlows) mi?.let { it.setParameter("baseColorFactor", 1f, 1f, 1f, glow); glowing += it }
        }
        view.fogOptions = view.fogOptions.apply {
            enabled = true
            distance = 38f
            maximumOpacity = 0.9f
            height = 0f
            heightFalloff = 0.06f
            density = 0.018f
            color = floatArrayOf(lin[0], lin[1], lin[2])
            cutOffDistance = 220f
        }
        invalidator.requestRender()
    }
}
