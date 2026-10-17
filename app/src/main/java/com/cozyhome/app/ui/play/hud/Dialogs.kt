package com.cozyhome.app.ui.play.hud

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.BuildConfig
import com.cozyhome.app.R
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.render.rememberCropThumb
import com.cozyhome.app.ui.components.ChunkyButton
import com.cozyhome.app.ui.components.CoinIcon
import com.cozyhome.app.ui.components.Panel
import com.cozyhome.app.ui.components.bouncyClick
import com.cozyhome.app.ui.text.cropName
import com.cozyhome.app.ui.theme.Bad
import com.cozyhome.app.ui.theme.Coral
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.InkSoft
import com.cozyhome.app.ui.theme.Lilac
import com.cozyhome.app.ui.theme.Mint
import com.cozyhome.app.ui.theme.MintLight
import com.cozyhome.app.ui.theme.Scrim
import com.cozyhome.app.ui.theme.Sky
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.SunLight
import com.cozyhome.app.ui.theme.White
import com.cozyhome.game.garden.Crops
import com.cozyhome.game.sports.SportKind
import com.cozyhome.game.game.WorldEvent
import com.cozyhome.game.state.Rules

@Composable
fun Overlay(onDismiss: (() -> Unit)?, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val s by animateFloatAsState(if (shown) 1f else 0.85f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "dlg")
    Box(
        Modifier
            .fillMaxSize()
            .background(Scrim)
            .clickable(remember { MutableInteractionSource() }, null) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .graphicsLayer { scaleX = s; scaleY = s; alpha = s.coerceIn(0f, 1f) }
                .clickable(remember { MutableInteractionSource() }, null) {},
        ) { content() }
    }
}

private fun formatSeconds(s: Int): String = if (s >= 60) "${s / 60}m${if (s % 60 > 0) "${s % 60}s" else ""}" else "${s}s"

