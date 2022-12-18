package dev.emortal.immortal.config

@kotlinx.serialization.Serializable
data class GameConfig(
    val serverName: String = "",

    /**
     * Also decides whether to connect to redis
     * if empty, it will not
     */
    val redisAddress: String = "",
    /**
     * Which game to join if redis address is blank
     */
    val defaultGame: String = "",

    /**
     * Also decides online-mode; if empty, online mode is true
     */
    val velocitySecret: String = "",
    val ip: String = "0.0.0.0",
    val port: Int = 25565,
    val entityViewDistance: Int = 5,
    val chunkViewDistance: Int = 8,
)