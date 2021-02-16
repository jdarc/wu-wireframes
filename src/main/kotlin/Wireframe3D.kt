import Vector4.Companion.normalize
import java.awt.Color
import java.awt.Color.*
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.util.*
import kotlin.math.abs

class Wireframe3D(image: BufferedImage) {
    enum class Mode { LINE, POLYGON }

    private var world = Matrix4.IDENTITY
    private var view = Matrix4.IDENTITY
    private var proj = Matrix4.IDENTITY
    private var comb = Matrix4.IDENTITY
    private val vertices = Array(64) { Vertex() }
    private val viewport = Rectangle(0, 0, image.width, image.height - 3)
    private val colorBuffer = (image.raster.dataBuffer as DataBufferInt).data
    private val depthBuffer = DoubleArray(colorBuffer.size)
    private var drawMode = Mode.LINE
    private var dirty = true
    private var count = 0
    private var rgb = 0xff shl 24 and 0x000000
    private var fill = 0xff shl 24 and 0x000000
    private var offset = 0.0

    var color: Color
        get() = Color(rgb)
        set(color) {
            rgb = color.rgb
        }

    fun clear() {
        fill = rgb
        Arrays.fill(colorBuffer, fill)
        Arrays.fill(depthBuffer, Double.POSITIVE_INFINITY)
    }

    fun identity() {
        dirty = true
        world = Matrix4.IDENTITY
    }

    fun rotate(x: Double, y: Double, z: Double, angle: Double) {
        dirty = true
        world *= Matrix4.axisAngle(normalize(Vector4(x, y, z)), angle)
    }

    fun transform(m: Matrix4) {
        dirty = true
        world *= m
    }

    fun view(m: Matrix4) {
        dirty = true
        view = m
    }

    fun proj(m: Matrix4) {
        dirty = true
        proj = m
    }

    fun begin(mode: Mode) {
        drawMode = mode
        count = 0
    }

    fun vertex(x: Double, y: Double, z: Double) {
        vertices[count++].set(x, y, z, 1.0)
    }

    fun end() {
        offset = 0.0
        val oldRgb = rgb
        val matrix = updateTransform()
        when (drawMode) {
            Mode.LINE -> drawLines(matrix)
            Mode.POLYGON -> drawPolygon(matrix)
        }
        rgb = oldRgb
    }

    private fun drawLines(matrix: Matrix4) {
        if (count > 1) {
            var i = 0
            while (i < count - 1) {
                drawLine(vertices[i].project(matrix), vertices[i + 1].project(matrix))
                i += 2
            }
        }
    }

    private fun drawPolygon(matrix: Matrix4) {
        if (count > 2) {
            for (i in 0 until count) {
                vertices[i].project(matrix)
            }
            if (isBackFacing(vertices[0], vertices[1], vertices[2])) {
                rgb = blend(rgb, fill, 0.20)
                offset = 0.05
            }
            val pad = (count shl 1) - 1
            val v0 = vertices[pad - 1]
            val v1 = vertices[pad]
            drawLine(v0.copy(vertices[pad shr 1]), v1.copy(vertices[0]))
            var i = pad - 2
            while (i > 0) {
                drawLine(v0.copy(vertices[(i + 1 shr 1) - 1]), v1.copy(vertices[i + 1 shr 1]))
                i -= 2
            }
        }
    }

    fun drawAxis(scale: Double = 1.0, cx: Color = RED, cy: Color = GREEN, cz: Color = BLUE) {
        color = cx
        begin(Mode.LINE)
        vertex(0.0, 0.0, 0.0)
        vertex(scale, 0.0, 0.0)
        end()
        color = cy
        begin(Mode.LINE)
        vertex(0.0, 0.0, 0.0)
        vertex(0.0, scale, 0.0)
        end()
        color = cz
        begin(Mode.LINE)
        vertex(0.0, 0.0, 0.0)
        vertex(0.0, 0.0, scale)
        end()
    }

