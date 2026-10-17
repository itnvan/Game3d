package com.cozyhome.app.ui.play.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.R
import com.cozyhome.app.game.DecorPanel
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.render.rememberItemThumb
import com.cozyhome.app.ui.components.Bubble
import com.cozyhome.app.ui.components.ChunkyButton
import com.cozyhome.app.ui.components.Chip
import com.cozyhome.app.ui.components.CoinIcon
import com.cozyhome.app.ui.components.Pill
import com.cozyhome.app.ui.components.RoundButton
import com.cozyhome.app.ui.components.bouncyClick
import com.cozyhome.app.ui.text.categoryEmoji
import com.cozyhome.app.ui.text.categoryName
import com.cozyhome.app.ui.text.itemName
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
import com.cozyhome.app.ui.theme.SkyLight
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.White
import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.decor.Category
import com.cozyhome.game.decor.ItemDef
import com.cozyhome.game.decor.ZoneRule
import com.cozyhome.game.models.Palette
import com.cozyhome.game.state.Rules
import com.cozyhome.game.state.Zone

@Composable
fun BoxScope.DecorHud(vm: GameViewModel) {
    val hud = vm.hud
    val zone = hud.decorZone
    val editing = hud.decorEdit != null
    Row(Modifier.align(Alignment.TopStart).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(stringResource(R.string.zone_house), zone == Zone.HOUSE, emoji = "🏠") { vm.switchDecorZone(Zone.HOUSE) }
        Chip(stringResource(R.string.zone_yard), zone == Zone.YARD, emoji = "🌳") { vm.switchDecorZone(Zone.YARD) }
        if (zone == Zone.HOUSE && !editing) {
            Chip(stringResource(R.string.paint), hud.decorPanel == DecorPanel.PAINT, emoji = "🎨") {
                hud.decorPanel = if (hud.decorPanel == DecorPanel.PAINT) DecorPanel.CATALOG else DecorPanel.PAINT
            }
        }
        Box(Modifier.weight(1f))
        Pill {
            CoinIcon(16.dp)
            Text(vm.save.coins.toString(), color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        ChunkyButton(stringResource(R.string.done), Mint, emoji = "✅", height = 36.dp, fontSize = 13.sp) { vm.exitDecor() }
    }
    var hintVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(6000)
        hintVisible = false
    }
    if (hintVisible && !editing) {
        Bubble(stringResource(R.string.decor_hint), Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.42f).alpha(0.92f))
    }
    AnimatedVisibility(
        !editing,
        modifier = Modifier.align(Alignment.BottomStart).padding(top = 44.dp),
        enter = slideInHorizontally { -it } + fadeIn(),
        exit = slideOutHorizontally { -it } + fadeOut(),
    ) {
        if (hud.decorPanel == DecorPanel.PAINT && zone == Zone.HOUSE) PaintPanel(vm) else CatalogPanel(vm, zone)
    }
    AnimatedVisibility(
        editing,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        EditTools(vm)
    }
}

