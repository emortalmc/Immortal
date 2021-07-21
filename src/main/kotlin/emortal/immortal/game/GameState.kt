package emortal.immortal.game

enum class GameState(val joinable: Boolean) {

    /** When the game is waiting for players to join */
    WAITING_FOR_PLAYERS(true),

    /** When the game is counting down */
    STARTING(true),

    /** When the game is in progress */
    PLAYING(false),

    /** When the victory screen is being shown, players will then be moved to another game */
    ENDING(false)

}