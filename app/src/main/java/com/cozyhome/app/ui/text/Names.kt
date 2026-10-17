package com.cozyhome.app.ui.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cozyhome.app.R
import com.cozyhome.game.decor.Category
import com.cozyhome.game.game.ActionKind
import com.cozyhome.game.game.Interaction
import com.cozyhome.game.game.Msg

@StringRes
fun itemName(id: String): Int = when (id) {
    "sofa" -> R.string.item_sofa
    "armchair" -> R.string.item_armchair
    "beanbag" -> R.string.item_beanbag
    "chair" -> R.string.item_chair
    "stool" -> R.string.item_stool
    "coffee_table" -> R.string.item_coffee_table
    "dining_table" -> R.string.item_dining_table
    "side_table" -> R.string.item_side_table
    "desk" -> R.string.item_desk
    "bed_single" -> R.string.item_bed_single
    "bed_double" -> R.string.item_bed_double
    "nightstand" -> R.string.item_nightstand
    "bookshelf" -> R.string.item_bookshelf
    "wardrobe" -> R.string.item_wardrobe
    "tv" -> R.string.item_tv
    "fridge" -> R.string.item_fridge
    "stove" -> R.string.item_stove
    "counter" -> R.string.item_counter
    "sink" -> R.string.item_sink
    "floor_lamp" -> R.string.item_floor_lamp
    "globe_lamp" -> R.string.item_globe_lamp
    "monstera" -> R.string.item_monstera
    "cactus" -> R.string.item_cactus
    "fiddle_tree" -> R.string.item_fiddle_tree
    "tulips" -> R.string.item_tulips
    "rug_round" -> R.string.item_rug_round
    "rug_rect" -> R.string.item_rug_rect
    "rug_runner" -> R.string.item_rug_runner
    "piano" -> R.string.item_piano
    "aquarium" -> R.string.item_aquarium
    "teddy" -> R.string.item_teddy
    "arcade" -> R.string.item_arcade
    "bench" -> R.string.item_bench
    "picnic_table" -> R.string.item_picnic_table
    "flower_bed" -> R.string.item_flower_bed
    "lamp_post" -> R.string.item_lamp_post
    "parasol" -> R.string.item_parasol
    "trampoline" -> R.string.item_trampoline
    "swing" -> R.string.item_swing
    "sandbox" -> R.string.item_sandbox
    "bird_bath" -> R.string.item_bird_bath
    "gnome" -> R.string.item_gnome
    "hedge" -> R.string.item_hedge
    "fruit_tree" -> R.string.item_fruit_tree
    "bbq" -> R.string.item_bbq
    "pool" -> R.string.item_pool
    else -> R.string.app_name
}

@StringRes
fun cropName(id: String?): Int = when (id) {
    "lettuce" -> R.string.crop_lettuce
    "carrot" -> R.string.crop_carrot
    "tomato" -> R.string.crop_tomato
    "corn" -> R.string.crop_corn
    "strawberry" -> R.string.crop_strawberry
    "pumpkin" -> R.string.crop_pumpkin
    else -> R.string.crop_sunflower
}

@StringRes
fun categoryName(c: Category): Int = when (c) {
    Category.SEATING -> R.string.cat_seating
    Category.TABLES -> R.string.cat_tables
    Category.BEDS -> R.string.cat_beds
    Category.STORAGE -> R.string.cat_storage
    Category.KITCHEN -> R.string.cat_kitchen
    Category.LIGHTS -> R.string.cat_lights
    Category.PLANTS -> R.string.cat_plants
    Category.RUGS -> R.string.cat_rugs
    Category.FUN -> R.string.cat_fun
    Category.OUTDOOR -> R.string.cat_outdoor
}

fun categoryEmoji(c: Category): String = when (c) {
    Category.SEATING -> "🛋"
    Category.TABLES -> "☕"
    Category.BEDS -> "🛏"
    Category.STORAGE -> "📚"
    Category.KITCHEN -> "🍳"
    Category.LIGHTS -> "💡"
    Category.PLANTS -> "🌵"
    Category.RUGS -> "🔶"
    Category.FUN -> "🎮"
    Category.OUTDOOR -> "🌳"
}

