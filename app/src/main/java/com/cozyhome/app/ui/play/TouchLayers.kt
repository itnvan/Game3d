package com.cozyhome.app.ui.play

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.R
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.JoyBase
import com.cozyhome.app.ui.theme.JoyKnob
import com.cozyhome.app.ui.theme.JoyRing
import com.cozyhome.app.ui.theme.Mint
import com.cozyhome.app.ui.theme.SkyBottom
import com.cozyhome.app.ui.theme.SkyTop
import com.cozyhome.app.ui.theme.White
import com.cozyhome.game.game.FrameInput
import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3

@Composable
fun ControlsLayer(input: FrameInput, modifier: Modifier = Modifier, stickEnabled: Boolean = true) {
    val density = LocalDensity.current
    val radiusPx = with(density) { 56.dp.toPx() }
    var origin by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    val enabled by rememberUpdatedState(stickEnabled)

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var stickId: PointerId? = null
                    val lookPointers = LinkedHashMap<PointerId, Offset>()
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            val id = change.id
                            if (change.pressed && !change.previousPressed) {
                                if (enabled && stickId == null && change.position.x < size.width * 0.42f) {
                                    stickId = id
                                    origin = change.position
                                    knob = Offset.Zero
                                } else {
                                    lookPointers[id] = change.position
                                }
                                change.consume()
                            } else if (change.pressed) {
                                if (id == stickId) {
                                    val o = origin ?: change.position
                                    var d = change.position - o
                                    val len = d.getDistance()
                                    if (len > radiusPx) d = d * (radiusPx / len)
                                    knob = d
                                    input.stickX = d.x / radiusPx
                                    input.stickY = -d.y / radiusPx
                                } else if (lookPointers.containsKey(id)) {
                                    val prev = lookPointers.getValue(id)
                                    if (lookPointers.size >= 2) {
                                        val other = lookPointers.entries.first { it.key != id }.value
                                        val before = (prev - other).getDistance()
                                        val after = (change.position - other).getDistance()
                                        if (before > 1f && after > 1f) input.zoom *= before / after
                                    } else {
                                        val delta = change.position - prev
                                        input.lookYaw += delta.x / size.width
                                        input.lookPitch += delta.y / size.height
                                    }
                                    lookPointers[id] = change.position
                                }
                                change.consume()
                            } else if (!change.pressed && change.previousPressed) {
                                if (id == stickId) {
                                    stickId = null
                                    origin = null
                                    knob = Offset.Zero
                                    input.stickX = 0f
                                    input.stickY = 0f
                                }
                                lookPointers.remove(id)
                            }
                        }
                    }
                }
            }
    ) {
        if (stickEnabled) {
            Canvas(Modifier.fillMaxSize()) {
                val base = origin ?: Offset(radiusPx * 1.9f, size.height - radiusPx * 1.9f)
                drawCircle(JoyBase, radius = radiusPx * 1.15f, center = base)
                drawCircle(JoyRing, radius = radiusPx * 1.15f, center = base, style = Stroke(width = 3.dp.toPx()))
                drawCircle(JoyKnob, radius = radiusPx * 0.48f, center = base + knob)
            }
        }
    }
}

@Composable
fun TapLayer(onTap: () -> Unit) {
    val tap by rememberUpdatedState(onTap)
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        for (c in e.changes) {
                            if (c.pressed && !c.previousPressed) {
                                tap()
                                c.consume()
                            }
                        }
                    }
                }
            }
    )
}

