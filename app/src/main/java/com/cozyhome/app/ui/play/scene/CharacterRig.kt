package com.cozyhome.app.ui.play.scene

import androidx.compose.runtime.Composable
import com.cozyhome.app.render.rememberMeshInstance
import com.cozyhome.game.anim.Pose
import com.cozyhome.game.math.V3
import com.cozyhome.game.models.CharacterModel
import com.cozyhome.game.models.Outfit
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.Node

class RigRefs {
    var root: Node? = null
    var tilt: Node? = null
    var body: Node? = null
    var head: Node? = null
    var eyes: Node? = null
    var armL: Node? = null
    var armR: Node? = null
    var legL: Node? = null
    var legR: Node? = null
    var prop: Node? = null

    fun pose(position: V3, yaw: Float, p: Pose, roll: Float = 0f) {
        root?.let {
            it.position = Position(position.x, position.y + p.lift, position.z)
            it.rotation = Rotation(y = yaw)
        }
        tilt?.rotation = Rotation(x = -90f * p.lie, z = roll)
        body?.let {
            it.position = Position(0f, CharacterModel.HIP + p.bob, 0f)
            it.rotation = Rotation(x = p.lean, z = p.roll)
        }
        head?.rotation = Rotation(x = p.headPitch, y = p.headYaw)
        eyes?.scale = Scale(1f, 1f - 0.92f * p.blink, 1f)
        armL?.rotation = Rotation(x = -p.armLFwd, z = -p.armLOut)
        armR?.rotation = Rotation(x = -p.armRFwd, z = p.armROut)
        legL?.rotation = Rotation(x = -p.legLFwd)
        legR?.rotation = Rotation(x = -p.legRFwd)
    }

    fun setVisible(v: Boolean) {
        root?.isVisible = v
    }

    fun clear() {
        root = null; tilt = null; body = null; head = null; eyes = null
        armL = null; armR = null; legL = null; legR = null; prop = null
    }
}

@Composable
fun SceneScope.CharacterRig(outfit: Outfit, refs: RigRefs, keyPrefix: String, withCan: Boolean = false, gloves: Boolean = false) {
    val torso = rememberMeshInstance(modelLoader, "$keyPrefix-torso-$outfit") { CharacterModel.torso(outfit) }
    val head = rememberMeshInstance(modelLoader, "$keyPrefix-head-$outfit") { CharacterModel.head(outfit) }
    val eyes = rememberMeshInstance(modelLoader, "$keyPrefix-eyes-$outfit") { CharacterModel.eyes(outfit) }
    val armL = rememberMeshInstance(modelLoader, "$keyPrefix-armL-$outfit") { CharacterModel.arm(outfit) }
    val armR = rememberMeshInstance(modelLoader, "$keyPrefix-armR-$outfit") { CharacterModel.arm(outfit) }
    val legL = rememberMeshInstance(modelLoader, "$keyPrefix-legL-$outfit") { CharacterModel.leg(outfit) }
    val legR = rememberMeshInstance(modelLoader, "$keyPrefix-legR-$outfit") { CharacterModel.leg(outfit) }
    val can = if (withCan) rememberMeshInstance(modelLoader, "$keyPrefix-can") { CharacterModel.wateringCan() } else null
    val gloveL = if (gloves) rememberMeshInstance(modelLoader, "$keyPrefix-gloveL") { CharacterModel.gloves() } else null
    val gloveR = if (gloves) rememberMeshInstance(modelLoader, "$keyPrefix-gloveR") { CharacterModel.gloves() } else null

    Node(apply = { refs.root = this }) {
        Node(apply = { refs.tilt = this }) {
            Node(position = Position(0f, CharacterModel.HIP, 0f), apply = { refs.body = this }) {
                torso?.let { Part(it) }
                Node(position = Position(0f, CharacterModel.NECK, 0f), apply = { refs.head = this }) {
                    head?.let { Part(it) }
                    Node(position = Position(0f, CharacterModel.EYE_Y, 0f), apply = { refs.eyes = this }) {
                        eyes?.let { Part(it, shadow = false) }
                    }
                }
                Node(position = Position(-CharacterModel.SHOULDER_X, CharacterModel.SHOULDER_Y, 0f), apply = { refs.armL = this }) {
                    armL?.let { Part(it) }
                    gloveL?.let { Part(it) }
                }
                Node(position = Position(CharacterModel.SHOULDER_X, CharacterModel.SHOULDER_Y, 0f), apply = { refs.armR = this }) {
                    armR?.let { Part(it) }
                    gloveR?.let { Part(it) }
                    can?.let { c ->
                        Node(isVisible = false, apply = { refs.prop = this }) { Part(c) }
                    }
                }
                Node(position = Position(-CharacterModel.LEG_X, 0f, 0f), apply = { refs.legL = this }) {
                    legL?.let { Part(it) }
                }
                Node(position = Position(CharacterModel.LEG_X, 0f, 0f), apply = { refs.legR = this }) {
                    legR?.let { Part(it) }
                }
            }
        }
    }
}

@Composable
fun SceneScope.Part(instance: io.github.sceneview.model.ModelInstance, position: Position = Position(0f), rotation: Rotation = Rotation(0f), shadow: Boolean = true, apply: io.github.sceneview.node.ModelNode.() -> Unit = {}) {
    ModelNode(
        modelInstance = instance,
        autoAnimate = false,
        position = position,
        rotation = rotation,
        apply = {
            isShadowCaster = shadow
            isShadowReceiver = true
            apply()
        },
    )
}
