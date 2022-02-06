package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.util.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import world.cepi.kstom.command.kommand.Kommand

object ForceStartCommand : Kommand({

    onlyPlayers

    default {
        if (!player.hasLuckPermission("immortal.forcestart")) {
            player.sendMessage(Component.text("No permission", NamedTextColor.RED))
            return@default
        }

        if (player.game == null) {
            player.sendMessage(Component.text("You are not in a game", NamedTextColor.RED))
            return@default
        }

        player.game!!.start()
    }

}, "forcestart")