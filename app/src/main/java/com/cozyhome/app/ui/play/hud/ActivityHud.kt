package com.cozyhome.app.ui.play.hud

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.R
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.game.HudPanel
import com.cozyhome.app.render.rememberCropThumb
import com.cozyhome.app.render.rememberMeshThumb
import com.cozyhome.app.ui.components.ChunkyButton
import com.cozyhome.app.ui.components.CoinIcon
import com.cozyhome.app.ui.components.GameIcon
import com.cozyhome.app.ui.components.Ico
import com.cozyhome.app.ui.components.IconButton
import com.cozyhome.app.ui.components.Panel
import com.cozyhome.app.ui.components.Pill
import com.cozyhome.app.ui.components.ProgressBar
import com.cozyhome.app.ui.components.bouncyClick
import com.cozyhome.app.ui.components.outlined
import com.cozyhome.app.ui.text.bugName
import com.cozyhome.app.ui.text.cropName
import com.cozyhome.app.ui.text.dishName
import com.cozyhome.app.ui.text.festivalName
import com.cozyhome.app.ui.text.fishName
import com.cozyhome.app.ui.text.questText
import com.cozyhome.app.ui.theme.Bad
import com.cozyhome.app.ui.theme.Coral
import com.cozyhome.app.ui.theme.Cream
import com.cozyhome.app.ui.theme.Good
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.InkSoft
import com.cozyhome.app.ui.theme.Lilac
import com.cozyhome.app.ui.theme.Mint
import com.cozyhome.app.ui.theme.Peach
import com.cozyhome.app.ui.theme.Shadow
import com.cozyhome.app.ui.theme.Sky
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.SunDark
import com.cozyhome.app.ui.theme.White
import com.cozyhome.game.activities.BugDex
import com.cozyhome.game.activities.Cookbook
import com.cozyhome.game.activities.FishDex
import com.cozyhome.game.game.Emote
import com.cozyhome.game.garden.Crops
import com.cozyhome.game.models.ActivityModels
import com.cozyhome.game.models.DishModels
import com.cozyhome.game.progress.AlbumBook
import com.cozyhome.game.progress.Progression
import kotlinx.coroutines.delay

@Composable
fun Badge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier.size(18.dp).shadow(3.dp, CircleShape).background(Coral, CircleShape).border(1.5.dp, White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(if (count > 9) "9+" else "$count", color = White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
}

@Composable
fun ActivityMenu(vm: GameViewModel, modifier: Modifier) {
    val hud = vm.hud
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        MenuButton(Ico.QUEST, Sky, stringResource(R.string.menu_quests), hud.questsReady) { vm.openPanel(HudPanel.QUESTS) }
        MenuButton(Ico.BOOK, Lilac, stringResource(R.string.menu_album), hud.albumReady) { vm.openPanel(HudPanel.ALBUM) }
        MenuButton(Ico.CALENDAR, Coral, stringResource(R.string.menu_gift), if (hud.loginReady) 1 else 0) { vm.openPanel(HudPanel.LOGIN) }
        MenuButton(Ico.SMILE, Peach, stringResource(R.string.emotes), 0) { vm.openPanel(HudPanel.EMOTES) }
    }
}

@Composable
private fun MenuButton(ico: Ico, color: Color, label: String, badge: Int, onClick: () -> Unit) {
    Box {
        IconButton(ico, color, size = 42.dp, label = label, onClick = onClick)
        Badge(badge, Modifier.align(Alignment.TopEnd).padding(end = 0.dp))
    }
}