    fun drawGrid(size: Double = 20.0, step: Double = 1.0, color: Color = Color(0, 0, 0, 64)) {
        val half = size * 0.5
        var i = -half
        this.color = Color(blend(color.rgb, fill, 0.25))
        while (i <= half) {
            if (i != 0.0) {
                begin(Mode.LINE)
                vertex(-half, 0.0, i)
                vertex(half, 0.0, i)
                vertex(i, 0.0, -half)
                vertex(i, 0.0, half)
                end()
            }
            i += step
        }
        this.color = Color(blend(color.rgb, fill, 0.5))
        begin(Mode.LINE)
        vertex(-half, 0.0, 0.0)
        vertex(half, 0.0, 0.0)
        vertex(0.0, 0.0, -half)
        vertex(0.0, 0.0, half)
        end()
    }

    fun drawPlane(width: Double, depth: Double, color: Color) {
        this.color = color
        begin(Mode.POLYGON)
        vertex(-width * 0.5, 0.0, +depth * 0.5)
        vertex(+width * 0.5, 0.0, +depth * 0.5)
        vertex(+width * 0.5, 0.0, -depth * 0.5)
        vertex(-width * 0.5, 0.0, -depth * 0.5)
        end()
    }

    fun drawCube(min: Vector4, max: Vector4, color: Color) {
        this.color = color
        val vertices = arrayOf(
            Vector4(min.x, max.y, min.z),
            Vector4(max.x, max.y, min.z),
            Vector4(max.x, min.y, min.z),
            Vector4(min.x, min.y, min.z),
            Vector4(max.x, max.y, max.z),
            Vector4(min.x, max.y, max.z),
            Vector4(min.x, min.y, max.z),
            Vector4(max.x, min.y, max.z)
        )
        val indices = intArrayOf(0, 1, 2, 3, 1, 4, 7, 2, 4, 5, 6, 7, 0, 3, 6, 5, 0, 5, 4, 1, 3, 2, 7, 6)

        for (side in 0..5) {
            begin(Mode.POLYGON)
            for (index in 0..3) {
                val i = indices[side * 4 + index]
                vertex(vertices[i].x, vertices[i].y, vertices[i].z)
            }
            end()
        }
    }

    fun drawSphere(radius: Double, stacks: Int, slices: Int, color: Color) {
        this.color = color

        val curve = ArrayList<Vector4>()
        val stackAngle = Math.PI / stacks
        for (stack in 0..stacks) {
            curve.add(Matrix4.rotationZ(stackAngle * stack) * Vector4.UNIT_Y)
        }

        val vertices = ArrayList<Vector4>()
        val sliceAngle = Math.PI * 2.0 / slices
        for (slice in 0..slices) {
            val aboutY = Matrix4.rotationY(sliceAngle * slice)
            for (point in curve) {
                vertices.add(aboutY * point * radius)
            }
        }

        for (slice in 0 until slices) {
            val index = slice * (stacks + 1)
            for (stack in 1 until stacks - 1) {
                val ma = stack + index
                val md = stack + index + 1
                val mc = stack + index + (stacks + 1) + 1
                val mb = stack + index + (stacks + 1)
                if (stack < stacks - 1) {
                    renderPolygon(vertices[ma], vertices[mb], vertices[mc], vertices[md])
                } else {
                    renderPolygon(vertices[md], vertices[ma], vertices[mb], vertices[mc])
                }
            }
        }
    }

