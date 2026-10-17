package com.cozyhome.app.ui.play.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import com.cozyhome.app.game.HudState
import com.cozyhome.app.render.rememberMeshInstance
import com.cozyhome.game.activities.BugDex
import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.decor.Use
import com.cozyhome.game.game.Emote
import com.cozyhome.game.game.GameWorld
import com.cozyhome.game.math.DEG
import com.cozyhome.game.math.Rng
import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.models.ActivityModels
import com.cozyhome.game.models.FestivalModels
import com.cozyhome.game.progress.Festival
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.Node
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sin

class ActivityRefs {
    var rod: Node? = null
    var lineYaw: Node? = null
    var linePitch: Node? = null
    var bobber: Node? = null
    var fishShow: Node? = null
    var netRoot: Node? = null
    var netSwing: Node? = null
    val emotes = HashMap<Emote, Node>()
    val steam = arrayOfNulls<Node>(3)
    var pot: Node? = null
    val bugNodes = HashMap<String, Array<Node?>>()
    val fireworks = arrayOfNulls<Node>(5)
    val fireworkTime = FloatArray(5) { -1f }
    val fireworkPos = Array(5) { V3.ZERO }
    var fireworkClock = 0f
    val rng = Rng(42)

    fun clear() {
        rod = null; lineYaw = null; linePitch = null; bobber = null; fishShow = null; netRoot = null; netSwing = null; pot = null
        emotes.clear(); steam.fill(null); bugNodes.clear(); fireworks.fill(null)
    }
}

private val FIREWORK_COLORS = listOf(rgb(0xFF5C7A), rgb(0xFFD54F), rgb(0x5AD1FF), rgb(0x9B7BFF), rgb(0x6FE39A))

@Composable
fun SceneScope.ActivityProps(act: ActivityRefs, hud: HudState) {
    DisposableEffect(act) { onDispose { act.clear() } }
    val rod = rememberMeshInstance(modelLoader, "act-rod") { ActivityModels.rod() }
    val line = rememberMeshInstance(modelLoader, "act-line") { ActivityModels.line() }
    val bobber = rememberMeshInstance(modelLoader, "act-bobber") { ActivityModels.bobber() }
    val net = rememberMeshInstance(modelLoader, "act-net") { ActivityModels.net() }
    val steam = rememberMeshInstance(modelLoader, "act-steam") { ActivityModels.steam() }
    val pot = rememberMeshInstance(modelLoader, "act-pot") { ActivityModels.pot() }

    Node(isVisible = false, apply = { act.rod = this }) { rod?.let { Part(it, shadow = true) } }
    Node(isVisible = false, apply = { act.lineYaw = this }) {
        Node(apply = { act.linePitch = this }) { line?.let { Part(it, shadow = false) } }
    }
    Node(isVisible = false, apply = { act.bobber = this }) { bobber?.let { Part(it, shadow = false) } }
    Node(isVisible = false, apply = { act.netRoot = this }) {
        Node(position = Position(0.24f, 1.05f, 0.2f), apply = { act.netSwing = this }) { net?.let { Part(it, shadow = false) } }
    }
    for (k in 0 until 3) {
        Node(isVisible = false, apply = { act.steam[k] = this }) { steam?.let { Part(it, shadow = false) } }
    }
    Node(isVisible = false, apply = { act.pot = this }) { pot?.let { Part(it, shadow = false) } }

    hud.fishCard?.species?.let { id ->
        key(id) {
            val fish = rememberMeshInstance(modelLoader, "act-fish-$id") { ActivityModels.fish(id) }
            Node(isVisible = false, apply = { act.fishShow = this }) { fish?.let { Part(it, shadow = false) } }
            DisposableEffect(id) { onDispose { act.fishShow = null } }
        }
    }

    for (e in Emote.entries) {
        key(e) {
            val bubble = rememberMeshInstance(modelLoader, "act-emote-$e") { ActivityModels.emoteBubble(e) }
            Node(isVisible = false, apply = { act.emotes[e] = this }) { bubble?.let { Part(it, shadow = false) } }
        }
    }

    for (sp in BugDex.all) {
        key(sp.id) {
            val mesh = rememberMeshInstance(modelLoader, "act-bug-${sp.id}") { ActivityModels.bug(sp.id) }
            for (k in 0 until 2) {
                Node(isVisible = false, apply = { act.bugNodes.getOrPut(sp.id) { arrayOfNulls(2) }[k] = this }) { mesh?.let { Part(it, shadow = false) } }
            }
        }
    }

    hud.festival?.let { f ->
        key(f) {
            val decor = rememberMeshInstance(modelLoader, "festival-decor-$f") { FestivalModels.decor(f) }
            decor?.let { Part(it, shadow = true) }
        }
        for (k in act.fireworks.indices) {
            key("fw-$k") {
                val fw = rememberMeshInstance(modelLoader, "firework-$k") { FestivalModels.firework(FIREWORK_COLORS[k % FIREWORK_COLORS.size], 100 + k) }
                Node(isVisible = false, apply = { act.fireworks[k] = this }) { fw?.let { Part(it, shadow = false) } }
            }
        }
    }
}