@Composable
fun DecorTouchLayer(vm: GameViewModel, screenRay: (Float, Float) -> Pair<V3, V3>?) {
    val floorY = if (vm.hud.decorZone == com.cozyhome.game.state.Zone.HOUSE) 0.04f else 0f
    val floorPoint: (Float, Float) -> V2? = { x, y -> screenRay(x, y)?.let { (o, d) -> vm.rayAt(o, d, floorY) } }
    val input = vm.input
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    val pointers = LinkedHashMap<PointerId, Offset>()
                    var grabbing = false
                    var grabOffset = V2.ZERO
                    var downAt = Offset.Zero
                    var moved = false
                    var downTime = 0L
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            val id = change.id
                            if (change.pressed && !change.previousPressed) {
                                pointers[id] = change.position
                                if (pointers.size == 1) {
                                    downAt = change.position
                                    downTime = change.uptimeMillis
                                    moved = false
                                    val p = floorPoint(change.position.x, change.position.y)
                                    val edit = vm.hud.decorEdit
                                    grabbing = p != null && screenRay(change.position.x, change.position.y)?.let { (o, d) -> vm.grabsEdit(o, d) } == true
                                    if (grabbing && p != null && edit != null) grabOffset = V2(edit.item.x, edit.item.z) - p
                                } else {
                                    grabbing = false
                                }
                                change.consume()
                            } else if (change.pressed && pointers.containsKey(id)) {
                                val prev = pointers.getValue(id)
                                if ((change.position - downAt).getDistance() > 14f) moved = true
                                if (pointers.size >= 2) {
                                    val other = pointers.entries.first { it.key != id }.value
                                    val before = (prev - other).getDistance()
                                    val after = (change.position - other).getDistance()
                                    if (before > 1f && after > 1f) input.zoom *= before / after
                                } else if (grabbing) {
                                    floorPoint(change.position.x, change.position.y)?.let { vm.moveEdit(it + grabOffset) }
                                } else {
                                    val delta = change.position - prev
                                    input.lookYaw += delta.x / size.width
                                    input.lookPitch += delta.y / size.height
                                }
                                pointers[id] = change.position
                                change.consume()
                            } else if (!change.pressed && change.previousPressed) {
                                val wasSingle = pointers.size == 1
                                pointers.remove(id)
                                if (wasSingle && !moved && change.uptimeMillis - downTime < 350) {
                                    screenRay(change.position.x, change.position.y)?.let { (o, d) -> vm.decorTap(o, d) }
                                }
                                if (pointers.isEmpty()) grabbing = false
                            }
                        }
                    }
                }
            }
    )
}

@Composable
fun SoccerTouchLayer(vm: GameViewModel, goalPoint: (Float, Float) -> V3?) {
    var trail by remember { mutableStateOf<List<Offset>>(emptyList()) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var start = Offset.Zero
                    var startTime = 0L
                    var active = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val c = event.changes.firstOrNull() ?: continue
                        if (c.pressed && !c.previousPressed) {
                            start = c.position
                            startTime = c.uptimeMillis
                            active = true
                            trail = listOf(c.position)
                        } else if (c.pressed && active) {
                            trail = (trail + c.position).takeLast(24)
                        } else if (!c.pressed && c.previousPressed && active) {
                            active = false
                            val d = c.position - start
                            val dtMs = (c.uptimeMillis - startTime).coerceAtLeast(40L)
                            if (-d.y > size.height * 0.08f) {
                                val speed = d.getDistance() / (dtMs / 1000f)
                                val power = (speed / (size.height * 3.2f)).coerceIn(0.15f, 1f)
                                val target = goalPoint(c.position.x, c.position.y)
                                    ?: V3(com.cozyhome.game.world.Lot.GOAL_X, 1f, com.cozyhome.game.world.Lot.GOAL_Z)
                                vm.kick(V3(target.x, target.y.coerceIn(0.15f, 2.6f), target.z.coerceIn(3f, 11f)), power)
                            }
                            trail = emptyList()
                        }
                        c.consume()
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 1 until trail.size) {
                val a = i / trail.size.toFloat()
                drawLine(White.copy(alpha = a * 0.8f), trail[i - 1], trail[i], strokeWidth = 10.dp.toPx() * a, cap = StrokeCap.Round)
            }
        }
    }
}
