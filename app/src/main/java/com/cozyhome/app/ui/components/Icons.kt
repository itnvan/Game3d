package com.cozyhome.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.SunDark
import com.cozyhome.app.ui.theme.SunLight
import com.cozyhome.app.ui.theme.White
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Ico {
    COIN, GEAR, SUN, MOON, SUNSET, SOFA, SEED, WATER, CARROT, HOURGLASS, BASKETBALL, SOCCER, RUN, BOLT, GIFT,
    CHAIR, SLEEP, TV, BULB, BOUNCE, BALLOON, STAND, CLOSE, BACK, CHECK, ROTATE, TRASH, BAG, STAR, FISH, NET,
    POT, SMILE, QUEST, CALENDAR, HEART, BOOK, PLUS, LOCK, SPARKLE, HOME, PAINT, DICE, PERSON, TREE, BOX, CUP,
    GAMEPAD, SHIRT, BED, CACTUS, PARTY, SWAP, DIAMOND, BLOCK, MUSIC, TROPHY, LEAF,
}

private val EMOJI = mapOf(
    "🛋️" to Ico.SOFA, "🛋" to Ico.SOFA, "⚙️" to Ico.GEAR, "⚙" to Ico.GEAR, "🧍" to Ico.STAND, "☀️" to Ico.SUN, "☀" to Ico.SUN,
    "🌙" to Ico.MOON, "🌇" to Ico.SUNSET, "🏀" to Ico.BASKETBALL, "⚽" to Ico.SOCCER, "🏃" to Ico.RUN, "⚡" to Ico.BOLT,
    "✖️" to Ico.CLOSE, "✖" to Ico.CLOSE, "🤸" to Ico.BOUNCE, "🎈" to Ico.BALLOON, "🌱" to Ico.SEED, "💧" to Ico.WATER,
    "🥕" to Ico.CARROT, "⏳" to Ico.HOURGLASS, "⏱️" to Ico.HOURGLASS, "🎁" to Ico.GIFT, "😴" to Ico.SLEEP, "📺" to Ico.TV,
    "💡" to Ico.BULB, "⬅️" to Ico.BACK, "⬅" to Ico.BACK, "🎲" to Ico.DICE, "🏡" to Ico.HOME, "🏠" to Ico.HOME, "🔒" to Ico.LOCK,
    "🎨" to Ico.PAINT, "🌳" to Ico.TREE, "✅" to Ico.CHECK, "🛏" to Ico.BED, "🛏️" to Ico.BED, "🔶" to Ico.DIAMOND,
    "🔄" to Ico.ROTATE, "🔁" to Ico.SWAP, "📦" to Ico.BOX, "📚" to Ico.BOOK, "👕" to Ico.SHIRT, "🎮" to Ico.GAMEPAD,
    "🎉" to Ico.PARTY, "🍳" to Ico.POT, "🌵" to Ico.CACTUS, "✨" to Ico.SPARKLE, "⛔" to Ico.BLOCK, "☕" to Ico.CUP,
    "🗑️" to Ico.TRASH, "🗑" to Ico.TRASH, "🐟" to Ico.FISH, "⭐" to Ico.STAR, "❤️" to Ico.HEART, "🏆" to Ico.TROPHY,
)

fun iconFor(emoji: String): Ico? = EMOJI[emoji]

@Composable
fun GameIcon(ico: Ico, size: Dp = 24.dp, tint: Color = White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawIco(ico, tint) }
}

private fun DrawScope.u(v: Float) = v / 24f * size.minDimension
private fun DrawScope.p(x: Float, y: Float) = Offset(u(x), u(y))

