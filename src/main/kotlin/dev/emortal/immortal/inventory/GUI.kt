package dev.emortal.immortal.inventory

import net.minestom.server.inventory.Inventory

abstract class GUI {

    abstract fun createInventory(): Inventory

    var inventory = createInventory()

    open fun refresh() {
        inventory = createInventory()
    }

}