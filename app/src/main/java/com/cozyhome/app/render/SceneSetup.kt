package com.cozyhome.app.render

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.cozyhome.game.mesh.Rgb
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.google.android.filament.utils.KTX1Loader
import io.github.sceneview.createEnvironment
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.node.LightNode
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberNode
import io.github.sceneview.utils.readBuffer

private const val NEUTRAL_IBL = "environments/neutral/neutral_ibl.ktx"

private val DAY_SKY_SH = floatArrayOf(
    0.57281f, 0.70409f, 0.77157f, 0.26892f, 0.23897f, 0.50901f, 0.24572f, 0.27329f, 0.33952f,
    0.09466f, 0.10867f, 0.14192f, 0.10284f, 0.11419f, 0.14429f, 0.27404f, 0.29291f, 0.34991f,
    0.01375f, 0.01587f, 0.02018f, 0.07026f, 0.07849f, 0.09988f, -0.07047f, -0.06917f, -0.07473f,
)

private fun loadSkyReflections(engine: Engine, context: Context): Texture? = runCatching {
    val bytes = context.assets.open("tex/sky_day_reflect.bin").use { it.readBytes() }
    val head = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    val size = head.getInt(0)
    val levels = head.getInt(4)
    val tex = Texture.Builder()
        .width(size).height(size).levels(levels)
        .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
        .format(Texture.InternalFormat.RGB16F)
        .build(engine)
    var offset = 8
    for (lv in 0 until levels) {
        val s = size shr lv
        val faceBytes = s * s * 6
        val buffer = ByteBuffer.allocateDirect(faceBytes * 6).order(ByteOrder.nativeOrder())
        buffer.put(bytes, offset, faceBytes * 6).flip()
        offset += faceBytes * 6
        val faces = IntArray(6) { it * faceBytes }
        tex.setImage(engine, lv, Texture.PixelBufferDescriptor(buffer, Texture.Format.RGB, Texture.Type.HALF), faces)
    }
    tex
}.getOrNull()

fun buildSkyEnvironment(engine: Engine, context: Context, sky: Rgb, iblIntensity: Float): Environment {
    val reflections = loadSkyReflections(engine, context)
    val bundle = if (reflections == null) KTX1Loader.createIndirectLight(engine, context.assets.readBuffer(NEUTRAL_IBL)) else null
    bundle?.indirectLight?.let { engine.destroyIndirectLight(it) }
    val cube = reflections ?: bundle?.cubemap
    val light = IndirectLight.Builder()
        .apply { cube?.let { reflections(it) } }
        .irradiance(3, DAY_SKY_SH)
        .intensity(iblIntensity)
        .build(engine)
    val lin = sky.linear()
    return createEnvironment(
        engine = engine,
        isOpaque = true,
        indirectLight = light,
        skybox = Skybox.Builder().color(lin[0], lin[1], lin[2], 1f).build(engine),
        sphericalHarmonics = null,
        textures = listOfNotNull(cube),
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
