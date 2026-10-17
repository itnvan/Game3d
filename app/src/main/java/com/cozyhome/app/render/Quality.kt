package com.cozyhome.app.render

import com.google.android.filament.LightManager
import com.google.android.filament.View
import io.github.sceneview.node.LightNode

class QualityGovernor(private val view: View, private val sun: LightNode) {
    var tier = -1
        private set
    private var sampleTime = 0f
    private var sampleFrames = 0
    private var settle = 2.5f

    fun apply(next: Int) {
        tier = next.coerceIn(0, TIERS.size - 1)
        val t = TIERS[tier]
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = false }
        view.renderQuality = view.renderQuality.apply { hdrColorBuffer = View.QualityLevel.LOW }
        view.bloomOptions = view.bloomOptions.apply {
            enabled = t.bloom
            strength = 0.08f
        }
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply { enabled = false }
        view.antiAliasing = View.AntiAliasing.FXAA
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = t.scale < 0.99f
            minScale = t.scale
            maxScale = t.scale
            quality = View.QualityLevel.MEDIUM
        }
        view.setShadowingEnabled(true)
        sampleTime = 0f
        sampleFrames = 0
        settle = 2.5f
    }

    private var lastPresented = 0L

    fun onPresented(frameTimeNanos: Long): Boolean {
        if (tier < 0) return false
        val prev = lastPresented
        lastPresented = frameTimeNanos
        if (prev == 0L) return false
        val dt = (frameTimeNanos - prev) / 1_000_000_000f
        if (dt <= 0f || dt > 0.25f) return false
        if (settle > 0f) {
            settle -= dt
            return false
        }
        sampleTime += dt
        sampleFrames++
        if (sampleTime < 3f) return false
        val avg = sampleTime / sampleFrames
        sampleTime = 0f
        sampleFrames = 0
        lastAverageMs = avg * 1000f
        if (avg > SLOW_FRAME && tier < TIERS.size - 1) {
            apply(tier + 1)
            return true
        }
        return false
    }

    var lastAverageMs = 0f
        private set

    private data class Tier(val scale: Float, val bloom: Boolean, val shadowMap: Int, val cascades: Int)

    companion object {
        private const val SLOW_FRAME = 0.0225f
        private val TIERS = listOf(
            Tier(0.8f, true, 1024, 1),
            Tier(0.66f, true, 1024, 1),
            Tier(0.56f, false, 1024, 1),
            Tier(0.46f, false, 1024, 1),
        )
    }
}
