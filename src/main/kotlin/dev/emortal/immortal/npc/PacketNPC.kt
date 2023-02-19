package dev.emortal.immortal.npc

import dev.emortal.immortal.util.sendServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.*
import net.minestom.server.network.packet.server.play.*
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.PacketUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class PacketNPC(val position: Pos, val hologramLines: List<Component>, val gameName: String, val playerSkin: PlayerSkin? = null, val entityType: EntityType = EntityType.PLAYER) {

    private val viewers: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    private val taskMap = ConcurrentHashMap<UUID, Task>()

    companion object {
        val viewerMap = ConcurrentHashMap<UUID, CopyOnWriteArrayList<PacketNPC>>()

        val createTeamPacket = MinecraftServer.getTeamManager().createBuilder("npcTeam")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build()
            .createTeamsCreationPacket()
    }

    val playerId = Entity.generateId()
    private val prop = if (playerSkin == null) listOf() else listOf(
        PlayerInfoUpdatePacket.Property(
            "textures",
            playerSkin.textures(),
            playerSkin.signature()
        )
    )
    private val uuid = UUID.randomUUID()

    private val playerInfo = PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Entry(uuid, gameName, prop, false,0, GameMode.CREATIVE, Component.empty(), null))
    private val spawnPlayer = SpawnPlayerPacket(playerId, uuid, position)
    private val teamPacket = TeamsPacket("npcTeam", TeamsPacket.AddEntitiesToTeamAction(listOf(gameName)))
    private val metaPacket = EntityMetaDataPacket(playerId, mapOf(17 to Metadata.Byte(127 /*All layers enabled*/)))

    fun addViewer(viewer: Player) {
        viewers.add(viewer)
        if (!viewerMap.containsKey(viewer.uuid)) viewerMap[viewer.uuid] = CopyOnWriteArrayList()
        viewerMap[viewer.uuid]?.add(this)


        if (entityType == EntityType.PLAYER) {
            viewer.sendPacket(playerInfo)
            viewer.sendPacket(spawnPlayer)
            viewer.sendPacket(metaPacket)
            viewer.sendPacket(createTeamPacket)
            viewer.sendPacket(teamPacket)
        } else {
            val entitySpawn = SpawnEntityPacket(playerId, uuid, entityType.id(), position, position.yaw, 0, 0, 0, 0)

            viewer.sendPacket(entitySpawn)
        }

        taskMap[viewer.uuid] = viewer.scheduler().buildTask {
            val lookFromPos = position.add(0.0, entityType.height(), 0.0)
            val lookToPos = viewer.position.add(0.0, if (viewer.isSneaking) 1.5 else 1.8, 0.0)

            if (lookFromPos.distanceSquared(lookToPos) > 10*10) return@buildTask
            val pos = lookFromPos.withDirection(lookToPos.sub(lookFromPos))

            val lookPacket = EntityRotationPacket(playerId, pos.yaw, pos.pitch, true)
            val headLook = EntityHeadLookPacket(playerId, pos.yaw)
            viewer.sendPacket(lookPacket)
            viewer.sendPacket(headLook)
        }.delay(TaskSchedule.seconds(4)).repeat(TaskSchedule.tick(3)).schedule()
    }

    fun removeViewer(viewer: Player) {
        viewers.remove(viewer)
        viewerMap[viewer.uuid]?.remove(this)

        viewer.sendPacket(DestroyEntitiesPacket(playerId))
        taskMap[viewer.uuid]?.cancel()
        taskMap.remove(viewer.uuid)
    }

    fun destroy() {
        PacketUtils.sendGroupedPacket(viewers, DestroyEntitiesPacket(playerId))

        viewers.forEach {
            viewerMap[it.uuid]?.remove(this)
        }
        taskMap.values.forEach {
            it.cancel()
        }
        viewers.clear()
        taskMap.clear()
    }

    fun onClick(clicker: Player) = runBlocking {
        launch {
            clicker.sendServer(gameName)
        }
    }

}