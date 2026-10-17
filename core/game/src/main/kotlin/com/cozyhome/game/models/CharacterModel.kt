package com.cozyhome.game.models

import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.MeshBuilder
import com.cozyhome.game.mesh.Rgb
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.state.Appearance

data class Outfit(val skin: Rgb, val hair: Rgb, val shirt: Rgb, val pants: Rgb, val shoes: Rgb, val hairStyle: Int, val hat: Int) {
    companion object {
        fun of(a: Appearance) = Outfit(
            skin = Palette.SKIN[a.skin.mod(Palette.SKIN.size)],
            hair = Palette.HAIR[a.hairColor.mod(Palette.HAIR.size)],
            shirt = Palette.SHIRT[a.shirt.mod(Palette.SHIRT.size)],
            pants = Palette.PANTS[a.pants.mod(Palette.PANTS.size)],
            shoes = Palette.SHOES[a.shoes.mod(Palette.SHOES.size)],
            hairStyle = a.hairStyle.mod(Palette.HAIR_STYLES),
            hat = a.hat.mod(Palette.HATS),
        )

        val KEEPER = Outfit(Palette.SKIN[2], Palette.HAIR[0], rgb(0xFF9F43), rgb(0x3B3355), rgb(0xFFFFFF), 2, 1)
    }
}

object CharacterModel {
    const val HIP = 0.42f
    const val NECK = 0.46f
    const val SHOULDER_X = 0.25f
    const val SHOULDER_Y = 0.38f
    const val LEG_X = 0.1f

    fun torso(o: Outfit): Mesh = mesh {
        mat = Mat.SOFT
        roundBox(0f, 0.04f, 0f, 0.36f, 0.2f, 0.25f, 0.08f, o.pants)
        roundBox(0f, 0.27f, 0f, 0.42f, 0.4f, 0.28f, 0.12f, o.shirt)
        roundBox(0f, 0.36f, 0.135f, 0.12f, 0.06f, 0.02f, 0.01f, o.shirt.darker(0.12f))
        mat = Mat.MATTE
        cylinder(0f, 0.42f, 0f, 0.07f, 0.08f, o.skin, sides = 10)
    }

    fun head(o: Outfit): Mesh = mesh {
        mat = Mat.MATTE
        ellipsoid(0f, 0.25f, 0f, 0.28f, 0.265f, 0.265f, o.skin, rings = 12, segments = 18)
        sphere(-0.265f, 0.23f, 0f, 0.055f, o.skin, rings = 5, segments = 8)
        sphere(0.265f, 0.23f, 0f, 0.055f, o.skin, rings = 5, segments = 8)
        withMat(Mat.GLOSSY) {
            ellipsoid(-0.095f, 0.25f, 0.236f, 0.036f, 0.05f, 0.03f, Palette.INK, rings = 6, segments = 10)
            ellipsoid(0.095f, 0.25f, 0.236f, 0.036f, 0.05f, 0.03f, Palette.INK, rings = 6, segments = 10)
            sphere(-0.083f, 0.27f, 0.262f, 0.012f, Palette.WHITE, rings = 3, segments = 6)
            sphere(0.107f, 0.27f, 0.262f, 0.012f, Palette.WHITE, rings = 3, segments = 6)
        }
        ellipsoid(-0.16f, 0.17f, 0.205f, 0.05f, 0.03f, 0.03f, rgb(0xFF9DAF), rings = 4, segments = 8)
        ellipsoid(0.16f, 0.17f, 0.205f, 0.05f, 0.03f, 0.03f, rgb(0xFF9DAF), rings = 4, segments = 8)
        group {
            translate(0f, 0.155f, 0.243f)
            rotateX(-12f)
            ellipsoid(0f, 0f, 0f, 0.045f, 0.022f, 0.02f, rgb(0xC2546A), rings = 4, segments = 8)
        }
        hair(o)
        hat(o)
    }

