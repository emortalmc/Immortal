package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.instance.SharedInstance
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.literal
import world.cepi.kstom.command.kommand.Kommand

object ListCommand : Kommand({

    val instances by literal
    val players by literal

    default {
        Manager.command.execute(sender, "list players")
    }

    syntax(instances) {
        if (!sender.hasLuckPermission("immortal.list")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@syntax
        }

        val message = Component.text()
        val instances = Manager.instance.instances

        message.append(Component.text("${instances.size} instances...\n", NamedTextColor.GOLD))

        instances.forEach {
            val isShared = it is SharedInstance
            val noUnregister = it.hasTag(GameManager.doNotUnregisterTag)
            val name = it.getTag(GameManager.gameNameTag) ?: "[no name]"
            
            message.append(
                Component.text()
                    .append(Component.text("\n - ", NamedTextColor.GRAY))
                    .append(Component.text(name, NamedTextColor.YELLOW))
                    .append(Component.text(" Shared: $isShared,", NamedTextColor.GRAY))
                    .append(Component.text(" No Unregister: $noUnregister", NamedTextColor.GRAY))
                    .append(Component.text(" (${it.uniqueId})", NamedTextColor.DARK_GRAY))
            )
        }

        sender.sendMessage(message)
    }

    syntax(players) {
        if (!sender.hasLuckPermission("immortal.list")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@syntax
        }

        val message = Component.text()
        val players = Manager.connection.onlinePlayers

        message.append(Component.text("${players.size} players...\n", NamedTextColor.GOLD))

        players.forEach {
            message.append(
                Component.text()
                    .append(Component.text("\n - ", NamedTextColor.GRAY))
                    .append(Component.text(it.username, NamedTextColor.YELLOW))
                    .append(Component.text(" (${it.uuid})", NamedTextColor.DARK_GRAY))
            )
        }

        sender.sendMessage(message)
    }

}, "list")