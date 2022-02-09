package dev.emortal.immortal.inventory

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.config.GameListing
import dev.emortal.immortal.event.GameDestroyEvent
import dev.emortal.immortal.event.PlayerJoinGameEvent
import dev.emortal.immortal.event.PlayerLeaveGameEvent
import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemHideFlag
import net.minestom.server.item.ItemStack
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.adventure.noItalic
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.item.item
import world.cepi.kstom.util.setItemStacks

object GameSelectorGUI : GUI() {

    override fun createInventory(): Inventory {
        val inventoryTitle = Component.text("Games", NamedTextColor.BLACK)
        val inventory = Inventory(InventoryType.CHEST_4_ROW, inventoryTitle)

        val itemStackMap = mutableMapOf<Int, ItemStack>()

        fun itemFromListing(gameName: String, gameListing: GameListing): ItemStack? {
            val gameClass = GameManager.gameNameToClassMap[gameName] ?: return null
            val gameType = GameManager.registeredGameMap[gameClass] ?: return null
            val games = GameManager.gameMap[gameName]

            val loreList = mutableListOf<String>()
            loreList.add("")
            loreList.addAll(gameListing.description)
            loreList.addAll(listOf(
                "",
                "<dark_gray>/play $gameName",
                "<green>● <bold>${games?.sumOf { it.players.size } ?: 0}</bold> playing"
            ))

            return item(gameListing.item) {
                displayName(gameType.gameTitle.noItalic())
                lore(loreList.map { loreLine -> loreLine.asMini().noItalic() })
                hideFlag(*ItemHideFlag.values())
                setTag(GameManager.gameNameTag, gameName)
            }
        }

        ImmortalExtension.gameListingConfig.gameListings.forEach {
            if (!it.value.itemVisible) return@forEach

            val item = itemFromListing(it.key, it.value) ?: return@forEach
            itemStackMap[it.value.slot] = item
        }

        inventory.setItemStacks(itemStackMap)

        fun refresh(game: Game) {
            val gameName = game.gameTypeInfo.gameName
            val gameListing = ImmortalExtension.gameListingConfig.gameListings[gameName] ?: return
            if (!gameListing.itemVisible) return

            val item = itemFromListing(gameName, gameListing) ?: return
            inventory.setItemStack(gameListing.slot, item)
        }

        Manager.globalEvent.listenOnly<PlayerJoinGameEvent> {
            refresh(getGame())
        }
        Manager.globalEvent.listenOnly<PlayerLeaveGameEvent> {
            refresh(getGame())
        }
        Manager.globalEvent.listenOnly<GameDestroyEvent> {
            refresh(getGame())
        }

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

}