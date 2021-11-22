package dev.emortal.immortal.inventory

import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.Material
import net.minestom.server.item.metadata.PlayerHeadMeta
import world.cepi.kstom.item.item
import world.cepi.kstom.util.setItemStack

class SpectatingGUI : GUI() {

    override fun createInventory(): Inventory {
        val inventory = Inventory(InventoryType.CHEST_6_ROW, "Spectate player")

        return inventory
    }

    fun refresh(players: Collection<Player>) {
        inventory.clear()

        players.forEachIndexed { i, player ->

            val headItemStack = item(Material.PLAYER_HEAD) {

                val meta = this as PlayerHeadMeta.Builder

                meta.skullOwner(player.uuid)
                meta.playerSkin(player.skin)
            }

            inventory.setItemStack(i % 8, i / 9, headItemStack)
        }
    }

}