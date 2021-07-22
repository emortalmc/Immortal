package emortal.immortal.game

import net.minestom.server.instance.Instance

abstract class GameOptions(
    val instances: Set<Instance>,
    val maxPlayers: Int = 10,
    val playersToStart: Int = 2,
    val joinableMidGame: Boolean = false
)