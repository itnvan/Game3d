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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.ui.theme.Cream
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.Shadow
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.SunDark
import com.cozyhome.app.ui.theme.SunLight
import com.cozyhome.app.ui.theme.White

fun Color.deeper(f: Float = 0.22f) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)

@Composable
fun Modifier.bouncyClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "press")
    return this
        .scale(s)
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
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
    val base = if (enabled) color else color.copy(alpha = 0.45f)
    Box(
        modifier
            .bouncyClick(enabled, onClick)
            .shadow(6.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .background(base.deeper(), shape)
            .padding(bottom = 4.dp)
            .background(base, shape)
            .padding(horizontal = 18.dp)
            .height(height - 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            emoji?.let { Text(it, fontSize = fontSize * 1.15f) }
            Text(text, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, maxLines = 1)
        }
    }
}

@Composable
fun RoundButton(
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    label: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .bouncyClick(enabled, onClick)
                .shadow(6.dp, CircleShape, ambientColor = Shadow, spotColor = Shadow)
                .background(color.deeper(), CircleShape)
                .padding(bottom = 4.dp)
                .background(if (enabled) color else color.copy(alpha = 0.5f), CircleShape)
                .border(2.dp, White.copy(alpha = 0.55f), CircleShape)
                .size(size - 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = (size.value * 0.42f).sp)
        }
        label?.let {
            Text(
                it,
                color = White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .background(Ink.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
fun Pill(modifier: Modifier = Modifier, color: Color = White.copy(alpha = 0.92f), shape: Shape = RoundedCornerShape(50), content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .shadow(4.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .background(color, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Cream, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .shadow(14.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .clip(shape)
            .background(color)
            .border(3.dp, White, shape)
            .padding(12.dp),
    ) { content() }
}

@Composable
fun CoinIcon(size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(SunDark, r)
        drawCircle(Sun, r * 0.86f, center = center.copy(y = center.y - r * 0.06f))
        drawCircle(SunLight, r * 0.5f, center = center + Offset(-r * 0.18f, -r * 0.24f), style = Stroke(r * 0.14f))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, emoji: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .bouncyClick(onClick = onClick)
            .background(if (selected) Ink else White, shape)
            .border(2.dp, if (selected) Ink else Ink.copy(alpha = 0.12f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        emoji?.let { Text(it, fontSize = 13.sp) }
        if (text.isNotEmpty()) Text(text, color = if (selected) White else Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun Bubble(text: String, modifier: Modifier = Modifier, color: Color = White) {
    Text(
        text,
        color = Ink,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Shadow, spotColor = Shadow)
            .background(color, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

fun Modifier.nudge(x: Dp = 0.dp, y: Dp = 0.dp) = this.offset(x, y)
