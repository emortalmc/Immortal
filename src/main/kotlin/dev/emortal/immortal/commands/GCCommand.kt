package dev.emortal.immortal.commands

import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import world.cepi.kstom.command.kommand.Kommand

object GCCommand : Kommand({

    default {
        if (!sender.hasLuckPermission("immortal.doagc")) {
            sender.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@default
        }

        sender.sendMessage("Doing a GC")
        System.gc()
    }

}, "doagc")