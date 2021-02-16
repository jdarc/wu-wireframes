import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class Matrix4(
    val m00: Double, val m01: Double, val m02: Double, val m03: Double,
    val m10: Double, val m11: Double, val m12: Double, val m13: Double,
    val m20: Double, val m21: Double, val m22: Double, val m23: Double,
    val m30: Double, val m31: Double, val m32: Double, val m33: Double
) {

    operator fun times(v: Vector4) = Vector4(
        m00 * v.x + m01 * v.y + m02 * v.z + m03 * v.w,
        m10 * v.x + m11 * v.y + m12 * v.z + m13 * v.w,
        m20 * v.x + m21 * v.y + m22 * v.z + m23 * v.w,
        m30 * v.x + m31 * v.y + m32 * v.z + m33 * v.w
    )

    operator fun times(m: Matrix4) = Matrix4(
        m00 * m.m00 + m01 * m.m10 + m02 * m.m20 + m03 * m.m30,
        m00 * m.m01 + m01 * m.m11 + m02 * m.m21 + m03 * m.m31,
        m00 * m.m02 + m01 * m.m12 + m02 * m.m22 + m03 * m.m32,
        m00 * m.m03 + m01 * m.m13 + m02 * m.m23 + m03 * m.m33,
        m10 * m.m00 + m11 * m.m10 + m12 * m.m20 + m13 * m.m30,
        m10 * m.m01 + m11 * m.m11 + m12 * m.m21 + m13 * m.m31,
        m10 * m.m02 + m11 * m.m12 + m12 * m.m22 + m13 * m.m32,
        m10 * m.m03 + m11 * m.m13 + m12 * m.m23 + m13 * m.m33,
        m20 * m.m00 + m21 * m.m10 + m22 * m.m20 + m23 * m.m30,
        m20 * m.m01 + m21 * m.m11 + m22 * m.m21 + m23 * m.m31,
        m20 * m.m02 + m21 * m.m12 + m22 * m.m22 + m23 * m.m32,
        m20 * m.m03 + m21 * m.m13 + m22 * m.m23 + m23 * m.m33,
        m30 * m.m00 + m31 * m.m10 + m32 * m.m20 + m33 * m.m30,
        m30 * m.m01 + m31 * m.m11 + m32 * m.m21 + m33 * m.m31,
        m30 * m.m02 + m31 * m.m12 + m32 * m.m22 + m33 * m.m32,
        m30 * m.m03 + m31 * m.m13 + m32 * m.m23 + m33 * m.m33
    )

    fun translateBy(x: Double, y: Double, z: Double) = Matrix4(
        m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23,
        m00 * x + m10 * y + m20 * z + m30,
        m01 * x + m11 * y + m21 * z + m31,
        m02 * x + m12 * y + m22 * z + m32,
        m03 * x + m13 * y + m23 * z + m33
    )

    fun rotateY(radians: Double): Matrix4 {
        val s = sin(radians)
        val c = cos(radians)
        return Matrix4(
            m00 * c - m20 * s,
            m01 * c - m21 * s,
            m02 * c - m22 * s,
            m03 * c - m23 * s,
            m10, m11, m12, m13,
            m00 * s + m20 * c,
            m01 * s + m21 * c,
            m02 * s + m22 * c,
            m03 * s + m23 * c,
            m30, m31, m32, m33
        )
    }

    fun rotateZ(radians: Double): Matrix4 {
        val s = sin(radians)
        val c = cos(radians)
        return Matrix4(
            m00 * c + m10 * s,
            m01 * c + m11 * s,
            m02 * c + m12 * s,
            m03 * c + m13 * s,
            m10 * c - m00 * s,
            m11 * c - m01 * s,
            m12 * c - m02 * s,
            m13 * c - m03 * s,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        )
    }

    companion object {

        val IDENTITY = Matrix4(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0)

        fun translation(x: Double, y: Double, z: Double) = IDENTITY.translateBy(x, y, z)

        fun rotationY(radians: Double) = IDENTITY.rotateY(radians)

        fun rotationZ(radians: Double) = IDENTITY.rotateZ(radians)

        fun axisAngle(axis: Vector4, radians: Double): Matrix4 {
            val c = cos(-radians)
            val s = sin(-radians)
            val t = 1.0 - c
            return Matrix4(
                c + axis.x * axis.x * t, axis.x * axis.y * t - axis.z * s, axis.x * axis.z * t + axis.y * s, 0.0,
                axis.x * axis.y * t + axis.z * s, c + axis.y * axis.y * t, axis.y * axis.z * t - axis.x * s, 0.0,
                axis.x * axis.z * t - axis.y * s, axis.y * axis.z * t + axis.x * s, c + axis.z * axis.z * t, 0.0,
                0.0, 0.0, 0.0, 1.0
            )
        }

        fun lookAt(eye: Vector4, center: Vector4, up: Vector4): Matrix4 {
            val z0 = eye.x - center.x
            val z1 = eye.y - center.y
            val z2 = eye.z - center.z
            val lz = 1.0 / sqrt(z0 * z0 + z1 * z1 + z2 * z2)
            val x0 = up.y * z2 - up.z * z1
            val x1 = up.z * z0 - up.x * z2
            val x2 = up.x * z1 - up.y * z0
            val lx = 1.0 / sqrt(x0 * x0 + x1 * x1 + x2 * x2)
            val y0 = z1 * x2 - z2 * x1
            val y1 = z2 * x0 - z0 * x2
            val y2 = z0 * x1 - z1 * x0
            val ly = 1.0 / sqrt(y0 * y0 + y1 * y1 + y2 * y2)
            return Matrix4(
                x0 * lx, y0 * ly, z0 * lz, 0.0,
                x1 * lx, y1 * ly, z1 * lz, 0.0,
                x2 * lx, y2 * ly, z2 * lz, 0.0,
                -(x0 * eye.x + x1 * eye.y + x2 * eye.z) * lx,
                -(y0 * eye.x + y1 * eye.y + y2 * eye.z) * ly,
                -(z0 * eye.x + z1 * eye.y + z2 * eye.z) * lz, 1.0
            )
        }

        fun perspective(fieldOfView: Double, aspectRatio: Double, nearPlane: Double, farPlane: Double): Matrix4 {
            val tan = tan(fieldOfView / 2.0)
            val nf = 1.0 / (nearPlane - farPlane)
            return Matrix4(
                1.0 / (tan * aspectRatio), 0.0, 0.0, 0.0,
                0.0, 1.0 / tan, 0.0, 0.0,
                0.0, 0.0, (farPlane + nearPlane) * nf, -1.0,
                0.0, 0.0, 2.0 * farPlane * nearPlane * nf, 0.0
            )
        }
    }
}
