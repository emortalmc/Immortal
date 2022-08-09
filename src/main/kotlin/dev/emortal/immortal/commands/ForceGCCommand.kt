package dev.emortal.immortal.commands

import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import world.cepi.kstom.command.kommand.Kommand

object ForceGCCommand : Kommand({

    condition {
        sender.hasLuckPermission("immortal.forcegc")
    }

    playerCallbackFailMessage = {
        it.sendMessage(Component.text("No permission", NamedTextColor.RED))
    }

    default {

        val totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        val ramUsage = totalMem - freeMem

        sender.sendMessage("Performing GC... ${ramUsage}mb")

        System.gc()

        val totalMem2 = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val freeMem2 = Runtime.getRuntime().freeMemory() / 1024 / 1024
        val ramUsage2 = totalMem2 - freeMem2

        sender.sendMessage("Done. ${ramUsage2}mb")
    }

}, "gc")