package dev.emortal.immortal.config

@kotlinx.serialization.Serializable
data class GameConfig(
    val serverName: String = "REPLACE ME",
    val redisAddress: String = "redis://172.17.0.1:6379",

    /**
     * Also decides online-mode; if empty, online mode is true
     */
    val velocitySecret: String = "",
    val ip: String = "0.0.0.0",
    val port: Int = 25565,
    val entityViewDistance: Int = 5,
    val chunkViewDistance: Int = 8,
)