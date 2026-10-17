package com.cozyhome.app.ui.play.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.cozyhome.app.game.DecorEdit
import com.cozyhome.app.game.PlotVisual
import com.cozyhome.app.render.rememberMeshInstance
import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.decor.Layer
import com.cozyhome.game.decor.Placement
import com.cozyhome.game.decor.Use
import com.cozyhome.game.game.GameWorld
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.models.CropModels
import com.cozyhome.game.models.FurnitureModels
import com.cozyhome.game.models.HouseModel
import com.cozyhome.game.models.Outfit
import com.cozyhome.game.models.Palette
import com.cozyhome.game.models.PropModels
import com.cozyhome.game.models.WorldModel
import com.cozyhome.game.state.PlacedItem
import com.cozyhome.game.state.Zone
import com.cozyhome.game.world.Lot
import com.cozyhome.game.world.WallSide
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.Node

class SceneRefs {
    val player = RigRefs()
    val keeper = RigRefs()
    val walls = arrayOfNulls<Node>(WallSide.entries.size)
    val cutWalls = arrayOfNulls<Node>(WallSide.entries.size)
    var roof: Node? = null
    var basketBall: Node? = null
    var soccerBall: Node? = null
    var hoopNet: Node? = null
    var goalNet: Node? = null
    var clouds: Node? = null
    var sky: io.github.sceneview.model.ModelInstance? = null
    val lamps = arrayOfNulls<io.github.sceneview.node.LightNode>(LAMP_SPOTS.size)
    var nextGate: Node? = null
    val gates = arrayOfNulls<Node>(8)
    val drops = arrayOfNulls<Node>(12)
    val stars = arrayOfNulls<Node>(12)
    val rings = arrayOfNulls<Node>(4)
    val swingSeats = HashMap<Long, Node>()
    val trampolines = HashMap<Long, Node>()
    var editNode: Node? = null
    var editUid = -1L
    var zzz: Node? = null
    var scenery: Node? = null
    var sceneryHidden = false
    val splashes = arrayOfNulls<Node>(6)
}

val LAMP_SPOTS = listOf(
    Position(-1.6f, 2.45f, -2.3f), Position(1.6f, 2.45f, -2.3f),
    Position(-2.4f, 2.45f, 18.8f), Position(2.4f, 2.45f, 18.8f),
    Position(-1.05f, 1.8f, -5.55f), Position(1.05f, 1.8f, -5.55f),
)

private class Holder {
    var node: Node? = null
}

@Composable
private fun rememberHolder() = remember { Holder() }

@Composable
fun SceneScope.StaticWorld(refs: SceneRefs, wallColor: Int, floorStyle: Int) {
    val ground = rememberMeshInstance(modelLoader, "ground") { WorldModel.ground() }
    val scenery = rememberMeshInstance(modelLoader, "scenery") { WorldModel.scenery() }
    val sky = rememberMeshInstance(modelLoader, "sky-dome") { WorldModel.skyDome() }
    val outskirts = rememberMeshInstance(modelLoader, "outskirts") { WorldModel.outskirts() }
    val floor = rememberMeshInstance(modelLoader, "floor-$floorStyle") { HouseModel.floor(floorStyle) }
    val roof = rememberMeshInstance(modelLoader, "roof") { HouseModel.roof() }
    val porch = rememberMeshInstance(modelLoader, "porch") { HouseModel.porch() }
    val hoopNet = rememberMeshInstance(modelLoader, "hoopNet") { WorldModel.hoopNet() }
    val goalNet = rememberMeshInstance(modelLoader, "goalNet") { WorldModel.goalNet() }

    ground?.let { Part(it, shadow = false) }
    outskirts?.let { Part(it, shadow = false) }
    Node(apply = { refs.scenery = this }) { scenery?.let { Part(it) } }
    floor?.let { Part(it, shadow = false) }
    porch?.let { Part(it) }
    DisposableEffect(sky) {
        refs.sky = sky
        onDispose { if (refs.sky === sky) refs.sky = null }
    }
    sky?.let { Part(it, shadow = false, apply = { isShadowReceiver = false }) }
    DisposableEffect(Unit) { onDispose { refs.lamps.fill(null) } }
    LAMP_SPOTS.forEachIndexed { i, p ->
        LightNode(
            type = com.google.android.filament.LightManager.Type.POINT,
            intensity = 0f,
            position = p,
            apply = {
                color(1f, 0.78f, 0.5f)
                falloff(8f)
                castShadows(false)
            },
            nodeApply = { refs.lamps[i] = this },
        )
    }
    Node(apply = { refs.roof = this }) { roof?.let { Part(it) } }
    for (side in WallSide.entries) {
        key(side) {
            val full = rememberMeshInstance(modelLoader, "wall-$side-$wallColor") { HouseModel.wall(side, wallColor, cut = false) }
            val cut = rememberMeshInstance(modelLoader, "wallcut-$side-$wallColor") { HouseModel.wall(side, wallColor, cut = true) }
            Node(apply = { refs.walls[side.ordinal] = this }) { full?.let { Part(it) } }
            Node(isVisible = false, apply = { refs.cutWalls[side.ordinal] = this }) { cut?.let { Part(it) } }
        }
    }
    Node(position = Position(Lot.RIM.x, Lot.RIM.y, Lot.RIM.z), apply = { refs.hoopNet = this }) { hoopNet?.let { Part(it, shadow = false) } }
    Node(position = Position(Lot.GOAL_X, 0f, 0f), apply = { refs.goalNet = this }) { goalNet?.let { Part(it, shadow = false) } }
}

