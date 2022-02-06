package dev.emortal.immortal.commands

import dev.emortal.immortal.util.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand
import java.util.*

object InstanceCommand : Kommand({

    onlyPlayers

    val instanceArgument = ArgumentType.Word("instance").map {
        Manager.instance.getInstance(UUID.fromString(it))
    }.suggest { Manager.instance.instances.map { it.uniqueId.toString() } }

    syntax(instanceArgument) {
        val instance = !instanceArgument

        if (!player.hasLuckPermission("immortal.instance")) {
            player.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@syntax
        }

        if (instance == null) {
            sender.sendMessage(Component.text("Invalid instance", NamedTextColor.RED))
            return@syntax
        }

        player.sendMessage("Teleporting...")
        player.setInstance(instance)
    }

}, "instance")