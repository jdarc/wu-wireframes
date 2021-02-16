import Vector4.Companion.normalize
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

class FirstPersonControl(private val camera: Camera) {
    private var lookAt = normalize(camera.target - camera.position)
    private var yaw = -atan2(-lookAt.x, -lookAt.z)
    private var pitch = -asin(lookAt.y)
    private var movementMask = 0
    private var lastX = 0
    private var lastY = 0
    private var dragging = false

    fun keyUp(code: Int) {
        when (code) {
            KeyEvent.VK_W -> movementMask = movementMask xor MOVEMENT_FORWARD
            KeyEvent.VK_S -> movementMask = movementMask xor MOVEMENT_BACK
            KeyEvent.VK_A -> movementMask = movementMask xor MOVEMENT_LEFT
            KeyEvent.VK_D -> movementMask = movementMask xor MOVEMENT_RIGHT
        }
    }

    fun keyDown(code: Int) {
        when (code) {
            KeyEvent.VK_W -> movementMask = movementMask or MOVEMENT_FORWARD
            KeyEvent.VK_S -> movementMask = movementMask or MOVEMENT_BACK
            KeyEvent.VK_A -> movementMask = movementMask or MOVEMENT_LEFT
            KeyEvent.VK_D -> movementMask = movementMask or MOVEMENT_RIGHT
        }
    }

    fun mouseDown(evt: MouseEvent) {
        dragging = true
        lastX = evt.x
        lastY = evt.y
    }

    fun mouseMove(evt: MouseEvent) {
        if (dragging) {
            yaw += 0.005 * (evt.x - lastX)
            pitch += 0.005 * (evt.y - lastY)
            pitch = if (pitch < -1.57) -1.57 else if (pitch > 1.57) 1.57 else pitch
            lookAt = Matrix4.axisAngle(Vector4.UNIT_Y, yaw) * Matrix4.axisAngle(Vector4.UNIT_X, pitch) * Vector4(0.0, 0.0, -1.0)
        }
        lastX = evt.x
        lastY = evt.y
    }

    fun mouseUp(evt: MouseEvent) {
        dragging = false
        lastX = evt.x
        lastY = evt.y
    }

    fun update(seconds: Double, speed: Double) {
        var xyz = camera.position
        val scaledSpeed = seconds * speed

        if (movementMask and MOVEMENT_FORWARD == MOVEMENT_FORWARD) {
            xyz += lookAt * scaledSpeed
        } else if (movementMask and MOVEMENT_BACK == MOVEMENT_BACK) {
            xyz -= lookAt * scaledSpeed
        }
        if (movementMask and MOVEMENT_LEFT == MOVEMENT_LEFT) {
            val scalar = scaledSpeed / sqrt(lookAt.z * lookAt.z + lookAt.x * lookAt.x)
            xyz -= Vector4(-lookAt.z * scalar, 0.0, lookAt.x * scalar)
        } else if (movementMask and MOVEMENT_RIGHT == MOVEMENT_RIGHT) {
            val scalar = scaledSpeed / sqrt(lookAt.z * lookAt.z + lookAt.x * lookAt.x)
            xyz += Vector4(-lookAt.z * scalar, 0.0, lookAt.x * scalar)
        }

        camera.position = xyz
        camera.target = xyz + lookAt
    }

    private companion object {
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_BACK = 2
        private const val MOVEMENT_LEFT = 4
        private const val MOVEMENT_RIGHT = 8
    }
}
