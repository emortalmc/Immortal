package dev.emortal.immortal.inventory

import dev.emortal.immortal.util.DefaultFontInfo
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.metadata.PlayerHeadMeta
import net.minestom.server.tag.Tag
import world.cepi.kstom.item.item
import world.cepi.kstom.util.asPlayer
import world.cepi.kstom.util.getSlotNumber
import java.util.*

class SpectatingGUI : GUI() {

    companion object {
        val playerUUIDTag = Tag.String("uuid")
    }

    override fun createInventory(): Inventory {
        val inventory = Inventory(InventoryType.CHEST_6_ROW, "Spectate player")

        inventory.addInventoryCondition { player, slot, clickType, inventoryConditionResult ->
            inventoryConditionResult.isCancel = true

            if (inventory.getItemStack(slot).material() != Material.PLAYER_HEAD) return@addInventoryCondition

            val clickedPlayer = UUID.fromString(inventory.getItemStack(slot).getTag(playerUUIDTag)).asPlayer()
                ?: return@addInventoryCondition

            player.teleport(clickedPlayer.position)
        }

        return inventory
    }

    fun refresh(players: Collection<Player>) {
        val contents = inventory.itemStacks

        players.forEachIndexed { i, player ->

            val headItemStack = ItemStack.builder(Material.PLAYER_HEAD).meta(PlayerHeadMeta::class.java) {
                it.skullOwner(player.uuid)
                it.playerSkin(PlayerSkin.fromUuid(player.uuid.toString()))
                it.displayName(Component.text(player.username, NamedTextColor.YELLOW))
                it.setTag(playerUUIDTag, player.uuid.toString())
            }.build()

            contents[inventory.getSlotNumber(i % 8, 1 + (i / 9))] = headItemStack
        }

        inventory.copyContents(contents)
    }

}