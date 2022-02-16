package dev.emortal.immortal.commands

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.util.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import world.cepi.kstom.command.kommand.Kommand

object ToggleMaintenanceCommand : Kommand({

    default {
        if (!sender.hasLuckPermission("immortal.setwhitelist")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@default
        }

        ImmortalExtension.whitelisted = !ImmortalExtension.whitelisted

        sender.sendMessage(Component.text("${if (ImmortalExtension.whitelisted) "Enabled" else "Disabled"} maintenance mode", NamedTextColor.GREEN))
    }

}, "maintenance")