package emortal.immortal

import net.minestom.server.extensions.Extension

class ImmortalExtension : Extension() {

    override fun initialize() {
        logger.info("[ImmortalExtension] has been enabled!")
    }

    override fun terminate() {
        logger.info("[ImmortalExtension] has been disabled!")
    }

}