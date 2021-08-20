package emortal.immortal.game

import net.minestom.server.instance.Instance

class GameOptions(
    val instanceCallback: () -> Instance,
    val maxPlayers: Int = 10,
    val playersToStart: Int = 2,
    val joinableMidGame: Boolean = false,
    val isRegistered: Boolean = true,
    val hasScoreboard: Boolean = true
)