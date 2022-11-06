package dev.emortal.immortal.util

/*
class VanillaExplosionSupplier : ExplosionSupplier {
    override fun createExplosion(
        centerX: Float, centerY: Float, centerZ: Float,
        strength: Float, additionalData: NBTCompound?
    ): Explosion {
        return object : Explosion(centerX, centerY, centerZ, strength) {
            private val playerKnockback: MutableMap<Player, Vec> = HashMap<Player, Vec>()
            protected override fun prepare(instance: Instance): List<Point> {
                val blocks: MutableList<Point> = ArrayList()
                val random = ThreadLocalRandom.current()
                var breakBlocks = true
                if (additionalData != null && additionalData.contains("breakBlocks")) breakBlocks =
                    Objects.requireNonNull<Byte>(additionalData.getByte("breakBlocks")) == 1.toByte()
                if (breakBlocks) {
                    for (x in 0..15) {
                        for (y in 0..15) {
                            for (z in 0..15) {
                                if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
                                    var xLength = (x.toFloat() / 15.0f * 2.0f - 1.0f).toDouble()
                                    var yLength = (y.toFloat() / 15.0f * 2.0f - 1.0f).toDouble()
                                    var zLength = (z.toFloat() / 15.0f * 2.0f - 1.0f).toDouble()
                                    val length = Math.sqrt(xLength * xLength + yLength * yLength + zLength * zLength)
                                    xLength /= length
                                    yLength /= length
                                    zLength /= length
                                    var centerX: Double = this.getCenterX().toDouble()
                                    var centerY: Double = this.getCenterY().toDouble()
                                    var centerZ: Double = this.getCenterZ().toDouble()
                                    var strengthLeft: Float = this.getStrength() * (0.7f + random.nextFloat() * 0.6f)
                                    while (strengthLeft > 0.0f) {
                                        val position = Vec(centerX, centerY, centerZ)
                                        val block = instance.getBlock(position)
                                        if (!block.isAir) {
                                            val explosionResistance = block.registry().explosionResistance()
                                            strengthLeft -= ((explosionResistance + 0.3f) * 0.3f).toFloat()
                                            if (strengthLeft > 0.0f) {
                                                val blockPosition: Vec = position.apply(Vec.Operator.FLOOR)
                                                if (!blocks.contains(blockPosition)) {
                                                    blocks.add(blockPosition)
                                                }
                                            }
                                        }
                                        centerX += xLength * 0.30000001192092896
                                        centerY += yLength * 0.30000001192092896
                                        centerZ += zLength * 0.30000001192092896
                                        strengthLeft -= 0.225f
                                    }
                                }
                            }
                        }
                    }
                }

                // Blocks list may be modified during the event call
                val explosionEvent = ExplosionEvent(instance, blocks)
                EventDispatcher.call(explosionEvent)
                if (explosionEvent.isCancelled()) return null
                val strength: Double = (this.getStrength() * 2.0f).toDouble()
                val minX_ = Math.floor(this.getCenterX() - strength - 1.0).toInt()
                val maxX_ = Math.floor(this.getCenterX() + strength + 1.0).toInt()
                val minY_ = Math.floor(this.getCenterY() - strength - 1.0).toInt()
                val maxY_ = Math.floor(this.getCenterY() + strength + 1.0).toInt()
                val minZ_ = Math.floor(this.getCenterZ() - strength - 1.0).toInt()
                val maxZ_ = Math.floor(this.getCenterZ() + strength + 1.0).toInt()
                val minX = Math.min(minX_, maxX_)
                val maxX = Math.max(minX_, maxX_)
                val minY = Math.min(minY_, maxY_)
                val maxY = Math.max(minY_, maxY_)
                val minZ = Math.min(minZ_, maxZ_)
                val maxZ = Math.max(minZ_, maxZ_)
                val explosionBox = BoundingBox(
                    (
                            maxX - minX).toDouble(),
                    (
                            maxY - minY).toDouble(),
                    (
                            maxZ - minZ
                            ).toDouble()
                )
                val src =
                    Vec(getCenterX().toDouble(), getCenterY() - explosionBox.height() / 2, getCenterZ().toDouble())
                val entities = instance.entities
                    .filter { explosionBox.intersectEntity(src, it) }
                val centerPoint =
                    Vec(this.getCenterX().toDouble(), this.getCenterY().toDouble(), this.getCenterZ().toDouble())
                var anchor = false
                if (additionalData != null && additionalData.contains("anchor")) {
                    anchor = java.lang.Boolean.TRUE == additionalData.getBoolean("anchor")
                }
                for (entity in entities) {
                    var currentStrength = entity.position.distance(centerPoint) / strength
                    if (currentStrength <= 1.0) {
                        var dx: Double = entity.position.x() - this.getCenterX()
                        var dy: Double =
                            (if (entity.entityType === EntityType.TNT) entity.position.y() else entity.position.y() + entity.eyeHeight) - this.getCenterY()
                        var dz: Double = entity.position.z() - this.getCenterZ()
                        val distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
                        if (distance != 0.0) {
                            dx /= distance
                            dy /= distance
                            dz /= distance
                            val exposure = getExposure(centerPoint, entity)
                            currentStrength = (1.0 - currentStrength) * exposure
                            val damage = (((currentStrength * currentStrength + currentStrength)
                                    / 2.0) * 7.0 * strength + 1.0).toFloat()
                            var knockback = currentStrength
                            if (entity is LivingEntity) {
                                if (!entity.damage(damageType, damage)) continue
                                knockback = EnchantmentUtils.getExplosionKnockback(entity, currentStrength)
                            }
                            val knockbackVec = Vec(
                                dx * knockback,
                                dy * knockback,
                                dz * knockback
                            )
                            val tps: Int = MinecraftServer.TICK_PER_SECOND
                            if (entity is Player) {
                                if (entity.gameMode.canTakeDamage() && !entity.isFlying) {
                                    playerKnockback[entity] = knockbackVec
                                    (entity as? PvpPlayer)?.addVelocity(knockbackVec.mul(tps.toDouble()))
                                }
                            } else {
                                entity.velocity = entity.velocity.add(knockbackVec.mul(tps.toDouble()))
                            }
                        }
                    }
                }
                return blocks
            }

            override fun apply(instance: Instance) {
                val blocks = prepare(instance) ?: return
                // Event was cancelled
                val records = ByteArray(3 * blocks.size)
                for (i in blocks.indices) {
                    val pos = blocks[i]
                    instance.setBlock(pos, Block.AIR)
                    val x = (pos.x() - Math.floor(getCenterX().toDouble())).toInt().toByte()
                    val y = (pos.y() - Math.floor(getCenterY().toDouble())).toInt().toByte()
                    val z = (pos.z() - Math.floor(getCenterZ().toDouble())).toInt().toByte()
                    records[i * 3] = x
                    records[i * 3 + 1] = y
                    records[i * 3 + 2] = z
                }
                val chunk = instance.getChunkAt(getCenterX().toDouble(), getCenterZ().toDouble())
                if (chunk != null) {
                    for (player in chunk.viewers) {
                        val knockbackVec: Vec = playerKnockback.getOrDefault(player, Vec.ZERO)
                        player.sendPacket(
                            ExplosionPacket(
                                centerX,
                                centerY,
                                centerZ,
                                strength,
                                records,
                                knockbackVec.x().toFloat(),
                                knockbackVec.y().toFloat(),
                                knockbackVec.z().toFloat()
                            )
                        )
                    }
                }
                playerKnockback.clear()
                if (additionalData != null && additionalData.contains("fire")) {
                    if (java.lang.Boolean.TRUE == additionalData.getBoolean("fire")) {
                        val random = ThreadLocalRandom.current()
                        for (point in blocks) {
                            if (random.nextInt(3) != 0 || !instance.getBlock(point).isAir
                                || !instance.getBlock(point.sub(0.0, 1.0, 0.0)).isSolid
                            ) continue
                            instance.setBlock(point, Block.FIRE)
                        }
                    }
                }
                postSend(instance, blocks)
            }

            private fun getCausingEntity(instance: Instance): LivingEntity? {
                var causingEntity: LivingEntity? = null
                if (additionalData != null && additionalData.contains("causingEntity")) {
                    val causingUuid = UUID.fromString(Objects.requireNonNull(additionalData.getString("causingEntity")))
                    causingEntity = instance.entities
                        .filter { entity -> entity is LivingEntity && entity.uuid == causingUuid }
                        .firstOrNull() as LivingEntity
                }
                return causingEntity
            }
        }
    }

    companion object {
        fun getExposure(center: Point, entity: Entity): Double {
            val box: BoundingBox = entity.boundingBox
            val xStep: Double = 1 / (box.width() * 2 + 1)
            val yStep: Double = 1 / (box.height() * 2 + 1)
            val zStep: Double = 1 / (box.depth() * 2 + 1)
            val g = (1 - Math.floor(1 / xStep) * xStep) / 2
            val h = (1 - Math.floor(1 / zStep) * zStep) / 2
            if (xStep < 0 || yStep < 0 || zStep < 0) return 0.0
            var exposedCount = 0
            var rayCount = 0
            var dx = 0.0
            while (dx <= 1) {
                var dy = 0.0
                while (dy <= 1) {
                    var dz = 0.0
                    while (dz <= 1) {
                        val rayX: Double = box.minX() + dx * box.width()
                        val rayY: Double = box.minY() + dy * box.height()
                        val rayZ: Double = box.minZ() + dz * box.depth()
                        val point: Point = Vec(rayX + g, rayY, rayZ + h).add(entity.position)
                        if (noBlocking(entity.instance, point, center)) exposedCount++
                        rayCount++
                        dz += zStep
                    }
                    dy += yStep
                }
                dx += xStep
            }
            return exposedCount / rayCount.toDouble()
        }

        fun noBlocking(instance: Instance, start: Point, end: Point): Boolean {
            return CollisionUtils.isLineOfSightReachingShape(instance, null, start, end, BoundingBox(1.0, 1.0, 1.0))
        }
    }
}*/