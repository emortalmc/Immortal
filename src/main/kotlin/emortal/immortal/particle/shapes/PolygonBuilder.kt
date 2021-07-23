package emortal.immortal.particle.shapes

class PolygonBuilder {

    val lines = mutableSetOf<ParticleLine>()

    fun append(line: ParticleLine) {
        lines.add(line)
    }

    fun build(): ParticlePoly {
        return ParticlePoly(lines)
    }

}