package com.cozyhome.app.ui.play.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.R
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.game.HudState
import com.cozyhome.app.ui.components.Bubble
import com.cozyhome.app.ui.components.ChunkyButton
import com.cozyhome.app.ui.components.CoinIcon
import com.cozyhome.app.ui.components.Pill
import com.cozyhome.app.ui.components.RoundButton
import com.cozyhome.app.ui.text.actionEmoji
import com.cozyhome.app.ui.text.interactionLabel
import com.cozyhome.app.ui.text.msgText
import com.cozyhome.app.ui.theme.Bad
import com.cozyhome.app.ui.theme.Coral
import com.cozyhome.app.ui.theme.Good
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.InkSoft
import com.cozyhome.app.ui.theme.Lilac
import com.cozyhome.app.ui.theme.Mint
import com.cozyhome.app.ui.theme.MintLight
import com.cozyhome.app.ui.theme.Peach
import com.cozyhome.app.ui.theme.Sky
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.SunDark
import com.cozyhome.app.ui.theme.White
import com.cozyhome.game.game.ActionKind
import com.cozyhome.game.game.Mode
import com.cozyhome.game.sports.JogRace
import com.cozyhome.game.state.Rules

@Composable
fun PlayHud(vm: GameViewModel) {
    val hud = vm.hud
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(hud.mode) {
        vm.input.stickX = 0f
        vm.input.stickY = 0f
        vm.input.sprint = false
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val sport = hud.mode == Mode.BASKETBALL || hud.mode == Mode.SOCCER || hud.mode == Mode.JOG
        if (hud.mode != Mode.DECOR) {
            if (!sport) ProfileChip(vm, Modifier.align(Alignment.TopStart))
            TopRight(vm, Modifier.align(Alignment.TopEnd)) { showSettings = true }
            ClockChip(hud, Modifier.align(Alignment.TopCenter))
        }
        when (hud.mode) {
            Mode.BASKETBALL -> BasketHud(vm)
            Mode.SOCCER -> SoccerHud(vm)
            Mode.JOG -> JogHud(vm)
            Mode.BOUNCE -> ToyHud(stringResource(R.string.bounce_combo, hud.bounceCombo), stringResource(R.string.act_jump), "🤸", Coral, vm)
            Mode.SWING -> ToyHud(stringResource(R.string.swing_hint), stringResource(R.string.act_push), "🎈", Sky, vm)
            Mode.DECOR -> DecorHud(vm)
            else -> FreeActions(vm)
        }
        Toasts(hud, Modifier.align(Alignment.TopCenter).padding(top = 44.dp))
        RewardPops(hud, Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 52.dp))
    }
    hud.seedPlot?.let { SeedPicker(vm) }
    hud.result?.let { ResultDialog(vm, it) }
    hud.levelUp?.let { LevelUpDialog(it) { vm.dismissLevelUp() } }
    if (showSettings) SettingsDialog(vm) { showSettings = false }
}

@Composable
private fun ProfileChip(vm: GameViewModel, modifier: Modifier) {
    val save = vm.save
    val level = Rules.level(save.xp)
    val progress by animateFloatAsState(Rules.levelProgress(save.xp), tween(600), label = "xp")
    Pill(modifier) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier.size(32.dp)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Mint.copy(red = 0.45f), Mint)), CircleShape)
                    .border(2.dp, White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(save.name.take(1).uppercase(), style = com.cozyhome.app.ui.components.outlined(15.sp))
            }
            Box(Modifier.size(15.dp).background(Lilac, CircleShape).border(1.5.dp, White, CircleShape), contentAlignment = Alignment.Center) {
                Text("$level", color = White, fontWeight = FontWeight.Black, fontSize = 8.sp)
            }
        }
        Column {
            Text(save.name, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.level_short, level), color = Lilac, fontWeight = FontWeight.Black, fontSize = 11.sp)
                com.cozyhome.app.ui.components.ProgressBar(progress, Lilac, Modifier.width(60.dp).height(7.dp))
            }
        }
    }
}

