package dev.emortal.immortal.util

import kotlin.math.exp
import kotlin.math.ln

fun expInterp(low: Float, high: Float, v: Float): Float {
    val v = v.coerceIn(0f, 1f)
    val delta = high - low
    return low + (exp(v * ln(1 + delta)) - 1)
}
fun lerp(low: Float, high: Float, v: Float): Float {
    val v = v.coerceIn(0f, 1f)
    val delta = high - low
    return low + v * delta
}