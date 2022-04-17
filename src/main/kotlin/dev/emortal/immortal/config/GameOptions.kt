package dev.emortal.immortal.config

@kotlinx.serialization.Serializable
class GameOptions(
    val maxPlayers: Int = 10,
    val minPlayers: Int = 2,
    val countdownSeconds: Int = 15,
    val canJoinDuringGame: Boolean = false,
    val showScoreboard: Boolean = true,
    val showsJoinLeaveMessages: Boolean = true,
    val allowsSpectators: Boolean = true,
    //var private: Boolean = false
)