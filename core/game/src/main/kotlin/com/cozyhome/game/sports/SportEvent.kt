package com.cozyhome.game.sports

sealed interface SportEvent {
    data object Released : SportEvent
    data object RimHit : SportEvent
    data object BoardHit : SportEvent
    data class Basket(val points: Int, val swish: Boolean, val streak: Int) : SportEvent
    data object BasketMiss : SportEvent
    data object Kicked : SportEvent
    data object PostHit : SportEvent
    data object KeeperSave : SportEvent
    data object Goal : SportEvent
    data object ShotMissed : SportEvent
    data class Countdown(val value: Int) : SportEvent
    data object Go : SportEvent
    data class Gate(val lap: Int, val index: Int) : SportEvent
    data class Lap(val lap: Int, val millis: Long) : SportEvent
    data object OffTrack : SportEvent
    data class Finished(val kind: SportKind, val score: Int, val detail: Int, val millis: Long = 0L) : SportEvent
}

enum class SportKind { BASKETBALL, SOCCER, JOGGING }
