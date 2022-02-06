package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.*
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameListing
import dev.emortal.immortal.config.GameListingConfig
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.util.PacketNPC
import dev.emortal.immortal.util.PermissionUtils.prefix
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Team
import net.minestom.server.scoreboard.TeamBuilder
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.register
import java.nio.file.Path

class ImmortalExtension : Extension() {

    companion object {
        val defaultTeamMap = mutableMapOf<Player, Team>()

        lateinit var luckperms: LuckPerms

        lateinit var gameListingConfig: GameListingConfig
        val gameListingPath = Path.of("./gameListings.json")
    }

    override fun initialize() {
        luckperms = LuckPermsProvider.get()

        gameListingConfig = ConfigHelper.initConfigFile(gameListingPath, GameListingConfig())

        GameManager.registeredGameMap.forEach {
            if (!gameListingConfig.gameListings.contains(it.value.gameName)) {
                gameListingConfig.gameListings[it.value.gameName] = GameListing()
            }
        }

        ConfigHelper.writeObjectToPath(gameListingPath, gameListingConfig)

        eventNode.listenOnly<PlayerChunkUnloadEvent> {
            val chunk = instance.getChunk(chunkX, chunkZ) ?: return@listenOnly

            if (chunk.viewers.isEmpty()) {
                instance.unloadChunk(chunkX, chunkZ)
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            player.game?.removePlayer(player)
            player.game?.removeSpectator(player)
            GameManager.playerGameMap.remove(player)

            if (instance.players.size == 0) {
                Manager.instance.unregisterInstance(instance)
            }
        }

        eventNode.listenOnly<PlayerPacketEvent> {
            if (packet is ClientInteractEntityPacket) {
                val packet = packet as ClientInteractEntityPacket
                if (packet.type != ClientInteractEntityPacket.Interact(Player.Hand.MAIN)) return@listenOnly
                PacketNPC.npcIdMap[packet.targetId]?.onClick(player)
            }
        }

        val defaultTeam = TeamBuilder("defaultTeam", Manager.team)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .build()
        eventNode.listenOnly<PlayerSpawnEvent> {
            if (this.isFirstSpawn) {
                player.team = defaultTeam
                defaultTeamMap[player] = defaultTeam

                val prefix = player.prefix ?: return@listenOnly
                this.player.displayName = "$prefix ${player.username}".asMini()
            } else {
                val viewingNpcs = (PacketNPC.viewerMap[player.uuid] ?: return@listenOnly).toMutableList()
                viewingNpcs.forEach {
                    it.removeViewer(player)
                }
            }
        }

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            if (player.gameMode == GameMode.ADVENTURE || player.gameMode == GameMode.SPECTATOR) {
                isCancelled = true
            }
        }
        eventNode.listenOnly<PlayerBlockBreakEvent> {
            if (player.gameMode == GameMode.ADVENTURE || player.gameMode == GameMode.SPECTATOR) {
                isCancelled = true
            }
        }

        val dimensionType = DimensionType.builder(NamespaceID.from("fullbright"))
            .ambientLight(1f)
            .build()
        Manager.dimensionType.addDimension(dimensionType)

        SignHandler.register("minecraft:sign")
        SkullHandler.register("minecraft:skull")

        ForceStartCommand.register()
        PlayCommand.register()
        SoundCommand.register()
        SpectateCommand.register()
        InstanceCommand.register()

        logger.info("[Immortal] Initialized!")
    }

    override fun terminate() {
        //ForceStartCommand.unregister()
        PlayCommand.unregister()
        SoundCommand.unregister()
        SpectateCommand.unregister()

        //InstanceCommand.unregister()

        logger.info("[Immortal] Terminated!")
    }

}