    fun drawCapsule(radius: Double, height: Double, stacks: Int, slices: Int, color: Color) {
        this.color = color

        val curve = ArrayList<Vector4>()
        val stackAngle = Math.PI / stacks
        for (stack in 0..stacks / 2) {
            val temp = Matrix4.rotationZ(stackAngle * stack)
            curve.add(temp * Vector4(0.0, radius, 0.0) + Vector4(0.0, height * 0.5, 0.0))
        }
        for (stack in stacks / 2..stacks) {
            val temp = Matrix4.rotationZ(stackAngle * stack)
            curve.add(temp * Vector4(0.0, radius, 0.0) + Vector4(0.0, -height * 0.5, 0.0))
        }

        val vertices = ArrayList<Vector4>()
        val sliceAngle = Math.PI * 2.0 / slices
        for (slice in 0..slices) {
            val aboutY = Matrix4.rotationY(sliceAngle * slice)
            for (index in curve.indices) {
                val vertex = aboutY * curve[index]
                vertices.add(Vector4(vertex.x, vertex.y, vertex.z))
            }
        }

        for (slice in 0 until slices) {
            val index = slice * (stacks + 2)
            for (stack in 0..stacks) {
                val ma = stack + index
                val md = stack + index + 1
                val mc = stack + index + (stacks + 2) + 1
                val mb = stack + index + (stacks + 2)
                if (stack < stacks) {
                    renderPolygon(vertices[ma], vertices[mb], vertices[mc], vertices[md])
                } else {
                    renderPolygon(vertices[md], vertices[ma], vertices[mb], vertices[mc])
                }
            }
        }
    }

    private fun renderPolygon(v0: Vector4, v1: Vector4, v2: Vector4, v3: Vector4) {
        begin(Mode.POLYGON)
        vertex(v0.x, v0.y, v0.z)
        vertex(v1.x, v1.y, v1.z)
        vertex(v2.x, v2.y, v2.z)
        vertex(v3.x, v3.y, v3.z)
        end()
    }

    private fun drawLine(a: Vertex, b: Vertex) {
        if (clip(a, b)) {
            a.screen(viewport)
            b.screen(viewport)
            wuLine(a.x, a.y, a.w + offset, b.x, b.y, b.w + offset)
        }
    }

    private fun wuLine(x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double) {
        var xa = x0
        var ya = y0
        var za = z0
        var xb = x1
        var yb = y1
        var zb = z1
        val steep = abs(yb - ya) > abs(xb - xa)

        var t: Double
        if (steep) {
            t = ya; ya = xa; xa = t
            t = yb; yb = xb; xb = t
        }

        if (xa > xb) {
            t = xa; xa = xb; xb = t
            t = ya; ya = yb; yb = t
            t = za; za = zb; zb = t
        }

        val dx = xb - xa
        val dy = yb - ya
        val dz = zb - za
        val gy = dy / dx
        val gz = dz / dx

        val xend0 = (xa + 0.5).toInt()
        val yend0 = ya + gy * (xend0 - xa)
        val zend0 = za + gz * (xend0 - xa)
        wupixel(steep, xend0, yend0, zend0, rfPart(xa + 0.5))

        val xend1 = (xb + 0.5).toInt()
        val yend1 = yb + gy * (xend1 - xb)
        val zend1 = zb + gz * (xend1 - xb)
        wupixel(steep, xend1, yend1, zend1, fPart(xb + 0.5))

        var intery = yend0 + gy
        var interz = zend0 + gz
        for (x in xend0 + 1 until xend1) {
            wupixel(steep, x, intery, interz, 1.0)
            intery += gy
            interz += gz
        }
    }

    private fun wupixel(steep: Boolean, x: Int, intery: Double, z: Double, gap: Double) {
        val y = intery.toInt()
        if (steep) {
            val addr = y + x * viewport.width
            if (z < depthBuffer[addr]) {
                depthBuffer[addr] = z
                plot(y, x, rfPart(intery) * gap)
                plot(y + 1, x, fPart(intery) * gap)
            }
        } else {
            val addr = x + y * viewport.width
            if (z < depthBuffer[addr]) {
                depthBuffer[addr] = z
                plot(x, y, rfPart(intery) * gap)
                plot(x, y + 1, fPart(intery) * gap)
            }
        }
    }

    private fun plot(x: Int, y: Int, c: Double) {
        val offset = y * viewport.width + x
        colorBuffer[offset] = (0xff shl 24) or blend(rgb, colorBuffer[offset], c)
    }

    private fun updateTransform(): Matrix4 {
        if (dirty) {
            comb = world * view * proj
            dirty = false
        }
        return comb
    }