fun staticReady(vararg keys: Any?) = keys.all { it != null }

@Composable
fun SceneScope.Furniture(placed: List<PlacedItem>, edit: DecorEdit?, refs: SceneRefs) {
    val editUid = edit?.item?.uid
    for (item in placed) {
        if (item.uid == editUid) continue
        key(item.uid) { FurnitureNode(item, refs, lifted = false) }
    }
    edit?.let { e ->
        key("edit-${e.item.uid}") {
            FurnitureNode(e.item, refs, lifted = true)
            val def = Catalog[e.item.itemId]
            if (def != null) {
                val w = def.w * Catalog.CELL
                val d = def.d * Catalog.CELL
                val ring = rememberMeshInstance(modelLoader, "sel-$w-$d-${e.valid}") { PropModels.selection(w, d, e.valid) }
                ring?.let {
                    Node(
                        position = Position(e.item.x, if (e.item.zone == Zone.HOUSE) 0.05f else 0.02f, e.item.z),
                        rotation = Rotation(y = Placement.yaw(e.item.rot)),
                    ) { Part(it, shadow = false) }
                }
            }
        }
    }
}

@Composable
private fun SceneScope.FurnitureNode(item: PlacedItem, refs: SceneRefs, lifted: Boolean) {
    val def = Catalog[item.itemId] ?: return
    val model = rememberMeshInstance(modelLoader, "f-${item.uid}-${item.itemId}-${item.variant}-${item.on}") {
        FurnitureModels.build(item.itemId, item.variant, item.on)
    }
    val baseY = if (item.zone == Zone.HOUSE) 0.04f else 0f
    val holder = rememberHolder()
    val uid = item.uid
    DisposableEffect(uid, lifted) {
        onDispose {
            val n = holder.node
            if (refs.trampolines[uid] === n) refs.trampolines.remove(uid)
            if (lifted && refs.editUid == uid && refs.editNode === n) {
                refs.editNode = null
                refs.editUid = -1L
            }
        }
    }
    Node(
        position = Position(item.x, baseY + if (lifted) 0.06f else 0f, item.z),
        rotation = Rotation(y = Placement.yaw(item.rot)),
        apply = {
            holder.node = this
            if (lifted) {
                refs.editNode = this
                refs.editUid = uid
            }
            if (def.use == Use.BOUNCE) refs.trampolines[uid] = this
        },
    ) {
        model?.let { Part(it, shadow = def.layer != Layer.FLOOR) }
        if (def.use == Use.SWING) SwingSeat(uid, item.variant, refs)
    }
}

@Composable
private fun SceneScope.SwingSeat(uid: Long, variant: Int, refs: SceneRefs) {
    val seat = rememberMeshInstance(modelLoader, "seat-$uid-$variant") { FurnitureModels.swingSeat(variant) }
    val holder = rememberHolder()
    DisposableEffect(uid) {
        onDispose { if (refs.swingSeats[uid] === holder.node) refs.swingSeats.remove(uid) }
    }
    Node(position = Position(0f, GameWorld.SWING_PIVOT, 0f), apply = {
        holder.node = this
        refs.swingSeats[uid] = this
    }) { seat?.let { Part(it) } }
}

