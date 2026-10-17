package com.cozyhome.game.state

import kotlinx.serialization.Serializable

@Serializable
enum class Zone { HOUSE, YARD }

@Serializable
data class Appearance(
    val skin: Int = 1,
    val hairStyle: Int = 0,
    val hairColor: Int = 1,
    val shirt: Int = 0,
    val pants: Int = 0,
    val shoes: Int = 0,
    val hat: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
    val eyeColor: Int = 0,
    val eyeStyle: Int = 0,
)

@Serializable
data class PlacedItem(
    val uid: Long,
    val itemId: String,
    val x: Float,
    val z: Float,
    val rot: Int,
    val variant: Int,
    val zone: Zone,
    val on: Boolean = false,
)

@Serializable
data class Plot(
    val crop: String? = null,
    val stage: Int = 0,
    val watered: Boolean = false,
    val stageStartedAt: Long = 0L,
)

@Serializable
data class Stats(
    val harvested: Int = 0,
    val basketBest: Int = 0,
    val soccerBest: Int = 0,
    val jogBestMs: Long = 0L,
    val gamesPlayed: Int = 0,
)

@Serializable
data class SaveData(
    val version: Int = 1,
    val created: Boolean = false,
    val name: String = "",
    val look: Appearance = Appearance(),
    val coins: Int = 400,
    val xp: Int = 0,
    val inventory: Map<String, Int> = emptyMap(),
    val placed: List<PlacedItem> = emptyList(),
    val wallColor: Int = 0,
    val floorStyle: Int = 0,
    val plots: List<Plot> = List(12) { Plot() },
    val stats: Stats = Stats(),
    val lastGiftDay: Long = -1L,
    val nextUid: Long = 1L,
    val seenTutorial: Boolean = false,
    val clockOffsetMs: Long = 0L,
    val pantry: Map<String, Int> = emptyMap(),
    val daily: com.cozyhome.game.progress.Daily = com.cozyhome.game.progress.Daily(),
    val album: com.cozyhome.game.progress.Album = com.cozyhome.game.progress.Album(),
    val tokens: Int = 0,
    val festivalDebug: String = "",
)
