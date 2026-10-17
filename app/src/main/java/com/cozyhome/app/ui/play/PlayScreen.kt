package com.cozyhome.app.ui.play

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.BuildConfig
import com.cozyhome.app.game.Haptic
import com.cozyhome.app.render.QualityGovernor
import com.cozyhome.app.render.buildSkyEnvironment
import com.cozyhome.app.render.rememberFill
import com.cozyhome.app.render.rememberSun
import com.cozyhome.app.ui.play.hud.PlayHud
import com.cozyhome.app.ui.play.scene.FrameApplier
import com.cozyhome.app.ui.play.scene.Furniture
import com.cozyhome.app.ui.play.scene.Garden
import com.cozyhome.app.ui.play.scene.SceneRefs
import com.cozyhome.app.ui.play.scene.SportsProps
import com.cozyhome.app.ui.play.scene.StaticWorld
import com.cozyhome.app.ui.play.scene.CharacterRig
import com.cozyhome.game.game.Mode
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.models.Outfit
import com.cozyhome.game.world.DayClock
import com.cozyhome.game.world.Lot
import io.github.sceneview.FrameRatePolicy
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberRenderInvalidator
import io.github.sceneview.rememberView
import io.github.sceneview.utils.screenToRay
import kotlinx.coroutines.isActive

private const val BASE_IBL = 9_000f

@Composable
fun PlayScreen(vm: GameViewModel) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val view = rememberView(engine)
    val invalidator = rememberRenderInvalidator()
    val context = LocalContext.current
    val cameraNode = rememberCameraNode(engine) {
        near = 0.15f
        far = 420f
        focalLength = 30.0
    }
    val environment = rememberEnvironment(environmentLoader) { buildSkyEnvironment(engine, context, rgb(0x9FDCF7), BASE_IBL) }
    val sun = rememberSun(engine)
    val fill = rememberFill(engine)
    val refs = remember { SceneRefs() }
    val applier = remember { FrameApplier(refs) }
    val save = vm.save
    val hud = vm.hud
    val outfit = remember(save.look) { Outfit.of(save.look) }
    var ready by remember { mutableStateOf(false) }
    val gfxPrefs = remember { context.getSharedPreferences("gfx", android.content.Context.MODE_PRIVATE) }
    val governor = remember(view, sun) { QualityGovernor(view, sun) }
    val gfxDebug = vm.gfxDebug
    LaunchedEffect(gfxDebug) {
        val g = gfxDebug ?: return@LaunchedEffect
        view.setShadowingEnabled(g.shadows)
        view.setPostProcessingEnabled(g.post)
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply { enabled = true; minScale = g.scale; maxScale = g.scale }
        refs.sceneryHidden = !g.scenery
        invalidator.requestRender()
    }
    LaunchedEffect(governor) {
        withFrameNanos { }
        withFrameNanos { }
        governor.apply(gfxPrefs.getInt("tier", 0))
    }

    val hostView = LocalView.current
    LaunchedEffect(Unit) {
        vm.haptics.collect { h ->
            val c = when (h) {
                Haptic.LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
                Haptic.SUCCESS -> HapticFeedbackConstants.LONG_PRESS
                Haptic.FAIL -> HapticFeedbackConstants.VIRTUAL_KEY
            }
            hostView.performHapticFeedback(c)
        }
    }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var skyTimer = 0f
        var warm = 0f
        while (isActive) {
            withFrameNanos { t ->
                val dt = ((t - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = t
                val now = System.currentTimeMillis()
                vm.tick(dt, now)
                applier.apply(vm.world, cameraNode, hud.plotVisuals, hud.giftReady, dt)
                skyTimer -= dt
                if (skyTimer <= 0f) {
                    skyTimer = 0.5f
                    applier.applySky(DayClock.sky(DayClock.hour(now, vm.save.clockOffsetMs)), sun, fill, environment, view, invalidator, BASE_IBL)
                }
                if (!ready) {
                    warm += dt
                    if (warm > 1.2f && refs.walls.all { it != null }) ready = true
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,
            view = view,
            renderInvalidator = invalidator,
            environment = environment,
            mainLightNode = sun,
            fillLightNode = fill,
            cameraNode = cameraNode,
            cameraManipulator = null,
            onGestureListener = null,
            autoCenterContent = false,
            frameRatePolicy = FrameRatePolicy.OnDemand(),
            onFrame = { t ->
                if (governor.onPresented(t)) gfxPrefs.edit().putInt("tier", governor.tier).apply()
                if (BuildConfig.DEBUG && governor.lastAverageMs > 0f && t % 64 == 0L) android.util.Log.d("Gfx", "tier=${governor.tier} avgMs=${governor.lastAverageMs}")
            },
        ) {
            StaticWorld(refs, save.wallColor, save.floorStyle)
            Furniture(save.placed, hud.decorEdit, refs)
            Garden(hud.plotVisuals, refs)
            SportsProps(refs, showKeeper = hud.mode == Mode.SOCCER, showGates = hud.mode == Mode.JOG)
            CharacterRig(outfit, refs.player, "player", withCan = true)
        }

        val screenRay: (Float, Float) -> Pair<V3, V3>? = remember(view) {
            { x, y ->
                view.screenToRay(x, y)?.let { ray ->
                    V3(ray.origin.x, ray.origin.y, ray.origin.z) to V3(ray.direction.x, ray.direction.y, ray.direction.z)
                }
            }
        }
        val goalPoint: (Float, Float) -> V3? = remember(view) {
            { x, y ->
                view.screenToRay(x, y)?.let { ray ->
                    val o = ray.origin
                    val d = ray.direction
                    if (kotlin.math.abs(d.x) < 1e-5f) null else {
                        val t = (Lot.GOAL_X - o.x) / d.x
                        if (t < 0f) null else V3(Lot.GOAL_X, o.y + d.y * t, o.z + d.z * t)
                    }
                }
            }
        }

        when (hud.mode) {
            Mode.DECOR -> DecorTouchLayer(vm, screenRay)
            Mode.SOCCER -> SoccerTouchLayer(vm, goalPoint)
            Mode.BASKETBALL -> TapLayer { vm.primary() }
            else -> ControlsLayer(vm.input, stickEnabled = hud.mode == Mode.FREE || hud.mode == Mode.JOG || hud.mode == Mode.SIT || hud.mode == Mode.SLEEP)
        }

        PlayHud(vm)
        if (!ready) LoadingOverlay()
    }
}
