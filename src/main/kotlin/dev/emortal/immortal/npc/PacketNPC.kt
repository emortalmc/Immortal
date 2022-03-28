package dev.emortal.immortal.npc

import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.sendServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.*
import net.minestom.server.network.packet.server.play.*
import net.minestom.server.utils.PacketUtils
import world.cepi.kstom.Manager
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PacketNPC(val position: Pos, val hologramLines: List<Component>, val gameName: String, val playerSkin: PlayerSkin? = null, val entityType: EntityType = EntityType.PLAYER) {

    private val viewers: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    //private var lookingTaskMap = ConcurrentHashMap<UUID, MutableList<MinestomRunnable>>()

    companion object {
        val viewerMap = ConcurrentHashMap<UUID, MutableList<PacketNPC>>()
        val npcIdMap = ConcurrentHashMap<Int, PacketNPC>()

        val createTeamPacket = Manager.team.createBuilder("npcTeam")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build()
            .createTeamsCreationPacket()

    }

    val playerId = Entity.generateId()
    val uuid = UUID.randomUUID()

    fun addViewer(viewer: Player) {
        viewers.add(viewer)
        val map = viewerMap[viewer.uuid] ?: mutableListOf()
        map.add(this)
        viewerMap[viewer.uuid] = map

        if (entityType == EntityType.PLAYER) {
            val prop = if (playerSkin == null) listOf() else listOf(
                PlayerInfoPacket.AddPlayer.Property(
                    "textures",
                    playerSkin.textures(),
                    playerSkin.signature()
                )
            )

            val playerInfo = PlayerInfoPacket(PlayerInfoPacket.Action.ADD_PLAYER, PlayerInfoPacket.AddPlayer(uuid, gameName, prop, GameMode.CREATIVE, 0, Component.empty()))
            val spawnPlayer = SpawnPlayerPacket(playerId, uuid, position)
            val teamPacket = TeamsPacket("npcTeam", TeamsPacket.AddEntitiesToTeamAction(listOf(gameName)))
            val metaPacket = EntityMetaDataPacket(playerId, listOf(Metadata.Entry(17, Metadata.Byte(127 /*All layers enabled*/))))
            val removeFromList = PlayerInfoPacket(PlayerInfoPacket.Action.REMOVE_PLAYER, PlayerInfoPacket.RemovePlayer(uuid))


            viewer.sendPacket(playerInfo)
            viewer.sendPacket(spawnPlayer)
            viewer.sendPacket(metaPacket)
            viewer.sendPacket(createTeamPacket)
            viewer.sendPacket(teamPacket)

            if (position.yaw != 0f) {
                val rotatePacket = EntityHeadLookPacket(playerId, position.yaw)

                viewer.sendPacket(rotatePacket)
            }

            Manager.scheduler.buildTask {
                viewer.sendPacket(removeFromList)
            }.delay(Duration.ofSeconds(3)).schedule()
        } else {
            val entitySpawn = SpawnEntityPacket(playerId, uuid, entityType.id(), position, 0, 0, 0, 0)

            viewer.sendPacket(entitySpawn)
        }

        object : MinestomRunnable(coroutineScope = coroutineScope, delay = Duration.ofSeconds(3), repeat = Duration.ofMillis(150)) {
            override suspend fun run() {
                val lookFromPos = position.add(0.0, entityType.height(), 0.0)
                val lookToPos = viewer.position.add(0.0, EntityType.PLAYER.height(), 0.0)

                if (lookFromPos.distanceSquared(lookToPos) > 10*10) return
                val pos = lookFromPos.withDirection(lookToPos.sub(lookFromPos))

                val lookPacket = EntityRotationPacket(playerId, pos.yaw, pos.pitch, true)
                val headLook = EntityHeadLookPacket(playerId, pos.yaw)
                viewer.sendPacket(lookPacket)
                viewer.sendPacket(headLook)
            }
        }

        npcIdMap[playerId] = this
    }

    fun removeViewer(viewer: Player) {
        viewers.remove(viewer)
        viewerMap[viewer.uuid]?.remove(this)
        //lookingTaskMap[viewer.uuid]?.forEach {
        //    it.cancel()
        //}
        //lookingTaskMap.remove(viewer.uuid)

        viewer.sendPackets(DestroyEntitiesPacket(playerId))
    }

    fun destroy() {
        PacketUtils.sendGroupedPacket(viewers, DestroyEntitiesPacket(playerId))

        try {
            coroutineScope.cancel()
        } catch (ignored: Throwable) {
        }
        //lookingTaskMap.clear()
        npcIdMap.remove(playerId)
        viewers.forEach {
            viewerMap[it.uuid]?.remove(this)
        }
    }

    fun onClick(clicker: Player) {
        clicker.sendServer(gameName)
    }

}