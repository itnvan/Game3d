package com.cozyhome.game.world

import com.cozyhome.game.math.V2
import com.cozyhome.game.math.V3

enum class WallSide { NORTH, SOUTH, WEST, EAST }

enum class Surface { GRASS, WOOD, HARD }

data class Opening(val side: WallSide, val center: Float, val width: Float, val bottom: Float, val top: Float)

data class TreeSpot(val pos: V2, val scale: Float, val seed: Int, val kind: Int)

object Lot {
    val FENCE = Rect(-24f, -20f, 24f, 20f)
    val WALK_BOUNDS = FENCE.inset(0.4f)

    val ROOM = Rect(-6f, -16f, 6f, -6f)
    const val WALL_T = 0.2f
    const val WALL_H = 3.0f
    const val DOOR_HALF = 0.75f
    const val DOOR_TOP = 2.3f
    val DOOR = V2(0f, -6f)
    val DOOR_CLEAR = Rect(-1.1f, -7.6f, 1.1f, -6f)
    val PORCH = Rect(-1.6f, -5.8f, 1.6f, -4.8f)

    val OPENINGS = listOf(
        Opening(WallSide.SOUTH, 0f, DOOR_HALF * 2, 0f, DOOR_TOP),
        Opening(WallSide.SOUTH, -3.6f, 1.8f, 1.0f, 2.3f),
        Opening(WallSide.SOUTH, 3.6f, 1.8f, 1.0f, 2.3f),
        Opening(WallSide.NORTH, -3f, 2.2f, 1.1f, 2.3f),
        Opening(WallSide.NORTH, 3f, 2.2f, 1.1f, 2.3f),
        Opening(WallSide.WEST, -11f, 2.0f, 1.0f, 2.3f),
        Opening(WallSide.EAST, -11f, 2.0f, 1.0f, 2.3f),
    )

    val GARDEN = Rect(9f, -16f, 19.5f, -6f)
    val GARDEN_GATE = Rect(9f, -6.2f, 10.8f, -5.8f)
    val PLOT_SIZE = 1.7f
    val PLOTS: List<V2> = buildList {
        val xs = listOf(10.65f, 13.05f, 15.45f, 17.85f)
        val zs = listOf(-14.35f, -11.95f, -9.55f)
        for (z in zs) for (x in xs) add(V2(x, z))
    }
    val GARDEN_TAP = V2(18.7f, -7.1f)
    val GARDEN_PATH = Rect(GARDEN.minX + 0.4f, -8.4f, GARDEN.maxX - 0.4f, -6.3f)

    val COURT = Rect(-20f, -17f, -10f, -7f)
    val RIM = V3(-15f, 3.05f, -15.2f)
    const val RIM_RADIUS = 0.26f
    const val BACKBOARD_Z = -15.62f
    val HOOP_POLE = V2(-15f, -16.5f)
    val SHOT_SPOTS = listOf(V2(-15f, -10.9f), V2(-17.7f, -11.9f), V2(-12.3f, -11.9f))

    val PITCH = Rect(-22.5f, 0.5f, -9.5f, 13.5f)
    const val GOAL_X = -21f
    const val GOAL_Z = 7f
    const val GOAL_HALF = 1.8f
    const val GOAL_HEIGHT = 1.8f
    const val GOAL_DEPTH = 1.1f
    val PENALTY_SPOT = V2(-14f, 7f)
    val KICK_STAND = V2(-13f, 7.75f)

    val TRACK = Track(Rect(-7.5f, -1f, 7.5f, 16f), radius = 3.5f, width = 2f)
    val TRACK_START = V2(0f, 16f)

    val YARD = Rect(-5.5f, 1f, 5.5f, 14f)
    val POND = Circle(15.5f, 7f, 2.8f)
    val MAILBOX = V2(2.4f, 18.4f)
    val GATE = V2(0f, 20f)
    val SPAWN = V2(0f, -3.6f)
    const val SPAWN_YAW = 180f

    val PATHS = listOf(
        Rect(-0.85f, -4.85f, 0.85f, -1.9f),
        Rect(-0.85f, 17f, 0.85f, 19.9f),
        Rect(0.85f, -4.4f, 10.6f, -3.4f),
        Rect(9.2f, -6.0f, 10.6f, -3.4f),
    )

    val FLOWER_BEDS = listOf(Rect(-5.2f, -5.4f, -2.2f, -4.6f), Rect(2.2f, -5.4f, 5.2f, -4.6f))
    val BENCH_BY_POND = V2(15.5f, 11.4f)

