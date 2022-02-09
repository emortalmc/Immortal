package dev.emortal.immortal.event

import dev.emortal.immortal.game.Game

class GameDestroyEvent(private val game: Game) : GameEvent {
    override fun getGame(): Game = this.game
}