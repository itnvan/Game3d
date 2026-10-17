package com.cozyhome.game.models

import com.cozyhome.game.math.DEG
import com.cozyhome.game.math.V3
import com.cozyhome.game.math.lerp
import com.cozyhome.game.math.smoothstep
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Paint
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.mesh.smoothPath
import com.cozyhome.game.state.Appearance
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

data class Outfit(
    val skin: Rgb,
    val hair: Rgb,
    val shirt: Rgb,
    val pants: Rgb,
    val shoes: Rgb,
    val hairStyle: Int,
    val hat: Int,
    val eyes: Rgb = Palette.EYES[0],
    val top: Int = 0,
    val bottom: Int = 0,
    val eyeStyle: Int = 0,
) {
    val bareLegs: Boolean get() = top == CharacterModel.TOP_DRESS || bottom == CharacterModel.BOTTOM_SKIRT
    val longSleeves: Boolean get() = top == CharacterModel.TOP_HOODIE || top == CharacterModel.TOP_SWEATER || top == CharacterModel.TOP_JACKET

    companion object {
        fun of(a: Appearance) = Outfit(
            skin = Palette.SKIN[a.skin.mod(Palette.SKIN.size)],
            hair = Palette.HAIR[a.hairColor.mod(Palette.HAIR.size)],
            shirt = Palette.SHIRT[a.shirt.mod(Palette.SHIRT.size)],
            pants = Palette.PANTS[a.pants.mod(Palette.PANTS.size)],
            shoes = Palette.SHOES[a.shoes.mod(Palette.SHOES.size)],
            hairStyle = a.hairStyle.mod(Palette.HAIR_STYLES),
            hat = a.hat.mod(Palette.HATS),
            eyes = Palette.EYES[a.eyeColor.mod(Palette.EYES.size)],
            top = a.top.mod(Palette.TOPS),
            bottom = a.bottom.mod(Palette.BOTTOMS),
            eyeStyle = a.eyeStyle.mod(Palette.EYE_STYLES),
        )

        val KEEPER = Outfit(
            Palette.SKIN[2], Palette.HAIR[0], rgb(0xFF9F43), rgb(0x3B3355), rgb(0xFFFFFF), 2, 1,
            eyes = Palette.EYES[1], top = CharacterModel.TOP_JACKET, bottom = CharacterModel.BOTTOM_SHORTS,
        )
    }
}

object CharacterModel {
    const val HIP = 0.42f
    const val NECK = 0.44f
    const val SHOULDER_X = 0.2f
    const val SHOULDER_Y = 0.355f
    const val LEG_X = 0.085f
    const val EYE_Y = 0.235f
    const val HAND_Y = -0.325f

    const val TOP_TEE = 0
    const val TOP_HOODIE = 1
    const val TOP_OVERALLS = 2
    const val TOP_SWEATER = 3
    const val TOP_DRESS = 4
    const val TOP_JACKET = 5
    const val BOTTOM_PANTS = 0
    const val BOTTOM_SHORTS = 1
    const val BOTTOM_SKIRT = 2

    private const val HEAD_CY = 0.27f
    private const val HEAD_RX = 0.3f
    private const val HEAD_RY = 0.28f
    private const val HEAD_RZ = 0.285f
    private val INK = rgb(0x2A2133)
    private val LIP = rgb(0xC0526A)
    private val BLUSH = rgb(0xFF8FA6)
    private val SOLE = rgb(0xF7F3EE)
    private val HIGHLIGHT = rgb(0xFFFFFF)

    private fun headOffset(ux: Float, uy: Float, uz: Float): V3 {
        val low = max(0f, -uy)
        val taper = 1f - 0.15f * low.pow(1.5f)
        val front = max(0f, uz)
        val cheek = 1f + 0.04f * exp(-((uy + 0.4f) / 0.28f).let { it * it }) * front
        val flat = 1f - 0.05f * front.pow(4)
        return V3(ux * HEAD_RX * taper * cheek, uy * HEAD_RY, uz * HEAD_RZ * taper * flat)
    }

    private fun headPoint(dir: V3, lift: Float = 0f): V3 {
        val o = headOffset(dir.x, dir.y, dir.z)
        val k = 1f + lift
        return V3(o.x * k, HEAD_CY + o.y * k, o.z * k)
    }

    private fun dirOf(azDeg: Float, polarDeg: Float): V3 {
        val a = azDeg * DEG
        val p = polarDeg * DEG
        return V3(sin(p) * sin(a), cos(p), sin(p) * cos(a))
    }

    private class Spot(val p: V3, val n: V3)

    private val spots = HashMap<Long, Spot>()

    private fun spotAt(el: Float, az: Float): V3 {
        val ce = cos(el)
        return headPoint(V3(ce * sin(az), sin(el), ce * cos(az)))
    }

