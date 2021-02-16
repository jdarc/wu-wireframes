import java.awt.AWTEvent
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.Timer
import javax.swing.WindowConstants
import kotlin.system.exitProcess

class MainFrame : JFrame("3D Wireframe Primitives") {
    private var camera: Camera
    private val backgroundColor: Color = Color(0x5e676e)
    private val controller: FirstPersonControl

    init {
        size = Dimension(1440, 900)
        setLocationRelativeTo(null)
        isResizable = false
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

        camera = Camera(Math.PI / 4.0, size.width / size.height.toDouble(), 0.1, 1000.0)
        camera.position = Vector4(7.0, 8.0, 10.0)
        camera.target = Vector4(1.0, 0.0, 0.0)
        controller = FirstPersonControl(camera)

        val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB)
        val gx = Wireframe3D(image)

        val mask = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK + AWTEvent.KEY_EVENT_MASK
        Toolkit.getDefaultToolkit().addAWTEventListener(fun(it: AWTEvent) {
            when (it) {
                is MouseEvent -> when (it.getID()) {
                    MouseEvent.MOUSE_PRESSED -> controller.mouseDown(it)
                    MouseEvent.MOUSE_RELEASED -> controller.mouseUp(it)
                    MouseEvent.MOUSE_MOVED, MouseEvent.MOUSE_DRAGGED -> controller.mouseMove(it)
                }
                is KeyEvent -> {
                    when (it.getID()) {
                        KeyEvent.KEY_PRESSED -> {
                            if (it.keyCode == KeyEvent.VK_ESCAPE) {
                                exitProcess(0)
                            }
                            controller.keyDown(it.keyCode)
                        }
                        KeyEvent.KEY_RELEASED -> controller.keyUp(it.keyCode)
                    }
                }
            }
        }, mask)

        Timer(16) {
            render(gx)
            controller.update(1.0 / 60.0, 10.0)
            graphics.drawImage(image, 0, 0, this)
        }.start()
    }

    private fun render(gx: Wireframe3D) {
        gx.color = backgroundColor
        gx.clear()

        gx.view(camera.view)
        gx.proj(camera.projection)

        gx.identity()
        gx.drawGrid()
        gx.drawAxis()

        gx.identity()
        gx.rotate(1.0, 0.0, 0.0, 1.45)
        gx.transform(Matrix4.translation(0.0, 1.0, 1.0))
        gx.drawPlane(2.0, 2.0, Color.RED)

        gx.identity()
        gx.rotate(1.0, 1.0, 2.0, 3.45)
        gx.transform(Matrix4.translation(2.0, 2.0, 1.0))
        gx.drawCube(Vector4(-0.5, -1.0, -0.5), Vector4(0.5, 1.0, 0.5), Color.YELLOW)

        gx.identity()
        gx.transform(Matrix4.translation(0.0, 1.0, 0.0))
        gx.drawSphere(1.0, 16, 16, Color.GREEN)

        gx.identity()
        gx.rotate(1.0, 3.3, 2.0, 0.45)
        gx.transform(Matrix4.translation(2.0, 2.0, -3.0))
        gx.drawCapsule(1.0, 3.0, 16, 16, Color.CYAN)
    }
}