    val TREES: List<TreeSpot> = listOf(
        TreeSpot(V2(-9f, -18.4f), 1.1f, 1, 0), TreeSpot(V2(-3f, -18.6f), 0.95f, 2, 1),
        TreeSpot(V2(3.5f, -18.4f), 1.05f, 3, 0), TreeSpot(V2(8.2f, -18.6f), 0.9f, 4, 2),
        TreeSpot(V2(-22.4f, -12f), 1.15f, 5, 1), TreeSpot(V2(-22.6f, -17.6f), 0.95f, 6, 0),
        TreeSpot(V2(-22.6f, -3.5f), 1.0f, 7, 2), TreeSpot(V2(-22.4f, 16.5f), 1.1f, 8, 0),
        TreeSpot(V2(-17f, 18f), 0.95f, 9, 1), TreeSpot(V2(-11f, 17.6f), 1.05f, 10, 0),
        TreeSpot(V2(-6.5f, 18.6f), 0.85f, 11, 2), TreeSpot(V2(6.5f, 18.6f), 0.9f, 12, 1),
        TreeSpot(V2(11f, 17.6f), 1.1f, 13, 0), TreeSpot(V2(17.5f, 18f), 0.95f, 14, 2),
        TreeSpot(V2(22.4f, 15f), 1.05f, 15, 0), TreeSpot(V2(22.4f, 0.5f), 1.0f, 16, 1),
        TreeSpot(V2(22.4f, -4f), 0.9f, 17, 0), TreeSpot(V2(22.2f, -12f), 1.1f, 18, 2),
        TreeSpot(V2(22.4f, -18f), 0.95f, 19, 1), TreeSpot(V2(11.4f, 2.6f), 1.0f, 20, 0),
        TreeSpot(V2(20f, 10.5f), 0.9f, 21, 1), TreeSpot(V2(12f, 13.4f), 0.8f, 22, 2),
        TreeSpot(V2(-8.4f, -18.9f), 0.7f, 23, 2), TreeSpot(V2(-12.4f, -3.2f), 0.9f, 24, 0),
    )

    fun wallSolids(): List<Rect> {
        val t = WALL_T
        return listOf(
            Rect(ROOM.minX - t, ROOM.minZ - t, ROOM.minX, ROOM.maxZ + t),
            Rect(ROOM.maxX, ROOM.minZ - t, ROOM.maxX + t, ROOM.maxZ + t),
            Rect(ROOM.minX - t, ROOM.minZ - t, ROOM.maxX + t, ROOM.minZ),
            Rect(ROOM.minX - t, ROOM.maxZ, DOOR.x - DOOR_HALF, ROOM.maxZ + t),
            Rect(DOOR.x + DOOR_HALF, ROOM.maxZ, ROOM.maxX + t, ROOM.maxZ + t),
        )
    }

    fun gardenFenceSolids(): List<Rect> {
        val g = GARDEN
        val t = 0.12f
        return listOf(
            Rect(g.minX - t, g.minZ - t, g.maxX + t, g.minZ),
            Rect(g.maxX, g.minZ - t, g.maxX + t, g.maxZ + t),
            Rect(g.minX - t, g.minZ, g.minX, g.maxZ + t),
            Rect(GARDEN_GATE.maxX, g.maxZ, g.maxX + t, g.maxZ + t),
        )
    }

    fun staticSolids(): List<Solid> = buildList {
        addAll(wallSolids())
        addAll(gardenFenceSolids())
        addAll(FLOWER_BEDS)
        add(POND)
        add(Circle(HOOP_POLE.x, HOOP_POLE.z, 0.18f))
        add(Rect(GOAL_X - GOAL_DEPTH, GOAL_Z - GOAL_HALF - 0.1f, GOAL_X, GOAL_Z + GOAL_HALF + 0.1f))
        add(Circle(MAILBOX.x, MAILBOX.z, 0.25f))
        add(Circle(GARDEN_TAP.x, GARDEN_TAP.z, 0.38f))
        for (t in TREES) add(Circle(t.pos.x, t.pos.z, 0.3f * t.scale))
    }

    fun isInsideHouse(p: V2) = p in ROOM

    fun surfaceAt(p: V2): Surface = when {
        p in ROOM -> Surface.WOOD
        p in PORCH || p in COURT || p in GARDEN_PATH || PATHS.any { p in it } || TRACK.isOnTrack(p) -> Surface.HARD
        else -> Surface.GRASS
    }
}
