package dev.emortal.immortal.game

class GameOptions(
    val maxPlayers: Int = 10,
    val minPlayers: Int = 2,
    val countdownSeconds: Int = 5,
    val canJoinDuringGame: Boolean = false,
    val showScoreboard: Boolean = true,
    val showsJoinLeaveMessages: Boolean = true,
    val onlyFriends: Boolean = false
)