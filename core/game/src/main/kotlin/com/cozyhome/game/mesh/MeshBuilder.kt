package com.cozyhome.game.mesh

import com.cozyhome.game.math.V3
import com.cozyhome.game.math.hash01
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

class MeshPart(
    val mat: Mat,
    val positions: FloatArray,
    val normals: FloatArray,
    val colors: FloatArray,
    val indices: IntArray,
    val uvs: FloatArray = FloatArray(positions.size / 3 * 2),
) {
    val vertexCount: Int get() = positions.size / 3
    val triangleCount: Int get() = indices.size / 3
}

class Mesh(val parts: List<MeshPart>) {
    val triangleCount: Int get() = parts.sumOf { it.triangleCount }
    val isEmpty: Boolean get() = parts.all { it.indices.isEmpty() }

    fun bounds(): Pair<V3, V3> {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        for (p in parts) {
            var i = 0
            while (i < p.positions.size) {
                minX = min(minX, p.positions[i]); maxX = max(maxX, p.positions[i])
                minY = min(minY, p.positions[i + 1]); maxY = max(maxY, p.positions[i + 1])
                minZ = min(minZ, p.positions[i + 2]); maxZ = max(maxZ, p.positions[i + 2])
                i += 3
            }
        }
        return V3(minX, minY, minZ) to V3(maxX, maxY, maxZ)
    }
}

private class PartBuffer(val mat: Mat) {
    private var capacity = 256
    var positions = FloatArray(capacity * 3)
    var normals = FloatArray(capacity * 3)
    var colors = FloatArray(capacity * 4)
    var uvs = FloatArray(capacity * 2)
    var indices = IntArray(capacity * 3)
    var vCount = 0
    var iCount = 0

    fun addVertex(p: V3, n: V3, c: Rgb, u: Float = 0f, v: Float = 0f): Int {
        if (vCount == capacity) {
            capacity *= 2
            positions = positions.copyOf(capacity * 3)
            normals = normals.copyOf(capacity * 3)
            colors = colors.copyOf(capacity * 4)
            uvs = uvs.copyOf(capacity * 2)
        }
        uvs[vCount * 2] = u
        uvs[vCount * 2 + 1] = v
        val i3 = vCount * 3
        positions[i3] = p.x; positions[i3 + 1] = p.y; positions[i3 + 2] = p.z
        normals[i3] = n.x; normals[i3 + 1] = n.y; normals[i3 + 2] = n.z
        val lin = c.linear()
        val i4 = vCount * 4
        colors[i4] = lin[0]; colors[i4 + 1] = lin[1]; colors[i4 + 2] = lin[2]; colors[i4 + 3] = 1f
        return vCount++
    }

    fun addTriangle(a: Int, b: Int, c: Int) {
        if (iCount + 3 > indices.size) indices = indices.copyOf(indices.size * 2)
        indices[iCount++] = a; indices[iCount++] = b; indices[iCount++] = c
    }

    fun toPart() = MeshPart(
        mat,
        positions.copyOf(vCount * 3),
        normals.copyOf(vCount * 3),
        colors.copyOf(vCount * 4),
        indices.copyOf(iCount),
        uvs.copyOf(vCount * 2),
    )
}

class MeshBuilder {
    private val buffers = LinkedHashMap<Mat, PartBuffer>()
    private val stack = ArrayDeque<Affine>()
    private var transform = Affine.IDENTITY
    private var mirrored = false
    var mat: Mat = Mat.MATTE

    private val current: PartBuffer get() = buffers.getOrPut(mat) { PartBuffer(mat) }

    fun group(block: MeshBuilder.() -> Unit) {
        stack.addLast(transform)
        val savedMat = mat
        block()
        mat = savedMat
        transform = stack.removeLast()
        mirrored = transform.determinant < 0f
    }

    fun withMat(m: Mat, block: MeshBuilder.() -> Unit) {
        val saved = mat
        mat = m
        block()
        mat = saved
    }

    fun translate(x: Float, y: Float, z: Float) = apply(Affine.translate(x, y, z))
    fun rotateX(deg: Float) = apply(Affine.rotateX(deg))
    fun rotateY(deg: Float) = apply(Affine.rotateY(deg))
    fun rotateZ(deg: Float) = apply(Affine.rotateZ(deg))
    fun scale(x: Float, y: Float = x, z: Float = x) = apply(Affine.scale(x, y, z))

    private fun apply(a: Affine) {
        transform = transform * a
        mirrored = transform.determinant < 0f
    }

    var uvOverride: ((Float, Float, Float) -> Pair<Float, Float>)? = null

