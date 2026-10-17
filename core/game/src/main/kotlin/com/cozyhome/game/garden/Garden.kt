package com.cozyhome.game.garden

import com.cozyhome.game.state.Plot

data class CropDef(
    val id: String,
    val seedPrice: Int,
    val sellPrice: Int,
    val stageSeconds: Int,
    val xp: Int,
    val level: Int,
)

object Crops {
    val all = listOf(
        CropDef("lettuce", 5, 14, 20, 2, 1),
        CropDef("carrot", 8, 22, 35, 3, 1),
        CropDef("sunflower", 10, 30, 50, 4, 1),
        CropDef("tomato", 12, 36, 60, 5, 2),
        CropDef("corn", 15, 48, 90, 6, 2),
        CropDef("strawberry", 20, 64, 120, 8, 3),
        CropDef("pumpkin", 30, 105, 180, 12, 4),
    )
    private val byId = all.associateBy { it.id }
    operator fun get(id: String?): CropDef? = id?.let { byId[it] }
}

sealed interface PlotStatus {
    data object Empty : PlotStatus
    data object Thirsty : PlotStatus
    data class Growing(val progress: Float, val secondsLeft: Int) : PlotStatus
    data object Ripe : PlotStatus
}

object Garden {
    const val RIPE = 3

    fun stageMillis(crop: CropDef) = crop.stageSeconds * 1000L

    fun settle(plot: Plot, now: Long): Plot {
        val crop = Crops[plot.crop] ?: return plot
        if (plot.stage >= RIPE || !plot.watered) return plot
        if (now - plot.stageStartedAt < stageMillis(crop)) return plot
        val next = plot.stage + 1
        return plot.copy(stage = next, watered = false, stageStartedAt = plot.stageStartedAt + stageMillis(crop))
    }

    fun status(plot: Plot, now: Long): PlotStatus {
        val p = settle(plot, now)
        val crop = Crops[p.crop] ?: return PlotStatus.Empty
        return when {
            p.stage >= RIPE -> PlotStatus.Ripe
            !p.watered -> PlotStatus.Thirsty
            else -> {
                val elapsed = (now - p.stageStartedAt).coerceAtLeast(0L)
                val total = stageMillis(crop)
                PlotStatus.Growing((elapsed.toFloat() / total).coerceIn(0f, 1f), ((total - elapsed + 999) / 1000).toInt().coerceAtLeast(0))
            }
        }
    }

    fun growth(plot: Plot, now: Long): Float {
        val p = settle(plot, now)
        val crop = Crops[p.crop] ?: return 0f
        if (p.stage >= RIPE) return RIPE.toFloat()
        val within = if (p.watered) ((now - p.stageStartedAt).toFloat() / stageMillis(crop)).coerceIn(0f, 1f) else 0f
        return p.stage + within
    }

    fun plant(plot: Plot, cropId: String, now: Long): Plot = Plot(crop = cropId, stage = 0, watered = false, stageStartedAt = now)

    fun water(plot: Plot, now: Long): Plot {
        val p = settle(plot, now)
        if (p.crop == null || p.stage >= RIPE || p.watered) return p
        return p.copy(watered = true, stageStartedAt = now)
    }

    fun harvest(plot: Plot): Plot = Plot()
}
