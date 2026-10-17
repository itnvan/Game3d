package com.cozyhome.app

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cozyhome.app.audio.LocalSfx
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.ui.root.AppRoot
import com.cozyhome.app.ui.theme.GameTheme
import com.cozyhome.game.audio.Sfx
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volumeControlStream = AudioManager.STREAM_MUSIC
        hideSystemBars()
        setContent {
            CompositionLocalProvider(LocalSfx provides vm.audio) {
                GameTheme { AppRoot(vm) }
            }
        }
        handleDebug(intent)
    }

    override fun onStart() {
        super.onStart()
        vm.audio.resume()
    }

    override fun onStop() {
        vm.audio.pause()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDebug(intent)
    }

    private fun handleDebug(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        if (intent.hasExtra("tp_x")) {
            vm.debugTeleport(intent.getFloatExtra("tp_x", 0f), intent.getFloatExtra("tp_z", 0f), intent.getFloatExtra("tp_yaw", 0f))
        }
        if (intent.hasExtra("coins")) vm.debugCoins(intent.getIntExtra("coins", 0))
        if (intent.hasExtra("xp")) vm.debugXp(intent.getIntExtra("xp", 0))
        if (intent.hasExtra("hour")) vm.debugHour(intent.getFloatExtra("hour", 9f))
        if (intent.hasExtra("grow")) vm.debugGrow()
        if (intent.hasExtra("festival")) vm.debugFestival(intent.getStringExtra("festival").orEmpty())
        if (intent.hasExtra("pantry")) vm.debugPantry()
        intent.getStringExtra("sfx")?.let { name -> Sfx.entries.firstOrNull { it.name == name }?.let { vm.audio.play(it) } }
        if (intent.hasExtra("gfx")) vm.gfxDebug = GameViewModel.GfxDebug(
            shadows = intent.getBooleanExtra("shadows", true),
            scale = intent.getFloatExtra("scale", 0.6f),
            post = intent.getBooleanExtra("post", true),
            scenery = intent.getBooleanExtra("scenery", true),
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
