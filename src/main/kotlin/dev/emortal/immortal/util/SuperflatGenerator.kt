package dev.emortal.immortal.util

import net.minestom.server.instance.ChunkGenerator
import net.minestom.server.instance.ChunkPopulator
import net.minestom.server.instance.batch.ChunkBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.world.biomes.Biome
import java.util.*

object SuperflatGenerator : ChunkGenerator {
    override fun generateChunkData(batch: ChunkBatch, chunkX: Int, chunkZ: Int) {
        for (batchX in 0 until 16) {
            for (batchZ in 0 until 16) {
                for (y in 0..4) {

                    val x = batchX + (chunkX * 16)
                    val z = batchZ + (chunkZ * 16)

                    if (y == 0) {
                        batch.setBlock(x, y, z, Block.BEDROCK)
                    } else if (y == 4) {
                        batch.setBlock(x, y, z, Block.GRASS_BLOCK)
                    } else {
                        batch.setBlock(x, y, z, Block.DIRT)
                    }

                }
            }

        }

    }
    override fun fillBiomes(biomes: Array<out Biome>, chunkX: Int, chunkZ: Int) = Arrays.fill(biomes, Biome.PLAINS)
    override fun getPopulators(): MutableList<ChunkPopulator>? = null
}