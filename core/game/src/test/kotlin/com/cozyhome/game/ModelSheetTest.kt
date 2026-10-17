package com.cozyhome.game

import com.cozyhome.game.decor.Catalog
import com.cozyhome.game.garden.Crops
import com.cozyhome.game.mesh.Glb
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.models.CharacterModel
import com.cozyhome.game.models.CropModels
import com.cozyhome.game.models.FurnitureModels
import com.cozyhome.game.models.HouseModel
import com.cozyhome.game.models.Outfit
import com.cozyhome.game.models.PropModels
import com.cozyhome.game.models.WorldModel
import com.cozyhome.game.raster.Thumbnail
import com.cozyhome.game.state.Appearance
import com.cozyhome.game.world.WallSide
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ModelSheetTest {
    private val outDir = File("build/sheets").apply { mkdirs() }

    private fun sheet(name: String, meshes: List<Pair<String, Mesh>>, cell: Int = 160, cols: Int = 8, yaw: Float = -32f, pitch: Float = 26f) {
        val rows = (meshes.size + cols - 1) / cols
        val img = BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color(0xE9, 0xF6, 0xFF)
        g.fillRect(0, 0, img.width, img.height)
        meshes.forEachIndexed { i, (label, m) ->
            val px = Thumbnail.render(m, cell, yaw, pitch)
            val tile = BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB)
            tile.setRGB(0, 0, cell, cell, px, 0, cell)
            g.drawImage(tile, (i % cols) * cell, (i / cols) * cell, null)
            g.color = Color(0x3B, 0x33, 0x55)
            g.drawString(label, (i % cols) * cell + 4, (i / cols) * cell + 14)
        }
        g.dispose()
        ImageIO.write(img, "png", File(outDir, "$name.png"))
    }

    @Test
    fun furnitureSheet() {
        val meshes = Catalog.items.map { def -> def.id to FurnitureModels.build(def.id, 0, on = def.id in setOf("tv", "floor_lamp", "globe_lamp", "lamp_post")) }
        for ((id, m) in meshes) {
            assertTrue("$id empty", !m.isEmpty)
            val glb = Glb.encode(m)
            assertTrue(glb.size > 100)
        }
        sheet("furniture", meshes)
        sheet("furniture_variants", Catalog.items.filter { it.variants > 1 }.take(16).map { it.id + "#1" to FurnitureModels.build(it.id, 1) })
    }

    @Test
    fun characterAndCropSheet() {
        val looks = (0 until 8).map { Appearance(skin = it % 6, hairStyle = it, hairColor = it + 1, shirt = it, pants = it % 6, hat = 0, top = it % 6, bottom = it % 3, eyeColor = it, eyeStyle = it % 3) }
        val chars = looks.map { a -> "h${a.hairStyle} t${a.top} b${a.bottom}" to CharacterModel.full(Outfit.of(a)) }
        val hats = (1 until 8).map { h -> "hat$h" to CharacterModel.full(Outfit.of(Appearance(hairStyle = h, hat = h, top = h % 6))) }
        val heads = looks.map { a ->
            val o = Outfit.of(a)
            "face${a.hairStyle}" to com.cozyhome.game.mesh.mesh {
                include(CharacterModel.head(o))
                group { translate(0f, CharacterModel.EYE_Y, 0f); include(CharacterModel.eyes(o)) }
            }
        }
        sheet("characters_hats", hats, cell = 240, cols = 7, yaw = -24f, pitch = 12f)
        sheet("faces", heads, cell = 240, cols = 4, yaw = -12f, pitch = 6f)
        sheet("faces_top", heads, cell = 160, cols = 8, yaw = 30f, pitch = 70f)
        sheet("faces_side", heads + hats.map { (n, _) -> n to CharacterModel.head(Outfit.of(Appearance(hairStyle = n.removePrefix("hat").toInt(), hat = n.removePrefix("hat").toInt()))) }, cell = 160, cols = 8, yaw = 75f, pitch = 8f)
        sheet("characters_back", chars.take(4), cell = 260, cols = 4, yaw = 160f, pitch = 14f)
        val crops = Crops.all.flatMap { c -> (0..3).map { s -> "${c.id}$s" to CropModels.plant(c.id, s) } }
        val icons = Crops.all.map { "icon_${it.id}" to CropModels.icon(it.id) }
        sheet("characters", chars, cell = 260, cols = 4, yaw = -24f, pitch = 12f)
        sheet("crops", crops + icons, cell = 140, cols = 8)
        sheet("props", listOf(
            "basketball" to PropModels.basketball(), "soccer" to PropModels.soccerBall(), "drop" to PropModels.waterDrop(),
            "star" to PropModels.star(), "checkpoint" to PropModels.checkpoint(true), "zzz" to PropModels.zzz(),
            "soil" to CropModels.soil(false, true), "soilWet" to CropModels.soil(true, true), "net" to WorldModel.hoopNet(),
            "goalNet" to WorldModel.goalNet(), "can" to CharacterModel.wateringCan(), "swingSeat" to FurnitureModels.swingSeat(0),
        ), cols = 6)
    }

    @Test
    fun textureSheet() {
        val texes = com.cozyhome.game.mesh.Tex.entries.filter { it != com.cozyhome.game.mesh.Tex.NONE }
        val cell = 256
        val cols = 5
        val img = BufferedImage(cols * cell, ((texes.size + cols - 1) / cols) * cell, BufferedImage.TYPE_INT_ARGB)
        texes.forEachIndexed { i, t ->
            val im = com.cozyhome.game.mesh.TextureGen.image(t)
            assertTrue(com.cozyhome.game.mesh.TextureGen.png(t).size > 64)
            for (y in 0 until cell) for (x in 0 until cell) {
                val k = ((y * 2 % im.size) * im.size + (x * 2 % im.size)) * 4
                val a = im.rgba[k + 3].toInt() and 0xFF
                val tint = 0xFF
                val r = (im.rgba[k].toInt() and 0xFF) * tint / 255
                val argb = (0xFF shl 24) or (blend(r, a) shl 16) or (blend(im.rgba[k + 1].toInt() and 0xFF, a) shl 8) or blend(im.rgba[k + 2].toInt() and 0xFF, a)
                img.setRGB((i % cols) * cell + x, (i / cols) * cell + y, argb)
            }
            val g = img.createGraphics()
            g.color = Color(0xE0, 0x20, 0x60)
            g.drawString(t.name, (i % cols) * cell + 4, (i / cols) * cell + 14)
            g.dispose()
        }
        ImageIO.write(img, "png", File(outDir, "textures.png"))
    }

    private fun blend(c: Int, a: Int) = (c * a + 0xFF * (255 - a)) / 255

    @Test
    fun houseAndWorldBuild() {
        val parts = listOf(
            "floor0" to HouseModel.floor(0), "floor3" to HouseModel.floor(3),
            "wallS" to HouseModel.wall(WallSide.SOUTH, 0, false), "wallScut" to HouseModel.wall(WallSide.SOUTH, 0, true),
            "wallW" to HouseModel.wall(WallSide.WEST, 2, false), "roof" to HouseModel.roof(), "porch" to HouseModel.porch(),
        )
        sheet("house", parts, cell = 220, cols = 4)
        val ground = WorldModel.ground()
        val scenery = WorldModel.scenery()
        println("ground tris=${ground.triangleCount} scenery tris=${scenery.triangleCount}")
        assertTrue(ground.triangleCount < 120_000)
        assertTrue(scenery.triangleCount < 200_000)
        val house = com.cozyhome.game.mesh.mesh {
            include(HouseModel.floor(0))
            for (s in WallSide.entries) include(HouseModel.wall(s, 0, false))
            include(HouseModel.roof())
            include(HouseModel.porch())
        }
        sheet("house_full", listOf("house" to house), cell = 600, cols = 1)
    }
}
