import kotlin.math.sqrt

data class Vector4(val x: Double, val y: Double, val z: Double, val w: Double) {

    constructor(x: Double, y: Double, z: Double) : this(x, y, z, 0.0)

    val length get() = sqrt(x * x + y * y + z * z + w * w)

    operator fun plus(v: Vector4) = Vector4(x + v.x, y + v.y, z + v.z, w + v.w)

    operator fun minus(v: Vector4) = Vector4(x - v.x, y - v.y, z - v.z, w - v.w)

    operator fun times(s: Double) = Vector4(x * s, y * s, z * s, w * s)

    operator fun div(s: Double) = Vector4(x / s, y / s, z / s, w / s)

    companion object {
        val ZERO = Vector4(0.0, 0.0, 0.0)
        val UNIT_X = Vector4(1.0, 0.0, 0.0)
        val UNIT_Y = Vector4(0.0, 1.0, 0.0)
        val UNIT_Z = Vector4(0.0, 0.0, 1.0)

        fun normalize(v: Vector4) = v / v.length
    }
}
