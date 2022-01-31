package dev.emortal.immortal.inventory

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.inventory.GUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemHideFlag
import net.minestom.server.item.ItemStack
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.adventure.noItalic
import world.cepi.kstom.item.item
import world.cepi.kstom.util.setItemStacks

object GameSelectorGUI : GUI() {

    override fun createInventory(): Inventory {
        val inventoryTitle = Component.text("Games", NamedTextColor.BLACK)
        val inventory = Inventory(InventoryType.CHEST_4_ROW, inventoryTitle)

        val itemStackMap = mutableMapOf<Int, ItemStack>()

        ImmortalExtension.gameListingConfig.gameListings.forEach {
            if (!it.value.itemVisible) return@forEach

            val gameClass = GameManager.gameNameToClassMap[it.key] ?: return@forEach
            val gameType = GameManager.registeredGameMap[gameClass] ?: return@forEach
            val games = GameManager.gameMap[it.key] ?: return@forEach

            val loreList = it.value.description.toMutableList()
            loreList.addAll(listOf(
                "",
                "<dark_gray>/play ${it.key}",
                "<green>● <bold>${games.sumOf { it.players.size }}</bold> playing"
            ))

            itemStackMap[it.value.slot] = item(it.value.item) {
                displayName(gameType.gameTitle.noItalic())
                lore(loreList.map { loreLine -> loreLine.asMini().noItalic() })
                hideFlag(*ItemHideFlag.values())
                setTag(GameManager.gameNameTag, it.key)
            }
        }

        inventory.setItemStacks(itemStackMap)

        inventory.addInventoryCondition { player, _, _, inventoryConditionResult ->
            inventoryConditionResult.isCancel = true

            if (inventoryConditionResult.clickedItem == ItemStack.AIR) return@addInventoryCondition

            if (inventoryConditionResult.clickedItem.hasTag(GameManager.gameNameTag)) {
                val gameName = inventoryConditionResult.clickedItem.getTag(GameManager.gameNameTag) ?: return@addInventoryCondition
                player.joinGameOrNew(gameName)
                player.closeInventory()
            }
        }

        return inventory
    }

    override fun refresh() {
        val itemStackMap = mutableMapOf<Int, ItemStack>()

        ImmortalExtension.gameListingConfig.gameListings.forEach {
            if (!it.value.itemVisible) return@forEach

            val gameClass = GameManager.gameNameToClassMap[it.key] ?: return@forEach
            val gameType = GameManager.registeredGameMap[gameClass] ?: return@forEach
            val games = GameManager.gameMap[it.key] ?: return@forEach

            val loreList = it.value.description.toMutableList()
            loreList.addAll(listOf(
                "",
                "<dark_gray>/play ${it.key}",
                "<green>● <bold>${games.sumOf { game -> game.players.size }}</bold> playing"
            ))

            itemStackMap[it.value.slot] = item(it.value.item) {
                displayName(gameType.gameTitle.noItalic())
                lore(loreList.map { loreLine -> loreLine.asMini().noItalic() })
                hideFlag(*ItemHideFlag.values())
                setTag(GameManager.gameNameTag, it.key)
            }
        }

        inventory.setItemStacks(itemStackMap)
    }

}