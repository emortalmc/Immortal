package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisStorage {

    val jedisPool = JedisPool(JedisPoolConfig().also { it.jmxNameBase = ImmortalExtension.gameConfig.serverName }, "localhost", 6379)

}