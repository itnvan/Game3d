package com.cozyhome.game.activities

data class Recipe(
    val id: String,
    val needs: Map<String, Int>,
    val coins: Int,
    val xp: Int,
    val level: Int = 1,
    val plate: Int,
    val food: Int,
)

object Cookbook {
    val all = listOf(
        Recipe("salad", mapOf("lettuce" to 2, "tomato" to 1), 70, 14, 1, 0xF4F1EA, 0x7DB257),
        Recipe("carrot_soup", mapOf("carrot" to 2), 50, 10, 1, 0xE9E4DA, 0xE8782C),
        Recipe("corn_cob", mapOf("corn" to 2), 55, 11, 2, 0xF4F1EA, 0xF2C53D),
        Recipe("tomato_pasta", mapOf("tomato" to 3), 90, 16, 2, 0xF4F1EA, 0xD8352A),
        Recipe("veggie_stew", mapOf("carrot" to 1, "tomato" to 1, "corn" to 1), 110, 20, 3, 0x6E4B35, 0xC96A3A),
        Recipe("strawberry_cake", mapOf("strawberry" to 3), 120, 22, 3, 0xF4F1EA, 0xF29BB2),
        Recipe("pumpkin_pie", mapOf("pumpkin" to 1, "strawberry" to 1), 160, 28, 4, 0xF4F1EA, 0xE07A23),
        Recipe("seed_cookies", mapOf("sunflower" to 2, "strawberry" to 1), 130, 24, 4, 0xF4F1EA, 0xC9955A),
    )

    operator fun get(id: String?) = all.firstOrNull { it.id == id }

    fun canCook(r: Recipe, pantry: Map<String, Int>): Boolean = r.needs.all { (k, v) -> (pantry[k] ?: 0) >= v }

    fun consume(r: Recipe, pantry: Map<String, Int>): Map<String, Int> {
        val out = pantry.toMutableMap()
        for ((k, v) in r.needs) out[k] = ((out[k] ?: 0) - v).coerceAtLeast(0)
        return out.filterValues { it > 0 }
    }
}
