package dev.emortal.immortal.util

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec

operator fun Point.plus(other: Point): Point = this.add(other)

operator fun Point.minus(other: Point): Point = this.sub(other)

operator fun Point.times(other: Point): Point = this.mul(other)
operator fun Point.times(other: Double): Point = this.mul(other)

operator fun Point.div(other: Point): Point = this.div(other)
operator fun Point.div(other: Double): Point = this.div(other)

fun Point.roundToBlock(): Point = Vec(blockX().toDouble(), blockY().toDouble(), blockZ().toDouble())