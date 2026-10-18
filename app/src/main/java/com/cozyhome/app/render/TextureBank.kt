package com.cozyhome.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import com.cozyhome.game.mesh.Tex
import com.cozyhome.game.mesh.TexImage
import com.cozyhome.game.mesh.TextureGen
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.android.TextureHelper
import io.github.sceneview.model.ModelInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

val LocalTextureBank = compositionLocalOf<TextureBank?> { null }

class TextureBank(private val engine: Engine, private val context: Context) {
    private val albedo = HashMap<Tex, Texture>()
    private val normals = HashMap<Tex, Texture>()
    private val skies = HashMap<String, Texture>()
    private val sampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.REPEAT,
    ).apply { anisotropy = 4f }
    private val skySampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.REPEAT,
    ).apply { wrapModeT = TextureSampler.WrapMode.CLAMP_TO_EDGE }
    private var destroyed = false

    var ready by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0f)
        private set

    private val bound = ArrayList<Pair<ModelInstance, List<Tex>>>()

    suspend fun load() {
        val jobs = buildList {
            for (t in Tex.entries) {
                if (t == Tex.NONE) continue
                val name = t.asset
                if (name == null) add(Job(t, null, false)) else {
                    add(Job(t, "tex/${name}_a.jpg", false))
                    if (t.normalScale > 0f) add(Job(t, "tex/${name}_n.jpg", true))
                }
            }
            add(Job(Tex.SKY, "tex/sky_day.jpg", false, sky = "day"))
            add(Job(Tex.SKY, "tex/sky_night.jpg", false, sky = "night"))
        }
        jobs.forEachIndexed { i, job ->
            val bitmap = if (job.path != null) withContext(Dispatchers.IO) { decode(job.path) } else null
            val generated = if (bitmap == null && job.sky == null && !job.normal) withContext(Dispatchers.Default) { TextureGen.image(job.tex) } else null
            if (destroyed) return
            withFrameNanos { }
            if (destroyed) return
            runCatching { upload(job, bitmap, generated) }.onFailure { Log.e("TextureBank", "upload ${job.path ?: job.tex}", it) }
            bitmap?.recycle()
            progress = (i + 1) / jobs.size.toFloat()
        }
        synchronized(bound) { bound.forEach { (inst, texes) -> applyTo(inst, texes) } }
        ready = true
    }

    private class Job(val tex: Tex, val path: String?, val normal: Boolean, val sky: String? = null)

    private fun decode(path: String): Bitmap? = runCatching {
        context.assets.open(path).use { s ->
            BitmapFactory.decodeStream(s, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
        }
    }.getOrNull()

    private fun levels(w: Int, h: Int): Int = 32 - Integer.numberOfLeadingZeros(maxOf(w, h))

    private fun newTexture(w: Int, h: Int, srgb: Boolean): Texture = Texture.Builder()
        .width(w).height(h).levels(levels(w, h))
        .sampler(Texture.Sampler.SAMPLER_2D)
        .format(if (srgb) Texture.InternalFormat.SRGB8_A8 else Texture.InternalFormat.RGBA8)
        .usage(Texture.Usage.DEFAULT or Texture.Usage.GEN_MIPMAPPABLE)
        .build(engine)

    private fun upload(job: Job, bitmap: Bitmap?, generated: TexImage?) {
        if (bitmap != null) {
            val tex = newTexture(bitmap.width, bitmap.height, !job.normal)
            TextureHelper.setBitmap(engine, tex, 0, bitmap)
            tex.generateMipmaps(engine)
            when {
                job.sky != null -> skies[job.sky] = tex
                job.normal -> normals[job.tex] = tex
                else -> albedo[job.tex] = tex
            }
        } else if (generated != null) {
            albedo[job.tex] = fromImage(generated)
        }
    }

    private fun fromImage(img: TexImage): Texture {
        val tex = newTexture(img.size, img.size, true)
        val buffer = ByteBuffer.allocateDirect(img.rgba.size).put(img.rgba).also { it.flip() }
        tex.setImage(engine, 0, Texture.PixelBufferDescriptor(buffer, Texture.Format.RGBA, Texture.Type.UBYTE))
        tex.generateMipmaps(engine)
        return tex
    }

    fun bind(instance: ModelInstance, texes: List<Tex>) {
        if (texes.none { it != Tex.NONE }) return
        synchronized(bound) { bound += instance to texes }
        if (ready) applyTo(instance, texes)
    }

    fun unbind(instance: ModelInstance) {
        synchronized(bound) { bound.removeAll { it.first === instance } }
    }

    private fun applyTo(instance: ModelInstance, texes: List<Tex>) {
        val rm = engine.renderableManager
        for (entity in instance.entities) {
            if (!rm.hasComponent(entity)) continue
            val ri = rm.getInstance(entity)
            val count = rm.getPrimitiveCount(ri)
            for (p in 0 until minOf(count, texes.size)) {
                val tex = texes[p]
                if (tex == Tex.NONE) continue
                val mi = rm.getMaterialInstanceAt(ri, p)
                if (tex == Tex.SKY) {
                    skies["day"]?.let { mi.setParameter("baseColorMap", it, skySampler) }
                    continue
                }
                albedo[tex]?.let { mi.setParameter("baseColorMap", it, sampler) }
                normals[tex]?.let { mi.setParameter("normalMap", it, sampler) }
            }
        }
    }

    fun setSky(mi: MaterialInstance, night: Boolean) {
        skies[if (night) "night" else "day"]?.let { mi.setParameter("baseColorMap", it, skySampler) }
    }

    fun destroy() {
        destroyed = true
        synchronized(bound) { bound.clear() }
        (albedo.values + normals.values + skies.values).forEach { runCatching { engine.destroyTexture(it) } }
        albedo.clear(); normals.clear(); skies.clear()
    }
}

@Composable
fun rememberTextureBank(engine: Engine): TextureBank {
    val context = LocalContext.current
    val bank = remember(engine) { TextureBank(engine, context.applicationContext) }
    DisposableEffect(bank) { onDispose { bank.destroy() } }
    return bank
}

object ThumbTextures {
    private val mutex = Mutex()
    @Volatile private var installed = false

    suspend fun ensure(context: Context) {
        if (installed) return
        mutex.withLock {
            if (installed) return
            withContext(Dispatchers.IO) {
                for (t in Tex.entries) {
                    val name = t.asset ?: continue
                    val bmp = runCatching {
                        context.assets.open("tex/${name}_a.jpg").use { s ->
                            BitmapFactory.decodeStream(s, null, BitmapFactory.Options().apply { inSampleSize = if (t.size >= 256) 4 else 4 })
                        }
                    }.getOrNull() ?: continue
                    val size = minOf(bmp.width, bmp.height)
                    val px = IntArray(size * size)
                    bmp.getPixels(px, 0, size, 0, 0, size, size)
                    bmp.recycle()
                    val rgba = ByteArray(size * size * 4)
                    for (i in px.indices) {
                        val c = px[i]
                        rgba[i * 4] = (c shr 16).toByte(); rgba[i * 4 + 1] = (c shr 8).toByte(); rgba[i * 4 + 2] = c.toByte(); rgba[i * 4 + 3] = 0xFF.toByte()
                    }
                    TextureGen.install(t, TexImage(size, rgba))
                }
            }
            installed = true
        }
    }
}
