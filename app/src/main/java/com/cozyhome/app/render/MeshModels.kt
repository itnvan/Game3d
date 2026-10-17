package com.cozyhome.app.render

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.cozyhome.game.mesh.Glb
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.Tex
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Mesh.toDirectBuffer(name: String): ByteBuffer {
    val bytes = Glb.encode(this, name, external = true)
    return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.flip() }
}

object MeshLoad {
    var pending by mutableIntStateOf(0)
        private set
    var finished by mutableIntStateOf(0)
        private set
    private val mutex = Mutex()
    private var sliceStart = 0L
    private const val SLICE_NS = 7_000_000L

    fun begin() { pending++ }

    fun end() {
        pending--
        finished++
    }

    suspend fun <T> onMain(block: () -> T): T = mutex.withLock {
        if (System.nanoTime() - sliceStart > SLICE_NS) {
            withFrameNanos { }
            sliceStart = System.nanoTime()
        }
        block()
    }
}

private class Built(val buffer: ByteBuffer, val texes: List<Tex>)

@Composable
fun rememberMeshInstance(modelLoader: ModelLoader, key: Any, build: () -> Mesh): ModelInstance? {
    val bank = LocalTextureBank.current
    val instance = produceState<ModelInstance?>(initialValue = null, modelLoader, key) {
        MeshLoad.begin()
        try {
            val built = withContext(Dispatchers.Default) {
                runCatching {
                    build().takeUnless { it.isEmpty }?.let { Built(it.toDirectBuffer(key.toString()), Glb.primitiveTextures(it)) }
                }.onFailure { Log.e("MeshModels", "build($key) failed", it) }.getOrNull()
            } ?: return@produceState
            value = MeshLoad.onMain {
                val t0 = System.nanoTime()
                runCatching { modelLoader.createModelInstance(built.buffer) }
                    .also { val ms = (System.nanoTime() - t0) / 1e6f; if (ms > 8f) Log.d("Load", "create $key ${ms}ms") }
                    .onFailure { Log.e("MeshModels", "createModelInstance($key) failed", it) }
                    .getOrNull()
                    ?.also { bank?.bind(it, built.texes) }
            }
        } finally {
            MeshLoad.end()
        }
    }.value
    DisposableEffect(instance) {
        onDispose {
            instance?.let {
                bank?.unbind(it)
                modelLoader.destroyModel(it.model)
            }
        }
    }
    return instance
}