@Composable
fun SceneScope.Garden(plots: List<PlotVisual>, refs: SceneRefs) {
    val drop = rememberMeshInstance(modelLoader, "drop") { PropModels.waterDrop() }
    val star = rememberMeshInstance(modelLoader, "star") { PropModels.star() }
    plots.forEachIndexed { i, v ->
        key(i) {
            val c = Lot.PLOTS[i]
            val soil = rememberMeshInstance(modelLoader, "soil-$i-${v.wet}-${v.crop != null}") { CropModels.soil(v.wet, v.crop != null) }
            soil?.let { Node(position = Position(c.x, 0f, c.z)) { Part(it, shadow = false) } }
            if (v.crop != null) {
                val plant = rememberMeshInstance(modelLoader, "plant-$i-${v.crop}-${v.stage}") { CropModels.plant(v.crop, v.stage) }
                plant?.let { Node(position = Position(c.x, 0f, c.z)) { Part(it) } }
            }
            Node(position = Position(c.x, 1.3f, c.z), isVisible = false, apply = { refs.drops[i] = this }) { drop?.let { Part(it, shadow = false) } }
            Node(position = Position(c.x, 1.5f, c.z), isVisible = false, apply = { refs.stars[i] = this }) { star?.let { Part(it, shadow = false) } }
        }
    }
}

@Composable
fun SceneScope.SportsProps(refs: SceneRefs, showKeeper: Boolean, showGates: Boolean) {
    val spots = listOf(
        Position(Lot.SHOT_SPOTS[0].x, 0f, Lot.SHOT_SPOTS[0].z),
        Position(Lot.KICK_STAND.x, 0f, Lot.KICK_STAND.z),
        Position(Lot.TRACK_START.x, 0f, Lot.TRACK_START.z),
        Position(Lot.MAILBOX.x, 0f, Lot.MAILBOX.z - 0.9f),
    )
    spots.forEachIndexed { i, p ->
        key("ring$i") {
            val ring = rememberMeshInstance(modelLoader, "ring$i") { PropModels.ring(if (i == 3) 0.55f else 0.75f) }
            Node(position = p, apply = { refs.rings[i] = this }) { ring?.let { Part(it, shadow = false) } }
        }
    }
    val ball = rememberMeshInstance(modelLoader, "basketball") { PropModels.basketball() }
    Node(position = Position(Lot.SHOT_SPOTS[0].x + 1.2f, 0.115f, Lot.SHOT_SPOTS[0].z + 0.6f), apply = { refs.basketBall = this }) { ball?.let { Part(it) } }
    val soccer = rememberMeshInstance(modelLoader, "soccerball") { PropModels.soccerBall() }
    Node(position = Position(Lot.PENALTY_SPOT.x, 0.11f, Lot.PENALTY_SPOT.z), apply = { refs.soccerBall = this }) { soccer?.let { Part(it) } }
    if (showKeeper) {
        DisposableEffect(Unit) { onDispose { refs.keeper.clear() } }
        CharacterRig(Outfit.KEEPER, refs.keeper, "keeper", gloves = true)
    }
    if (showGates) Gates(refs)
    val zzz = rememberMeshInstance(modelLoader, "zzz") { PropModels.zzz() }
    Node(isVisible = false, apply = { refs.zzz = this }) { zzz?.let { Part(it, shadow = false) } }
    val splash = rememberMeshInstance(modelLoader, "splash") {
        mesh {
            mat = Mat.glow(Palette.SKY, 1.2f)
            sphere(0f, 0f, 0f, 0.035f, Palette.SKY, 4, 6)
        }
    }
    for (i in 0 until refs.splashes.size) {
        key("splash$i") { Node(isVisible = false, apply = { refs.splashes[i] = this }) { splash?.let { Part(it, shadow = false) } } }
    }
}

@Composable
private fun SceneScope.Gates(refs: SceneRefs) {
    val gate = rememberMeshInstance(modelLoader, "gate") { PropModels.checkpoint(false) }
    val next = rememberMeshInstance(modelLoader, "gateNext") { PropModels.checkpoint(true) }
    DisposableEffect(Unit) {
        onDispose {
            refs.gates.fill(null)
            refs.nextGate = null
        }
    }
    for (i in 0 until refs.gates.size) {
        key("gate$i") { Node(apply = { refs.gates[i] = this }) { gate?.let { Part(it) } } }
    }
    Node(apply = { refs.nextGate = this }) { next?.let { Part(it, shadow = false) } }
}
