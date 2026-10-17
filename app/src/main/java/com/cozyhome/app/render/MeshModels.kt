package com.cozyhome.app.render

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import com.cozyhome.game.mesh.Glb
import com.cozyhome.game.mesh.Mesh
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Mesh.toDirectBuffer(name: String): ByteBuffer {
    val bytes = Glb.encode(this, name)
    return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.flip() }
}

fun ModelLoader.createMeshInstance(mesh: Mesh, name: String): ModelInstance? =
    if (mesh.isEmpty) null else runCatching { createModelInstance(mesh.toDirectBuffer(name)) }
        .onFailure { Log.e("MeshModels", "createModelInstance($name) failed", it) }
        .getOrNull()

@Composable
fun rememberMeshInstance(modelLoader: ModelLoader, key: Any, build: () -> Mesh): ModelInstance? {
    val instance = produceState<ModelInstance?>(initialValue = null, modelLoader, key) {
        val buffer = withContext(Dispatchers.Default) {
            runCatching { build().takeUnless { it.isEmpty }?.toDirectBuffer(key.toString()) }
                .onFailure { Log.e("MeshModels", "build($key) failed", it) }
                .getOrNull()
        } ?: return@produceState
        value = runCatching { modelLoader.createModelInstance(buffer) }
            .onFailure { Log.e("MeshModels", "createModelInstance($key) failed", it) }
            .getOrNull()
    }.value
    DisposableEffect(instance) {
        onDispose { instance?.let { modelLoader.destroyModel(it.model) } }
    }
    return instance
}
