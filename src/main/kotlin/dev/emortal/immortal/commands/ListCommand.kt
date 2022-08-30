package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.instance.SharedInstance
import world.cepi.kstom.Manager

internal object ListCommand : Command("list") {

    init {
        val instancesArgument = ArgumentType.Literal("instances")
        val playersArgument = ArgumentType.Literal("players")

        addSyntax({ sender, _ ->
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
        }, instancesArgument)

        addSyntax({ sender, _ ->
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
        }, playersArgument)
    }

}