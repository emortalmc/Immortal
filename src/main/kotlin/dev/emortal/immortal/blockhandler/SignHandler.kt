package dev.emortal.immortal.blockhandler

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import net.minestom.server.utils.NamespaceID

object SignHandler : BlockHandler {
    override fun getNamespaceId(): NamespaceID = NamespaceID.from(Key.key("minecraft:sign"))
    override fun getBlockEntityTags(): MutableCollection<Tag<*>> {
        return mutableListOf(
            Tag.Byte("GlowingText"),
            Tag.String("Color"),
            Tag.String("Text1"),
            Tag.String("Text2"),
            Tag.String("Text3"),
            Tag.String("Text4"),
        )
    }
}