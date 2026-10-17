package com.cozyhome.game.mesh

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Glb {
    private const val MAGIC = 0x46546C67
    private const val CHUNK_JSON = 0x4E4F534A
    private const val CHUNK_BIN = 0x004E4942
    private const val FLOAT = 5126
    private const val USHORT = 5123
    private const val UINT = 5125
    private const val ARRAY_BUFFER = 34962
    private const val ELEMENT_ARRAY_BUFFER = 34963

    fun primitiveTextures(mesh: Mesh): List<Tex> = mesh.parts.filter { it.indices.isNotEmpty() }.map { it.mat.tex }

    fun encode(mesh: Mesh, name: String = "mesh", external: Boolean = false): ByteArray {
        val parts = mesh.parts.filter { it.indices.isNotEmpty() }
        val bin = ByteArrayOutputStream()
        val views = StringBuilder()
        val accessors = StringBuilder()
        val primitives = StringBuilder()
        val materials = StringBuilder()
        var viewCount = 0
        var accessorCount = 0
        var usesUnlit = false
        var usesStrength = false
        var usesExternal = false

        fun addView(bytes: ByteArray, target: Int?): Int {
            while (bin.size() % 4 != 0) bin.write(0)
            val offset = bin.size()
            bin.write(bytes)
            if (viewCount > 0) views.append(',')
            val targetJson = if (target != null) ",\"target\":$target" else ""
            views.append("{\"buffer\":0,\"byteOffset\":$offset,\"byteLength\":${bytes.size}$targetJson}")
            return viewCount++
        }

        val textureIndex = LinkedHashMap<Tex, Int>()
        fun textureOf(tex: Tex): Int = textureIndex.getOrPut(tex) { textureIndex.size }

        fun addAccessor(view: Int, componentType: Int, count: Int, type: String, extra: String = ""): Int {
            if (accessorCount > 0) accessors.append(',')
            accessors.append("{\"bufferView\":$view,\"componentType\":$componentType,\"count\":$count,\"type\":\"$type\"$extra}")
            return accessorCount++
        }

        parts.forEachIndexed { index, part ->
            val count = part.vertexCount
            val pos = addAccessor(addView(floats(part.positions), ARRAY_BUFFER), FLOAT, count, "VEC3", minMax(part.positions))
            val nrm = addAccessor(addView(floats(part.normals), ARRAY_BUFFER), FLOAT, count, "VEC3")
            val col = addAccessor(addView(floats(part.colors), ARRAY_BUFFER), FLOAT, count, "VEC4")
            val idx = if (count <= 65535) {
                addAccessor(addView(ushorts(part.indices), ELEMENT_ARRAY_BUFFER), USHORT, part.indices.size, "SCALAR")
            } else {
                addAccessor(addView(uints(part.indices), ELEMENT_ARRAY_BUFFER), UINT, part.indices.size, "SCALAR")
            }
            val m = part.mat
            val textured = m.tex != Tex.NONE
            val uv = if (textured) addAccessor(addView(floats(part.uvs), ARRAY_BUFFER), FLOAT, count, "VEC2") else -1
            if (index > 0) primitives.append(',')
            val uvJson = if (textured) ",\"TEXCOORD_0\":$uv" else ""
            primitives.append("{\"attributes\":{\"POSITION\":$pos,\"NORMAL\":$nrm,\"COLOR_0\":$col$uvJson},\"indices\":$idx,\"material\":$index,\"mode\":4}")

            if (index > 0) materials.append(',')
            materials.append("{\"pbrMetallicRoughness\":{\"baseColorFactor\":[1,1,1,${num(m.alpha)}],")
            if (textured) materials.append("\"baseColorTexture\":{\"index\":${if (external) 0 else textureOf(m.tex)}},")
            materials.append("\"metallicFactor\":${num(m.metallic)},\"roughnessFactor\":${num(m.roughness)}}")
            if (textured && external && m.tex.normalScale > 0f && !m.unlit) {
                materials.append(",\"normalTexture\":{\"index\":1,\"scale\":${num(m.tex.normalScale)}}")
            }
            if (textured && external) usesExternal = true
            val ext = ArrayList<String>()
            m.emissive?.let { e ->
                val lin = e.linear()
                materials.append(",\"emissiveFactor\":[${num(lin[0])},${num(lin[1])},${num(lin[2])}]")
                if (m.emissiveStrength != 1f) {
                    ext += "\"KHR_materials_emissive_strength\":{\"emissiveStrength\":${num(m.emissiveStrength)}}"
                    usesStrength = true
                }
            }
            if (m.unlit) {
                ext += "\"KHR_materials_unlit\":{}"
                usesUnlit = true
            }
            if (m.alpha < 1f || m.blend) materials.append(",\"alphaMode\":\"BLEND\"")
            if (m.doubleSided) materials.append(",\"doubleSided\":true")
            if (ext.isNotEmpty()) materials.append(",\"extensions\":{${ext.joinToString(",")}}")
            materials.append('}')
        }
        val images = StringBuilder()
        val textures = StringBuilder()
        val imagePngs = if (usesExternal) listOf(PLACEHOLDER_ALBEDO, PLACEHOLDER_NORMAL) else textureIndex.keys.map { TextureGen.png(it) }
        imagePngs.forEachIndexed { i, png ->
            val view = addView(png, null)
            if (i > 0) { images.append(','); textures.append(',') }
            images.append("{\"bufferView\":$view,\"mimeType\":\"image/png\"}")
            textures.append("{\"sampler\":0,\"source\":$i}")
        }
        while (bin.size() % 4 != 0) bin.write(0)
        val binBytes = bin.toByteArray()

        val used = buildList {
            if (usesUnlit) add("\"KHR_materials_unlit\"")
            if (usesStrength) add("\"KHR_materials_emissive_strength\"")
        }
        val json = buildString {
            append("{\"asset\":{\"version\":\"2.0\",\"generator\":\"game3d-meshbuilder\"},")
            if (used.isNotEmpty()) append("\"extensionsUsed\":[${used.joinToString(",")}],")
            append("\"scene\":0,\"scenes\":[{\"nodes\":[0]}],")
            append("\"nodes\":[{\"mesh\":0,\"name\":\"${name.replace("\"", "")}\"}],")
            append("\"meshes\":[{\"primitives\":[$primitives]}],")
            append("\"materials\":[$materials],")
            if (imagePngs.isNotEmpty()) {
                append("\"images\":[$images],\"textures\":[$textures],")
                append("\"samplers\":[{\"magFilter\":9729,\"minFilter\":9987,\"wrapS\":10497,\"wrapT\":10497}],")
            }
            append("\"accessors\":[$accessors],")
            append("\"bufferViews\":[$views],")
            append("\"buffers\":[{\"byteLength\":${binBytes.size}}]}")
        }
        var jsonBytes = json.toByteArray(Charsets.UTF_8)
        val pad = (4 - jsonBytes.size % 4) % 4
        if (pad > 0) jsonBytes = jsonBytes + ByteArray(pad) { 0x20 }

        val total = 12 + 8 + jsonBytes.size + 8 + binBytes.size
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(MAGIC).putInt(2).putInt(total)
        out.putInt(jsonBytes.size).putInt(CHUNK_JSON).put(jsonBytes)
        out.putInt(binBytes.size).putInt(CHUNK_BIN).put(binBytes)
        return out.array()
    }

    private val PLACEHOLDER_ALBEDO by lazy { Png.encode(2, 2, ByteArray(16) { 0xFF.toByte() }) }
    private val PLACEHOLDER_NORMAL by lazy { Png.encode(2, 2, ByteArray(16) { i -> if (i % 4 == 2 || i % 4 == 3) 0xFF.toByte() else 0x80.toByte() }) }

    private fun num(v: Float): String {
        val f = if (v.isFinite()) v else 0f
        return if (f == f.toInt().toFloat()) f.toInt().toString() else f.toString()
    }

    private fun minMax(p: FloatArray): String {
        val mn = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val mx = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (i in p.indices) {
            val a = i % 3
            if (p[i] < mn[a]) mn[a] = p[i]
            if (p[i] > mx[a]) mx[a] = p[i]
        }
        return ",\"min\":[${num(mn[0])},${num(mn[1])},${num(mn[2])}],\"max\":[${num(mx[0])},${num(mx[1])},${num(mx[2])}]"
    }

    private fun floats(a: FloatArray): ByteArray {
        val b = ByteBuffer.allocate(a.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        b.asFloatBuffer().put(a)
        return b.array()
    }

    private fun ushorts(a: IntArray): ByteArray {
        val b = ByteBuffer.allocate(a.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (v in a) b.putShort(v.toShort())
        return b.array()
    }

    private fun uints(a: IntArray): ByteArray {
        val b = ByteBuffer.allocate(a.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (v in a) b.putInt(v)
        return b.array()
    }
}
