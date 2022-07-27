package dev.emortal.immortal.commands

import net.kyori.adventure.sound.Sound
import net.minestom.server.command.builder.arguments.number.ArgumentFloat
import world.cepi.kstom.command.arguments.ArgumentSound
import world.cepi.kstom.command.arguments.defaultValue
import world.cepi.kstom.command.kommand.Kommand

object SoundCommand : Kommand({

    onlyPlayers

    val soundArgument = ArgumentSound("sound")
    val volumeArgument = ArgumentFloat("volume").defaultValue(1f)
    val pitchArgument = ArgumentFloat("pitch").defaultValue(1f)

    syntax(soundArgument, volumeArgument, pitchArgument) {
        player.playSound(Sound.sound((!soundArgument)!!, Sound.Source.MASTER, !volumeArgument, !pitchArgument))
    }

}, "sound")