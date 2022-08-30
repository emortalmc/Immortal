package dev.emortal.immortal.commands

import dev.emortal.immortal.util.armify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import world.cepi.kstom.Manager

internal object VersionCommand : Command("version", "icanhasminestom", "extensions", "pl", "plugins") {

    init {
        setDefaultExecutor { sender, _ ->
            val extensions = Manager.extension.extensions

            val message = Component.text()
                .append(Component.text("This server is running ", NamedTextColor.GRAY))
                .append(
                    Component.text("Minestom ${MinecraftServer.VERSION_NAME}", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(Component.text("https://github.com/Minestom/Minestom", NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.openUrl("https://github.com/Minestom/Minestom"))
                )
                .append(Component.text("\n\n  → ${extensions.size} extensions:\n", NamedTextColor.LIGHT_PURPLE))

            extensions.forEachIndexed { i,it ->
                if (i != 0) message.append(Component.text(", ", NamedTextColor.DARK_GRAY))

                message.append(
                    Component.text(it.origin.name, NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(
                            Component.text()
                                .append(Component.text("Name: ", NamedTextColor.GRAY))
                                .append(Component.text(it.origin.name, NamedTextColor.GREEN))
                                .append(Component.text("\nVersion: ", NamedTextColor.GRAY))
                                .append(Component.text(it.origin.version, NamedTextColor.GREEN))
                                .also { msg ->
                                    if (it.origin.authors.isNotEmpty()) {
                                        msg.append(Component.text("\nAuthors", NamedTextColor.GRAY))
                                        msg.append(Component.text(it.origin.authors.joinToString(), NamedTextColor.GRAY))
                                    }

                                }

                        ))
                )
            }

            sender.sendMessage(message.armify())
        }
    }

}