@Composable
fun SeedPicker(vm: GameViewModel) {
    val level = Rules.level(vm.save.xp)
    Overlay({ vm.dismissSeeds() }) {
        Panel(Modifier.width(600.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.seeds_title), color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Crops.all, key = { it.id }) { crop ->
                        val locked = level < crop.level
                        val affordable = vm.save.coins >= crop.seedPrice
                        val thumb = rememberCropThumb(crop.id)
                        val shape = RoundedCornerShape(18.dp)
                        Column(
                            Modifier
                                .width(80.dp)
                                .bouncyClick(enabled = !locked && affordable) { vm.plant(crop.id) }
                                .background(Brush.verticalGradient(listOf(White, MintLight.copy(alpha = 0.6f))), shape)
                                .border(2.dp, if (!locked && affordable) Mint else Ink.copy(alpha = 0.1f), shape)
                                .padding(5.dp)
                                .alpha(if (locked || !affordable) 0.55f else 1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                thumb?.let { Image(it, null, Modifier.size(40.dp)) }
                                if (locked) com.cozyhome.app.ui.components.GameIcon(com.cozyhome.app.ui.components.Ico.LOCK, 24.dp, Ink.copy(alpha = 0.7f))
                            }
                            Text(stringResource(cropName(crop.id)), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                            if (locked) {
                                Text(stringResource(R.string.locked_level, crop.level), color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    CoinIcon(13.dp)
                                    Text("${crop.seedPrice} → ${crop.sellPrice}", color = if (affordable) Ink else Bad, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                                Text(stringResource(R.string.seed_time, formatSeconds(crop.stageSeconds * 3)), color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                ChunkyButton(stringResource(R.string.close), Coral, height = 40.dp, fontSize = 13.sp) { vm.dismissSeeds() }
            }
        }
    }
}

@Composable
fun ResultDialog(vm: GameViewModel, r: WorldEvent.SportResult) {
    Overlay(null) {
        Panel(Modifier.width(420.dp), color = SunLight) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val (emoji, title) = when (r.kind) {
                    SportKind.BASKETBALL -> "🏀" to stringResource(R.string.result_basket)
                    SportKind.SOCCER -> "⚽" to stringResource(R.string.result_soccer)
                    SportKind.JOGGING -> "🏃" to stringResource(R.string.result_jog)
                }
                Text(emoji, fontSize = 48.sp)
                Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                when (r.kind) {
                    SportKind.BASKETBALL -> Text(stringResource(R.string.result_points, r.score, r.detail), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    SportKind.SOCCER -> Text(stringResource(R.string.result_goals, r.score, r.detail), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    SportKind.JOGGING -> {
                        val medal = when (r.score) {
                            3 -> "🥇 " + stringResource(R.string.medal_3)
                            2 -> "🥈 " + stringResource(R.string.medal_2)
                            1 -> "🥉 " + stringResource(R.string.medal_1)
                            else -> stringResource(R.string.medal_0)
                        }
                        Text(medal, color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(stringResource(R.string.result_time, "%.1fs".format(r.millis / 1000f)), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
                if (r.best) Text("⭐ " + stringResource(R.string.new_best), color = Coral, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoinIcon(24.dp)
                    Text(stringResource(R.string.coins_earned, r.coins), color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChunkyButton(stringResource(R.string.play_again), Sky, emoji = "🔁") { vm.replaySport() }
                    ChunkyButton(stringResource(R.string.done), Mint, emoji = "✅") { vm.quitSport() }
                }
            }
        }
    }
}

@Composable
fun LevelUpDialog(level: Int, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        Panel(Modifier.width(380.dp), color = MintLight) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.cozyhome.app.ui.components.GameIcon(com.cozyhome.app.ui.components.Ico.PARTY, 56.dp, Coral)
                Text(stringResource(R.string.level_up_title, level), color = Ink, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text(stringResource(R.string.level_up_body), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                ChunkyButton(stringResource(R.string.ok), Sun, textColor = Ink, emoji = "✨") { onDismiss() }
            }
        }
    }
}

@Composable
fun SettingsDialog(vm: GameViewModel, onDismiss: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    val audio = vm.audio
    Overlay(onDismiss) {
        Panel(Modifier.width(520.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings), color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                val st = vm.save.stats
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.stats_title), color = Lilac, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(
                        stringResource(R.string.stats_line, st.harvested, st.basketBest, st.soccerBest, if (st.jogBestMs > 0) "%.1fs".format(st.jogBestMs / 1000f) else "—"),
                        color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center,
                    )
                }
                if (!confirmReset) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChunkyButton(stringResource(R.string.settings_look), Sky, emoji = "👕") { onDismiss(); vm.editLook() }
                        ChunkyButton(stringResource(R.string.settings_reset), Coral, emoji = "🗑️") { confirmReset = true }
                    }
                } else {
                    Text(stringResource(R.string.settings_reset_confirm), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChunkyButton(stringResource(R.string.cancel), InkSoft) { confirmReset = false }
                        ChunkyButton(stringResource(R.string.settings_reset_yes), Bad) { onDismiss(); vm.resetGame() }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val sound = stringResource(if (audio.soundOn) R.string.on else R.string.off)
                    val music = stringResource(if (audio.musicOn) R.string.on else R.string.off)
                    ChunkyButton(stringResource(R.string.settings_sound, sound), if (audio.soundOn) Sky else InkSoft, emoji = if (audio.soundOn) "🔊" else "🔇", height = 40.dp, fontSize = 13.sp) { audio.toggleSound() }
                    ChunkyButton(stringResource(R.string.settings_music, music), if (audio.musicOn) Lilac else InkSoft, emoji = "🎵", height = 40.dp, fontSize = 13.sp) { audio.toggleMusic() }
                    ChunkyButton(stringResource(R.string.close), Mint, height = 40.dp, fontSize = 13.sp) { onDismiss() }
                }
                Text(stringResource(R.string.settings_about, BuildConfig.VERSION_NAME), color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
