package dev.emortal.immortal.util

import redis.clients.jedis.JedisPooled


object RedisStorage {

    val pool = JedisPooled("localhost", 6379)

}