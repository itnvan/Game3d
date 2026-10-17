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
    "jack_o_lantern" -> R.string.item_jack_o_lantern
    "candy_cauldron" -> R.string.item_candy_cauldron
    "ghost_lamp" -> R.string.item_ghost_lamp
    "xmas_tree" -> R.string.item_xmas_tree
    "snowman" -> R.string.item_snowman
    "gift_pile" -> R.string.item_gift_pile
    "red_lantern" -> R.string.item_red_lantern
    "apricot_tree" -> R.string.item_apricot_tree
    "firecracker_string" -> R.string.item_firecracker_string
    "star_lantern" -> R.string.item_star_lantern
    "rabbit_lantern" -> R.string.item_rabbit_lantern
    "mooncake_table" -> R.string.item_mooncake_table
    "heart_arch" -> R.string.item_heart_arch
    "rose_vase" -> R.string.item_rose_vase
    "cherry_tree" -> R.string.item_cherry_tree
    "flower_cart" -> R.string.item_flower_cart
    "ice_cream_cart" -> R.string.item_ice_cream_cart
    "beach_chair" -> R.string.item_beach_chair
    "golden_trophy" -> R.string.item_golden_trophy
    "fish_trophy" -> R.string.item_fish_trophy
    "bug_case" -> R.string.item_bug_case
    "chef_statue" -> R.string.item_chef_statue
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
    Category.FESTIVAL -> R.string.cat_festival
    Category.SPECIAL -> R.string.cat_special
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
    Category.FESTIVAL -> "🎉"
    Category.SPECIAL -> "🏆"
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
    ActionKind.FISH -> "🐟"
    ActionKind.CATCH_BUG -> "✨"
    ActionKind.COOK -> "🍳"
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
    ActionKind.FISH -> stringResource(R.string.act_fish)
    ActionKind.CATCH_BUG -> stringResource(R.string.act_catch, stringResource(bugName(i.detail)))
    ActionKind.COOK -> stringResource(R.string.act_cook)
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
        Msg.FISH_ESCAPED -> stringResource(R.string.msg_fish_escaped)
        Msg.FISH_SNAPPED -> stringResource(R.string.msg_fish_snapped)
        Msg.TOO_EARLY -> stringResource(R.string.msg_too_early)
        Msg.BUG_MISSED -> stringResource(R.string.msg_bug_missed)
        Msg.BUG_CAUGHT -> stringResource(R.string.msg_bug_caught)
        Msg.COOKED -> stringResource(R.string.msg_cooked)
        Msg.MISSING_INGREDIENTS -> stringResource(R.string.msg_missing)
        Msg.QUEST_DONE -> stringResource(R.string.msg_quest_done)
        Msg.MILESTONE -> stringResource(R.string.msg_milestone)
        Msg.FESTIVAL_TOKENS -> stringResource(R.string.msg_tokens, arg)
    }
}

@StringRes
fun fishName(id: String?): Int = when (id) {
    "minnow" -> R.string.fish_minnow
    "goldfish" -> R.string.fish_goldfish
    "carp" -> R.string.fish_carp
    "bluegill" -> R.string.fish_bluegill
    "bass" -> R.string.fish_bass
    "trout" -> R.string.fish_trout
    "catfish" -> R.string.fish_catfish
    "koi" -> R.string.fish_koi
    "pufferfish" -> R.string.fish_pufferfish
    "golden_koi" -> R.string.fish_golden_koi
    "old_boot" -> R.string.fish_old_boot
    "treasure" -> R.string.fish_treasure
    "new" -> R.string.fish_new
    "record" -> R.string.fish_record
    else -> R.string.fish_minnow
}

@StringRes
fun bugName(id: String?): Int = when (id) {
    "butterfly" -> R.string.bug_butterfly
    "blue_morpho" -> R.string.bug_blue_morpho
    "bee" -> R.string.bug_bee
    "ladybug" -> R.string.bug_ladybug
    "dragonfly" -> R.string.bug_dragonfly
    "snail" -> R.string.bug_snail
    "stag_beetle" -> R.string.bug_stag_beetle
    "firefly" -> R.string.bug_firefly
    "moth" -> R.string.bug_moth
    "cicada" -> R.string.bug_cicada
    "title" -> R.string.bug_title
    else -> R.string.bug_butterfly
}

@StringRes
fun dishName(id: String?): Int = when (id) {
    "salad" -> R.string.dish_salad
    "carrot_soup" -> R.string.dish_carrot_soup
    "corn_cob" -> R.string.dish_corn_cob
    "tomato_pasta" -> R.string.dish_tomato_pasta
    "veggie_stew" -> R.string.dish_veggie_stew
    "strawberry_cake" -> R.string.dish_strawberry_cake
    "pumpkin_pie" -> R.string.dish_pumpkin_pie
    "seed_cookies" -> R.string.dish_seed_cookies
    else -> R.string.dish_salad
}

@StringRes
fun festivalName(id: String?): Int = when (id) {
    "LUNAR_NEW_YEAR" -> R.string.fest_lunar_new_year
    "VALENTINE" -> R.string.fest_valentine
    "SPRING_BLOOM" -> R.string.fest_spring_bloom
    "SUMMER_FUN" -> R.string.fest_summer_fun
    "MID_AUTUMN" -> R.string.fest_mid_autumn
    "HALLOWEEN" -> R.string.fest_halloween
    "CHRISTMAS" -> R.string.fest_christmas
    else -> R.string.fest_halloween
}

@StringRes
fun questText(id: String?): Int = when (id) {
    "HARVEST" -> R.string.quest_harvest
    "PLANT" -> R.string.quest_plant
    "WATER" -> R.string.quest_water
    "FISH" -> R.string.quest_fish
    "BUG" -> R.string.quest_bug
    "COOK" -> R.string.quest_cook
    "BASKET" -> R.string.quest_basket
    "SOCCER" -> R.string.quest_soccer
    "JOG" -> R.string.quest_jog
    "DECORATE" -> R.string.quest_decorate
    "BOUNCE" -> R.string.quest_bounce
    "RARE_FISH" -> R.string.quest_rare_fish
    else -> R.string.quest_harvest
}
