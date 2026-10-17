package com.cozyhome.app.ui.play

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.R
import com.cozyhome.app.ui.components.ProgressBar
import com.cozyhome.app.ui.components.outlined
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.Mint
import com.cozyhome.app.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

private val SKY_HIGH = Color(0xFF6FB8F2)
private val SKY_LOW = Color(0xFFD9F0FF)
private val HILL_BACK = Color(0xFF8CC46B)
private val HILL_FRONT = Color(0xFF5FA650)
private val ROOF = Color(0xFFC0613F)
private val WALL = Color(0xFFF6EFE3)
private val TRIM = Color(0xFF9A7356)
private val GLASS = Color(0xFFFFE2A0)
private val TRUNK = Color(0xFF7A5A43)
private val CANOPY = Color(0xFF4F8C3A)
private val CANOPY_LIGHT = Color(0xFF7DB257)
private val CLOUD = Color(0xF2FFFFFF)

@Composable
fun LoadingOverlay(progress: () -> Float = { -1f }) {
    val t = rememberInfiniteTransition(label = "load")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(26_000, easing = LinearEasing)), label = "drift")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob")
    var shown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var fake = 0f
        while (isActive) {
            withFrameNanos { }
            val real = progress()
            fake = (fake + 0.004f).coerceAtMost(0.9f)
            val target = if (real < 0f) fake else maxOf(real, 0.04f)
            shown += (target - shown) * 0.12f
        }
    }
    val tips = stringArrayResource(R.array.loading_tips)
    var tip by remember { mutableIntStateOf(0) }
    LaunchedEffect(tips.size) {
        while (isActive) {
            delay(2600)
            tip = (tip + 1) % tips.size
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SKY_HIGH, SKY_LOW)))) {
        Canvas(Modifier.fillMaxSize()) { scenery(drift, bob) }
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(stringResource(R.string.app_name), style = outlined(34.sp))
            Text(stringResource(R.string.tagline), color = Ink.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedContent(tip, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) }, label = "tip") { i ->
                Text(tips.getOrElse(i) { "" }, color = White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.graphicsLayer { shadowElevation = 0f })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.width(260.dp).height(14.dp)) { ProgressBar(shown, Mint, Modifier.fillMaxSize(), track = White.copy(alpha = 0.55f)) }
                PercentText { shown }
            }
            Text(stringResource(R.string.loading), color = Ink.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PercentText(value: () -> Float) {
    var pct by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { }
            val p = (value() * 100f).toInt().coerceIn(0, 100)
            if (p != pct) pct = p
        }
    }
    Text("$pct%", style = outlined(13.sp), modifier = Modifier.size(width = 40.dp, height = 18.dp))
}

private fun DrawScope.cloud(x: Float, y: Float, s: Float) {
    drawOval(CLOUD, Offset(x - 60f * s, y - 18f * s), Size(120f * s, 40f * s))
    drawCircle(CLOUD, 26f * s, Offset(x - 22f * s, y - 16f * s))
    drawCircle(CLOUD, 34f * s, Offset(x + 12f * s, y - 22f * s))
}

private fun DrawScope.scenery(drift: Float, bob: Float) {
    val w = size.width
    val h = size.height
    val d = density
    for (k in 0 until 4) {
        val speed = 0.6f + k * 0.25f
        val x = ((drift * speed + k * 0.27f) % 1f) * (w + 300f * d) - 150f * d
        cloud(x, h * (0.14f + 0.09f * k), d * (0.75f + 0.18f * (k % 2)))
    }
    val back = Path().apply {
        moveTo(0f, h * 0.72f)
        cubicTo(w * 0.25f, h * 0.6f, w * 0.45f, h * 0.66f, w * 0.62f, h * 0.62f)
        cubicTo(w * 0.8f, h * 0.58f, w * 0.92f, h * 0.66f, w, h * 0.64f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(back, HILL_BACK)
    val front = Path().apply {
        moveTo(0f, h * 0.84f)
        cubicTo(w * 0.3f, h * 0.74f, w * 0.6f, h * 0.8f, w, h * 0.76f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(front, HILL_FRONT)
    val cx = w * 0.5f
    val base = h * 0.68f - bob * 3f * d
    val s = d * 1.15f
    drawRect(WALL, Offset(cx - 56f * s, base - 52f * s), Size(112f * s, 52f * s))
    val roof = Path().apply {
        moveTo(cx - 68f * s, base - 48f * s)
        lineTo(cx, base - 92f * s)
        lineTo(cx + 68f * s, base - 48f * s)
        close()
    }
    drawPath(roof, ROOF)
    drawRect(Color(0xFFA9583F), Offset(cx + 26f * s, base - 92f * s), Size(14f * s, 26f * s))
    drawRect(TRIM, Offset(cx - 12f * s, base - 34f * s), Size(24f * s, 34f * s))
    for (sx in listOf(-1f, 1f)) {
        drawRect(TRIM, Offset(cx + sx * 34f * s - 13f * s, base - 40f * s), Size(26f * s, 22f * s))
        drawRect(GLASS.copy(alpha = 0.75f + 0.25f * sin(bob * 3.14f)), Offset(cx + sx * 34f * s - 11f * s, base - 38f * s), Size(22f * s, 18f * s))
    }
    val tx = cx + 104f * s
    drawRect(TRUNK, Offset(tx - 5f * s, base - 40f * s), Size(10f * s, 40f * s))
    drawCircle(CANOPY, 30f * s, Offset(tx, base - 62f * s))
    drawCircle(CANOPY_LIGHT, 18f * s, Offset(tx - 10f * s, base - 72f * s))
}