@Composable
private fun TopRight(vm: GameViewModel, modifier: Modifier, onSettings: () -> Unit) {
    val coins by animateFloatAsState(vm.save.coins.toFloat(), tween(500), label = "coins")
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pill {
            CoinIcon(18.dp)
            Text(coins.toInt().toString(), color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        com.cozyhome.app.ui.components.IconButton(com.cozyhome.app.ui.components.Ico.GEAR, InkSoft, size = 38.dp, onClick = onSettings)
    }
}

@Composable
private fun ClockChip(hud: HudState, modifier: Modifier) {
    val h = hud.hourQuarter / 4
    val m = (hud.hourQuarter % 4) * 15
    val icon = when {
        h in 6..17 -> com.cozyhome.app.ui.components.Ico.SUN
        h in 18..19 -> com.cozyhome.app.ui.components.Ico.SUNSET
        else -> com.cozyhome.app.ui.components.Ico.MOON
    }
    val tint = when (icon) {
        com.cozyhome.app.ui.components.Ico.SUN -> SunDark
        com.cozyhome.app.ui.components.Ico.SUNSET -> Coral
        else -> Lilac
    }
    if (hud.mode == Mode.FREE || hud.mode == Mode.ACTION || hud.mode == Mode.SIT || hud.mode == Mode.SLEEP) {
        Pill(modifier) {
            com.cozyhome.app.ui.components.GameIcon(icon, 17.dp, tint)
            Text("%02d:%02d".format(h, m), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BoxScope.FreeActions(vm: GameViewModel) {
    val hud = vm.hud
    val i = hud.interaction
    Column(
        Modifier.align(Alignment.BottomEnd),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (hud.mode == Mode.FREE) {
            RoundButton("🛋️", Peach, size = 46.dp, label = stringResource(R.string.decorate)) { vm.enterDecor() }
        }
        when (hud.mode) {
            Mode.SIT -> BigAction("🧍", stringResource(R.string.act_stand), Sky) { vm.primary() }
            Mode.SLEEP -> BigAction("☀️", stringResource(R.string.act_wake), Sun) { vm.primary() }
            else -> AnimatedVisibility(i != null, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                val last = remember { mutableStateOf(i) }
                if (i != null) last.value = i
                last.value?.let { cur ->
                    val color = when (cur.kind) {
                        ActionKind.HARVEST, ActionKind.GIFT -> Sun
                        ActionKind.WATER, ActionKind.WATER_ALL -> Sky
                        ActionKind.PLANT -> Mint
                        ActionKind.GROWING -> InkSoft
                        ActionKind.BASKETBALL, ActionKind.SOCCER, ActionKind.JOG -> Coral
                        else -> Lilac
                    }
                    BigAction(actionEmoji(cur.kind), interactionLabel(cur), color) { vm.primary() }
                }
            }
        }
    }
}

@Composable
fun BigAction(emoji: String, label: String, color: Color, onClick: () -> Unit) {
    RoundButton(emoji, color, size = 70.dp, label = label, onClick = onClick)
}

@Composable
private fun Toasts(hud: HudState, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (t in hud.toasts) {
            key(t.id) {
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                AnimatedVisibility(shown, enter = slideInVertically { -it } + fadeIn()) {
                    Bubble(msgText(t.msg, t.arg))
                }
            }
        }
    }
}

@Composable
private fun RewardPops(hud: HudState, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        for (r in hud.rewards) {
            key(r.id) {
                var go by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { go = true }
                val t by animateFloatAsState(if (go) 1f else 0f, tween(1300), label = "pop")
                Row(
                    Modifier.graphicsLayer {
                        translationY = -t * 18.dp.toPx()
                        alpha = (1f - t * t).coerceIn(0f, 1f)
                        scaleX = 0.8f + 0.4f * kotlin.math.min(1f, t * 4f)
                        scaleY = scaleX
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (r.coins != 0) {
                        Text((if (r.coins > 0) "+" else "") + r.coins, color = if (r.coins > 0) SunDark else Bad, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        CoinIcon(18.dp)
                    }
                    if (r.xp > 0) Text("+${r.xp} XP", color = Lilac, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ExitButton(vm: GameViewModel) {
    ChunkyButton(stringResource(R.string.exit), Coral, Modifier.align(Alignment.BottomStart), emoji = "✖️", height = 42.dp, fontSize = 13.sp) { vm.quitSport() }
}

@Composable
private fun BoxScope.BasketHud(vm: GameViewModel) {
    val hud = vm.hud
    val game = vm.world.basketball
    Column(Modifier.align(Alignment.TopStart), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill {
            com.cozyhome.app.ui.components.GameIcon(com.cozyhome.app.ui.components.Ico.BASKETBALL, 17.dp, Coral)
            Text(stringResource(R.string.basket_hud, (hud.basketShot + 1).coerceAtMost(game?.totalShots ?: 10), game?.totalShots ?: 10, hud.basketScore), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        if (hud.basketStreak >= 2) Pill(color = Sun) { Text(stringResource(R.string.streak, hud.basketStreak), color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp) }
    }
    PowerMeter(hud, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
    if (hud.basketAiming) {
        Bubble(stringResource(R.string.basket_hint), Modifier.align(Alignment.BottomCenter).alpha(0.92f))
    }
    Box(Modifier.align(Alignment.BottomEnd).padding(end = 52.dp)) {
        BigAction("🏀", stringResource(R.string.act_shoot), Coral) { vm.primary() }
    }
    ExitButton(vm)
}

@Composable
private fun PowerMeter(hud: HudState, modifier: Modifier) {
    val meter = hud.basketMeter
    val zone = hud.basketZone
    Canvas(modifier.width(34.dp).height(220.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(Ink.copy(alpha = 0.55f), size = size, cornerRadius = CornerRadius(w / 2, w / 2))
        val inset = 5.dp.toPx()
        val inner = Size(w - inset * 2, h - inset * 2)
        drawRoundRect(White.copy(alpha = 0.85f), topLeft = Offset(inset, inset), size = inner, cornerRadius = CornerRadius(inner.width / 2, inner.width / 2))
        val center = com.cozyhome.game.sports.BasketballGame.ZONE_CENTER
        val ok = 0.2f
        fun yOf(v: Float) = inset + inner.height * (1f - v)
        drawRect(Sun.copy(alpha = 0.8f), topLeft = Offset(inset, yOf(center + ok)), size = Size(inner.width, inner.height * ok * 2))
        drawRect(Good, topLeft = Offset(inset, yOf(center + zone.floatValue)), size = Size(inner.width, inner.height * zone.floatValue * 2))
        val y = yOf(meter.floatValue)
        drawRoundRect(Ink, topLeft = Offset(0f, y - 4.dp.toPx()), size = Size(w, 8.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
    }
}

@Composable
private fun BoxScope.SoccerHud(vm: GameViewModel) {
    val hud = vm.hud
    val game = vm.world.soccer
    Column(Modifier.align(Alignment.TopStart), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill {
            com.cozyhome.app.ui.components.GameIcon(com.cozyhome.app.ui.components.Ico.SOCCER, 17.dp, Ink)
            Text(stringResource(R.string.soccer_hud, hud.soccerGoals, game?.totalKicks ?: 5), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            for (k in 0 until (game?.totalKicks ?: 5)) {
                val r = hud.soccerResults.getOrNull(k)
                Box(Modifier.size(14.dp).background(when (r) { true -> Good; false -> Bad; null -> Ink.copy(alpha = 0.15f) }, CircleShape))
            }
        }
    }
    if (hud.soccerAiming && hud.soccerKicks == 0) Bubble(stringResource(R.string.soccer_hint), Modifier.align(Alignment.TopCenter).padding(top = 44.dp))
    ExitButton(vm)
}

@Composable
private fun BoxScope.JogHud(vm: GameViewModel) {
    val hud = vm.hud
    val race = vm.world.jog
    val laps = race?.laps ?: 3
    Column(Modifier.align(Alignment.TopStart), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill {
            com.cozyhome.app.ui.components.GameIcon(com.cozyhome.app.ui.components.Ico.HOURGLASS, 17.dp, Coral)
            val tenths = hud.jogTenths
            Text("%02d:%02d.%d".format(tenths / 600, (tenths / 10) % 60, tenths % 10), color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(stringResource(R.string.jog_lap, hud.jogLap, laps), color = Coral, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        if (hud.jogRunning && !hud.jogOnTrack) Bubble(stringResource(R.string.off_track_hint), color = Sun)
    }
    if (hud.jogCountdown > 0 || (hud.jogRunning && hud.jogTenths < 8)) {
        val text = if (hud.jogCountdown > 0) hud.jogCountdown.toString() else stringResource(R.string.jog_go)
        key(text) {
            var go by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { go = true }
            val s by animateFloatAsState(if (go) 1f else 2.2f, tween(350), label = "count")
            Text(text, color = White, fontSize = 96.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center).graphicsLayer { scaleX = s; scaleY = s; shadowElevation = 12f })
        }
    } else if (race?.phase == JogRace.Phase.RUN && hud.jogTenths < 60) {
        Bubble(stringResource(R.string.jog_hint), Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
    }
    val stamina = hud.jogStamina
    Column(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.width(90.dp).height(12.dp)) {
            drawRoundRect(Ink.copy(alpha = 0.4f), cornerRadius = CornerRadius(size.height / 2, size.height / 2))
            drawRoundRect(Sky, size = Size(size.width * stamina.floatValue, size.height), cornerRadius = CornerRadius(size.height / 2, size.height / 2))
        }
        Box(
            Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        vm.input.sprint = e.changes.any { it.pressed }
                        e.changes.forEach { it.consume() }
                    }
                }
            }
        ) {
            RoundButton("⚡", Sun, size = 84.dp, label = stringResource(R.string.act_sprint)) {}
        }
    }
    ExitButtonTop(vm)
}

@Composable
private fun BoxScope.ExitButtonTop(vm: GameViewModel) {
    ChunkyButton(stringResource(R.string.exit), Coral, Modifier.align(Alignment.TopEnd).padding(top = 48.dp), emoji = "✖️", height = 40.dp, fontSize = 13.sp) { vm.quitSport() }
}

@Composable
private fun BoxScope.ToyHud(hint: String, action: String, emoji: String, color: Color, vm: GameViewModel) {
    Bubble(hint, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
    Column(Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RoundButton("🧍", MintLight, size = 54.dp, label = stringResource(R.string.act_stand)) { vm.world.standUp() }
        BigAction(emoji, action, color) { vm.primary() }
    }
    Spacer(Modifier.height(0.dp))
}
