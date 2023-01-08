package dev.emortal.immortal.commands

import net.kyori.adventure.sound.Sound
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.number.ArgumentFloat

internal object SoundCommand : Command("sound") {

    init {
        val soundArgument = ArgumentSound("sound")
        val volumeArgument = ArgumentFloat("volume").setDefaultValue { 1f }
        val pitchArgument = ArgumentFloat("pitch").setDefaultValue { 1f }

        addSyntax({ sender, ctx ->
            sender.playSound(Sound.sound(ctx.get(soundArgument), Sound.Source.MASTER, ctx.get(volumeArgument), ctx.get(pitchArgument)))
        }, soundArgument, volumeArgument, pitchArgument)
    }

}