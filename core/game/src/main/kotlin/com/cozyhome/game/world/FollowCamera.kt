package com.cozyhome.game.world

import com.cozyhome.game.math.DEG
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.damp
import com.cozyhome.game.math.dampAngle
import kotlin.math.cos
import kotlin.math.sin

data class CameraRig(
    val minPitch: Float = 16f,
    val maxPitch: Float = 72f,
    val minDistance: Float = 5f,
    val maxDistance: Float = 16f,
    val yaw: Float? = null,
    val pitch: Float? = null,
    val distance: Float? = null,
    val follow: Float = 9f,
    val turn: Float = 6f,
)

class FollowCamera {
    var yaw = 0f
        private set
    var pitch = 30f
        private set
    var distance = 8f
        private set
    var target = V3.ZERO
        private set
    var eye = V3(0f, 6f, 9f)
        private set
    private var initialized = false

    fun snap(focus: V3, yawDeg: Float) {
        target = focus
        yaw = yawDeg
        initialized = true
        updateEye()
    }

    fun setOrbit(pitchDeg: Float, distanceM: Float) {
        pitch = pitchDeg
        distance = distanceM
    }

    fun update(dt: Float, focus: V3, dYaw: Float, dPitch: Float, zoom: Float, rig: CameraRig) {
        if (!initialized) snap(focus, yaw)
        yaw -= dYaw * 230f
        pitch += dPitch * 110f
        distance *= zoom
        rig.yaw?.let { yaw = dampAngle(yaw, it, rig.turn, dt) }
        rig.pitch?.let { pitch = damp(pitch, it, rig.turn, dt) }
        rig.distance?.let { distance = damp(distance, it, rig.turn, dt) }
        pitch = if (pitch < rig.minPitch) damp(pitch, rig.minPitch, 8f, dt) else if (pitch > rig.maxPitch) damp(pitch, rig.maxPitch, 8f, dt) else pitch
        distance = if (distance < rig.minDistance) damp(distance, rig.minDistance, 8f, dt) else if (distance > rig.maxDistance) damp(distance, rig.maxDistance, 8f, dt) else distance
        pitch = pitch.coerceIn(rig.minPitch - 20f, rig.maxPitch + 10f)
        distance = distance.coerceIn(rig.minDistance * 0.7f, rig.maxDistance * 1.3f)
        target = damp(target, focus, rig.follow, dt)
        updateEye()
    }

    private fun updateEye() {
        val p = pitch * DEG
        val y = yaw * DEG
        eye = V3(
            target.x + sin(y) * cos(p) * distance,
            target.y + sin(p) * distance,
            target.z + cos(y) * cos(p) * distance,
        )
    }
}
