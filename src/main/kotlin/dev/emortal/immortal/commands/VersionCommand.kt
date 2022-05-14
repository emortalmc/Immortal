package dev.emortal.immortal.commands

import dev.emortal.immortal.util.armify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand

object VersionCommand : Kommand({

    default {
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
            message.append(
                Component.text(it.origin.name, NamedTextColor.GREEN)
                    .hoverEvent(HoverEvent.showText(
                        Component.text()
                            .append(Component.text("Name: ", NamedTextColor.GRAY))
                            .append(Component.text(it.origin.name, NamedTextColor.GREEN))

                            .also { msg ->
                                if (it.origin.version != "1.0.0") {
                                    msg.append(Component.text("\nVersion: ", NamedTextColor.GRAY))
                                    msg.append(Component.text(it.origin.version, NamedTextColor.GREEN))
                                }
                                if (it.origin.authors.isNotEmpty()) {
                                    msg.append(Component.text("\nAuthors", NamedTextColor.GRAY))
                                    msg.append(Component.text(it.origin.authors.joinToString(), NamedTextColor.GRAY))
                                }

                            }

                    ))
            )
            if (i != extensions.size - 1) message.append(Component.text(", ", NamedTextColor.DARK_GRAY))
        }

        sender.sendMessage(message.armify())
    }

}, "version", "icanhasminestom", "extensions")