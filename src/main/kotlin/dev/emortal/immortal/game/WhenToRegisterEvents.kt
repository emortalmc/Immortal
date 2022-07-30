package dev.emortal.immortal.game

/**
 * Allows configuration of when events start being listened to
 *
 * Defaults to [GAME_START]
 */
enum class WhenToRegisterEvents {
    /**
     * When game object is initialized
     */
    IMMEDIATELY,

    /**
     * When start() is called
     */
    GAME_START,

    /**
     * Doesn't get registered automatically, can be used for setting events at a custom time by doing registerEvents() manually
     */
    NEVER
}