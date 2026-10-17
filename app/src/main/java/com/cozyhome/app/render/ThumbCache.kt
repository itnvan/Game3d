package com.cozyhome.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.models.CropModels
import com.cozyhome.game.models.FurnitureModels
import com.cozyhome.game.raster.Thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ThumbCache(private val context: Context) {
    private val dir = File(context.cacheDir, "thumbs_v$VERSION").apply { mkdirs() }
    private val memory = ConcurrentHashMap<String, ImageBitmap>()
    private val gate = Semaphore(2)

    fun cached(key: String): ImageBitmap? = memory[key]

    suspend fun get(key: String, size: Int, yaw: Float = -32f, pitch: Float = 26f, build: () -> Mesh): ImageBitmap = memory[key] ?: gate.withPermit {
        ThumbTextures.ensure(context)
        memory[key] ?: withContext(Dispatchers.Default) {
            val file = File(dir, "$key.png")
            val fromDisk = if (file.exists()) runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() else null
            val bitmap = fromDisk ?: Bitmap.createBitmap(Thumbnail.render(build(), size, yaw, pitch), size, size, Bitmap.Config.ARGB_8888).also { bmp ->
                runCatching { file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            }
            bitmap.asImageBitmap().also { memory[key] = it }
        }
    }

    companion object {
        const val VERSION = 2
    }
}

@Composable
fun rememberItemThumb(id: String, variant: Int = 0, size: Int = 192): ImageBitmap? {
    val cache = koinInject<ThumbCache>()
    val key = "item_${id}_$variant"
    return produceState(cache.cached(key), key) {
        value = cache.get(key, size) { FurnitureModels.build(id, variant, on = true) }
    }.value
}

@Composable
fun rememberCropThumb(id: String, size: Int = 160): ImageBitmap? {
    val cache = koinInject<ThumbCache>()
    val key = "crop_$id"
    return produceState(cache.cached(key), key) {
        value = cache.get(key, size) { CropModels.icon(id) }
    }.value
}

@Composable
fun rememberMeshThumb(key: String, size: Int = 144, yaw: Float = -18f, pitch: Float = 8f, build: () -> Mesh): ImageBitmap? {
    val cache = koinInject<ThumbCache>()
    return produceState(cache.cached(key), key) {
        value = cache.get(key, size, yaw, pitch, build)
    }.value
}
