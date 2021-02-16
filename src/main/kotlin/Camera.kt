import kotlin.math.max
import kotlin.math.min

class Camera(val fov: Double = Math.PI / 3.0, val aspect: Double = 1.0, near: Double = 0.1, far: Double = 1000.0) {
    private val nearPlane = min(near, far)
    private val farPlane = max(near, far)

    var position = Vector4.UNIT_Z

    var target = Vector4.ZERO

    val view get() = Matrix4.lookAt(position, target, Vector4.UNIT_Y)

    val projection get() = Matrix4.perspective(fov, aspect, nearPlane, farPlane)
}