    private fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, paint: Paint, eu: Float = Float.NaN, ev: Float = Float.NaN): Int {
        val p = transform.point(x, y, z)
        val n = transform.normal(nx, ny, nz)
        val m = mat
        var u = 0f
        var v = 0f
        val override = uvOverride
        if (!eu.isNaN()) {
            u = eu
            v = ev
        } else if (override != null) {
            val uv = override(x, y, z)
            u = uv.first
            v = uv.second
        } else if (m.tex != Tex.NONE) {
            val s = 1f / m.texScale
            val ax = abs(n.x)
            val ay = abs(n.y)
            val az = abs(n.z)
            when {
                ay >= ax && ay >= az -> { u = p.x * s; v = p.z * s }
                ax >= az -> { u = p.z * s; v = -p.y * s }
                else -> { u = p.x * s; v = -p.y * s }
            }
        }
        return current.addVertex(p, n, paint.at(p.x, p.y, p.z), u, v)
    }

    fun vertexPublic(p: V3, n: V3, paint: Paint): Int = vertex(p.x, p.y, p.z, n.x, n.y, n.z, paint)

    fun triPublic(a: Int, b: Int, c: Int) = triangle(a, b, c)

    private fun triangle(a: Int, b: Int, c: Int) {
        if (mirrored) current.addTriangle(a, c, b) else current.addTriangle(a, b, c)
    }

    fun quad(a: V3, b: V3, c: V3, d: V3, paint: Paint) {
        val n = ((b - a) cross (c - a)).let { if (it.length < 1e-9f) ((c - a) cross (d - a)) else it }.normalized()
        val i0 = vertex(a.x, a.y, a.z, n.x, n.y, n.z, paint)
        val i1 = vertex(b.x, b.y, b.z, n.x, n.y, n.z, paint)
        val i2 = vertex(c.x, c.y, c.z, n.x, n.y, n.z, paint)
        val i3 = vertex(d.x, d.y, d.z, n.x, n.y, n.z, paint)
        triangle(i0, i1, i2)
        triangle(i0, i2, i3)
    }

    fun tri(a: V3, b: V3, c: V3, paint: Paint) {
        val n = ((b - a) cross (c - a)).normalized()
        val i0 = vertex(a.x, a.y, a.z, n.x, n.y, n.z, paint)
        val i1 = vertex(b.x, b.y, b.z, n.x, n.y, n.z, paint)
        val i2 = vertex(c.x, c.y, c.z, n.x, n.y, n.z, paint)
        triangle(i0, i1, i2)
    }

    fun block(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, paint: Paint) {
        val ax = min(x0, x1); val bx = max(x0, x1)
        val ay = min(y0, y1); val by = max(y0, y1)
        val az = min(z0, z1); val bz = max(z0, z1)
        quad(V3(bx, ay, bz), V3(bx, ay, az), V3(bx, by, az), V3(bx, by, bz), paint)
        quad(V3(ax, ay, az), V3(ax, ay, bz), V3(ax, by, bz), V3(ax, by, az), paint)
        quad(V3(ax, by, bz), V3(bx, by, bz), V3(bx, by, az), V3(ax, by, az), paint)
        quad(V3(ax, ay, az), V3(bx, ay, az), V3(bx, ay, bz), V3(ax, ay, bz), paint)
        quad(V3(ax, ay, bz), V3(bx, ay, bz), V3(bx, by, bz), V3(ax, by, bz), paint)
        quad(V3(bx, ay, az), V3(ax, ay, az), V3(ax, by, az), V3(bx, by, az), paint)
    }

    fun box(cx: Float, cy: Float, cz: Float, w: Float, h: Float, d: Float, paint: Paint) =
        block(cx - w / 2, cy - h / 2, cz - d / 2, cx + w / 2, cy + h / 2, cz + d / 2, paint)

    fun roundBox(cx: Float, cy: Float, cz: Float, w: Float, h: Float, d: Float, radius: Float, paint: Paint, segments: Int = 2) {
        val half = floatArrayOf(w / 2, h / 2, d / 2)
        val r = min(radius, min(half[0], min(half[1], half[2])))
        if (r <= 1e-4f) {
            box(cx, cy, cz, w, h, d, paint)
            return
        }
        val inner = FloatArray(3) { half[it] - r }
        val values = Array(3) { axis -> axisSamples(half[axis], inner[axis], r, segments) }
        val faces = arrayOf(
            intArrayOf(0, 1, 1, 2), intArrayOf(0, -1, 2, 1),
            intArrayOf(1, 1, 2, 0), intArrayOf(1, -1, 0, 2),
            intArrayOf(2, 1, 0, 1), intArrayOf(2, -1, 1, 0),
        )
        val q = FloatArray(3)
        for (f in faces) {
            val nAxis = f[0]; val sign = f[1]; val uAxis = f[2]; val vAxis = f[3]
            val us = values[uAxis]; val vs = values[vAxis]
            val grid = IntArray(us.size * vs.size)
            for (i in us.indices) for (j in vs.indices) {
                q[nAxis] = sign * half[nAxis]; q[uAxis] = us[i]; q[vAxis] = vs[j]
                val c0 = q[0].coerceIn(-inner[0], inner[0])
                val c1 = q[1].coerceIn(-inner[1], inner[1])
                val c2 = q[2].coerceIn(-inner[2], inner[2])
                val n = V3(q[0] - c0, q[1] - c1, q[2] - c2).normalized()
                grid[i * vs.size + j] = vertex(
                    cx + c0 + n.x * r, cy + c1 + n.y * r, cz + c2 + n.z * r,
                    n.x, n.y, n.z, paint,
                )
            }
            for (i in 0 until us.size - 1) for (j in 0 until vs.size - 1) {
                val a = grid[i * vs.size + j]
                val b = grid[(i + 1) * vs.size + j]
                val c = grid[(i + 1) * vs.size + j + 1]
                val dd = grid[i * vs.size + j + 1]
                triangle(a, b, c)
                triangle(a, c, dd)
            }
        }
    }

    private fun axisSamples(half: Float, inner: Float, r: Float, segments: Int): FloatArray {
        val out = ArrayList<Float>()
        for (k in segments downTo 1) out += -(inner + r * tan(k.toFloat() / segments * (PI / 4).toFloat()))
        out += -inner
        if (inner > 1e-4f) out += inner
        for (k in 1..segments) out += inner + r * tan(k.toFloat() / segments * (PI / 4).toFloat())
        out[0] = -half
        out[out.size - 1] = half
        return out.toFloatArray()
    }

    fun frustum(
        cx: Float, y0: Float, cz: Float,
        rBottom: Float, rTop: Float, height: Float,
        paint: Paint, sides: Int = 16, smooth: Boolean = true,
        capTop: Boolean = true, capBottom: Boolean = true,
    ) {
        val y1 = y0 + height
        val ny = rBottom - rTop
        for (s in 0 until sides) {
            val t0 = (s.toFloat() / sides) * 2f * PI.toFloat()
            val t1 = ((s + 1).toFloat() / sides) * 2f * PI.toFloat()
            val c0 = cos(t0); val s0 = sin(t0); val c1 = cos(t1); val s1 = sin(t1)
            if (smooth) {
                val n0 = V3(c0 * height, ny, s0 * height).normalized()
                val n1 = V3(c1 * height, ny, s1 * height).normalized()
                val a = vertex(cx + c0 * rBottom, y0, cz + s0 * rBottom, n0.x, n0.y, n0.z, paint)
                val b = vertex(cx + c0 * rTop, y1, cz + s0 * rTop, n0.x, n0.y, n0.z, paint)
                val c = vertex(cx + c1 * rTop, y1, cz + s1 * rTop, n1.x, n1.y, n1.z, paint)
                val d = vertex(cx + c1 * rBottom, y0, cz + s1 * rBottom, n1.x, n1.y, n1.z, paint)
                triangle(a, b, c)
                triangle(a, c, d)
            } else {
                quad(
                    V3(cx + c0 * rBottom, y0, cz + s0 * rBottom),
                    V3(cx + c0 * rTop, y1, cz + s0 * rTop),
                    V3(cx + c1 * rTop, y1, cz + s1 * rTop),
                    V3(cx + c1 * rBottom, y0, cz + s1 * rBottom),
                    paint,
                )
            }
        }
        if (capTop && rTop > 1e-4f) disc(cx, y1, cz, rTop, paint, sides, up = true)
        if (capBottom && rBottom > 1e-4f) disc(cx, y0, cz, rBottom, paint, sides, up = false)
    }

    fun cylinder(cx: Float, y0: Float, cz: Float, radius: Float, height: Float, paint: Paint, sides: Int = 16, smooth: Boolean = true, caps: Boolean = true) =
        frustum(cx, y0, cz, radius, radius, height, paint, sides, smooth, caps, caps)

    fun cone(cx: Float, y0: Float, cz: Float, radius: Float, height: Float, paint: Paint, sides: Int = 16, smooth: Boolean = true) =
        frustum(cx, y0, cz, radius, 0f, height, paint, sides, smooth, capTop = false, capBottom = true)

    fun disc(cx: Float, y: Float, cz: Float, radius: Float, paint: Paint, sides: Int = 16, up: Boolean = true) {
        val ny = if (up) 1f else -1f
        val center = vertex(cx, y, cz, 0f, ny, 0f, paint)
        val ring = IntArray(sides + 1) { s ->
            val t = (s.toFloat() / sides) * 2f * PI.toFloat()
            vertex(cx + cos(t) * radius, y, cz + sin(t) * radius, 0f, ny, 0f, paint)
        }
        for (s in 0 until sides) {
            if (up) triangle(center, ring[s + 1], ring[s]) else triangle(center, ring[s], ring[s + 1])
        }
    }

    fun annulus(cx: Float, y: Float, cz: Float, rIn: Float, rOut: Float, paint: Paint, sides: Int = 24) {
        for (s in 0 until sides) {
            val t0 = (s.toFloat() / sides) * 2f * PI.toFloat()
            val t1 = ((s + 1).toFloat() / sides) * 2f * PI.toFloat()
            quad(
                V3(cx + cos(t0) * rIn, y, cz + sin(t0) * rIn),
                V3(cx + cos(t1) * rIn, y, cz + sin(t1) * rIn),
                V3(cx + cos(t1) * rOut, y, cz + sin(t1) * rOut),
                V3(cx + cos(t0) * rOut, y, cz + sin(t0) * rOut),
                paint,
            )
        }
    }

    fun sphere(cx: Float, cy: Float, cz: Float, radius: Float, paint: Paint, rings: Int = 8, segments: Int = 12) =
        ellipsoid(cx, cy, cz, radius, radius, radius, paint, rings, segments)

    fun ellipsoid(cx: Float, cy: Float, cz: Float, rx: Float, ry: Float, rz: Float, paint: Paint, rings: Int = 8, segments: Int = 12) {
        val idx = Array(rings + 1) { IntArray(segments + 1) }
        for (i in 0..rings) {
            val phi = i.toFloat() / rings * PI.toFloat()
            for (j in 0..segments) {
                val theta = j.toFloat() / segments * 2f * PI.toFloat()
                val ux = sin(phi) * cos(theta)
                val uy = cos(phi)
                val uz = sin(phi) * sin(theta)
                val n = V3(ux / rx, uy / ry, uz / rz).normalized()
                idx[i][j] = vertex(cx + ux * rx, cy + uy * ry, cz + uz * rz, n.x, n.y, n.z, paint)
            }
        }
        for (i in 0 until rings) for (j in 0 until segments) {
            val a = idx[i + 1][j]
            val b = idx[i][j]
            val c = idx[i][j + 1]
            val d = idx[i + 1][j + 1]
            if (i != 0) triangle(a, b, c)
            if (i != rings - 1) triangle(a, c, d)
        }
    }

    fun blob(cx: Float, cy: Float, cz: Float, rx: Float, ry: Float, rz: Float, paint: Paint, subdivisions: Int = 1, jitter: Float = 0.12f, seed: Int = 0) {
        val (verts, faces) = icosphere(subdivisions)
        val pts = verts.map { v ->
            val k = 1f + (hash01((v.x * 97f).roundToInt(), (v.y * 89f + v.z * 61f).roundToInt(), seed) - 0.5f) * 2f * jitter
            V3(cx + v.x * rx * k, cy + v.y * ry * k, cz + v.z * rz * k)
        }
        val center = V3(cx, cy, cz)
        for (f in faces) {
            val a = pts[f[0]]; var b = pts[f[1]]; var c = pts[f[2]]
            val n = (b - a) cross (c - a)
            if ((n dot ((a + b + c) * (1f / 3f) - center)) < 0f) { val t = b; b = c; c = t }
            tri(a, b, c, paint)
        }
    }

    fun warpSphere(paint: Paint, rings: Int = 12, segments: Int = 18, shape: (Float, Float, Float) -> V3) {
        val grid = Array(rings + 1) { i ->
            val phi = i.toFloat() / rings * PI.toFloat()
            Array(segments + 1) { j ->
                val theta = j.toFloat() / segments * 2f * PI.toFloat()
                if (j == segments) null else shape(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta))
            }
        }
        val pts = Array(rings + 1) { i -> Array(segments + 1) { j -> grid[i][j % segments]!! } }
        val acc = Array(rings + 1) { Array(segments) { floatArrayOf(0f, 0f, 0f) } }
        fun add(i: Int, j: Int, n: V3) { val a = acc[i][j % segments]; a[0] += n.x; a[1] += n.y; a[2] += n.z }
        for (i in 0 until rings) for (j in 0 until segments) {
            val a = pts[i + 1][j]; val b = pts[i][j]; val c = pts[i][j + 1]; val d = pts[i + 1][j + 1]
            val n1 = (b - a) cross (c - a)
            val n2 = (c - a) cross (d - a)
            add(i + 1, j, n1 + n2); add(i, j, n1); add(i, j + 1, n1 + n2); add(i + 1, j + 1, n2)
        }
        val pole0 = acc[0].fold(V3(0f, 0f, 0f)) { s, v -> s + V3(v[0], v[1], v[2]) }
        val pole1 = acc[rings].fold(V3(0f, 0f, 0f)) { s, v -> s + V3(v[0], v[1], v[2]) }
        val ids = Array(rings + 1) { i ->
            IntArray(segments + 1) { j ->
                val p = pts[i][j]
                val n = when (i) {
                    0 -> pole0
                    rings -> pole1
                    else -> acc[i][j % segments].let { V3(it[0], it[1], it[2]) }
                }.normalized()
                vertex(p.x, p.y, p.z, n.x, n.y, n.z, paint)
            }
        }
        for (i in 0 until rings) for (j in 0 until segments) {
            val a = ids[i + 1][j]; val b = ids[i][j]; val c = ids[i][j + 1]; val d = ids[i + 1][j + 1]
            if (i != 0) triangle(a, b, c)
            if (i != rings - 1) triangle(a, c, d)
        }
    }

    fun tube(path: List<V3>, radius: (Float) -> Float, paint: Paint, sides: Int = 8, flatten: Float = 1f, up: V3 = V3(0f, 1f, 0f), caps: Boolean = true) {
        if (path.size < 2) return
        val n = path.size
        val tangents = List(n) { i ->
            val a = path[(i - 1).coerceAtLeast(0)]
            val b = path[(i + 1).coerceAtMost(n - 1)]
            (b - a).normalized()
        }
        var side = (tangents[0] cross up).let { if (it.length < 1e-4f) tangents[0] cross V3(1f, 0f, 0f) else it }.normalized()
        val rows = Array(n) { IntArray(sides + 1) }
        for (i in 0 until n) {
            val t = tangents[i]
            side = (side - t * (side dot t)).let { if (it.length < 1e-5f) side else it.normalized() }
            val bin = (t cross side).normalized()
            val r = radius(i / (n - 1f))
            for (s in 0..sides) {
                val a = s.toFloat() / sides * 2f * PI.toFloat()
                val ca = cos(a); val sa = sin(a)
                val dir = side * ca + bin * (sa * flatten)
                val nrm = (side * (ca * flatten) + bin * sa).normalized()
                val p = path[i] + dir * r
                rows[i][s] = vertex(p.x, p.y, p.z, nrm.x, nrm.y, nrm.z, paint)
            }
        }
        for (i in 0 until n - 1) for (s in 0 until sides) {
            val a = rows[i][s]; val b = rows[i][s + 1]; val c = rows[i + 1][s + 1]; val d = rows[i + 1][s]
            triangle(a, b, c)
            triangle(a, c, d)
        }
        if (caps) {
            val r0 = radius(0f); val r1 = radius(1f)
            if (r0 > 1e-4f) ellipsoid(path[0].x, path[0].y, path[0].z, r0, r0, r0, paint, 4, sides)
            if (r1 > 1e-4f) ellipsoid(path[n - 1].x, path[n - 1].y, path[n - 1].z, r1, r1, r1, paint, 4, sides)
        }
    }

    fun dome(radius: Float, rings: Int, segments: Int, maxPolarDeg: Float, azimuthOffsetDeg: Float, paint: Paint) {
        val ids = Array(rings + 1) { IntArray(segments + 1) }
        for (i in 0..rings) {
            val polar = maxPolarDeg * i / rings
            val th = polar * PI.toFloat() / 180f
            val v = (polar / 90f).coerceAtMost(0.995f)
            for (j in 0..segments) {
                val u = j.toFloat() / segments
                val ph = (u * 360f + azimuthOffsetDeg) * PI.toFloat() / 180f
                val dx = sin(th) * cos(ph)
                val dy = cos(th)
                val dz = sin(th) * sin(ph)
                ids[i][j] = vertex(dx * radius, dy * radius, dz * radius, -dx, -dy, -dz, paint, u, v)
            }
        }
        for (i in 0 until rings) for (j in 0 until segments) {
            val a = ids[i][j]; val b = ids[i][j + 1]; val c = ids[i + 1][j + 1]; val d = ids[i + 1][j]
            if (i != 0) triangle(a, b, c)
            triangle(a, c, d)
        }
    }

    fun puff(
        cx: Float, cy: Float, cz: Float, rx: Float, ry: Float, rz: Float, paint: Paint,
        subdivisions: Int = 2, wobble: Float = 0.08f, seed: Int = 0, floorY: Float = Float.NEGATIVE_INFINITY,
    ) {
        val (verts, faces) = icosphere(subdivisions)
        val a = hash01(seed, 1, 3) * 6.28f
        val b = hash01(seed, 2, 5) * 6.28f
        val c = hash01(seed, 3, 7) * 6.28f
        val pts = verts.map { v ->
            val lump = (sin(v.x * 3.1f + a) + sin(v.y * 2.7f + b) + sin(v.z * 3.3f + c)) / 3f +
                0.5f * (sin(v.x * 6.3f + b) * sin(v.z * 5.9f + c)) * sin(v.y * 6.1f + a)
            val k = 1f + wobble * lump
            V3(cx + v.x * rx * k, maxOf(floorY, cy + v.y * ry * k), cz + v.z * rz * k)
        }
        val acc = Array(pts.size) { floatArrayOf(0f, 0f, 0f) }
        val center = V3(cx, cy, cz)
        val oriented = faces.map { f ->
            val p0 = pts[f[0]]; val p1 = pts[f[1]]; val p2 = pts[f[2]]
            val n = (p1 - p0) cross (p2 - p0)
            val flip = (n dot ((p0 + p1 + p2) * (1f / 3f) - center)) < 0f
            val fn = if (flip) n * -1f else n
            for (k in f) { acc[k][0] += fn.x; acc[k][1] += fn.y; acc[k][2] += fn.z }
            if (flip) intArrayOf(f[0], f[2], f[1]) else f
        }
        val ids = IntArray(pts.size) { k ->
            val n = V3(acc[k][0], acc[k][1], acc[k][2]).normalized()
            vertex(pts[k].x, pts[k].y, pts[k].z, n.x, n.y, n.z, paint)
        }
        for (f in oriented) triangle(ids[f[0]], ids[f[1]], ids[f[2]])
    }

    fun lathe(cx: Float, cz: Float, profile: List<Pair<Float, Float>>, paint: Paint, sides: Int = 16, creaseDeg: Float = 40f) {
        if (profile.size < 2) return
        val segNormals = (0 until profile.size - 1).map { i ->
            val dr = profile[i + 1].first - profile[i].first
            val dy = profile[i + 1].second - profile[i].second
            val l = kotlin.math.sqrt(dr * dr + dy * dy).coerceAtLeast(1e-6f)
            dy / l to -dr / l
        }
        val creaseCos = cos(creaseDeg * PI.toFloat() / 180f)
        fun endNormal(seg: Int, atStart: Boolean): Pair<Float, Float> {
            val n = segNormals[seg]
            val neighbor = if (atStart) seg - 1 else seg + 1
            if (neighbor < 0 || neighbor >= segNormals.size) return n
            val m = segNormals[neighbor]
            if (n.first * m.first + n.second * m.second < creaseCos) return n
            val sx = n.first + m.first
            val sy = n.second + m.second
            val l = kotlin.math.sqrt(sx * sx + sy * sy).coerceAtLeast(1e-6f)
            return sx / l to sy / l
        }
        for (seg in 0 until profile.size - 1) {
            val (r0, y0) = profile[seg]
            val (r1, y1) = profile[seg + 1]
            if (abs(r0) < 1e-5f && abs(r1) < 1e-5f) continue
            val n0 = endNormal(seg, true)
            val n1 = endNormal(seg, false)
            val bottom = IntArray(sides + 1)
            val top = IntArray(sides + 1)
            for (s in 0..sides) {
                val t = (s.toFloat() / sides) * 2f * PI.toFloat()
                val c = cos(t); val sn = sin(t)
                bottom[s] = vertex(cx + c * r0, y0, cz + sn * r0, c * n0.first, n0.second, sn * n0.first, paint)
                top[s] = vertex(cx + c * r1, y1, cz + sn * r1, c * n1.first, n1.second, sn * n1.first, paint)
            }
            for (s in 0 until sides) {
                if (r1 > 1e-5f) triangle(bottom[s], top[s], top[s + 1])
                if (r0 > 1e-5f) triangle(bottom[s], top[s + 1], bottom[s + 1])
            }
        }
    }

    fun torus(cx: Float, cy: Float, cz: Float, major: Float, minor: Float, paint: Paint, segments: Int = 24, sides: Int = 10) {
        val idx = Array(segments + 1) { IntArray(sides + 1) }
        for (i in 0..segments) {
            val t = i.toFloat() / segments * 2f * PI.toFloat()
            for (j in 0..sides) {
                val p = j.toFloat() / sides * 2f * PI.toFloat()
                val nx = cos(p) * cos(t); val ny = sin(p); val nz = cos(p) * sin(t)
                idx[i][j] = vertex(
                    cx + (major + minor * cos(p)) * cos(t), cy + minor * sin(p), cz + (major + minor * cos(p)) * sin(t),
                    nx, ny, nz, paint,
                )
            }
        }
        for (i in 0 until segments) for (j in 0 until sides) {
            val a = idx[i][j]; val b = idx[i][j + 1]; val c = idx[i + 1][j + 1]; val d = idx[i + 1][j]
            triangle(a, b, c)
            triangle(a, c, d)
        }
    }

    fun arc(cx: Float, cy: Float, cz: Float, radius: Float, tube: Float, fromDeg: Float, toDeg: Float, paint: Paint, segments: Int = 12, sides: Int = 6) {
        val idx = Array(segments + 1) { IntArray(sides + 1) }
        for (i in 0..segments) {
            val t = (fromDeg + (toDeg - fromDeg) * i / segments) * PI.toFloat() / 180f
            val ox = cos(t) * radius
            val oy = sin(t) * radius
            for (j in 0..sides) {
                val p = j.toFloat() / sides * 2f * PI.toFloat()
                val nx = cos(p) * cos(t)
                val ny = cos(p) * sin(t)
                val nz = sin(p)
                idx[i][j] = vertex(cx + ox + nx * tube, cy + oy + ny * tube, cz + nz * tube, nx, ny, nz, paint)
            }
        }
        for (i in 0 until segments) for (j in 0 until sides) {
            val a = idx[i][j]; val b = idx[i][j + 1]; val c = idx[i + 1][j + 1]; val d = idx[i + 1][j]
            triangle(a, c, b)
            triangle(a, d, c)
        }
        sphere(cx + cos(fromDeg * PI.toFloat() / 180f) * radius, cy + sin(fromDeg * PI.toFloat() / 180f) * radius, cz, tube, paint, 4, 6)
        sphere(cx + cos(toDeg * PI.toFloat() / 180f) * radius, cy + sin(toDeg * PI.toFloat() / 180f) * radius, cz, tube, paint, 4, 6)
    }

    fun shadowBlob(cx: Float, cz: Float, rx: Float, rz: Float, y: Float = 0.012f) {
        withMat(Mat.SHADOW_BLOB) {
            uvOverride = { x, _, z -> ((x - cx) / (2 * rx) + 0.5f) to ((z - cz) / (2 * rz) + 0.5f) }
            quad(V3(cx - rx, y, cz + rz), V3(cx + rx, y, cz + rz), V3(cx + rx, y, cz - rz), V3(cx - rx, y, cz - rz), Rgb.BLACK)
            uvOverride = null
        }
    }

    fun capsule(cx: Float, y0: Float, cz: Float, radius: Float, height: Float, paint: Paint, sides: Int = 12, capRings: Int = 4) {
        val prof = ArrayList<Pair<Float, Float>>()
        for (k in 0..capRings) {
            val a = (-PI / 2 + k * (PI / 2) / capRings).toFloat()
            prof += cos(a) * radius to y0 + radius + sin(a) * radius
        }
        for (k in 0..capRings) {
            val a = (k * (PI / 2) / capRings).toFloat()
            prof += cos(a) * radius to y0 + height - radius + sin(a) * radius
        }
        lathe(cx, cz, prof, paint, sides, creaseDeg = 80f)
    }

    fun terrain(x0: Float, z0: Float, x1: Float, z1: Float, cellsX: Int, cellsZ: Int, paint: Paint, height: (Float, Float) -> Float = { _, _ -> 0f }) {
        val dx = (x1 - x0) / cellsX
        val dz = (z1 - z0) / cellsZ
        for (i in 0 until cellsX) for (j in 0 until cellsZ) {
            val ax = x0 + i * dx; val bx = ax + dx
            val az = z0 + j * dz; val bz = az + dz
            val p00 = V3(ax, height(ax, az), az)
            val p10 = V3(bx, height(bx, az), az)
            val p01 = V3(ax, height(ax, bz), bz)
            val p11 = V3(bx, height(bx, bz), bz)
            tri(p01, p11, p10, paint)
            tri(p01, p10, p00, paint)
        }
    }

    fun heightField(x0: Float, z0: Float, x1: Float, z1: Float, cellsX: Int, cellsZ: Int, paint: Paint, height: (Float, Float) -> Float) {
        val dx = (x1 - x0) / cellsX
        val dz = (z1 - z0) / cellsZ
        val ids = Array(cellsX + 1) { IntArray(cellsZ + 1) }
        for (i in 0..cellsX) for (j in 0..cellsZ) {
            val x = x0 + i * dx
            val z = z0 + j * dz
            val hx = height(x + dx * 0.5f, z) - height(x - dx * 0.5f, z)
            val hz = height(x, z + dz * 0.5f) - height(x, z - dz * 0.5f)
            val n = V3(-hx / dx, 1f, -hz / dz).normalized()
            ids[i][j] = vertex(x, height(x, z), z, n.x, n.y, n.z, paint)
        }
        for (i in 0 until cellsX) for (j in 0 until cellsZ) {
            val a = ids[i][j]; val b = ids[i + 1][j]; val c = ids[i + 1][j + 1]; val d = ids[i][j + 1]
            triangle(a, d, c)
            triangle(a, c, b)
        }
    }

    fun include(mesh: Mesh) {
        for (part in mesh.parts) {
            withMat(part.mat) {
                val base = IntArray(part.vertexCount)
                for (v in 0 until part.vertexCount) {
                    val i3 = v * 3
                    val p = transform.point(part.positions[i3], part.positions[i3 + 1], part.positions[i3 + 2])
                    val n = transform.normal(part.normals[i3], part.normals[i3 + 1], part.normals[i3 + 2])
                    val i4 = v * 4
                    base[v] = current.addVertexLinear(p, n, part.colors[i4], part.colors[i4 + 1], part.colors[i4 + 2], part.uvs.getOrElse(v * 2) { 0f }, part.uvs.getOrElse(v * 2 + 1) { 0f })
                }
                var t = 0
                while (t < part.indices.size) {
                    triangle(base[part.indices[t]], base[part.indices[t + 1]], base[part.indices[t + 2]])
                    t += 3
                }
            }
        }
    }

    fun build(): Mesh = Mesh(buffers.values.filter { it.iCount > 0 }.map { it.toPart() })

    private fun PartBuffer.addVertexLinear(p: V3, n: V3, r: Float, g: Float, b: Float, u: Float, v: Float): Int {
        val i = addVertex(p, n, Rgb.WHITE, u, v)
        val i4 = i * 4
        colors[i4] = r; colors[i4 + 1] = g; colors[i4 + 2] = b
        return i
    }

    companion object {
        private val icoCache = HashMap<Int, Pair<List<V3>, List<IntArray>>>()

        fun icosphere(subdivisions: Int): Pair<List<V3>, List<IntArray>> = synchronized(icoCache) {
            icoCache.getOrPut(subdivisions) {
                val t = ((1.0 + kotlin.math.sqrt(5.0)) / 2.0).toFloat()
                val verts = mutableListOf(
                    V3(-1f, t, 0f), V3(1f, t, 0f), V3(-1f, -t, 0f), V3(1f, -t, 0f),
                    V3(0f, -1f, t), V3(0f, 1f, t), V3(0f, -1f, -t), V3(0f, 1f, -t),
                    V3(t, 0f, -1f), V3(t, 0f, 1f), V3(-t, 0f, -1f), V3(-t, 0f, 1f),
                ).map { it.normalized() }.toMutableList()
                var faces = listOf(
                    intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
                    intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
                    intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
                    intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1),
                )
                repeat(subdivisions) {
                    val mid = HashMap<Long, Int>()
                    fun midpoint(a: Int, b: Int): Int {
                        val key = if (a < b) (a.toLong() shl 32) or b.toLong() else (b.toLong() shl 32) or a.toLong()
                        return mid.getOrPut(key) {
                            verts += ((verts[a] + verts[b]) * 0.5f).normalized()
                            verts.size - 1
                        }
                    }
                    faces = faces.flatMap { f ->
                        val ab = midpoint(f[0], f[1]); val bc = midpoint(f[1], f[2]); val ca = midpoint(f[2], f[0])
                        listOf(intArrayOf(f[0], ab, ca), intArrayOf(f[1], bc, ab), intArrayOf(f[2], ca, bc), intArrayOf(ab, bc, ca))
                    }
                }
                verts.toList() to faces
            }
        }
    }
}

inline fun mesh(block: MeshBuilder.() -> Unit): Mesh = MeshBuilder().apply(block).build()

fun smoothPath(points: List<V3>, perSegment: Int = 4): List<V3> {
    if (points.size < 3) return points
    val out = ArrayList<V3>()
    for (i in 0 until points.size - 1) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
        for (k in 0 until perSegment) {
            val t = k / perSegment.toFloat()
            val t2 = t * t
            val t3 = t2 * t
            out += (p1 * 2f + (p2 - p0) * t + (p0 * 2f - p1 * 5f + p2 * 4f - p3) * t2 + (p1 * 3f - p0 - p2 * 3f + p3) * t3) * 0.5f
        }
    }
    out += points.last()
    return out
}
