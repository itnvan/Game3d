package com.cozyhome.app.ui.root

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.game.Screen
import com.cozyhome.app.ui.create.CreateScreen
import com.cozyhome.app.ui.play.LoadingOverlay
import com.cozyhome.app.ui.play.PlayScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(vm: GameViewModel = koinViewModel()) {
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flush() }
    Crossfade(vm.screen, label = "screen") { screen ->
        when (screen) {
            Screen.LOADING -> LoadingOverlay()
            Screen.CREATE -> CreateScreen(
                initialName = vm.save.name,
                initial = vm.save.look,
                editing = vm.editingLook,
                onDone = { name, look -> vm.createCharacter(name, look) },
                onCancel = { vm.cancelEditLook() },
            )
            Screen.PLAY -> PlayScreen(vm)
        }
    }
}
