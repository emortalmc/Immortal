package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.util.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand

object CheckForEmptyInstanceCommand : Kommand({

    default {
        if (!sender.hasLuckPermission("immortal.checkemptyinstance")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@default
        }

        var i = 0

        Manager.instance.instances.forEach {
            if (it.players.size == 0) {
                sender.sendMessage(it.getTag(GameManager.gameNameTag) ?: "idk")
                sender.sendMessage(it.getTag(GameManager.gameIdTag).toString())

                if (it.hasTag(GameManager.doNotUnregisterTag)) {
                    sender.sendMessage(Component.text("This instance has asked to not be unregistered."))
                    return@default
                }

                sender.sendMessage(it.getTag(GameManager.gameNameTag) ?: "idk")
                sender.sendMessage(it.getTag(GameManager.gameIdTag).toString())

                Manager.instance.unregisterInstance(it)
                i++
            }
        }

        sender.sendMessage(Component.text("Unregistered ${i} instances", NamedTextColor.GREEN))
    }

}, "cleanupinstance")