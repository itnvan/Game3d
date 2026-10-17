package com.cozyhome.app

import android.app.Application
import com.cozyhome.app.audio.GameAudio
import com.cozyhome.app.data.SaveStore
import com.cozyhome.app.game.GameViewModel
import com.cozyhome.app.render.ThumbCache
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { SaveStore(androidContext()) }
    single { ThumbCache(androidContext()) }
    single { GameAudio(androidContext()) }
    viewModelOf(::GameViewModel)
}

class GameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GameApp)
            modules(appModule)
        }
    }
}
