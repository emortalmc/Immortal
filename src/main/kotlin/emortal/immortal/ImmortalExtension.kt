package emortal.immortal

import net.minestom.server.extensions.Extension
import world.cepi.kstom.command.register

class ImmortalExtension : Extension() {

    override fun initialize() {
        logger.info("[ImmortalExtension] has been enabled!")

        PlayCommand.register()
    }

    override fun terminate() {
        logger.info("[ImmortalExtension] has been disabled!")
    }

}