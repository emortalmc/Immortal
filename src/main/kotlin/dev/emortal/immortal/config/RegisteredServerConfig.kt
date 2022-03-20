package dev.emortal.immortal.config

data class RegisteredServerConfig(
    val serverName: String,
    val serverPort: Int,
    val gameName: String
) : java.io.Serializable