fun actionEmoji(kind: ActionKind): String = when (kind) {
    ActionKind.PLANT -> "🌱"
    ActionKind.WATER, ActionKind.WATER_ALL -> "💧"
    ActionKind.HARVEST -> "🥕"
    ActionKind.GROWING -> "⏳"
    ActionKind.BASKETBALL -> "🏀"
    ActionKind.SOCCER -> "⚽"
    ActionKind.JOG -> "🏃"
    ActionKind.GIFT -> "🎁"
    ActionKind.SIT -> "🛋"
    ActionKind.SLEEP -> "😴"
    ActionKind.TV_ON, ActionKind.TV_OFF -> "📺"
    ActionKind.LAMP_ON, ActionKind.LAMP_OFF -> "💡"
    ActionKind.BOUNCE -> "🤸"
    ActionKind.SWING -> "🎈"
}

@Composable
fun interactionLabel(i: Interaction): String = when (i.kind) {
    ActionKind.PLANT -> stringResource(R.string.act_plant)
    ActionKind.WATER -> stringResource(R.string.act_water)
    ActionKind.HARVEST -> stringResource(R.string.act_harvest)
    ActionKind.GROWING -> stringResource(R.string.act_growing, i.detail)
    ActionKind.BASKETBALL -> stringResource(R.string.act_basketball)
    ActionKind.SOCCER -> stringResource(R.string.act_soccer)
    ActionKind.JOG -> stringResource(R.string.act_jog)
    ActionKind.GIFT -> stringResource(R.string.act_gift)
    ActionKind.WATER_ALL -> stringResource(R.string.act_water_all)
    ActionKind.SIT -> stringResource(R.string.act_sit)
    ActionKind.SLEEP -> stringResource(R.string.act_sleep)
    ActionKind.TV_ON -> stringResource(R.string.act_tv_on)
    ActionKind.TV_OFF -> stringResource(R.string.act_tv_off)
    ActionKind.LAMP_ON -> stringResource(R.string.act_lamp_on)
    ActionKind.LAMP_OFF -> stringResource(R.string.act_lamp_off)
    ActionKind.BOUNCE -> stringResource(R.string.act_bounce)
    ActionKind.SWING -> stringResource(R.string.act_swing)
}

@Composable
fun msgText(msg: Msg, arg: String): String {
    val cropArg = if (msg == Msg.PLANTED || msg == Msg.HARVESTED) stringResource(cropName(arg)) else arg
    return when (msg) {
        Msg.GROWING -> stringResource(R.string.msg_growing, arg)
        Msg.GIFT_TAKEN -> stringResource(R.string.msg_gift_taken)
        Msg.GIFT_RECEIVED -> stringResource(R.string.msg_gift_received, arg)
        Msg.NOTHING_TO_WATER -> stringResource(R.string.msg_nothing_to_water)
        Msg.WATERED_ALL -> stringResource(R.string.msg_watered_all, arg)
        Msg.NOT_ENOUGH_COINS -> stringResource(R.string.msg_not_enough)
        Msg.PLANTED -> stringResource(R.string.msg_planted, cropArg)
        Msg.HARVESTED -> stringResource(R.string.msg_harvested, cropArg)
        Msg.GOOD_NIGHT -> stringResource(R.string.msg_good_night)
        Msg.NAP -> stringResource(R.string.msg_nap)
        Msg.LEVEL_UP -> stringResource(R.string.msg_level_up)
        Msg.OFF_TRACK -> stringResource(R.string.msg_off_track)
        Msg.PERFECT_BOUNCE -> stringResource(R.string.msg_perfect_bounce, arg)
        Msg.PLACE_INVALID -> stringResource(R.string.msg_place_invalid)
        Msg.STORED -> stringResource(R.string.msg_stored)
        Msg.BOUGHT -> stringResource(R.string.msg_bought)
        Msg.LOCKED -> stringResource(R.string.msg_locked, arg)
        Msg.NO_SPACE -> stringResource(R.string.msg_no_space)
        Msg.PLACED -> stringResource(R.string.msg_placed)
        Msg.WELCOME -> stringResource(R.string.msg_welcome, arg)
    }
}