    private fun faceSpot(x: Float, y: Float): Spot = synchronized(spots) {
        val key = ((x * 10000f).toInt().toLong() shl 32) xor (y * 10000f).toInt().toLong()
        spots.getOrPut(key) {
            var bestEl = 0f
            var bestAz = 0f
            var span = 1.5f
            repeat(4) {
                var best = Float.MAX_VALUE
                val e0 = bestEl
                val a0 = bestAz
                for (i in -16..16) for (j in -16..16) {
                    val el = e0 + span * i / 16f
                    val az = a0 + span * j / 16f
                    val p = spotAt(el, az)
                    val d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y)
                    if (d < best) { best = d; bestEl = el; bestAz = az }
                }
                span /= 8f
            }
            val d = 0.01f
            val p = spotAt(bestEl, bestAz)
            val tAz = spotAt(bestEl, bestAz + d) - spotAt(bestEl, bestAz - d)
            val tEl = spotAt(bestEl + d, bestAz) - spotAt(bestEl - d, bestAz)
            Spot(p, (tAz cross tEl).normalized())
        }
    }

    private fun MeshBuilder.onFace(x: Float, y: Float, out: Float, block: MeshBuilder.() -> Unit) {
        val s = faceSpot(x, y)
        group {
            translate(s.p.x + s.n.x * out, s.p.y + s.n.y * out, s.p.z + s.n.z * out)
            rotateY(atan2(s.n.x, s.n.z) / DEG)
            rotateX(-asin(s.n.y.coerceIn(-1f, 1f)) / DEG)
            block()
        }
    }

    fun torso(o: Outfit): Mesh = mesh {
        val cloth = if (o.top == TOP_SWEATER) Mat.KNIT else Mat.CLOTH
        val pantsMat = Mat.CLOTH
        if (o.top != TOP_DRESS) {
            if (o.bottom == BOTTOM_SKIRT) skirt(o.pants, pantsMat, 0.05f)
            else withMat(pantsMat) {
                warpSphere(o.pants, 8, 16) { x, y, z -> V3(x * 0.16f, 0.035f + y * 0.1f, z * 0.118f) }
            }
        }
        withMat(cloth) {
            group {
                scale(1f, 1f, 0.74f)
                lathe(
                    0f, 0f,
                    listOf(
                        0.17f to -0.02f, 0.166f to 0.04f, 0.167f to 0.15f, 0.16f to 0.25f,
                        0.146f to 0.32f, 0.118f to 0.375f, 0.07f to 0.405f, 0.0f to 0.415f,
                    ),
                    o.shirt, sides = 20, creaseDeg = 70f,
                )
            }
        }
        withMat(Mat.SKIN) { cylinder(0f, 0.36f, 0f, 0.052f, 0.12f, o.skin, sides = 12) }
        val trim = o.shirt.darker(0.1f)
        when (o.top) {
            TOP_TEE -> withMat(cloth) {
                collar(0.07f, 0.012f, trim)
                hem(trim)
                roundBox(-0.075f, 0.27f, 0.117f, 0.06f, 0.06f, 0.012f, 0.008f, trim)
            }
            TOP_HOODIE -> withMat(cloth) {
                collar(0.075f, 0.02f, trim)
                hem(trim)
                puff(0f, 0.375f, -0.095f, 0.14f, 0.075f, 0.08f, o.shirt, subdivisions = 2, wobble = 0.04f, seed = 3)
                roundBox(0f, 0.115f, 0.118f, 0.2f, 0.09f, 0.022f, 0.012f, trim)
                withMat(Mat.SOFT) {
                    for (s in listOf(-1f, 1f)) {
                        cylinder(s * 0.032f, 0.31f, 0.108f, 0.006f, 0.085f, Palette.WHITE, sides = 6)
                        sphere(s * 0.032f, 0.305f, 0.108f, 0.011f, Palette.WHITE, 3, 6)
                    }
                }
            }
            TOP_OVERALLS -> {
                withMat(cloth) { collar(0.07f, 0.012f, trim); hem(trim) }
                withMat(pantsMat) {
                    roundBox(0f, 0.2f, 0.11f, 0.18f, 0.17f, 0.03f, 0.012f, o.pants)
                    roundBox(0f, 0.2f, 0.126f, 0.09f, 0.06f, 0.006f, 0.004f, o.pants.darker(0.1f))
                    for (s in listOf(-1f, 1f)) {
                        tube(
                            listOf(V3(s * 0.07f, 0.28f, 0.118f), V3(s * 0.095f, 0.36f, 0.085f), V3(s * 0.1f, 0.395f, 0f), V3(s * 0.09f, 0.35f, -0.1f), V3(s * 0.07f, 0.25f, -0.122f)),
                            { 0.016f }, o.pants, sides = 6, flatten = 0.35f, up = V3(0f, 0f, 1f), caps = false,
                        )
                    }
                }
                withMat(Mat.METAL) {
                    sphere(-0.07f, 0.275f, 0.128f, 0.014f, Palette.YELLOW, 3, 8)
                    sphere(0.07f, 0.275f, 0.128f, 0.014f, Palette.YELLOW, 3, 8)
                }
            }
            TOP_SWEATER -> withMat(cloth) {
                collar(0.072f, 0.022f, trim)
                hem(trim, 0.02f)
                val band = Palette.WHITE.mix(o.shirt, 0.2f)
                group { scale(1f, 1f, 0.74f); lathe(0f, 0f, listOf(0.1695f to 0.17f, 0.1685f to 0.25f), band, sides = 20) }
                for (k in 0 until 8) {
                    val a = k * 45f * DEG
                    sphere(sin(a) * 0.168f, 0.21f, cos(a) * 0.168f * 0.74f, 0.012f, o.shirt.darker(0.1f), 3, 6)
                }
            }
            TOP_DRESS -> {
                skirt(o.shirt, cloth, 0.07f)
                withMat(Mat.SOFT) {
                    for (s in listOf(-1f, 1f)) {
                        group {
                            translate(s * 0.045f, 0.395f, 0.075f)
                            rotateZ(s * -25f)
                            ellipsoid(0f, 0f, 0f, 0.05f, 0.022f, 0.035f, Palette.WHITE, 4, 10)
                        }
                    }
                    group { scale(1f, 1f, 0.74f); torus(0f, 0.075f, 0f, 0.158f, 0.014f, Palette.WHITE, 24, 6) }
                    sphere(0f, 0.075f, -0.125f, 0.03f, Palette.WHITE, 4, 8)
                    ellipsoid(-0.04f, 0.08f, -0.125f, 0.04f, 0.025f, 0.015f, Palette.WHITE, 4, 8)
                    ellipsoid(0.04f, 0.08f, -0.125f, 0.04f, 0.025f, 0.015f, Palette.WHITE, 4, 8)
                }
            }
            else -> withMat(cloth) {
                roundBox(0f, 0.22f, 0.112f, 0.075f, 0.31f, 0.02f, 0.008f, Palette.OFFWHITE)
                roundBox(0f, 0.22f, 0.123f, 0.012f, 0.29f, 0.006f, 0.003f, trim.darker(0.2f))
                for (s in listOf(-1f, 1f)) {
                    group {
                        translate(s * 0.055f, 0.36f, 0.09f)
                        rotateY(s * 25f)
                        rotateZ(s * 30f)
                        roundBox(0f, 0f, 0f, 0.07f, 0.1f, 0.018f, 0.008f, trim)
                    }
                    roundBox(s * 0.1f, 0.11f, 0.111f, 0.06f, 0.012f, 0.012f, 0.005f, trim.darker(0.1f))
                }
                hem(trim, 0.016f)
            }
        }
    }

    private fun MeshBuilder.hem(color: Rgb, tube: Float = 0.012f) {
        group { scale(1f, 1f, 0.74f); torus(0f, -0.012f, 0f, 0.168f, tube, color, 24, 6) }
    }

    private fun MeshBuilder.collar(radius: Float, tube: Float, color: Rgb) {
        group { scale(1f, 1f, 0.82f); torus(0f, 0.4f, 0f, radius, tube, color, 20, 6) }
    }

    private fun MeshBuilder.skirt(color: Rgb, cloth: Mat, waist: Float) {
        withMat(cloth) {
            frustum(0f, -0.17f, 0f, 0.245f, 0.158f, waist + 0.17f, color, sides = 16, smooth = false, capTop = false, capBottom = false)
            frustum(0f, -0.17f, 0f, 0.2f, 0.245f, 0.001f, color.darker(0.18f), sides = 16, smooth = false, capTop = false, capBottom = true)
            group { scale(1f, 1f, 0.8f); torus(0f, waist, 0f, 0.158f, 0.014f, color.darker(0.1f), 24, 6) }
        }
    }

    fun head(o: Outfit): Mesh = mesh {
        withMat(Mat.SKIN) {
            warpSphere(o.skin, 16, 24) { x, y, z -> headOffset(x, y, z).let { V3(it.x, HEAD_CY + it.y, it.z) } }
            for (s in listOf(-1f, 1f)) {
                ellipsoid(s * 0.292f, HEAD_CY - 0.03f, -0.01f, 0.038f, 0.058f, 0.044f, o.skin, 6, 10)
                ellipsoid(s * 0.312f, HEAD_CY - 0.03f, 0.0f, 0.016f, 0.036f, 0.024f, o.skin.darker(0.08f), 4, 8)
            }
            onFace(0f, EYE_Y - 0.06f, 0f) { ellipsoid(0f, 0f, 0f, 0.017f, 0.012f, 0.012f, o.skin.darker(0.06f), 4, 8) }
        }
        withMat(Mat.SOFT) {
            onFace(0f, EYE_Y - 0.115f, 0f) {
                if (o.eyeStyle == 2) {
                    ellipsoid(0f, 0.004f, 0f, 0.03f, 0.022f, 0.012f, LIP, 4, 10)
                    ellipsoid(0f, -0.006f, 0.006f, 0.017f, 0.01f, 0.008f, rgb(0xFF8C9E), 3, 8)
                } else {
                    arc(0f, 0.022f, 0f, 0.03f, 0.0075f, 205f, 335f, LIP, segments = 10, sides = 5)
                }
            }
            val brow = o.hair.darker(0.3f)
            for (s in listOf(-1f, 1f)) {
                onFace(s * 0.105f, EYE_Y + 0.098f, 0.002f) {
                    rotateZ(s * -6f)
                    arc(0f, -0.05f, 0f, 0.06f, 0.0095f, 66f, 114f, brow, segments = 6, sides = 5)
                }
            }
        }
        withMat(Mat.SOFT_DOT.copy(alpha = 0.75f)) {
            for (s in listOf(-1f, 1f)) {
                onFace(s * 0.168f, EYE_Y - 0.08f, 0.005f) {
                    uvOverride = { x, y, _ -> (x / 0.11f + 0.5f) to (y / 0.065f + 0.5f) }
                    quad(V3(-0.055f, -0.0325f, 0f), V3(0.055f, -0.0325f, 0f), V3(0.055f, 0.0325f, 0f), V3(-0.055f, 0.0325f, 0f), BLUSH)
                    uvOverride = null
                }
            }
        }
        hair(o)
        hat(o)
    }

    fun eyes(o: Outfit): Mesh = mesh {
        group {
            translate(0f, -EYE_Y, 0f)
            for (s in listOf(-1f, 1f)) {
                onFace(s * 0.108f, EYE_Y, 0f) {
                    if (s < 0f) scale(-1f, 1f, 1f)
                    eye(o)
                }
            }
        }
    }

    private fun MeshBuilder.eye(o: Outfit) {
        if (o.eyeStyle == 2) {
            withMat(Mat.SOFT) { arc(0f, -0.022f, 0.006f, 0.04f, 0.011f, 25f, 155f, INK, segments = 10, sides = 6) }
            return
        }
        val iris = Paint { _, y, _ -> o.eyes.darker(0.35f).mix(o.eyes.lighter(0.2f), smoothstep(0.03f, -0.05f, y)) }
        withMat(Mat.EYE) {
            ellipsoid(0f, 0f, -0.004f, 0.05f, 0.064f, 0.016f, INK, 8, 14)
            ellipsoid(0f, -0.012f, 0.004f, 0.039f, 0.046f, 0.009f, iris, 6, 12)
            ellipsoid(0f, -0.006f, 0.01f, 0.02f, 0.026f, 0.005f, INK, 4, 10)
        }
        withMat(Mat.UNLIT) {
            ellipsoid(0.017f, 0.022f, 0.014f, 0.016f, 0.019f, 0.004f, HIGHLIGHT, 4, 10)
            ellipsoid(-0.017f, -0.03f, 0.013f, 0.008f, 0.008f, 0.003f, HIGHLIGHT, 3, 8)
        }
        withMat(Mat.SOFT) {
            group {
                scale(1f, 1.15f, 1f)
                arc(0f, 0f, 0.004f, 0.054f, 0.0085f, 18f, 162f, INK, segments = 12, sides = 5)
            }
            if (o.eyeStyle == 1) {
                tube(listOf(V3(0.048f, 0.03f, 0.008f), V3(0.066f, 0.045f, 0.004f), V3(0.078f, 0.062f, 0f)), { 0.007f * (1f - it * 0.8f) }, INK, sides = 5, caps = false)
                tube(listOf(V3(0.036f, 0.05f, 0.01f), V3(0.048f, 0.068f, 0.006f), V3(0.054f, 0.082f, 0.002f)), { 0.006f * (1f - it * 0.8f) }, INK, sides = 5, caps = false)
            } else {
                tube(listOf(V3(0.05f, 0.028f, 0.008f), V3(0.064f, 0.038f, 0.004f)), { 0.007f * (1f - it * 0.7f) }, INK, sides = 5, caps = false)
            }
        }
    }

    private fun hairPaint(c: Rgb): Paint {
        val hi = c.lighter(0.34f)
        val lo = c.darker(0.2f)
        return Paint { _, y, z ->
            val yy = y - HEAD_CY
            val band = smoothstep(0.075f, 0f, abs(yy - 0.17f)) * smoothstep(-0.12f, 0.12f, z)
            val shade = smoothstep(0.08f, -0.3f, yy)
            c.mix(lo, shade * 0.85f).mix(hi, band * 0.65f)
        }
    }

    private class HairKit(val b: MeshBuilder, val paint: Paint, val sq: Float) {
        fun cap(front: Float, side: Float, back: Float, lift: Float = 0.07f) {
            b.warpSphere(paint, 32, 44) { x, y, z ->
                val polar = acos(y.coerceIn(-1f, 1f)) / DEG
                val f = abs(atan2(x, z) / DEG) / 180f
                val limit = if (f < 0.5f) lerp(front, side, smoothstep(0.12f, 0.5f, f)) else lerp(side, back, smoothstep(0.52f, 0.72f, f))
                val inside = smoothstep(limit - 8f, limit + 8f, polar)
                val o = headOffset(x, y, z)
                val k = lerp(1f + lift * sq, 0.84f, inside)
                V3(o.x * k, HEAD_CY + o.y * k, o.z * k)
            }
        }

        fun lock(
            az: Float, polarFrom: Float, polarTo: Float, width: Float,
            sweep: Float = 0f, liftFrom: Float = 0.05f, liftTo: Float = 0.1f, curl: Float = 0.02f, flatten: Float = 0.42f,
        ) {
            val steps = 10
            val path = (0..steps).map { k ->
                val t = k / steps.toFloat()
                val d = dirOf(az + sweep * t * t, lerp(polarFrom, polarTo, t))
                val p = headPoint(d, lerp(liftFrom, liftTo, t) * sq)
                p + d * (curl * sq * t * t * t)
            }
            b.tube(path, { t -> max(0.002f, width * (1f - 0.86f * t.pow(1.5f))) }, paint, sides = 7, flatten = flatten, up = dirOf(az, polarFrom), caps = false)
        }

        fun tuft(az: Float, polar: Float, length: Float, width: Float, bendAz: Float = 0f, bendPolar: Float = 25f) {
            val path = (0..6).map { k ->
                val t = k / 6f
                headPoint(dirOf(az + bendAz * t, polar + bendPolar * t), (0.03f + length / HEAD_RY * t) * sq)
            }
            b.tube(path, { t -> max(0.003f, width * (1f - t).pow(0.8f)) }, paint, sides = 8, flatten = 0.75f, up = dirOf(az, polar), caps = false)
        }

        fun fringe(part: Float, count: Int, spread: Float, tipPolar: Float, width: Float) {
            for (i in 0 until count) {
                val a = -spread + 2f * spread * i / (count - 1f)
                val sweep = if (part == 0f) (if (a < 0f) -10f else 10f) else part * 22f
                val tip = tipPolar + (if (i % 2 == 0) 0f else 4f) - abs(a) * 0.08f
                lock(a + part * 8f, 14f, tip, width, sweep = sweep, liftFrom = 0.04f, liftTo = 0.075f, curl = 0.018f)
            }
        }

        fun tail(points: List<V3>, thickness: Float, up: V3 = V3(0f, 0f, 1f)) {
            val path = smoothPath(points, 5)
            b.tube(
                path,
                { t -> 0.014f + thickness * sin((0.1f + t * 0.9f) * Math.PI.toFloat()).coerceAtLeast(0f).pow(0.6f) * (1f - t * 0.4f) },
                paint, sides = 12, flatten = 0.92f, up = up, caps = false,
            )
        }
    }

    private fun MeshBuilder.hair(o: Outfit) {
        val paint = hairPaint(o.hair)
        val h = HairKit(this, paint, if (o.hat == 1 || o.hat == 3 || o.hat == 4) 0.5f else 1f)
        withMat(Mat.HAIR) {
            when (o.hairStyle) {
                0 -> {
                    h.cap(front = 42f, side = 96f, back = 152f)
                    h.fringe(part = 1f, count = 6, spread = 48f, tipPolar = 66f, width = 0.07f)
                    for (s in listOf(-1f, 1f)) h.lock(s * 76f, 50f, 98f, 0.06f, sweep = s * 6f, liftTo = 0.06f, curl = 0.01f)
                    h.tuft(-10f, 8f, 0.1f, 0.03f, bendAz = 40f, bendPolar = 40f)
                }
                1 -> {
                    h.cap(front = 40f, side = 100f, back = 150f)
                    h.fringe(part = 0f, count = 7, spread = 46f, tipPolar = 70f, width = 0.065f)
                    for (s in listOf(-1f, 1f)) {
                        h.lock(s * 70f, 40f, 132f, 0.075f, sweep = s * 4f, liftFrom = 0.06f, liftTo = 0.1f, curl = 0.03f, flatten = 0.5f)
                        h.lock(s * 95f, 50f, 140f, 0.08f, sweep = s * 6f, liftFrom = 0.07f, liftTo = 0.12f, curl = 0.04f, flatten = 0.55f)
                    }
                    puff(0f, HEAD_CY - 0.12f, -0.1f, 0.27f, 0.26f, 0.17f, paint, subdivisions = 2, wobble = 0.03f, seed = 9)
                    for (k in 0 until 5) {
                        val a = 140f + k * 20f
                        h.lock(a, 60f, 150f, 0.085f, liftFrom = 0.08f, liftTo = 0.14f, curl = 0.05f, flatten = 0.5f)
                    }
                }
                2 -> {
                    h.cap(front = 40f, side = 92f, back = 148f)
                    h.fringe(part = -1f, count = 5, spread = 40f, tipPolar = 62f, width = 0.075f)
                    val spikes = listOf(0f to 6f, 35f to 24f, -35f to 24f, 80f to 34f, -80f to 34f, 125f to 36f, -125f to 36f, 170f to 30f, -170f to 30f, 15f to 38f, -55f to 40f, 60f to 12f, -100f to 14f, 150f to 10f)
                    spikes.forEachIndexed { i, (az, pol) ->
                        h.tuft(az, pol, 0.06f + (i % 3) * 0.015f, 0.055f, bendAz = if (az >= 0f) 10f else -10f, bendPolar = 16f)
                    }
                }
                3 -> {
                    h.cap(front = 34f, side = 94f, back = 152f)
                    h.fringe(part = 0f, count = 4, spread = 34f, tipPolar = 60f, width = 0.07f)
                    for (s in listOf(-1f, 1f)) h.lock(s * 72f, 48f, 112f, 0.035f, sweep = s * 4f, liftTo = 0.05f, curl = 0.02f, flatten = 0.6f)
                    puff(0f, HEAD_CY + 0.3f, -0.07f, 0.12f, 0.11f, 0.12f, paint, subdivisions = 2, wobble = 0.08f, seed = 4)
                    withMat(Mat.GLOSSY) { torus(0f, HEAD_CY + 0.215f, -0.07f, 0.085f, 0.022f, Palette.PINK, 18, 6) }
                }
                4 -> {
                    h.cap(front = 38f, side = 98f, back = 152f)
                    h.fringe(part = 0f, count = 6, spread = 44f, tipPolar = 66f, width = 0.065f)
                    for (s in listOf(-1f, 1f)) {
                        val root = V3(s * 0.27f, HEAD_CY + 0.12f, -0.05f)
                        h.tail(listOf(root, root + V3(s * 0.08f, 0.0f, -0.02f), root + V3(s * 0.14f, -0.1f, -0.03f), root + V3(s * 0.13f, -0.24f, -0.01f), root + V3(s * 0.16f, -0.37f, 0.01f)), 0.075f)
                        withMat(Mat.GLOSSY) { sphere(root.x + s * 0.03f, root.y, root.z, 0.04f, Palette.YELLOW, 5, 10) }
                    }
                }
                5 -> {
                    h.cap(front = 38f, side = 112f, back = 140f, lift = 0.09f)
                    h.fringe(part = 0f, count = 8, spread = 52f, tipPolar = 72f, width = 0.06f)
                    for (k in 0 until 9) {
                        val a = 60f + k * 30f
                        h.lock(a, 60f, 118f, 0.11f, liftFrom = 0.09f, liftTo = 0.16f, curl = -0.04f, flatten = 0.5f)
                        h.lock(-a, 60f, 118f, 0.11f, liftFrom = 0.09f, liftTo = 0.16f, curl = -0.04f, flatten = 0.5f)
                    }
                }
                6 -> {
                    h.cap(front = 36f, side = 96f, back = 152f, lift = 0.05f)
                    var seed = 30
                    for (pol in listOf(18f, 42f, 66f, 92f)) {
                        val n = when (pol) { 18f -> 4; 42f -> 9; 66f -> 12; else -> 10 }
                        for (k in 0 until n) {
                            val az = k * 360f / n + pol
                            val front = abs(((az + 180f) % 360f) - 180f) < 42f
                            if (pol > 50f && front) continue
                            val c = headPoint(dirOf(az, pol), 0.12f)
                            puff(c.x, c.y, c.z, 0.075f, 0.07f, 0.075f, paint, subdivisions = 1, wobble = 0.12f, seed = seed++)
                        }
                    }
                    h.fringe(part = 1f, count = 5, spread = 36f, tipPolar = 58f, width = 0.06f)
                }
                else -> {
                    h.cap(front = 40f, side = 96f, back = 152f)
                    h.fringe(part = 1f, count = 6, spread = 46f, tipPolar = 68f, width = 0.068f)
                    for (s in listOf(-1f, 1f)) h.lock(s * 74f, 48f, 112f, 0.05f, sweep = s * 4f, liftTo = 0.06f, curl = 0.02f)
                    val root = V3(0f, HEAD_CY + 0.17f, -0.27f)
                    h.tail(listOf(root, root + V3(0f, 0.03f, -0.08f), root + V3(0.02f, -0.08f, -0.14f), root + V3(0.0f, -0.24f, -0.12f), root + V3(-0.02f, -0.4f, -0.09f)), 0.085f, up = V3(1f, 0f, 0f))
                    withMat(Mat.GLOSSY) { torus(0f, root.y, root.z - 0.02f, 0.035f, 0.018f, Palette.CORAL, 14, 6) }
                }
            }
        }
    }

    private fun MeshBuilder.hat(o: Outfit) {
        val top = HEAD_CY + HEAD_RY * 1.07f
        when (o.hat) {
            1 -> {
                val c = Palette.CORAL
                withMat(Mat.CLOTH) {
                    warpSphere(c, 10, 22) { x, y, z ->
                        val yy = max(y, 0.08f)
                        val r = 1.19f
                        V3(x * HEAD_RX * r, HEAD_CY + 0.02f + yy * HEAD_RY * r, z * HEAD_RZ * r - 0.01f)
                    }
                    group {
                        translate(0f, HEAD_CY + 0.065f, 0.27f)
                        rotateX(-14f)
                        warpSphere(c.darker(0.08f), 6, 18) { x, y, z -> V3(x * 0.24f, y * 0.018f, max(z, -0.2f) * 0.17f + 0.05f) }
                    }
                    sphere(0f, top + 0.045f, -0.01f, 0.026f, c.darker(0.1f), 4, 8)
                }
                withMat(Mat.SOFT) {
                    group {
                        translate(0f, HEAD_CY + 0.2f, 0.275f)
                        rotateX(57f)
                        cylinder(0f, 0f, 0f, 0.045f, 0.012f, Palette.WHITE, sides = 14)
                        cylinder(0f, 0.004f, 0f, 0.028f, 0.012f, Palette.YELLOW, sides = 12)
                    }
                }
            }
            2 -> withMat(Mat.GLOSSY) {
                val cx = 0.2f
                val cy = HEAD_CY + 0.2f
                val cz = 0.17f
                group {
                    translate(cx, cy, cz)
                    rotateY(35f)
                    rotateZ(-20f)
                    for (k in 0 until 5) {
                        group {
                            rotateZ(k * 72f)
                            ellipsoid(0f, 0.04f, 0f, 0.028f, 0.042f, 0.014f, Palette.PINK, 4, 10)
                        }
                    }
                    sphere(0f, 0f, 0.01f, 0.022f, Palette.YELLOW, 4, 8)
                }
            }
            3 -> {
                val c = Palette.SKY
                withMat(Mat.KNIT) {
                    warpSphere(c, 12, 22) { x, y, z ->
                        val yy = max(y, -0.05f)
                        val r = 1.18f
                        V3(x * HEAD_RX * r, HEAD_CY + 0.03f + yy * HEAD_RY * r * 1.05f, z * HEAD_RZ * r - 0.01f)
                    }
                    group { scale(1f, 1f, HEAD_RZ / HEAD_RX); torus(0f, HEAD_CY + 0.04f, -0.01f, HEAD_RX * 1.17f, 0.04f, c.darker(0.12f), 26, 8) }
                }
                withMat(Mat.CARPET) { puff(0f, top + 0.09f, -0.02f, 0.075f, 0.07f, 0.075f, Palette.WHITE, subdivisions = 2, wobble = 0.14f, seed = 5) }
            }
            4 -> {
                val straw = rgb(0xF2D38B)
                withMat(Mat.FABRIC.copy(texScale = 0.06f)) {
                    lathe(
                        0f, -0.01f,
                        listOf(0.5f to HEAD_CY + 0.1f, 0.48f to HEAD_CY + 0.115f, 0.33f to HEAD_CY + 0.14f, 0.3f to HEAD_CY + 0.25f, 0.24f to top + 0.04f, 0.0f to top + 0.06f),
                        straw, sides = 28, creaseDeg = 50f,
                    )
                    lathe(0f, -0.01f, listOf(0.0f to HEAD_CY + 0.12f, 0.3f to HEAD_CY + 0.12f, 0.48f to HEAD_CY + 0.09f, 0.5f to HEAD_CY + 0.1f), straw.darker(0.12f), sides = 28)
                }
                withMat(Mat.CLOTH) { cylinder(0f, HEAD_CY + 0.145f, -0.01f, 0.302f, 0.05f, Palette.CORAL, sides = 28, caps = false) }
            }
            5 -> {
                withMat(Mat.GLOSSY) {
                    group {
                        rotateX(-8f)
                        arc(0f, HEAD_CY, 0f, HEAD_RX * 1.08f, 0.016f, 10f, 170f, Palette.PINK, segments = 18, sides = 6)
                    }
                }
                withMat(Mat.CARPET) {
                    for (s in listOf(-1f, 1f)) {
                        group {
                            translate(s * 0.12f, top + 0.12f, -0.02f)
                            rotateZ(s * -14f)
                            ellipsoid(0f, 0f, 0f, 0.055f, 0.16f, 0.035f, Palette.WHITE, 8, 12)
                            ellipsoid(0f, 0.005f, 0.022f, 0.032f, 0.12f, 0.016f, Palette.PINK, 6, 10)
                        }
                    }
                }
            }
            6 -> withMat(Mat.SOFT) {
                for (s in listOf(-1f, 1f)) {
                    group {
                        translate(s * 0.17f, top - 0.04f, -0.01f)
                        rotateZ(s * -22f)
                        cone(0f, 0f, 0f, 0.09f, 0.16f, o.hair, sides = 10)
                        group { translate(0f, 0.012f, 0.03f); cone(0f, 0f, 0f, 0.055f, 0.11f, Palette.PINK, sides = 10) }
                    }
                }
            }
            7 -> withMat(Mat.METAL) {
                val gold = rgb(0xFFCF4A)
                cylinder(0f, top - 0.055f, -0.01f, 0.17f, 0.06f, gold, sides = 24, caps = false)
                for (k in 0 until 6) {
                    val a = k * 60f * DEG
                    group {
                        translate(sin(a) * 0.17f, top + 0.005f, cos(a) * 0.17f - 0.01f)
                        cone(0f, 0f, 0f, 0.03f, 0.07f, gold, sides = 6)
                        sphere(0f, 0.075f, 0f, 0.014f, gold, 3, 6)
                    }
                }
                withMat(Mat.GLOSSY) { sphere(0f, top - 0.025f, 0.165f, 0.022f, Palette.RED, 4, 8) }
            }
            else -> Unit
        }
    }

    fun arm(o: Outfit): Mesh = mesh {
        val cloth = if (o.top == TOP_SWEATER) Mat.KNIT else Mat.CLOTH
        val sleeve = if (o.top == TOP_OVERALLS || o.top == TOP_TEE) o.shirt else o.shirt
        withMat(cloth) { sphere(0f, -0.01f, 0f, 0.06f, sleeve, 6, 12) }
        if (o.longSleeves) {
            withMat(cloth) {
                tube(listOf(V3(0f, -0.01f, 0f), V3(0f, -0.14f, 0.004f), V3(0f, -0.27f, 0.012f)), { t -> 0.06f - 0.009f * t }, sleeve, sides = 12, caps = false)
                torus(0f, -0.268f, 0.012f, 0.047f, 0.012f, sleeve.darker(0.1f), 14, 6)
            }
            withMat(Mat.SKIN) { cylinder(0f, -0.3f, 0.012f, 0.036f, 0.04f, o.skin, sides = 10, caps = false) }
        } else {
            withMat(cloth) {
                frustum(0f, -0.135f, 0f, 0.066f, 0.06f, 0.125f, sleeve, sides = 12, capTop = false, capBottom = false)
                torus(0f, -0.135f, 0f, 0.063f, 0.008f, sleeve.darker(0.1f), 14, 5)
            }
            withMat(Mat.SKIN) {
                tube(listOf(V3(0f, -0.1f, 0f), V3(0f, -0.2f, 0.005f), V3(0f, -0.29f, 0.012f)), { t -> 0.044f - 0.007f * t }, o.skin, sides = 10, caps = false)
            }
        }
        withMat(Mat.SKIN) { hand(o.skin) }
    }

    private fun MeshBuilder.hand(skin: Rgb) {
        warpSphere(skin, 8, 12) { x, y, z -> V3(x * 0.05f, HAND_Y + y * 0.058f, 0.012f + z * 0.044f) }
        ellipsoid(0f, HAND_Y + 0.02f, 0.052f, 0.02f, 0.03f, 0.02f, skin, 4, 8)
    }

    fun leg(o: Outfit): Mesh = mesh {
        when {
            o.bareLegs -> withMat(Mat.SKIN) {
                tube(listOf(V3(0f, 0.0f, 0f), V3(0f, -0.18f, 0.003f), V3(0f, -0.35f, 0.006f)), { t -> 0.052f - 0.01f * t }, o.skin, sides = 10, caps = false)
            }
            o.bottom == BOTTOM_SHORTS && o.top != TOP_DRESS -> {
                withMat(Mat.CLOTH) {
                    frustum(0f, -0.14f, 0f, 0.068f, 0.072f, 0.17f, o.pants, sides = 12, capTop = false, capBottom = false)
                    torus(0f, -0.14f, 0f, 0.068f, 0.008f, o.pants.darker(0.12f), 14, 5)
                }
                withMat(Mat.SKIN) {
                    tube(listOf(V3(0f, -0.11f, 0f), V3(0f, -0.23f, 0.004f), V3(0f, -0.35f, 0.006f)), { t -> 0.048f - 0.008f * t }, o.skin, sides = 10, caps = false)
                }
            }
            else -> withMat(Mat.CLOTH) {
                tube(listOf(V3(0f, 0.03f, 0f), V3(0f, -0.15f, 0.004f), V3(0f, -0.32f, 0.006f)), { t -> 0.07f - 0.012f * t }, o.pants, sides = 12, caps = false)
                torus(0f, -0.318f, 0.006f, 0.058f, 0.009f, o.pants.darker(0.1f), 14, 5)
            }
        }
        if (o.bareLegs || o.bottom == BOTTOM_SHORTS) {
            withMat(Mat.KNIT) { cylinder(0f, -0.37f, 0.006f, 0.045f, 0.055f, Palette.WHITE, sides = 10, caps = false) }
        }
        shoe(o.shoes)
    }

    private fun MeshBuilder.shoe(c: Rgb) {
        withMat(Mat.MATTE) { roundBox(0f, -0.405f, 0.032f, 0.128f, 0.032f, 0.24f, 0.014f, SOLE) }
        withMat(Mat.PLASTIC) {
            puff(0f, -0.365f, 0.04f, 0.064f, 0.058f, 0.118f, c, subdivisions = 2, wobble = 0f, floorY = -0.395f)
            puff(0f, -0.378f, 0.105f, 0.052f, 0.04f, 0.05f, c.lighter(0.25f), subdivisions = 2, wobble = 0f, floorY = -0.395f)
            torus(0f, -0.322f, -0.008f, 0.048f, 0.013f, c.darker(0.2f), 14, 6)
        }
        withMat(Mat.SOFT) {
            for (k in 0 until 3) {
                group {
                    translate(0f, -0.318f - k * 0.009f, 0.03f + k * 0.022f)
                    rotateX(-28f)
                    roundBox(0f, 0f, 0f, 0.055f, 0.007f, 0.012f, 0.003f, Palette.WHITE)
                }
            }
        }
    }

    fun wateringCan(): Mesh = mesh {
        mat = Mat.GLOSSY
        group {
            translate(0f, HAND_Y - 0.09f, 0.08f)
            cylinder(0f, -0.08f, 0f, 0.09f, 0.16f, Palette.SKY, sides = 14)
            torus(0f, 0.1f, -0.02f, 0.06f, 0.012f, Palette.SKY.darker(0.15f), 14, 6)
            group {
                translate(0f, -0.02f, 0.08f)
                rotateX(55f)
                cylinder(0f, 0f, 0f, 0.018f, 0.2f, Palette.SKY.darker(0.1f), sides = 8)
                cylinder(0f, 0.19f, 0f, 0.04f, 0.03f, Palette.SKY.darker(0.2f), sides = 8)
            }
        }
    }

    fun gloves(): Mesh = mesh {
        withMat(Mat.PLASTIC) {
            warpSphere(Palette.YELLOW, 8, 12) { x, y, z -> V3(x * 0.068f, HAND_Y + y * 0.075f, 0.012f + z * 0.06f) }
            torus(0f, HAND_Y + 0.06f, 0.012f, 0.058f, 0.016f, Palette.YELLOW.darker(0.12f), 14, 6)
        }
    }

    fun full(o: Outfit): Mesh = mesh {
        group {
            translate(0f, HIP, 0f)
            include(torso(o))
            group {
                translate(0f, NECK, 0f)
                include(head(o))
                group { translate(0f, EYE_Y, 0f); include(eyes(o)) }
            }
            group { translate(-SHOULDER_X, SHOULDER_Y, 0f); rotateZ(-6f); include(arm(o)) }
            group { translate(SHOULDER_X, SHOULDER_Y, 0f); rotateZ(6f); include(arm(o)) }
            group { translate(-LEG_X, 0f, 0f); include(leg(o)) }
            group { translate(LEG_X, 0f, 0f); include(leg(o)) }
        }
    }
}