fun applyActivities(act: ActivityRefs, world: GameWorld, festival: Festival?, dt: Float) {
    val f = world.frame
    val yawRot = Rotation(y = f.yaw)
    val pos = Position(f.position.x, f.position.y, f.position.z)
    act.rod?.let {
        if (it.isVisible != f.rodVisible) it.isVisible = f.rodVisible
        if (f.rodVisible) {
            it.position = pos
            it.rotation = yawRot
        }
    }
    val lineOn = f.bobberVisible
    act.lineYaw?.let { node ->
        if (node.isVisible != lineOn) node.isVisible = lineOn
        if (lineOn) {
            val d = f.bobber - f.rodTip
            val len = d.length.coerceAtLeast(0.01f)
            node.position = Position(f.rodTip.x, f.rodTip.y, f.rodTip.z)
            node.rotation = Rotation(y = atan2(d.x, d.z) / DEG)
            act.linePitch?.let { p ->
                p.rotation = Rotation(x = acos((d.y / len).coerceIn(-1f, 1f)) / DEG)
                p.scale = Scale(1f, len, 1f)
            }
        }
    }
    act.bobber?.let {
        if (it.isVisible != lineOn) it.isVisible = lineOn
        if (lineOn) it.position = Position(f.bobber.x, f.bobber.y, f.bobber.z)
    }
    act.fishShow?.let {
        val on = f.fishShown != null
        if (it.isVisible != on) it.isVisible = on
        if (on) {
            it.position = Position(f.position.x, f.position.y + 1.95f + sin(f.clock * 4f) * 0.04f, f.position.z)
            it.rotation = Rotation(y = world.camera.yaw + 90f, z = sin(f.clock * 9f) * 12f)
            it.scale = Scale(1.6f)
        }
    }
    act.netRoot?.let {
        val on = f.netSwing >= 0f
        if (it.isVisible != on) it.isVisible = on
        if (on) {
            it.position = pos
            it.rotation = yawRot
            val t = (f.netSwing / 0.55f).coerceIn(0f, 1f)
            act.netSwing?.rotation = Rotation(x = -70f + 110f * t)
        }
    }
    val emote = f.emote
    for ((e, node) in act.emotes) {
        val on = e == emote
        if (node.isVisible != on) node.isVisible = on
        if (on) {
            val pop = (f.emoteTime / 0.25f).coerceIn(0f, 1f)
            node.position = Position(f.position.x, f.position.y + 1.85f + sin(f.emoteTime * 5f) * 0.04f, f.position.z)
            node.rotation = Rotation(y = world.camera.yaw)
            node.scale = Scale(0.4f + 0.6f * pop)
        }
    }
    val cooking = f.cookTime >= 0f
    val stove = if (cooking) world.currentSave().placed.filter { Catalog[it.itemId]?.use == Use.COOK }.minByOrNull { (it.x - f.position.x) * (it.x - f.position.x) + (it.z - f.position.z) * (it.z - f.position.z) } else null
    act.pot?.let {
        val on = stove != null
        if (it.isVisible != on) it.isVisible = on
        if (stove != null) it.position = Position(stove.x + 0.17f, 0.94f, stove.z - 0.17f)
    }
    act.steam.forEachIndexed { k, node ->
        node ?: return@forEachIndexed
        val on = stove != null
        if (node.isVisible != on) node.isVisible = on
        if (stove != null) {
            val t = ((f.cookTime * 0.7f + k / 3f) % 1f)
            node.position = Position(stove.x + 0.17f + sin(t * 6f + k) * 0.05f, 1.1f + t * 0.6f, stove.z - 0.17f)
            node.scale = Scale(0.6f + t * 1.2f)
        }
    }
    val used = HashMap<String, Int>()
    for (list in act.bugNodes.values) for (n in list) if (n?.isVisible == true) n.isVisible = false
    for (b in f.bugs) {
        val list = act.bugNodes[b.species] ?: continue
        val idx = used.getOrDefault(b.species, 0)
        if (idx >= list.size) continue
        used[b.species] = idx + 1
        val n = list[idx] ?: continue
        n.isVisible = true
        n.position = Position(b.pos.x, b.pos.y, b.pos.z)
        n.rotation = Rotation(y = b.heading)
        val sp = BugDex[b.species]
        n.scale = if (sp?.flyer == true) Scale(0.35f + 0.65f * kotlin.math.abs(sin(b.flap)), 1f, 1f) else Scale(1f)
    }
    val night = world.hour >= 19.5f || world.hour < 1f
    act.fireworkClock -= dt
    for (k in act.fireworks.indices) {
        val node = act.fireworks[k] ?: continue
        var t = act.fireworkTime[k]
        if (t < 0f && festival != null && night && act.fireworkClock <= 0f) {
            act.fireworkClock = act.rng.range(0.6f, 1.6f)
            act.fireworkTime[k] = 0f
            act.fireworkPos[k] = V3(act.rng.range(-18f, 18f), act.rng.range(13f, 20f), act.rng.range(12f, 30f))
            t = 0f
        }
        if (t >= 0f) {
            t += dt
            act.fireworkTime[k] = t
            if (t > 1.7f) {
                act.fireworkTime[k] = -1f
                if (node.isVisible) node.isVisible = false
                continue
            }
            if (!node.isVisible) node.isVisible = true
            val grow = 1f - (1f - (t / 1.7f).coerceIn(0f, 1f)).let { it * it * it }
            val p = act.fireworkPos[k]
            node.position = Position(p.x, p.y - t * t * 0.6f, p.z)
            node.scale = Scale(0.3f + grow * 5.5f)
        } else if (node.isVisible) node.isVisible = false
    }
}
