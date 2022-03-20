package dev.emortal.immortal.util

import org.redisson.Redisson
import org.redisson.config.Config


object RedisStorage {

    val redisson = Redisson.create(Config().also { it.useSingleServer().setAddress("redis://localhost:6379").setClientName("Proxy") })

}