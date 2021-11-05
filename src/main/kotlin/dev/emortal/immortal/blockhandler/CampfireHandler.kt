package dev.emortal.immortal.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID
import org.jglrxavpok.hephaistos.nbt.NBTCompound
import org.jglrxavpok.hephaistos.nbt.NBTList

object CampfireHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID = NamespaceID.from(Key.key("minecraft:skull"))
    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return mutableListOf(
            Tag.IntArray("CookingTimes"),
            Tag.IntArray("CookingTotalTimes"),
            Tag.NBT<NBTList<*>>("Items")
        )
    }
}