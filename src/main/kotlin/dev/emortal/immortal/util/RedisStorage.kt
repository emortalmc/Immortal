package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPooled


object RedisStorage {

    val jedisPool = JedisPool("localhost", 6379)
    val pool get() = jedisPool.resource

    init {
        pool.clientSetname("Immortal-${ImmortalExtension.gameConfig.serverName}")
    }

}