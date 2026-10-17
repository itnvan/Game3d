package com.cozyhome.game.decor

import com.cozyhome.game.math.V3

enum class Category { SEATING, TABLES, BEDS, STORAGE, KITCHEN, LIGHTS, PLANTS, RUGS, FUN, OUTDOOR, FESTIVAL, SPECIAL }

enum class Layer { FLOOR, OBJECT }

enum class ZoneRule { HOUSE, YARD }

enum class Use { NONE, SIT, SLEEP, TV, LAMP, BOUNCE, SWING, COOK }

data class ItemDef(
    val id: String,
    val category: Category,
    val price: Int,
    val w: Int,
    val d: Int,
    val height: Float,
    val zone: ZoneRule,
    val variants: Int,
    val layer: Layer = Layer.OBJECT,
    val use: Use = Use.NONE,
    val seat: V3? = null,
    val level: Int = 1,
    val festival: com.cozyhome.game.progress.Festival? = null,
    val tokenPrice: Int = 0,
    val reward: Boolean = false,
)

object Catalog {
    const val CELL = 0.5f

    val items: List<ItemDef> = listOf(
        ItemDef("sofa", Category.SEATING, 120, 4, 2, 0.9f, ZoneRule.HOUSE, 4, use = Use.SIT, seat = V3(0f, 0.44f, 0.08f)),
        ItemDef("armchair", Category.SEATING, 80, 2, 2, 0.9f, ZoneRule.HOUSE, 4, use = Use.SIT, seat = V3(0f, 0.44f, 0.08f)),
        ItemDef("beanbag", Category.SEATING, 45, 2, 2, 0.6f, ZoneRule.HOUSE, 4, use = Use.SIT, seat = V3(0f, 0.32f, 0.05f)),
        ItemDef("chair", Category.SEATING, 25, 1, 1, 0.9f, ZoneRule.HOUSE, 3, use = Use.SIT, seat = V3(0f, 0.46f, 0.02f)),
        ItemDef("stool", Category.SEATING, 15, 1, 1, 0.5f, ZoneRule.HOUSE, 3, use = Use.SIT, seat = V3(0f, 0.5f, 0f)),
        ItemDef("coffee_table", Category.TABLES, 60, 3, 2, 0.45f, ZoneRule.HOUSE, 3),
        ItemDef("dining_table", Category.TABLES, 90, 3, 2, 0.76f, ZoneRule.HOUSE, 3),
        ItemDef("side_table", Category.TABLES, 30, 1, 1, 0.7f, ZoneRule.HOUSE, 3),
        ItemDef("desk", Category.TABLES, 85, 3, 2, 1.0f, ZoneRule.HOUSE, 3),
        ItemDef("bed_single", Category.BEDS, 120, 2, 4, 0.9f, ZoneRule.HOUSE, 4, use = Use.SLEEP, seat = V3(0f, 0.55f, 0.1f)),
        ItemDef("bed_double", Category.BEDS, 200, 3, 4, 1.0f, ZoneRule.HOUSE, 4, use = Use.SLEEP, seat = V3(0.35f, 0.58f, 0.1f), level = 3),
        ItemDef("nightstand", Category.STORAGE, 35, 1, 1, 0.6f, ZoneRule.HOUSE, 3),
        ItemDef("bookshelf", Category.STORAGE, 95, 3, 1, 1.8f, ZoneRule.HOUSE, 3),
        ItemDef("wardrobe", Category.STORAGE, 130, 3, 1, 2.0f, ZoneRule.HOUSE, 3, level = 2),
        ItemDef("tv", Category.FUN, 150, 3, 1, 1.3f, ZoneRule.HOUSE, 2, use = Use.TV),
        ItemDef("fridge", Category.KITCHEN, 140, 2, 2, 1.8f, ZoneRule.HOUSE, 3),
        ItemDef("stove", Category.KITCHEN, 110, 2, 2, 1.0f, ZoneRule.HOUSE, 2, use = Use.COOK),
        ItemDef("counter", Category.KITCHEN, 70, 2, 2, 0.95f, ZoneRule.HOUSE, 3),
        ItemDef("sink", Category.KITCHEN, 90, 2, 2, 1.1f, ZoneRule.HOUSE, 3),
        ItemDef("floor_lamp", Category.LIGHTS, 40, 1, 1, 1.6f, ZoneRule.HOUSE, 3, use = Use.LAMP),
        ItemDef("globe_lamp", Category.LIGHTS, 35, 1, 1, 0.7f, ZoneRule.HOUSE, 3, use = Use.LAMP),
        ItemDef("monstera", Category.PLANTS, 30, 1, 1, 1.2f, ZoneRule.HOUSE, 3),
        ItemDef("cactus", Category.PLANTS, 20, 1, 1, 0.8f, ZoneRule.HOUSE, 3),
        ItemDef("fiddle_tree", Category.PLANTS, 55, 2, 2, 1.9f, ZoneRule.HOUSE, 2),
        ItemDef("tulips", Category.PLANTS, 25, 1, 1, 0.7f, ZoneRule.HOUSE, 4),
        ItemDef("rug_round", Category.RUGS, 50, 4, 4, 0.02f, ZoneRule.HOUSE, 4, layer = Layer.FLOOR),
        ItemDef("rug_rect", Category.RUGS, 70, 6, 4, 0.02f, ZoneRule.HOUSE, 4, layer = Layer.FLOOR),
        ItemDef("rug_runner", Category.RUGS, 35, 2, 5, 0.02f, ZoneRule.HOUSE, 3, layer = Layer.FLOOR),
        ItemDef("piano", Category.FUN, 260, 3, 2, 1.3f, ZoneRule.HOUSE, 2, level = 4),
        ItemDef("aquarium", Category.FUN, 180, 3, 1, 1.3f, ZoneRule.HOUSE, 2, level = 3),
        ItemDef("teddy", Category.FUN, 30, 1, 1, 0.6f, ZoneRule.HOUSE, 3),
        ItemDef("arcade", Category.FUN, 220, 2, 2, 1.8f, ZoneRule.HOUSE, 3, level = 5),
        ItemDef("bench", Category.OUTDOOR, 60, 3, 1, 0.9f, ZoneRule.YARD, 3, use = Use.SIT, seat = V3(0f, 0.45f, 0.02f)),
        ItemDef("picnic_table", Category.OUTDOOR, 120, 4, 4, 0.8f, ZoneRule.YARD, 3),
        ItemDef("flower_bed", Category.OUTDOOR, 30, 2, 1, 0.5f, ZoneRule.YARD, 4),
        ItemDef("lamp_post", Category.OUTDOOR, 70, 1, 1, 2.6f, ZoneRule.YARD, 2, use = Use.LAMP),
        ItemDef("parasol", Category.OUTDOOR, 140, 3, 3, 2.4f, ZoneRule.YARD, 4),
        ItemDef("trampoline", Category.OUTDOOR, 250, 5, 5, 0.9f, ZoneRule.YARD, 3, use = Use.BOUNCE, seat = V3(0f, 0.75f, 0f), level = 2),
        ItemDef("swing", Category.OUTDOOR, 220, 4, 3, 2.3f, ZoneRule.YARD, 3, use = Use.SWING, seat = V3(0f, 0.52f, 0f), level = 3),
        ItemDef("sandbox", Category.OUTDOOR, 90, 4, 4, 0.4f, ZoneRule.YARD, 2),
        ItemDef("bird_bath", Category.OUTDOOR, 40, 1, 1, 0.9f, ZoneRule.YARD, 2),
        ItemDef("gnome", Category.OUTDOOR, 25, 1, 1, 0.6f, ZoneRule.YARD, 3),
        ItemDef("hedge", Category.OUTDOOR, 35, 3, 1, 0.8f, ZoneRule.YARD, 2),
        ItemDef("fruit_tree", Category.OUTDOOR, 60, 2, 2, 2.6f, ZoneRule.YARD, 3),
        ItemDef("bbq", Category.OUTDOOR, 110, 2, 2, 1.1f, ZoneRule.YARD, 2, level = 2),
        ItemDef("pool", Category.OUTDOOR, 300, 6, 4, 0.45f, ZoneRule.YARD, 2, level = 5),
        ItemDef("jack_o_lantern", Category.FESTIVAL, 0, 1, 1, 0.5f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.HALLOWEEN, tokenPrice = 4),
        ItemDef("candy_cauldron", Category.FESTIVAL, 0, 2, 2, 0.55f, ZoneRule.HOUSE, 1, festival = com.cozyhome.game.progress.Festival.HALLOWEEN, tokenPrice = 6),
        ItemDef("ghost_lamp", Category.FESTIVAL, 0, 2, 2, 1.0f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.HALLOWEEN, tokenPrice = 8),
        ItemDef("xmas_tree", Category.FESTIVAL, 0, 3, 3, 2.5f, ZoneRule.HOUSE, 1, festival = com.cozyhome.game.progress.Festival.CHRISTMAS, tokenPrice = 10),
        ItemDef("snowman", Category.FESTIVAL, 0, 2, 2, 1.4f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.CHRISTMAS, tokenPrice = 6),
        ItemDef("gift_pile", Category.FESTIVAL, 0, 2, 2, 0.6f, ZoneRule.HOUSE, 1, festival = com.cozyhome.game.progress.Festival.CHRISTMAS, tokenPrice = 4),
        ItemDef("red_lantern", Category.FESTIVAL, 0, 2, 1, 1.9f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.LUNAR_NEW_YEAR, tokenPrice = 5),
        ItemDef("apricot_tree", Category.FESTIVAL, 0, 3, 3, 2.4f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.LUNAR_NEW_YEAR, tokenPrice = 10),
        ItemDef("firecracker_string", Category.FESTIVAL, 0, 1, 1, 1.9f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.LUNAR_NEW_YEAR, tokenPrice = 4),
        ItemDef("star_lantern", Category.FESTIVAL, 0, 2, 1, 1.8f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.MID_AUTUMN, tokenPrice = 5),
        ItemDef("rabbit_lantern", Category.FESTIVAL, 0, 2, 1, 0.7f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.MID_AUTUMN, tokenPrice = 5),
        ItemDef("mooncake_table", Category.FESTIVAL, 0, 2, 2, 0.6f, ZoneRule.HOUSE, 1, festival = com.cozyhome.game.progress.Festival.MID_AUTUMN, tokenPrice = 7),
        ItemDef("heart_arch", Category.FESTIVAL, 0, 5, 1, 2.4f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.VALENTINE, tokenPrice = 9),
        ItemDef("rose_vase", Category.FESTIVAL, 0, 1, 1, 0.6f, ZoneRule.HOUSE, 1, festival = com.cozyhome.game.progress.Festival.VALENTINE, tokenPrice = 4),
        ItemDef("cherry_tree", Category.FESTIVAL, 0, 3, 3, 2.4f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.SPRING_BLOOM, tokenPrice = 10),
        ItemDef("flower_cart", Category.FESTIVAL, 0, 3, 2, 1.0f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.SPRING_BLOOM, tokenPrice = 7),
        ItemDef("ice_cream_cart", Category.FESTIVAL, 0, 3, 2, 2.3f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.SUMMER_FUN, tokenPrice = 9),
        ItemDef("beach_chair", Category.FESTIVAL, 0, 2, 2, 0.7f, ZoneRule.YARD, 1, festival = com.cozyhome.game.progress.Festival.SUMMER_FUN, tokenPrice = 5, use = Use.SIT, seat = V3(0f, 0.35f, 0f)),
        ItemDef("golden_trophy", Category.SPECIAL, 0, 1, 1, 0.65f, ZoneRule.HOUSE, 1, reward = true),
        ItemDef("fish_trophy", Category.SPECIAL, 0, 2, 1, 0.7f, ZoneRule.HOUSE, 1, reward = true),
        ItemDef("bug_case", Category.SPECIAL, 0, 2, 1, 0.55f, ZoneRule.HOUSE, 1, reward = true),
        ItemDef("chef_statue", Category.SPECIAL, 0, 1, 1, 0.7f, ZoneRule.HOUSE, 1, reward = true),
    )

    private val byId = items.associateBy { it.id }

    operator fun get(id: String): ItemDef? = byId[id]

    fun categoriesFor(zone: ZoneRule): List<Category> =
        items.filter { it.zone == zone }.map { it.category }.distinct()
}