private fun DrawScope.line(c: Color, w: Float, vararg pts: Float) {
    val path = Path()
    path.moveTo(u(pts[0]), u(pts[1]))
    var i = 2
    while (i < pts.size) { path.lineTo(u(pts[i]), u(pts[i + 1])); i += 2 }
    drawPath(path, c, style = Stroke(u(w), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.fill(c: Color, vararg pts: Float) {
    val path = Path()
    path.moveTo(u(pts[0]), u(pts[1]))
    var i = 2
    while (i < pts.size) { path.lineTo(u(pts[i]), u(pts[i + 1])); i += 2 }
    path.close()
    drawPath(path, c)
}

private fun DrawScope.ring(c: Color, x: Float, y: Float, r: Float, w: Float) = drawCircle(c, u(r), p(x, y), style = Stroke(u(w)))
private fun DrawScope.dot(c: Color, x: Float, y: Float, r: Float) = drawCircle(c, u(r), p(x, y))
private fun DrawScope.rrect(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float) =
    drawRoundRect(c, p(x, y), Size(u(w), u(h)), CornerRadius(u(r), u(r)))
private fun DrawScope.rrectLine(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float, sw: Float) =
    drawRoundRect(c, p(x, y), Size(u(w), u(h)), CornerRadius(u(r), u(r)), style = Stroke(u(sw)))

private fun DrawScope.arcLine(c: Color, x: Float, y: Float, r: Float, start: Float, sweep: Float, w: Float) =
    drawArc(c, start, sweep, false, p(x - r, y - r), Size(u(r * 2), u(r * 2)), style = Stroke(u(w), cap = StrokeCap.Round))

fun DrawScope.drawIco(ico: Ico, c: Color) {
    val w = 2.2f
    when (ico) {
        Ico.COIN -> {
            dot(SunDark, 12f, 12.6f, 10f)
            dot(Sun, 12f, 11.6f, 9f)
            ring(SunLight, 12f, 11.6f, 6f, 1.4f)
            line(SunDark, 1.8f, 12f, 8.6f, 12f, 14.6f)
        }
        Ico.GEAR -> {
            for (k in 0 until 8) {
                val a = k * PI.toFloat() / 4f
                line(c, 3.2f, 12f + cos(a) * 6f, 12f + sin(a) * 6f, 12f + cos(a) * 9.5f, 12f + sin(a) * 9.5f)
            }
            ring(c, 12f, 12f, 6.2f, 2.6f)
            dot(c, 12f, 12f, 2.2f)
        }
        Ico.SUN -> {
            dot(c, 12f, 12f, 5f)
            for (k in 0 until 8) {
                val a = k * PI.toFloat() / 4f
                line(c, w, 12f + cos(a) * 8f, 12f + sin(a) * 8f, 12f + cos(a) * 10.5f, 12f + sin(a) * 10.5f)
            }
        }
        Ico.MOON -> {
            val path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(u(3f), u(3f), u(21f), u(21f)))
            }
            val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(u(8f), u(0f), u(25f), u(17f))) }
            val moon = Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, path, cut)
            drawPath(moon, c)
            dot(c, 18.5f, 15f, 1f)
        }
        Ico.SUNSET -> {
            arcLine(c, 12f, 16f, 6f, 180f, 180f, w)
            for (k in 0..4) {
                val a = PI.toFloat() + k * PI.toFloat() / 4f
                line(c, w, 12f + cos(a) * 8.5f, 16f + sin(a) * 8.5f, 12f + cos(a) * 10.5f, 16f + sin(a) * 10.5f)
            }
            line(c, w, 3f, 19f, 21f, 19f)
        }
        Ico.SOFA -> {
            rrect(c, 4.5f, 6f, 15f, 7f, 2.5f)
            rrect(c, 2f, 10f, 5f, 8f, 2f)
            rrect(c, 17f, 10f, 5f, 8f, 2f)
            rrect(c, 5f, 13f, 14f, 4.5f, 1.5f)
            line(c, w, 5f, 18.5f, 5f, 20.5f)
            line(c, w, 19f, 18.5f, 19f, 20.5f)
        }
        Ico.SEED -> {
            line(c, w, 12f, 21f, 12f, 11f)
            fill(c, 12f, 13f, 6f, 11.5f, 3.5f, 6.5f, 8.5f, 6f, 12f, 10f)
            fill(c, 12f, 11f, 14f, 6.5f, 19.5f, 4.5f, 20f, 9.5f, 16f, 12f)
            line(c, w, 7f, 21f, 17f, 21f)
        }
        Ico.WATER -> {
            val path = Path().apply {
                moveTo(u(12f), u(2.5f))
                cubicTo(u(15f), u(7f), u(19f), u(11f), u(19f), u(15f))
                cubicTo(u(19f), u(19f), u(15.8f), u(21.5f), u(12f), u(21.5f))
                cubicTo(u(8.2f), u(21.5f), u(5f), u(19f), u(5f), u(15f))
                cubicTo(u(5f), u(11f), u(9f), u(7f), u(12f), u(2.5f))
                close()
            }
            drawPath(path, c)
            arcLine(Color.Black.copy(alpha = 0.18f), 12f, 15f, 4.2f, 100f, 70f, 1.6f)
        }
        Ico.CARROT -> {
            fill(c, 8f, 9f, 16f, 9f, 13f, 22f, 11f, 22f)
            line(c, w, 12f, 8f, 12f, 3f)
            line(c, w, 12f, 7f, 8f, 3.5f)
            line(c, w, 12f, 7f, 16f, 3.5f)
        }
        Ico.HOURGLASS -> {
            line(c, w, 6f, 3f, 18f, 3f)
            line(c, w, 6f, 21f, 18f, 21f)
            line(c, w, 7f, 3f, 7f, 6f, 12f, 12f, 17f, 6f, 17f, 3f)
            line(c, w, 7f, 21f, 7f, 18f, 12f, 12f, 17f, 18f, 17f, 21f)
            fill(c, 9f, 19.5f, 15f, 19.5f, 12f, 16f)
        }
        Ico.BASKETBALL -> {
            ring(c, 12f, 12f, 9f, w)
            line(c, w * 0.8f, 3f, 12f, 21f, 12f)
            line(c, w * 0.8f, 12f, 3f, 12f, 21f)
            arcLine(c, 4f, 12f, 6.5f, -55f, 110f, w * 0.8f)
            arcLine(c, 20f, 12f, 6.5f, 125f, 110f, w * 0.8f)
        }
        Ico.SOCCER -> {
            ring(c, 12f, 12f, 9f, w)
            fill(c, 12f, 8f, 15.5f, 10.5f, 14.2f, 14.5f, 9.8f, 14.5f, 8.5f, 10.5f)
            line(c, w * 0.7f, 12f, 8f, 12f, 3.5f)
            line(c, w * 0.7f, 15.5f, 10.5f, 20f, 9f)
            line(c, w * 0.7f, 8.5f, 10.5f, 4f, 9f)
            line(c, w * 0.7f, 14.2f, 14.5f, 17f, 19f)
            line(c, w * 0.7f, 9.8f, 14.5f, 7f, 19f)
        }
        Ico.RUN -> {
            dot(c, 14.5f, 4f, 2.2f)
            line(c, w, 13.5f, 7.5f, 11f, 13f, 14f, 16f, 13.5f, 21f)
            line(c, w, 11f, 13f, 7.5f, 16.5f, 4.5f, 16f)
            line(c, w, 12.8f, 9f, 16.5f, 11.5f, 19f, 10f)
            line(c, w, 12.8f, 9f, 8.5f, 9.5f, 6.5f, 12f)
        }
        Ico.BOLT -> fill(c, 13.5f, 2f, 5f, 13.5f, 11f, 13.5f, 9.5f, 22f, 19f, 10f, 13f, 10f)
        Ico.GIFT -> {
            rrect(c, 4f, 10f, 16f, 11f, 1.5f)
            rrect(c, 3f, 7f, 18f, 4f, 1.2f)
            drawRect(Color.Black.copy(alpha = 0.22f), p(10.8f, 7f), Size(u(2.4f), u(14f)))
            line(c, w, 12f, 7f, 8.5f, 3.5f, 7f, 5f, 12f, 7f)
            line(c, w, 12f, 7f, 15.5f, 3.5f, 17f, 5f, 12f, 7f)
        }
        Ico.CHAIR -> {
            rrect(c, 6f, 3f, 12f, 9f, 2f)
            rrect(c, 5f, 12f, 14f, 3f, 1f)
            line(c, w, 7f, 15f, 6.5f, 21f)
            line(c, w, 17f, 15f, 17.5f, 21f)
        }
        Ico.SLEEP -> {
            line(c, w, 4f, 5f, 10f, 5f, 4f, 12f, 10f, 12f)
            line(c, w * 0.85f, 12f, 11f, 16f, 11f, 12f, 16f, 16f, 16f)
            line(c, w * 0.7f, 17f, 17f, 20f, 17f, 17f, 21f, 20f, 21f)
        }
        Ico.TV -> {
            rrectLine(c, 2.5f, 5f, 19f, 12.5f, 2f, w)
            line(c, w, 8f, 21f, 16f, 21f)
            line(c, w, 12f, 17.5f, 12f, 21f)
            line(c, w * 0.8f, 9f, 2f, 12f, 5f, 15f, 2f)
        }
        Ico.BULB -> {
            val path = Path().apply {
                moveTo(u(9f), u(16f))
                cubicTo(u(9f), u(13f), u(5f), u(11.5f), u(5f), u(8.5f))
                cubicTo(u(5f), u(4.5f), u(8.2f), u(2f), u(12f), u(2f))
                cubicTo(u(15.8f), u(2f), u(19f), u(4.5f), u(19f), u(8.5f))
                cubicTo(u(19f), u(11.5f), u(15f), u(13f), u(15f), u(16f))
                close()
            }
            drawPath(path, c)
            rrect(c, 9f, 17f, 6f, 2f, 0.8f)
            rrect(c, 10f, 20f, 4f, 2f, 0.8f)
        }
        Ico.BOUNCE -> {
            line(c, w, 4f, 21f, 20f, 21f)
            line(c, w, 6f, 21f, 7f, 17.5f, 17f, 17.5f, 18f, 21f)
            dot(c, 12f, 5f, 2.3f)
            line(c, w, 12f, 7.5f, 12f, 12.5f)
            line(c, w, 7f, 6f, 12f, 9f, 17f, 6f)
            line(c, w, 9f, 15f, 12f, 12.5f, 15f, 15f)
        }
        Ico.BALLOON -> {
            drawOval(c, p(6f, 2f), Size(u(12f), u(14f)))
            fill(c, 11f, 15.5f, 13f, 15.5f, 12f, 17.5f)
            line(c, 1.6f, 12f, 17.5f, 10.5f, 20f, 13f, 22f)
        }
        Ico.STAND -> {
            dot(c, 12f, 4.5f, 2.6f)
            line(c, w, 12f, 8.5f, 12f, 15f)
            line(c, w, 7f, 10.5f, 12f, 9f, 17f, 10.5f)
            line(c, w, 12f, 15f, 9f, 21f)
            line(c, w, 12f, 15f, 15f, 21f)
        }
        Ico.CLOSE -> {
            line(c, w * 1.3f, 6f, 6f, 18f, 18f)
            line(c, w * 1.3f, 18f, 6f, 6f, 18f)
        }
        Ico.BACK -> {
            line(c, w * 1.2f, 20f, 12f, 4.5f, 12f)
            line(c, w * 1.2f, 10f, 6f, 4f, 12f, 10f, 18f)
        }
        Ico.CHECK -> line(c, w * 1.4f, 4.5f, 12.5f, 9.5f, 17.5f, 19.5f, 6.5f)
        Ico.ROTATE -> {
            arcLine(c, 12f, 12f, 7.5f, -70f, 290f, w)
            fill(c, 15f, 2.5f, 20.5f, 5.5f, 15.5f, 8.5f)
        }
        Ico.SWAP -> {
            line(c, w, 4f, 8f, 18f, 8f)
            line(c, w, 14f, 4f, 18f, 8f, 14f, 12f)
            line(c, w, 20f, 16f, 6f, 16f)
            line(c, w, 10f, 12f, 6f, 16f, 10f, 20f)
        }
        Ico.TRASH -> {
            line(c, w, 4f, 6f, 20f, 6f)
            line(c, w, 9f, 6f, 9.5f, 3f, 14.5f, 3f, 15f, 6f)
            fill(c, 5.5f, 8f, 18.5f, 8f, 17f, 21f, 7f, 21f)
            line(Color.Black.copy(alpha = 0.2f), 1.4f, 10f, 11f, 10f, 18f)
            line(Color.Black.copy(alpha = 0.2f), 1.4f, 14f, 11f, 14f, 18f)
        }
        Ico.BAG -> {
            rrect(c, 4f, 8f, 16f, 13.5f, 2.2f)
            arcLine(c, 12f, 8.5f, 4f, 180f, 180f, w)
        }
        Ico.STAR -> {
            val pts = ArrayList<Float>()
            for (k in 0 until 10) {
                val a = -PI.toFloat() / 2f + k * PI.toFloat() / 5f
                val r = if (k % 2 == 0) 10f else 4.4f
                pts += 12f + cos(a) * r; pts += 12.8f + sin(a) * r
            }
            fill(c, *pts.toFloatArray())
        }
        Ico.SPARKLE -> {
            fill(c, 10f, 2f, 12f, 8f, 18f, 10f, 12f, 12f, 10f, 18f, 8f, 12f, 2f, 10f, 8f, 8f)
            fill(c, 18f, 13f, 19f, 16f, 22f, 17f, 19f, 18f, 18f, 21f, 17f, 18f, 14f, 17f, 17f, 16f)
        }
        Ico.FISH -> {
            drawOval(c, p(3f, 7f), Size(u(14f), u(10f)))
            fill(c, 15.5f, 12f, 22f, 6.5f, 22f, 17.5f)
            dot(Color.Black.copy(alpha = 0.45f), 7.5f, 11f, 1.2f)
        }
        Ico.NET -> {
            ring(c, 9f, 9f, 6.5f, w)
            line(c, 1.2f, 4f, 9f, 14f, 9f)
            line(c, 1.2f, 9f, 4f, 9f, 14f)
            line(c, w * 1.1f, 13.5f, 13.5f, 21f, 21f)
        }
        Ico.POT -> {
            rrect(c, 4f, 9f, 16f, 11f, 3f)
            line(c, w, 2f, 11f, 4f, 11f)
            line(c, w, 20f, 11f, 22f, 11f)
            rrect(c, 3.5f, 7f, 17f, 2.2f, 1f)
            line(c, w * 0.8f, 9f, 2.5f, 9f, 4.5f)
            line(c, w * 0.8f, 12f, 2f, 12f, 4.5f)
            line(c, w * 0.8f, 15f, 2.5f, 15f, 4.5f)
        }
        Ico.SMILE -> {
            ring(c, 12f, 12f, 9.5f, w)
            dot(c, 8.5f, 10f, 1.4f)
            dot(c, 15.5f, 10f, 1.4f)
            arcLine(c, 12f, 12.5f, 5f, 25f, 130f, w)
        }
        Ico.QUEST -> {
            rrectLine(c, 5f, 3f, 14f, 18f, 2f, w)
            line(c, w * 0.8f, 8f, 8.5f, 9.5f, 10f, 12f, 7.5f)
            line(c, w * 0.8f, 13.5f, 9f, 16f, 9f)
            line(c, w * 0.8f, 8f, 14.5f, 9.5f, 16f, 12f, 13.5f)
            line(c, w * 0.8f, 13.5f, 15f, 16f, 15f)
        }
        Ico.CALENDAR -> {
            rrectLine(c, 3.5f, 5f, 17f, 15.5f, 2f, w)
            drawRect(c, p(3.5f, 5f), Size(u(17f), u(4.5f)))
            line(c, w, 8f, 3f, 8f, 6f)
            line(c, w, 16f, 3f, 16f, 6f)
            dot(c, 8.5f, 13f, 1.2f); dot(c, 12f, 13f, 1.2f); dot(c, 15.5f, 13f, 1.2f)
            dot(c, 8.5f, 16.5f, 1.2f); dot(c, 12f, 16.5f, 1.2f)
        }
        Ico.HEART -> {
            val path = Path().apply {
                moveTo(u(12f), u(21f))
                cubicTo(u(4f), u(15f), u(2f), u(11f), u(2f), u(8f))
                cubicTo(u(2f), u(5f), u(4.5f), u(3f), u(7.2f), u(3f))
                cubicTo(u(9.4f), u(3f), u(11f), u(4.4f), u(12f), u(6f))
                cubicTo(u(13f), u(4.4f), u(14.6f), u(3f), u(16.8f), u(3f))
                cubicTo(u(19.5f), u(3f), u(22f), u(5f), u(22f), u(8f))
                cubicTo(u(22f), u(11f), u(20f), u(15f), u(12f), u(21f))
                close()
            }
            drawPath(path, c)
        }
        Ico.BOOK -> {
            fill(c, 2.5f, 5f, 11f, 6.5f, 11f, 20.5f, 2.5f, 19f)
            fill(c, 21.5f, 5f, 13f, 6.5f, 13f, 20.5f, 21.5f, 19f)
        }
        Ico.PLUS -> {
            line(c, w * 1.4f, 12f, 4.5f, 12f, 19.5f)
            line(c, w * 1.4f, 4.5f, 12f, 19.5f, 12f)
        }
        Ico.LOCK -> {
            rrect(c, 5f, 10.5f, 14f, 10.5f, 2f)
            arcLine(c, 12f, 10.5f, 4.2f, 180f, 180f, w)
            line(c, w, 7.8f, 10.5f, 7.8f, 9f)
            line(c, w, 16.2f, 10.5f, 16.2f, 9f)
            dot(Color.Black.copy(alpha = 0.3f), 12f, 15f, 1.5f)
        }
        Ico.HOME -> {
            fill(c, 12f, 3f, 22f, 11.5f, 19f, 11.5f, 19f, 21f, 5f, 21f, 5f, 11.5f, 2f, 11.5f)
            drawRoundRect(Color.Black.copy(alpha = 0.25f), p(10f, 14f), Size(u(4f), u(7f)), CornerRadius(u(1f), u(1f)))
        }
        Ico.PAINT -> {
            rrect(c, 3f, 3f, 15f, 6f, 2f)
            line(c, w, 18f, 6f, 21f, 6f, 21f, 12f, 12f, 12f, 12f, 15f)
            rrect(c, 10.5f, 15f, 3f, 7f, 1.2f)
        }
        Ico.DICE -> {
            rotate(12f, center) {
                rrect(c, 4f, 4f, 16f, 16f, 3.5f)
                val k = Color.Black.copy(alpha = 0.3f)
                dot(k, 8.5f, 8.5f, 1.5f); dot(k, 15.5f, 8.5f, 1.5f); dot(k, 12f, 12f, 1.5f); dot(k, 8.5f, 15.5f, 1.5f); dot(k, 15.5f, 15.5f, 1.5f)
            }
        }
        Ico.PERSON -> {
            dot(c, 12f, 7.5f, 4.5f)
            val path = Path().apply {
                moveTo(u(3.5f), u(21.5f))
                cubicTo(u(3.5f), u(16f), u(7.5f), u(13.5f), u(12f), u(13.5f))
                cubicTo(u(16.5f), u(13.5f), u(20.5f), u(16f), u(20.5f), u(21.5f))
                close()
            }
            drawPath(path, c)
        }
        Ico.TREE -> {
            dot(c, 12f, 9f, 6.5f)
            dot(c, 7.5f, 12f, 4f)
            dot(c, 16.5f, 12f, 4f)
            line(c, w * 1.2f, 12f, 13f, 12f, 21.5f)
        }
        Ico.LEAF -> {
            val path = Path().apply {
                moveTo(u(4f), u(20f))
                cubicTo(u(4f), u(9f), u(10f), u(4f), u(20f), u(4f))
                cubicTo(u(20f), u(14f), u(15f), u(20f), u(4f), u(20f))
                close()
            }
            drawPath(path, c)
            line(Color.Black.copy(alpha = 0.22f), 1.4f, 5f, 19f, 15f, 9f)
        }
        Ico.BOX -> {
            fill(c, 3f, 7f, 12f, 3f, 21f, 7f, 21f, 17f, 12f, 21f, 3f, 17f)
            line(Color.Black.copy(alpha = 0.22f), 1.4f, 3f, 7f, 12f, 11f, 21f, 7f)
            line(Color.Black.copy(alpha = 0.22f), 1.4f, 12f, 11f, 12f, 21f)
        }
        Ico.CUP -> {
            rrect(c, 4f, 7f, 12f, 12f, 3f)
            arcLine(c, 16.5f, 12.5f, 3f, -90f, 180f, w)
            line(c, w * 0.8f, 8f, 2f, 8f, 4.5f)
            line(c, w * 0.8f, 12f, 2f, 12f, 4.5f)
        }
        Ico.GAMEPAD -> {
            rrect(c, 2f, 7f, 20f, 11f, 5.5f)
            val k = Color.Black.copy(alpha = 0.3f)
            line(k, 1.8f, 7f, 10f, 7f, 15f)
            line(k, 1.8f, 4.5f, 12.5f, 9.5f, 12.5f)
            dot(k, 16f, 11f, 1.3f)
            dot(k, 18.5f, 13.5f, 1.3f)
        }
        Ico.SHIRT -> fill(c, 8f, 3f, 12f, 5f, 16f, 3f, 22f, 7f, 19.5f, 11f, 17.5f, 10f, 17.5f, 21f, 6.5f, 21f, 6.5f, 10f, 4.5f, 11f, 2f, 7f)
        Ico.BED -> {
            rrect(c, 2f, 11f, 20f, 6f, 1.5f)
            rrect(c, 2f, 5f, 3f, 14f, 1f)
            rrect(c, 5.5f, 8.5f, 5f, 3f, 1.5f)
            line(c, w, 21f, 17f, 21f, 19.5f)
        }
        Ico.CACTUS -> {
            rrect(c, 9.5f, 3f, 5f, 16f, 2.5f)
            line(c, w * 1.4f, 9.5f, 12f, 6f, 12f, 6f, 8f)
            line(c, w * 1.4f, 14.5f, 14f, 18f, 14f, 18f, 9.5f)
            line(c, w, 6f, 21f, 18f, 21f)
        }
        Ico.PARTY -> {
            fill(c, 3f, 21f, 8f, 7f, 17f, 16f)
            dot(c, 15f, 4f, 1.4f); dot(c, 20f, 9f, 1.4f); dot(c, 19f, 3.5f, 1.1f)
            line(c, 1.6f, 12f, 6f, 13f, 3f)
            line(c, 1.6f, 18f, 12f, 21f, 11f)
        }
        Ico.DIAMOND -> fill(c, 12f, 2.5f, 21f, 12f, 12f, 21.5f, 3f, 12f)
        Ico.BLOCK -> {
            ring(c, 12f, 12f, 9f, w * 1.1f)
            line(c, w * 1.1f, 5.8f, 18.2f, 18.2f, 5.8f)
        }
        Ico.MUSIC -> {
            line(c, w, 9f, 18f, 9f, 5f, 20f, 3f, 20f, 16f)
            dot(c, 6.5f, 18f, 3f)
            dot(c, 17.5f, 16f, 3f)
        }
        Ico.TROPHY -> {
            fill(c, 6f, 3f, 18f, 3f, 17f, 11f, 12f, 14f, 7f, 11f)
            arcLine(c, 5.5f, 7f, 3f, 90f, 180f, w * 0.8f)
            arcLine(c, 18.5f, 7f, 3f, -90f, 180f, w * 0.8f)
            line(c, w, 12f, 14f, 12f, 18f)
            rrect(c, 7.5f, 18f, 9f, 3.5f, 1f)
        }
    }
}
