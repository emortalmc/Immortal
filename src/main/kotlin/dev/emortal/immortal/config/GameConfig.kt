package dev.emortal.immortal.config

@kotlinx.serialization.Serializable
data class GameConfig(
    val serverName: String,
    val serverPort: Int,
    val address: String = "redis://172.17.0.1:6379"
)