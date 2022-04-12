package dev.emortal.immortal.util

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.Generator

object SuperflatGenerator : Generator {

    override fun generate(unit: GenerationUnit) {
        unit.modifier().fillHeight(0, 1, Block.BEDROCK)
        unit.modifier().fillHeight(1, 4, Block.DIRT)
        unit.modifier().fillHeight(4, 5, Block.GRASS_BLOCK)
    }

}