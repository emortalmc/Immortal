package dev.emortal.immortal.commands

import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.minestom.server.command.builder.Command

internal object ForceGCCommand : Command("gc") {

    init {
        setCondition { sender, _ ->
            sender.hasLuckPermission("immortal.forcegc")
        }

        setDefaultExecutor { sender, _ ->
            if (!sender.hasLuckPermission("immortal.forcegc")) {
                sender.sendMessage("No permission")
                return@setDefaultExecutor
            }

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
    }

}