package dev.emortal.immortal.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID

object SignHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID = NamespaceID.from(Key.key("minecraft:sign"))
    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        val tags = mutableListOf<Tag<*>>()

        tags.add(Tag.Byte("GlowingText"))
        tags.add(Tag.String("Color"))
        tags.add(Tag.String("Text1"))
        tags.add(Tag.String("Text2"))
        tags.add(Tag.String("Text3"))
        tags.add(Tag.String("Text4"))

        return tags
    }
}