package com.cozyhome.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.audio.LocalSfx
import com.cozyhome.app.ui.theme.Cream
import com.cozyhome.app.ui.theme.Glass
import com.cozyhome.app.ui.theme.GlassEdge
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.Shadow
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.SunDark
import com.cozyhome.app.ui.theme.SunLight
import com.cozyhome.app.ui.theme.White
import com.cozyhome.game.audio.Sfx

fun Color.deeper(f: Float = 0.22f) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
fun Color.brighter(f: Float = 0.25f) = Color(red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha)

@Composable
fun Modifier.bouncyClick(enabled: Boolean = true, sound: Sfx? = Sfx.TAP, onClick: () -> Unit): Modifier {
    val sfx = LocalSfx.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.42f, stiffness = 650f), label = "press")
    return this
        .scale(s)
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            sound?.let { sfx.play(it) }
            onClick()
        }
}

private fun Modifier.gloss(color: Color, shape: Shape, enabled: Boolean): Modifier {
    val base = if (enabled) color else color.copy(alpha = 0.5f)
    return this
        .shadow(8.dp, shape, ambientColor = Shadow, spotColor = Shadow)
        .background(base.deeper(0.28f), shape)
        .padding(bottom = 3.5.dp)
        .background(Brush.verticalGradient(listOf(base.brighter(0.22f), base, base.deeper(0.06f))), shape)
        .border(1.5.dp, Brush.verticalGradient(listOf(White.copy(alpha = 0.85f), White.copy(alpha = 0.15f))), shape)
}

@Composable
fun ChunkyButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    textColor: Color = White,
    enabled: Boolean = true,
    height: Dp = 42.dp,
    fontSize: TextUnit = 15.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier
            .bouncyClick(enabled, onClick = onClick)
            .gloss(color, shape, enabled)
            .padding(horizontal = 18.dp)
            .height(height - 3.5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            emoji?.let { e ->
                val ico = iconFor(e)
                if (ico != null) GameIcon(ico, (fontSize.value * 1.25f).dp, textColor) else Text(e, fontSize = fontSize * 1.1f)
            }
            Text(text, style = outlined(fontSize, textColor), maxLines = 1)
        }
    }
}

@Composable
fun outlined(size: TextUnit, color: Color = White) = TextStyle(
    color = color,
    fontSize = size,
    fontWeight = FontWeight.Black,
    letterSpacing = 0.2.sp,
    shadow = androidx.compose.ui.graphics.Shadow(Color(0x55000000), Offset(0f, 2f), 3f),
)

@Composable
fun RoundButton(
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    label: String? = null,
    enabled: Boolean = true,
    sound: Sfx? = Sfx.TAP,
    onClick: () -> Unit,
) {
    val ico = iconFor(emoji)
    IconButton(ico, color, modifier, size, label, enabled, emoji, sound, onClick)
}

@Composable
fun IconButton(
    ico: Ico?,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    label: String? = null,
    enabled: Boolean = true,
    fallback: String = "",
    sound: Sfx? = Sfx.TAP,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .bouncyClick(enabled, sound, onClick)
                .gloss(color, CircleShape, enabled)
                .size(size - 3.5.dp)
                .drawBehind {
                    drawArc(
                        White.copy(alpha = 0.35f), 200f, 140f, false,
                        topLeft = Offset(this.size.width * 0.16f, this.size.height * 0.12f),
                        size = Size(this.size.width * 0.68f, this.size.height * 0.6f),
                        style = Stroke(this.size.width * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (ico != null) GameIcon(ico, size * 0.5f, White) else Text(fallback, fontSize = (size.value * 0.42f).sp)
        }
        label?.let {
            Text(
                it,
                style = outlined(11.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(Ink.copy(alpha = 0.62f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 7.dp, vertical = 1.5.dp),
            )
        }
    }
}

@Composable
fun Pill(modifier: Modifier = Modifier, color: Color = Glass, shape: Shape = RoundedCornerShape(50), content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .shadow(6.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .background(Brush.verticalGradient(listOf(color, color.copy(alpha = (color.alpha * 0.9f).coerceAtMost(1f)))), shape)
            .border(1.5.dp, GlassEdge, shape)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Cream, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .shadow(18.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(White, color)))
            .border(2.dp, White, shape)
            .padding(14.dp),
    ) { content() }
}

@Composable
fun CoinIcon(size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(SunDark, r)
        drawCircle(Sun, r * 0.86f, center = center.copy(y = center.y - r * 0.08f))
        drawCircle(SunLight, r * 0.55f, center = center.copy(y = center.y - r * 0.08f), style = Stroke(r * 0.13f))
        drawRoundRect(SunDark, Offset(center.x - r * 0.08f, center.y - r * 0.38f), Size(r * 0.16f, r * 0.6f), CornerRadius(r * 0.08f))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, emoji: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .bouncyClick(onClick = onClick)
            .shadow(if (selected) 4.dp else 0.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .background(if (selected) Brush.verticalGradient(listOf(Ink.brighter(0.12f), Ink)) else Brush.verticalGradient(listOf(White, White.copy(alpha = 0.9f))), shape)
            .border(1.5.dp, if (selected) Ink else Ink.copy(alpha = 0.1f), shape)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        emoji?.let { e ->
            val ico = iconFor(e)
            if (ico != null) GameIcon(ico, 14.dp, if (selected) White else Ink) else Text(e, fontSize = 13.sp)
        }
        if (text.isNotEmpty()) Text(text, color = if (selected) White else Ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun Bubble(text: String, modifier: Modifier = Modifier, color: Color = White) {
    Box(
        modifier
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Shadow, spotColor = Shadow)
            .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.92f))), RoundedCornerShape(18.dp))
            .border(1.5.dp, White, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier, track: Color = Ink.copy(alpha = 0.12f)) {
    Canvas(modifier) {
        val h = size.height
        drawRoundRect(track, cornerRadius = CornerRadius(h / 2, h / 2))
        val w = (size.width * progress.coerceIn(0f, 1f)).coerceAtLeast(h)
        drawRoundRect(Brush.verticalGradient(listOf(color.brighter(0.3f), color, color.deeper(0.1f))), size = Size(w, h), cornerRadius = CornerRadius(h / 2, h / 2))
        drawRoundRect(White.copy(alpha = 0.45f), topLeft = Offset(h * 0.4f, h * 0.18f), size = Size((w - h * 0.8f).coerceAtLeast(0f), h * 0.22f), cornerRadius = CornerRadius(h * 0.11f, h * 0.11f))
    }
}

fun Modifier.nudge(x: Dp = 0.dp, y: Dp = 0.dp) = this.offset(x, y)
