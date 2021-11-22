package dev.emortal.immortal.inventory

import net.minestom.server.inventory.Inventory

abstract class GUI {

    abstract fun createInventory(): Inventory

    val inventory = createInventory()

}