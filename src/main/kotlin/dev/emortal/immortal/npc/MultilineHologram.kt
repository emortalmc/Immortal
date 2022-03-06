package dev.emortal.immortal.npc

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.instance.Instance

class MultilineHologram(val components: MutableList<Component>) {

    val entities = mutableListOf<Entity>()

    fun setInstance(position: Pos, instance: Instance) {
        components.forEachIndexed { i, it ->
            val entity = Entity(EntityType.ARMOR_STAND)
            val meta = entity.entityMeta as ArmorStandMeta
            meta.setNotifyAboutChanges(false)
            //meta.radius = 0f
            meta.isSmall = true
            meta.isHasNoBasePlate = true
            meta.isMarker = true
            meta.isInvisible = true
            meta.isCustomNameVisible = true
            meta.customName = it
            meta.isHasNoGravity = true
            meta.setNotifyAboutChanges(true)

            entity.setInstance(instance, position.add(0.0, 0.5 + (0.30 * (components.size - i)), 0.0))
            entities.add(entity)
        }
    }

    fun setLine(index: Int, component: Component) {
        components[index] = component
        entities[index].entityMeta.customName = component
    }

    fun remove() {
        entities.forEach(Entity::remove)
    }

}