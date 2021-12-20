package dev.emortal.immortal.commands

import net.kyori.adventure.sound.Sound
import net.minestom.server.command.builder.arguments.ArgumentWord
import net.minestom.server.command.builder.arguments.number.ArgumentFloat
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object SoundCommand : Kommand({
     onlyPlayers

    val soundArgument = ArgumentWord("sound")
        .map { input: String -> SoundEvent.fromNamespaceId(input) }
        .suggest { SoundEvent.values().map { it.namespace().value() } }
    val volumeArgument = ArgumentFloat("volume")
    val pitchArgument = ArgumentFloat("pitch")

    syntax(soundArgument, volumeArgument, pitchArgument) {
        player.playSound(Sound.sound((!soundArgument)!!, Sound.Source.MASTER, !volumeArgument, !pitchArgument))
    }

}, "sound")