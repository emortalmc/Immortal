package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import org.redisson.Redisson
import org.redisson.config.Config


object RedisStorage {

    val redisson = Redisson.create(Config().also { it.useSingleServer().setAddress(ImmortalExtension.gameConfig.address).setClientName(ImmortalExtension.gameConfig.serverName) })

}