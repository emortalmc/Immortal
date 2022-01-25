package dev.emortal.immortal.config

import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.item.Material
import world.cepi.kstom.serializer.ComponentSerializer
import world.cepi.kstom.serializer.MaterialSerializer
import world.cepi.kstom.serializer.PositionSerializer

@Serializable
class GameListing(
    val description: Array<String> = arrayOf(),
    val itemVisible: Boolean = false,
    @Serializable(with = MaterialSerializer::class)
    val item: Material = Material.GRASS_BLOCK,
    val slot: Int = 0,
    val npcVisible: Boolean = false,
    @Serializable(with = PositionSerializer::class)
    val npcPosition: Pos = Pos.ZERO,
    val npcEntityType: String = EntityType.PLAYER.toString(),
    val npcTitles: Array<String> = arrayOf("Example text!"),
    val npcSkinValue: String = "",
    val npcSkinSignature: String = ""
)