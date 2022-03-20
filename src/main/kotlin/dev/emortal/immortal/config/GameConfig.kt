package dev.emortal.immortal.config

@kotlinx.serialization.Serializable
data class GameConfig(
    val serverName: String,
    val serverPort: Int
)