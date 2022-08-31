package dev.emortal.immortal.commands

import net.kyori.adventure.sound.Sound
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.number.ArgumentFloat
import world.cepi.kstom.command.arguments.defaultValue

internal object SoundCommand : Command("sound") {

    init {
        val soundArgument = ArgumentSound("sound")
        val volumeArgument = ArgumentFloat("volume").defaultValue(1f)
        val pitchArgument = ArgumentFloat("pitch").defaultValue(1f)

        addSyntax({ sender, ctx ->
            sender.playSound(Sound.sound(ctx.get(soundArgument), Sound.Source.MASTER, ctx.get(volumeArgument), ctx.get(pitchArgument)))
        }, soundArgument, volumeArgument, pitchArgument)
    }

}