package com.cozyhome.app.render

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.cozyhome.game.mesh.Rgb
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.utils.KTX1Loader
import io.github.sceneview.createEnvironment
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.node.LightNode
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberNode
import io.github.sceneview.utils.readBuffer

private const val NEUTRAL_IBL = "environments/neutral/neutral_ibl.ktx"

fun buildSkyEnvironment(engine: Engine, context: Context, sky: Rgb, iblIntensity: Float): Environment {
    val bundle = KTX1Loader.createIndirectLight(engine, context.assets.readBuffer(NEUTRAL_IBL))
    val lin = sky.linear()
    return createEnvironment(
        engine = engine,
        isOpaque = true,
        indirectLight = bundle.indirectLight?.also { it.intensity = iblIntensity },
        skybox = Skybox.Builder().color(lin[0], lin[1], lin[2], 1f).build(engine),
        sphericalHarmonics = null,
        textures = listOfNotNull(bundle.cubemap),
    )
}

@Composable
fun rememberSkyEnvironment(engine: Engine, environmentLoader: EnvironmentLoader, sky: Rgb, iblIntensity: Float = 9_000f): Environment {
    val context = LocalContext.current
    return rememberEnvironment(environmentLoader, key = sky) {
        buildSkyEnvironment(engine, context, sky, iblIntensity)
    }
}

@Composable
fun rememberSun(engine: Engine): LightNode = rememberNode {
    LightNode(
        engine = engine,
        type = LightManager.Type.DIRECTIONAL,
        apply = {
            color(1f, 0.96f, 0.88f)
            intensity(32_000f)
            direction(-0.45f, -1f, -0.55f)
            castShadows(true)
            shadowOptions(
                LightManager.ShadowOptions().apply {
                    mapSize = 1024
                    shadowCascades = 1
                    shadowFar = 36f
                    constantBias = 0.002f
                    normalBias = 1.2f
                    stable = false
                }
            )
        },
    )
}

@Composable
fun rememberFill(engine: Engine): LightNode = rememberNode {
    LightNode(
        engine = engine,
        type = LightManager.Type.DIRECTIONAL,
        apply = {
            color(0.78f, 0.88f, 1f)
            intensity(6_000f)
            direction(0.6f, -0.5f, 0.7f)
            castShadows(false)
        },
    )
}