@Composable
private fun EditTools(vm: GameViewModel) {
    val edit = vm.hud.decorEdit
    val def = edit?.let { Catalog[it.item.itemId] }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (edit != null && def != null) {
            Pill {
                Text(stringResource(itemName(def.id)), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                if (edit.isNew && edit.fromShop && def.tokenPrice > 0) {
                    com.cozyhome.app.ui.components.GameIcon(com.cozyhome.app.ui.components.Ico.STAR, 14.dp, com.cozyhome.app.ui.theme.SunDark)
                    Text(def.tokenPrice.toString(), color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                } else if (edit.isNew && edit.fromShop) {
                    CoinIcon(14.dp)
                    Text(def.price.toString(), color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                if (!edit.valid) Text("⛔", fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            RoundButton("🔄", Sky, size = 46.dp, label = stringResource(R.string.rotate)) { vm.rotateEdit() }
            if ((def?.variants ?: 1) > 1) RoundButton("🎨", Lilac, size = 46.dp, label = stringResource(R.string.recolor)) { vm.recolorEdit() }
            if (edit?.isNew == false) RoundButton("📦", Peach, size = 46.dp, label = stringResource(R.string.store)) { vm.storeEdit() }
            RoundButton("✖️", Coral, size = 46.dp, label = stringResource(R.string.cancel)) { vm.cancelEdit() }
            RoundButton("✔️", if (edit?.valid == true) Good else Bad.copy(alpha = 0.7f), size = 56.dp, label = stringResource(R.string.place)) { vm.confirmEdit() }
        }
    }
}

private val SIDE_PANEL = 214.dp

@Composable
private fun CatalogPanel(vm: GameViewModel, zone: Zone) {
    val rule = if (zone == Zone.HOUSE) ZoneRule.HOUSE else ZoneRule.YARD
    val festival = vm.hud.festival
    val inventory = vm.save.inventory
    val categories = remember(rule, festival, inventory) {
        Catalog.categoriesFor(rule).filter { c ->
            when (c) {
                Category.FESTIVAL -> festival != null && Catalog.items.any { it.zone == rule && it.festival == festival }
                Category.SPECIAL -> Catalog.items.any { it.zone == rule && it.reward && (inventory[it.id] ?: 0) > 0 }
                else -> true
            }
        }.sortedBy { if (it == Category.FESTIVAL) -1 else it.ordinal }
    }
    var selected by rememberSaveable(rule) { mutableStateOf(categories.first()) }
    if (selected !in categories) selected = categories.first()
    val items = remember(rule, selected, festival, inventory) {
        Catalog.items.filter { def ->
            def.zone == rule && def.category == selected && when (def.category) {
                Category.FESTIVAL -> def.festival == festival || (inventory[def.id] ?: 0) > 0
                Category.SPECIAL -> (inventory[def.id] ?: 0) > 0
                else -> true
            }
        }
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .width(SIDE_PANEL)
            .fillMaxHeight()
            .shadow(10.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .background(Cream.copy(alpha = 0.97f), shape)
            .border(2.dp, White, shape)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (categories.size > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in categories) Chip(if (c == selected) stringResource(categoryName(c)) else "", c == selected, emoji = categoryEmoji(c)) { selected = c }
            }
        } else {
            Text(stringResource(categoryName(selected)), color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items, key = { it.id }) { def -> ItemCard(vm, def) }
        }
    }
}

@Composable
private fun ItemCard(vm: GameViewModel, def: ItemDef) {
    val owned = vm.save.inventory[def.id] ?: 0
    val locked = Rules.level(vm.save.xp) < def.level
    val thumb = rememberItemThumb(def.id)
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .bouncyClick { vm.pickItem(def) }
            .background(Brush.verticalGradient(listOf(White, SkyLight.copy(alpha = 0.5f))), shape)
            .border(2.dp, if (owned > 0) Mint else Ink.copy(alpha = 0.08f), shape)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            if (thumb != null) Image(thumb, null, Modifier.size(52.dp).alpha(if (locked) 0.4f else 1f))
            else Box(Modifier.size(36.dp).background(Ink.copy(alpha = 0.06f), CircleShape))
            if (locked) Text("🔒", fontSize = 22.sp)
        }
        Text(stringResource(itemName(def.id)), color = Ink, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        when {
            locked -> Text(stringResource(R.string.locked_level, def.level), color = InkSoft, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            owned > 0 -> Text(stringResource(R.string.owned_count, owned), color = Mint, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            def.tokenPrice > 0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                com.cozyhome.app.ui.components.GameIcon(com.cozyhome.app.ui.components.Ico.STAR, 12.dp, com.cozyhome.app.ui.theme.SunDark)
                Text(def.tokenPrice.toString(), color = if (vm.save.tokens >= def.tokenPrice) Ink else Bad, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CoinIcon(11.dp)
                Text(def.price.toString(), color = if (vm.save.coins >= def.price) Ink else Bad, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PaintPanel(vm: GameViewModel) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .width(SIDE_PANEL)
            .shadow(12.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .background(Cream.copy(alpha = 0.97f), shape)
            .border(3.dp, White, shape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SwatchRow(stringResource(R.string.walls), Palette.WALLS.map { Color(it.argb) }, vm.save.wallColor) { vm.setWallColor(it) }
        SwatchRow(stringResource(R.string.floors), Palette.FLOORS.map { Color(it.first.argb) }, vm.save.floorStyle, striped = Palette.FLOORS.map { Color(it.second.argb) }) { vm.setFloor(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(title: String, colors: List<Color>, selected: Int, striped: List<Color>? = null, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            colors.forEachIndexed { i, c ->
                val brush = striped?.let { Brush.linearGradient(listOf(c, it[i], c, it[i])) } ?: Brush.linearGradient(listOf(c, c))
                Box(
                    Modifier
                        .size(34.dp)
                        .bouncyClick { onPick(i) }
                        .shadow(3.dp, CircleShape)
                        .background(brush, CircleShape)
                        .border(if (i == selected) 4.dp else 2.dp, if (i == selected) Sun else White, CircleShape),
                )
            }
        }
    }
}
