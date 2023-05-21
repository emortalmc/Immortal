package dev.emortal.immortal.game

enum class GameState(val joinable: Boolean) {

    /** When the game has not been created yet */
    NOT_CREATED(false),

    /** When the game has been created but not started yet */
    WAITING_FOR_PLAYERS(true),

    /** When the game is in progress */
    PLAYING(false),

    /** When the victory screen is being shown */
    ENDING(false),

    /** When the game been destroyed */
    DESTROYED(false)

}