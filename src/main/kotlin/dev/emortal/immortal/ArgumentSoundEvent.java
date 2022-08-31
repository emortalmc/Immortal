package dev.emortal.immortal;

import net.minestom.server.command.builder.arguments.minecraft.SuggestionType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentRegistry;
import net.minestom.server.sound.SoundEvent;

/**
 * Represents an argument giving an {@link ArgumentSoundEvent}.
 */
public class ArgumentSoundEvent extends ArgumentRegistry<SoundEvent> {

    public ArgumentSoundEvent(String id) {
        super(id);
        suggestionType = SuggestionType.AVAILABLE_SOUNDS;
    }

    @Override
    public String parser() {
        return "minecraft:resource_location";
    }

    @Override
    public SoundEvent getRegistry(String value) {
        return SoundEvent.fromNamespaceId(value);
    }

    @Override
    public String toString() {
        return String.format("SoundEvent<%s>", getId());
    }
}