    private fun clip(a: Vertex, b: Vertex): Boolean {
        for (i in 0..4) {
            val opCodeA = computeClipMask(a)
            val opCodeB = computeClipMask(b)
            if (opCodeA == 0 && opCodeB == 0) {
                return true
            }
            if (opCodeA and opCodeB != 0) {
                return false
            }
            if (opCodeA != 0) {
                clipLine(a, b, opCodeA)
            }
            if (opCodeB != 0) {
                clipLine(b, a, opCodeB)
            }
        }
        return false
    }

    private inner class Vertex {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var w = 0.0

        fun set(x: Double, y: Double, z: Double, w: Double): Vertex {
            this.x = x
            this.y = y
            this.z = z
            this.w = w
            return this
        }

        fun copy(other: Vertex) = set(other.x, other.y, other.z, other.w)

        fun lerp(from: Vertex, to: Vertex, t: Double) = set(
            (1.0 - t) * from.x + t * to.x,
            (1.0 - t) * from.y + t * to.y,
            (1.0 - t) * from.z + t * to.z,
            (1.0 - t) * from.w + t * to.w
        )

        fun project(mat: Matrix4): Vertex {
            return set(
                x * mat.m00 + y * mat.m10 + z * mat.m20 + w * mat.m30,
                x * mat.m01 + y * mat.m11 + z * mat.m21 + w * mat.m31,
                x * mat.m02 + y * mat.m12 + z * mat.m22 + w * mat.m32,
                x * mat.m03 + y * mat.m13 + z * mat.m23 + w * mat.m33
            )
        }

        fun screen(viewport: Rectangle) = set(
            viewport.x + viewport.width * (1.0 + x / w) * 0.5,
            viewport.y + viewport.height * (1.0 - y / w) * 0.5,
            1.0 + z / w, w
        )
    }

    private companion object {
        fun fPart(x: Double) = if (x < 0.0) x.toInt() - x else x - x.toInt()

        fun rfPart(x: Double) = 1.0 - fPart(x)

        fun isBackFacing(a: Vertex, b: Vertex, c: Vertex): Boolean {
            val cax = c.x / c.w - a.x / a.w
            val cay = a.y / a.w - b.y / b.w
            val bax = b.x / b.w - a.x / a.w
            val bay = a.y / a.w - c.y / c.w
            return cax * cay < bax * bay
        }

        fun computeClipMask(v: Vertex): Int {
            val m0 = if (v.x > v.w) 32 else 0
            val m1 = if (v.y > v.w) 16 else 0
            val m2 = if (v.z > v.w) 8 else 0
            val m3 = if (v.x < -v.w) 4 else 0
            val m4 = if (v.y < -v.w) 2 else 0
            val m5 = if (v.z < -v.w) 1 else 0
            return m0 or m1 or m2 or m3 or m4 or m5
        }

        fun clipLine(from: Vertex, to: Vertex, mask: Int) {
            val t = when {
                mask and 32 > 0 -> (from.w - from.x) / (from.w - from.x - to.w + to.x)
                mask and 16 > 0 -> (from.w - from.y) / (from.w - from.y - to.w + to.y)
                mask and 8 > 0 -> (from.w - from.z) / (from.w - from.z - to.w + to.z)
                mask and 4 > 0 -> (from.w + from.x) / (from.w + from.x - to.w - to.x)
                mask and 2 > 0 -> (from.w + from.y) / (from.w + from.y - to.w - to.y)
                else -> (from.w + from.z) / (from.w + from.z - to.w - to.z)
            }
            from.lerp(from, to, t)
        }

        fun blend(col1: Int, col2: Int, factor: Double): Int {
            val c8 = (256.0 * factor).toInt()
            val rg = (0xff00ff and col1) * c8 + (0xff00ff and col2) * (256 - c8) shr 8 and 0xff00ff
            val go = (0x00ff00 and col1) * c8 + (0x00ff00 and col2) * (256 - c8) shr 8 and 0x00ff00
            return rg or go
        }
    }
}