@Composable
fun FestivalChip(vm: GameViewModel, modifier: Modifier) {
    val hud = vm.hud
    val f = hud.festival
    if (f != null) {
        Pill(modifier, color = Sun.copy(alpha = 0.95f)) {
            GameIcon(Ico.PARTY, 15.dp, Coral)
            Text(stringResource(R.string.festival_now, stringResource(festivalName(f.name))), color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
            GameIcon(Ico.STAR, 13.dp, SunDark)
            Text("${vm.save.tokens}", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    } else hud.festivalNext?.let { (next, days) ->
        if (days <= 14) Pill(modifier) {
            GameIcon(Ico.PARTY, 14.dp, Lilac)
            Text(stringResource(R.string.festival_soon, stringResource(festivalName(next.name)), days), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
fun BoxScope.FishingHud(vm: GameViewModel) {
    val hud = vm.hud
    val phase = hud.fishPhase
    val text = when (phase) {
        0 -> stringResource(R.string.fishing_cast)
        1 -> stringResource(R.string.fishing_wait)
        2 -> stringResource(R.string.fishing_bite)
        3 -> stringResource(R.string.fishing_reel)
        else -> ""
    }
    if (text.isNotEmpty()) {
        val t = rememberInfiniteTransition(label = "bite")
        val pulse by t.animateFloat(1f, 1.12f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "p")
        com.cozyhome.app.ui.components.Bubble(
            text,
            Modifier.align(Alignment.TopCenter).padding(top = 44.dp).graphicsLayer { if (phase == 2) { scaleX = pulse; scaleY = pulse } },
            color = if (phase == 2) Sun else White,
        )
    }
    if (phase == 3) {
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 104.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TensionGauge(hud.fishTension, Modifier.width(30.dp).height(170.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).width(280.dp).height(16.dp)) {
            ProgressFromState(hud.fishProgress, Mint)
        }
    }
    Column(Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.CenterHorizontally) {
        if (phase == 3) {
            Box(
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            vm.setHold(e.changes.any { it.pressed })
                            e.changes.forEach { it.consume() }
                        }
                    }
                }
            ) { IconButton(Ico.ROTATE, Coral, size = 84.dp, label = stringResource(R.string.act_reel), sound = null) {} }
        } else {
            IconButton(Ico.FISH, if (phase == 2) Sun else Sky, size = 76.dp, label = stringResource(R.string.act_hook)) { vm.primary() }
        }
    }
    ChunkyButton(stringResource(R.string.act_stop), Coral, Modifier.align(Alignment.BottomStart), emoji = "✖️", height = 40.dp, fontSize = 13.sp) { vm.stopFishing() }
}

@Composable
private fun ProgressFromState(state: androidx.compose.runtime.MutableFloatState, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val v = state.floatValue.coerceIn(0f, 1f)
        val h = size.height
        drawRoundRect(Ink.copy(alpha = 0.35f), cornerRadius = CornerRadius(h / 2, h / 2))
        drawRoundRect(Brush.verticalGradient(listOf(color.copy(alpha = 0.9f), color)), size = Size((size.width * v).coerceAtLeast(h), h), cornerRadius = CornerRadius(h / 2, h / 2))
        drawRoundRect(White.copy(alpha = 0.4f), topLeft = Offset(h * 0.4f, h * 0.18f), size = Size(((size.width * v) - h * 0.8f).coerceAtLeast(0f), h * 0.22f), cornerRadius = CornerRadius(h * 0.1f, h * 0.1f))
    }
}

@Composable
private fun TensionGauge(state: androidx.compose.runtime.MutableFloatState, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Ink.copy(alpha = 0.5f), cornerRadius = CornerRadius(w / 2, w / 2))
        val inset = 4.dp.toPx()
        drawRoundRect(
            Brush.verticalGradient(listOf(Bad, Sun, Good, Good)),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            cornerRadius = CornerRadius(w / 2, w / 2),
            alpha = 0.85f,
        )
        val v = state.floatValue.coerceIn(0f, 1f)
        val y = inset + (h - inset * 2) * (1f - v)
        drawRoundRect(White, topLeft = Offset(0f, y - 5.dp.toPx()), size = Size(w, 10.dp.toPx()), cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()))
        drawRoundRect(Ink, topLeft = Offset(2.dp.toPx(), y - 3.dp.toPx()), size = Size(w - 4.dp.toPx(), 6.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))
    }
}

@Composable
private fun Stars(n: Int, size: Int = 14) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        for (k in 0 until 4) GameIcon(Ico.STAR, size.dp, if (k < n) Sun else Ink.copy(alpha = 0.15f))
    }
}