    private fun MeshBuilder.hair(o: Outfit) {
        val c = o.hair
        mat = Mat.SOFT
        ellipsoid(0f, 0.36f, -0.03f, 0.295f, 0.2f, 0.29f, c, rings = 10, segments = 18)
        ellipsoid(0f, 0.24f, -0.085f, 0.285f, 0.245f, 0.235f, c, rings = 10, segments = 16)
        when (o.hairStyle) {
            0 -> {
                blob(-0.1f, 0.44f, 0.16f, 0.12f, 0.07f, 0.09f, c, 1, 0.08f, 11)
                blob(0.08f, 0.45f, 0.17f, 0.13f, 0.07f, 0.09f, c, 1, 0.08f, 12)
            }
            1 -> {
                ellipsoid(-0.23f, 0.17f, -0.02f, 0.09f, 0.2f, 0.16f, c, rings = 8, segments = 10)
                ellipsoid(0.23f, 0.17f, -0.02f, 0.09f, 0.2f, 0.16f, c, rings = 8, segments = 10)
                ellipsoid(0f, 0.41f, 0.17f, 0.25f, 0.08f, 0.1f, c, rings = 6, segments = 12)
            }
            2 -> {
                for (i in 0 until 7) {
                    val a = i / 6f
                    group {
                        translate(-0.2f + a * 0.4f, 0.47f, 0.02f - kotlin.math.abs(a - 0.5f) * 0.1f)
                        rotateZ((a - 0.5f) * -60f)
                        rotateX(-18f)
                        cone(0f, 0f, 0f, 0.075f, 0.2f, c, sides = 6, smooth = false)
                    }
                }
            }
            3 -> {
                sphere(0f, 0.57f, -0.1f, 0.12f, c, rings = 8, segments = 12)
                ellipsoid(0f, 0.42f, 0.17f, 0.24f, 0.07f, 0.1f, c, rings = 6, segments = 12)
                withMat(Mat.GLOSSY) { torus(0f, 0.5f, -0.08f, 0.08f, 0.02f, Palette.PINK, 16, 6) }
            }
            else -> {
                ellipsoid(-0.32f, 0.18f, -0.06f, 0.09f, 0.17f, 0.09f, c, rings = 8, segments = 10)
                ellipsoid(0.32f, 0.18f, -0.06f, 0.09f, 0.17f, 0.09f, c, rings = 8, segments = 10)
                sphere(-0.3f, 0.36f, -0.05f, 0.05f, Palette.YELLOW, rings = 4, segments = 8)
                sphere(0.3f, 0.36f, -0.05f, 0.05f, Palette.YELLOW, rings = 4, segments = 8)
                ellipsoid(0f, 0.42f, 0.17f, 0.24f, 0.07f, 0.1f, c, rings = 6, segments = 12)
            }
        }
        mat = Mat.MATTE
    }

    private fun MeshBuilder.hat(o: Outfit) {
        when (o.hat) {
            1 -> {
                mat = Mat.SOFT
                ellipsoid(0f, 0.43f, -0.01f, 0.3f, 0.17f, 0.3f, Palette.CORAL, rings = 8, segments = 16)
                group {
                    translate(0f, 0.43f, 0.24f)
                    rotateX(-8f)
                    roundBox(0f, 0f, 0.06f, 0.36f, 0.03f, 0.18f, 0.012f, Palette.CORAL.darker(0.1f))
                }
                sphere(0f, 0.6f, -0.01f, 0.03f, Palette.WHITE, rings = 4, segments = 6)
            }
            2 -> {
                withMat(Mat.GLOSSY) {
                    for (k in 0 until 5) {
                        val a = k * 72f * (Math.PI / 180).toFloat()
                        sphere(0.2f + kotlin.math.cos(a) * 0.045f, 0.46f + kotlin.math.sin(a) * 0.045f, 0.13f, 0.04f, Palette.PINK, rings = 4, segments = 8)
                    }
                    sphere(0.2f, 0.46f, 0.15f, 0.03f, Palette.YELLOW, rings = 4, segments = 8)
                }
            }
            3 -> {
                mat = Mat.SOFT
                ellipsoid(0f, 0.43f, -0.02f, 0.31f, 0.2f, 0.31f, Palette.SKY, rings = 8, segments = 16)
                torus(0f, 0.33f, -0.02f, 0.29f, 0.04f, Palette.SKY.darker(0.12f), 20, 6)
                sphere(0f, 0.65f, -0.02f, 0.07f, Palette.WHITE, rings = 5, segments = 8)
            }
            else -> Unit
        }
        mat = Mat.MATTE
    }

    fun arm(o: Outfit): Mesh = mesh {
        mat = Mat.SOFT
        capsule(0f, -0.2f, 0f, 0.075f, 0.24f, o.shirt, sides = 10, capRings = 3)
        mat = Mat.MATTE
        capsule(0f, -0.33f, 0f, 0.06f, 0.18f, o.skin, sides = 10, capRings = 3)
        sphere(0f, -0.35f, 0f, 0.068f, o.skin, rings = 6, segments = 10)
    }

    fun leg(o: Outfit): Mesh = mesh {
        mat = Mat.SOFT
        capsule(0f, -0.33f, 0f, 0.085f, 0.36f, o.pants, sides = 10, capRings = 3)
        mat = Mat.GLOSSY
        roundBox(0f, -0.375f, 0.03f, 0.15f, 0.09f, 0.22f, 0.04f, o.shoes)
        roundBox(0f, -0.41f, 0.03f, 0.152f, 0.022f, 0.222f, 0.01f, o.shoes.darker(0.25f))
    }

    fun wateringCan(): Mesh = mesh {
        mat = Mat.GLOSSY
        group {
            translate(0f, -0.44f, 0.08f)
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
        mat = Mat.SOFT
        sphere(0f, -0.36f, 0f, 0.085f, Palette.YELLOW, rings = 6, segments = 10)
    }
}
