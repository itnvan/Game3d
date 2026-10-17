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

    private fun sheet(name: String, meshes: List<Pair<String, Mesh>>, cell: Int = 160, cols: Int = 8) {
        val rows = (meshes.size + cols - 1) / cols
        val img = BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color(0xE9, 0xF6, 0xFF)
        g.fillRect(0, 0, img.width, img.height)
        meshes.forEachIndexed { i, (label, m) ->
            val px = Thumbnail.render(m, cell)
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
        val looks = (0 until 5).map { Appearance(skin = it, hairStyle = it, hairColor = it + 1, shirt = it, pants = it % 3, hat = it % 4) }
        val chars = looks.map { a ->
            val o = Outfit.of(a)
            "hair${a.hairStyle}" to com.cozyhome.game.mesh.mesh {
                group {
                    translate(0f, CharacterModel.HIP, 0f)
                    include(CharacterModel.torso(o))
                    group { translate(0f, CharacterModel.NECK, 0f); include(CharacterModel.head(o)) }
                    group { translate(-CharacterModel.SHOULDER_X, CharacterModel.SHOULDER_Y, 0f); include(CharacterModel.arm(o)) }
                    group { translate(CharacterModel.SHOULDER_X, CharacterModel.SHOULDER_Y, 0f); include(CharacterModel.arm(o)) }
                    group { translate(-CharacterModel.LEG_X, 0f, 0f); include(CharacterModel.leg(o)) }
                    group { translate(CharacterModel.LEG_X, 0f, 0f); include(CharacterModel.leg(o)) }
                }
            }
        }
        val crops = Crops.all.flatMap { c -> (0..3).map { s -> "${c.id}$s" to CropModels.plant(c.id, s) } }
        val icons = Crops.all.map { "icon_${it.id}" to CropModels.icon(it.id) }
        sheet("characters", chars, cell = 200, cols = 5)
        sheet("crops", crops + icons, cell = 140, cols = 8)
        sheet("props", listOf(
            "basketball" to PropModels.basketball(), "soccer" to PropModels.soccerBall(), "drop" to PropModels.waterDrop(),
            "star" to PropModels.star(), "checkpoint" to PropModels.checkpoint(true), "zzz" to PropModels.zzz(),
            "soil" to CropModels.soil(false, true), "soilWet" to CropModels.soil(true, true), "net" to WorldModel.hoopNet(),
            "goalNet" to WorldModel.goalNet(), "can" to CharacterModel.wateringCan(), "swingSeat" to FurnitureModels.swingSeat(0),
        ), cols = 6)
    }

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
