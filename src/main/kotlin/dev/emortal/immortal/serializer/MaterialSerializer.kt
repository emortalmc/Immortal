package dev.emortal.immortal.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minestom.server.coordinate.Pos
import net.minestom.server.item.Material

@Serializer(forClass = Pos::class)
@OptIn(ExperimentalSerializationApi::class)
object MaterialSerializer : KSerializer<Material> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("material", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Material) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Material {
        return Material.fromNamespaceId(decoder.decodeString())!!
    }
}