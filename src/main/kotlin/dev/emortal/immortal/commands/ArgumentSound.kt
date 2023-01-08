package dev.emortal.immortal.commands

import net.kyori.adventure.key.Key
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentRegistry
import net.minestom.server.sound.SoundEvent

class ArgumentSound(id: String?) : ArgumentRegistry<Key>(id) {
    init {
        suggestionType = SuggestionType.AVAILABLE_SOUNDS
    }

    override fun parser(): String {
        return "minecraft:resource_location"
    }

    override fun getRegistry(value: String): Key {
        return Key.key(value)
    }

    override fun toString(): String {
        return String.format("Sound<%s>", id)
    }
}