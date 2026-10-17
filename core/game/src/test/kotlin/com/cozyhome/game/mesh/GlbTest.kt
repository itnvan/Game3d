package com.cozyhome.game.mesh

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GlbTest {

    private val sample = mesh {
        block(0f, 0f, 0f, 1f, 1f, 1f, rgb(0xFF8844))
        withMat(Mat.GLOSSY) { sphere(0f, 2f, 0f, 0.5f, rgb(0x44AAFF)) }
        withMat(Mat.glow(rgb(0xFFEEAA), 4f)) { cylinder(2f, 0f, 0f, 0.2f, 1f, rgb(0xFFFFFF)) }
        withMat(Mat.UNLIT) { disc(0f, 3f, 0f, 1f, rgb(0x00FF00)) }
    }

    @Test
    fun headerAndChunksAreConsistent() {
        val bytes = Glb.encode(sample)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x46546C67, b.getInt(0))
        assertEquals(2, b.getInt(4))
        assertEquals(bytes.size, b.getInt(8))
        val jsonLen = b.getInt(12)
        assertEquals(0x4E4F534A, b.getInt(16))
        assertEquals(0, jsonLen % 4)
        val binLen = b.getInt(20 + jsonLen)
        assertEquals(0x004E4942, b.getInt(24 + jsonLen))
        assertEquals(bytes.size, 28 + jsonLen + binLen)
    }

    @Test
    fun accessorsFitInsideBuffer() {
        val bytes = Glb.encode(sample)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val jsonLen = b.getInt(12)
        val json = Json.parseToJsonElement(String(bytes, 20, jsonLen, Charsets.UTF_8).trim()).jsonObject
        val binLen = b.getInt(20 + jsonLen)
        val views = json["bufferViews"]!!.jsonArray
        for (v in views) {
            val o = v.jsonObject
            val end = o["byteOffset"]!!.jsonPrimitive.int + o["byteLength"]!!.jsonPrimitive.int
            assertTrue(end <= binLen)
            assertEquals(0, o["byteOffset"]!!.jsonPrimitive.int % 4)
        }
        val prims = json["meshes"]!!.jsonArray[0].jsonObject["primitives"]!!.jsonArray
        assertEquals(4, prims.size)
        assertEquals(4, json["materials"]!!.jsonArray.size)
        val used = json["extensionsUsed"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("KHR_materials_unlit" in used)
        assertTrue("KHR_materials_emissive_strength" in used)
    }

    @Test
    fun boxHasOutwardNormals() {
        val m = mesh { block(-1f, -1f, -1f, 1f, 1f, 1f, rgb(0xFFFFFF)) }
        val p = m.parts.single()
        assertEquals(24, p.vertexCount)
        assertEquals(12, p.triangleCount)
        var t = 0
        while (t < p.indices.size) {
            val a = p.indices[t]; val bb = p.indices[t + 1]; val c = p.indices[t + 2]
            val ax = p.positions[a * 3]; val ay = p.positions[a * 3 + 1]; val az = p.positions[a * 3 + 2]
            val ux = p.positions[bb * 3] - ax; val uy = p.positions[bb * 3 + 1] - ay; val uz = p.positions[bb * 3 + 2] - az
            val vx = p.positions[c * 3] - ax; val vy = p.positions[c * 3 + 1] - ay; val vz = p.positions[c * 3 + 2] - az
            val nx = uy * vz - uz * vy; val ny = uz * vx - ux * vz; val nz = ux * vy - uy * vx
            val cx = (ax + p.positions[bb * 3] + p.positions[c * 3]) / 3f
            val cy = (ay + p.positions[bb * 3 + 1] + p.positions[c * 3 + 1]) / 3f
            val cz = (az + p.positions[bb * 3 + 2] + p.positions[c * 3 + 2]) / 3f
            assertTrue("triangle $t faces inward", nx * cx + ny * cy + nz * cz > 0f)
            t += 3
        }
    }

    @Test
    fun windingIsOutwardForCurvedPrimitives() {
        val shapes = listOf(
            mesh { sphere(0f, 0f, 0f, 1f, rgb(0xFFFFFF)) },
            mesh { roundBox(0f, 0f, 0f, 2f, 1f, 1.5f, 0.3f, rgb(0xFFFFFF)) },
            mesh { cylinder(0f, -1f, 0f, 1f, 2f, rgb(0xFFFFFF)) },
            mesh { blob(0f, 0f, 0f, 1f, 1f, 1f, rgb(0xFFFFFF)) },
            mesh { group { rotateY(30f); scale(-1f, 1f, 1f); block(-1f, -1f, -1f, 1f, 1f, 1f, rgb(0xFFFFFF)) } },
        )
        for (m in shapes) {
            val p = m.parts.single()
            var outward = 0
            var total = 0
            var t = 0
            while (t < p.indices.size) {
                val i = IntArray(3) { p.indices[t + it] }
                val ax = p.positions[i[0] * 3]; val ay = p.positions[i[0] * 3 + 1]; val az = p.positions[i[0] * 3 + 2]
                val ux = p.positions[i[1] * 3] - ax; val uy = p.positions[i[1] * 3 + 1] - ay; val uz = p.positions[i[1] * 3 + 2] - az
                val vx = p.positions[i[2] * 3] - ax; val vy = p.positions[i[2] * 3 + 1] - ay; val vz = p.positions[i[2] * 3 + 2] - az
                val nx = uy * vz - uz * vy; val ny = uz * vx - ux * vz; val nz = ux * vy - uy * vx
                val area = nx * nx + ny * ny + nz * nz
                if (area > 1e-10f) {
                    total++
                    val nnx = p.normals[i[0] * 3]; val nny = p.normals[i[0] * 3 + 1]; val nnz = p.normals[i[0] * 3 + 2]
                    if (nx * nnx + ny * nny + nz * nnz > 0f) outward++
                }
                t += 3
            }
            assertEquals("winding disagrees with normals", total, outward)
        }
    }
}