@Composable
fun CatchCards(vm: GameViewModel) {
    val hud = vm.hud
    val fish = hud.fishCard
    val bug = hud.bugCard
    val dish = hud.dishCard
    if (fish == null && bug == null && dish == null) return
    LaunchedEffect(fish, bug, dish) {
        delay(3600)
        vm.dismissCards()
    }
    Overlay({ vm.dismissCards() }) {
        Panel(Modifier.width(300.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    fish != null -> {
                        val sp = FishDex[fish.species]
                        Text(stringResource(R.string.caught_title), color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val img = rememberMeshThumb("fish-${fish.species}", 192, 90f, 12f) { ActivityModels.fish(fish.species) }
                        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) { img?.let { Image(it, null, Modifier.fillMaxSize()) } }
                        Text(stringResource(fishName(fish.species)), color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Stars(sp?.rarity?.stars ?: 1)
                        Text("${fish.cm} cm", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        if (fish.firstTime) Pill(color = Coral) { Text(stringResource(R.string.fish_new), color = White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                        else if (fish.record) Pill(color = Sun) { Text(stringResource(R.string.fish_record), color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                    }
                    bug != null -> {
                        val sp = BugDex[bug.species]
                        Text(stringResource(R.string.bug_title), color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val img = rememberMeshThumb("bug-${bug.species}", 192, -30f, 40f) { ActivityModels.bug(bug.species) }
                        Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) { img?.let { Image(it, null, Modifier.fillMaxSize()) } }
                        Text(stringResource(bugName(bug.species)), color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Stars(sp?.rarity?.stars ?: 1)
                        if (bug.firstTime) Pill(color = Coral) { Text(stringResource(R.string.fish_new), color = White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                    }
                    dish != null -> {
                        Text(stringResource(R.string.cooked_title), color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val img = rememberMeshThumb("dish-${dish.recipe}", 192, -20f, 35f) { DishModels.dish(dish.recipe) }
                        Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) { img?.let { Image(it, null, Modifier.fillMaxSize()) } }
                        Text(stringResource(dishName(dish.recipe)), color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        if (dish.firstTime) Pill(color = Coral) { Text(stringResource(R.string.fish_new), color = White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityPanels(vm: GameViewModel) {
    when (vm.hud.panel) {
        HudPanel.QUESTS -> QuestPanel(vm)
        HudPanel.ALBUM -> AlbumPanel(vm)
        HudPanel.LOGIN -> LoginPanel(vm)
        HudPanel.EMOTES -> EmotePanel(vm)
        HudPanel.COOK -> CookPanel(vm)
        null -> Unit
    }
}

@Composable
private fun PanelTitle(ico: Ico, color: Color, title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(30.dp).background(color, CircleShape), contentAlignment = Alignment.Center) { GameIcon(ico, 18.dp, White) }
        Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Box(Modifier.size(30.dp).bouncyClick(onClick = onClose).background(Ink.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) { GameIcon(Ico.CLOSE, 14.dp, InkSoft) }
    }
}

@Composable
private fun QuestPanel(vm: GameViewModel) {
    val quests = vm.save.daily.quests
    Overlay({ vm.openPanel(null) }) {
        Panel(Modifier.width(520.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelTitle(Ico.QUEST, Sky, stringResource(R.string.quests_title)) { vm.openPanel(null) }
                quests.forEachIndexed { i, q ->
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().background(if (q.done && !q.claimed) Sun.copy(alpha = 0.18f) else White, shape).border(1.5.dp, Ink.copy(alpha = 0.06f), shape).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(questText(q.kind), q.target), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProgressBar(q.progress / q.target.toFloat(), if (q.done) Good else Sky, Modifier.width(180.dp).height(9.dp))
                                Text("${q.progress}/${q.target}", color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            CoinIcon(14.dp)
                            Text("${q.coins}", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            if (q.tokens > 0) { GameIcon(Ico.STAR, 13.dp, SunDark); Text("${q.tokens}", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                        }
                        if (q.claimed) Text(stringResource(R.string.claimed), color = Good, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        else ChunkyButton(stringResource(R.string.claim), if (q.done) Mint else InkSoft, enabled = q.done, height = 34.dp, fontSize = 12.sp) { vm.claimQuest(i) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginPanel(vm: GameViewModel) {
    val d = vm.save.daily
    Overlay({ vm.openPanel(null) }) {
        Panel(Modifier.width(560.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PanelTitle(Ico.CALENDAR, Coral, stringResource(R.string.login_title)) { vm.openPanel(null) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Progression.LOGIN.forEachIndexed { i, r ->
                        val day = i + 1
                        val today = day == d.streak
                        val past = day < d.streak || (today && d.loginClaimed)
                        val shape = RoundedCornerShape(14.dp)
                        Column(
                            Modifier.width(68.dp)
                                .shadow(if (today) 6.dp else 1.dp, shape, ambientColor = Shadow, spotColor = Shadow)
                                .background(Brush.verticalGradient(if (today) listOf(Sun.copy(alpha = 0.5f), Sun.copy(alpha = 0.2f)) else listOf(White, Cream)), shape)
                                .border(if (today) 2.5.dp else 1.dp, if (today) Sun else Ink.copy(alpha = 0.08f), shape)
                                .padding(vertical = 6.dp)
                                .alpha(if (past && !today) 0.55f else 1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(stringResource(R.string.login_day, day), color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            if (r.item != null) GameIcon(Ico.TROPHY, 26.dp, SunDark) else CoinIcon(26.dp)
                            Text("${r.coins}", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            if (past) GameIcon(Ico.CHECK, 14.dp, Good)
                        }
                    }
                }
                if (!d.loginClaimed) ChunkyButton(stringResource(R.string.claim), Mint, emoji = "🎁", height = 42.dp) { vm.claimLogin(); vm.openPanel(null) }
                else Text(stringResource(R.string.login_hint), color = InkSoft, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmotePanel(vm: GameViewModel) {
    val names = mapOf(
        Emote.WAVE to R.string.emote_wave, Emote.CHEER to R.string.emote_cheer, Emote.DANCE to R.string.emote_dance, Emote.CLAP to R.string.emote_clap,
        Emote.LAUGH to R.string.emote_laugh, Emote.HEART to R.string.emote_heart, Emote.THINK to R.string.emote_think, Emote.SAD to R.string.emote_sad,
    )
    val icons = mapOf(
        Emote.WAVE to Ico.STAND, Emote.CHEER to Ico.PARTY, Emote.DANCE to Ico.MUSIC, Emote.CLAP to Ico.SPARKLE,
        Emote.LAUGH to Ico.SMILE, Emote.HEART to Ico.HEART, Emote.THINK to Ico.QUEST, Emote.SAD to Ico.WATER,
    )
    val colors = listOf(Sky, Sun, Lilac, Mint, Peach, Coral, Lilac, Sky)
    Overlay({ vm.openPanel(null) }) {
        Panel(Modifier.width(470.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PanelTitle(Ico.SMILE, Peach, stringResource(R.string.emotes)) { vm.openPanel(null) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Emote.entries.forEachIndexed { i, e ->
                        IconButton(icons.getValue(e), colors[i], size = 46.dp, label = stringResource(names.getValue(e))) { vm.emote(e) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookPanel(vm: GameViewModel) {
    val pantry = vm.save.pantry
    val level = vm.level
    Overlay({ vm.openPanel(null) }) {
        Panel(Modifier.width(620.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelTitle(Ico.POT, Coral, stringResource(R.string.cook_title)) { vm.openPanel(null) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.pantry_title), color = InkSoft, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    if (pantry.isEmpty()) Text(stringResource(R.string.pantry_empty), color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    for ((id, n) in pantry) {
                        val img = rememberCropThumb(id, 96)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            img?.let { Image(it, null, Modifier.size(22.dp)) }
                            Text("×$n", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
                LazyVerticalGrid(GridCells.Fixed(4), Modifier.heightIn(max = 210.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Cookbook.all, key = { it.id }) { r ->
                        val can = com.cozyhome.game.activities.Cookbook.canCook(r, pantry) && level >= r.level
                        val shape = RoundedCornerShape(16.dp)
                        Column(
                            Modifier.background(Brush.verticalGradient(listOf(White, Cream)), shape).border(1.5.dp, if (can) Mint else Ink.copy(alpha = 0.08f), shape).padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            val img = rememberMeshThumb("dish-${r.id}", 144, -20f, 35f) { DishModels.dish(r.id) }
                            Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) { img?.let { Image(it, null, Modifier.fillMaxSize()) } }
                            Text(stringResource(dishName(r.id)), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                for ((id, n) in r.needs) {
                                    val ok = (pantry[id] ?: 0) >= n
                                    val ci = rememberCropThumb(id, 96)
                                    ci?.let { Image(it, null, Modifier.size(16.dp)) }
                                    Text("$n", color = if (ok) Good else Bad, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                }
                            }
                            if (level < r.level) {
                                Row(
                                    Modifier.height(30.dp).background(Ink.copy(alpha = 0.07f), RoundedCornerShape(15.dp)).padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    GameIcon(Ico.LOCK, 13.dp, InkSoft)
                                    Text(stringResource(R.string.level_short, r.level), color = InkSoft, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            } else {
                                ChunkyButton(stringResource(R.string.cook_btn), if (can) Coral else InkSoft, enabled = can, height = 30.dp, fontSize = 11.sp) { vm.cook(r.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumPanel(vm: GameViewModel) {
    var tab by remember { mutableStateOf(AlbumBook.FISH) }
    val album = vm.save.album
    Overlay({ vm.openPanel(null) }) {
        Panel(Modifier.width(640.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelTitle(Ico.BOOK, Lilac, stringResource(R.string.album_title)) { vm.openPanel(null) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (b in AlbumBook.entries) {
                        val label = when (b) {
                            AlbumBook.FISH -> R.string.album_fish
                            AlbumBook.BUGS -> R.string.album_bugs
                            AlbumBook.DISHES -> R.string.album_dishes
                            AlbumBook.CROPS -> R.string.album_crops
                        }
                        com.cozyhome.app.ui.components.Chip("${stringResource(label)} ${Progression.found(album, b)}/${b.total()}", b == tab) { tab = b }
                    }
                }
                Progression.unclaimedMilestones(album).firstOrNull()?.let { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameIcon(Ico.TROPHY, 18.dp, SunDark)
                        CoinIcon(14.dp)
                        Text("${m.coins}", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        ChunkyButton(stringResource(R.string.milestone_btn), Sun, textColor = Ink, height = 32.dp, fontSize = 12.sp) { vm.claimMilestone(m.key) }
                    }
                }
                val entries: List<Pair<String, Boolean>> = when (tab) {
                    AlbumBook.FISH -> FishDex.all.map { it.id to (it.id in album.fish) }
                    AlbumBook.BUGS -> BugDex.all.map { it.id to (it.id in album.bugs) }
                    AlbumBook.DISHES -> Cookbook.all.map { it.id to (it.id in album.dishes) }
                    AlbumBook.CROPS -> Crops.all.map { it.id to (it.id in album.crops) }
                }
                LazyVerticalGrid(GridCells.Fixed(6), Modifier.heightIn(max = 200.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(entries, key = { it.first }) { (id, found) ->
                        val shape = RoundedCornerShape(14.dp)
                        Column(
                            Modifier.background(Brush.verticalGradient(listOf(White, Cream)), shape).border(1.5.dp, if (found) Lilac.copy(alpha = 0.5f) else Ink.copy(alpha = 0.06f), shape).padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val img = when (tab) {
                                AlbumBook.FISH -> rememberMeshThumb("fish-$id", 144, 90f, 12f) { ActivityModels.fish(id) }
                                AlbumBook.BUGS -> rememberMeshThumb("bug-$id", 144, -30f, 40f) { ActivityModels.bug(id) }
                                AlbumBook.DISHES -> rememberMeshThumb("dish-$id", 144, -20f, 35f) { DishModels.dish(id) }
                                AlbumBook.CROPS -> rememberCropThumb(id, 144)
                            }
                            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                                img?.let { Image(it, null, Modifier.fillMaxSize(), colorFilter = if (found) null else ColorFilter.tint(Ink.copy(alpha = 0.75f))) }
                            }
                            val name = when (tab) {
                                AlbumBook.FISH -> stringResource(fishName(id))
                                AlbumBook.BUGS -> stringResource(bugName(id))
                                AlbumBook.DISHES -> stringResource(dishName(id))
                                AlbumBook.CROPS -> stringResource(cropName(id))
                            }
                            Text(if (found) name else "???", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, maxLines = 1)
                            if (tab == AlbumBook.FISH) {
                                val best = album.fish[id]?.takeIf { found }?.let { stringResource(R.string.best_size, it.bestCm) }
                                Text(best ?: " ", color = InkSoft, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            if (tab == AlbumBook.FISH) FishDex[id]?.let { Stars(it.rarity.stars, 9) }
                            if (tab == AlbumBook.BUGS) BugDex[id]?.let { Stars(it.rarity.stars, 9) }
                        }
                    }
                }
            }
        }
    }
}

