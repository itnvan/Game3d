package com.cozyhome.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.cozyhome.game.audio.Cue
import com.cozyhome.game.audio.MusicSynth
import com.cozyhome.game.audio.Sfx
import com.cozyhome.game.audio.SfxSynth
import com.cozyhome.game.audio.Song
import kotlin.concurrent.thread
import kotlin.random.Random

interface SfxPlayer {
    fun play(sfx: Sfx, volume: Float = 1f, pitch: Float = 1f)

    object Silent : SfxPlayer {
        override fun play(sfx: Sfx, volume: Float, pitch: Float) = Unit
    }
}

val LocalSfx = staticCompositionLocalOf<SfxPlayer> { SfxPlayer.Silent }

class GameAudio(context: Context) : SfxPlayer {
    private val prefs = context.getSharedPreferences("audio", Context.MODE_PRIVATE)
    private val rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf { it in 16_000..96_000 } ?: 48_000
    private val mixer = Mixer(rate, rate / 2)

    var soundOn by mutableStateOf(prefs.getBoolean(KEY_SOUND, true))
        private set
    var musicOn by mutableStateOf(prefs.getBoolean(KEY_MUSIC, true))
        private set

    init {
        mixer.sfxOn = soundOn
        mixer.musicOn = musicOn
        thread(name = "audio-synth", isDaemon = true) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            val t0 = SystemClock.elapsedRealtime()
            val cache = PcmCache(context, rate)
            for (sfx in Sfx.entries) mixer.setSfx(sfx, cache.get("sfx_${sfx.name}") { SfxSynth.render(sfx, rate) })
            val t1 = SystemClock.elapsedRealtime()
            for (song in Song.entries.sortedBy { it != mixer.song }) {
                mixer.setSong(song, cache.get("song_${song.name}") { MusicSynth.render(song, rate / 2) })
            }
            Log.d(TAG, "rate=$rate sfx ${t1 - t0} ms, music ${SystemClock.elapsedRealtime() - t1} ms")
        }
    }

    override fun play(sfx: Sfx, volume: Float, pitch: Float) {
        if (!soundOn) return
        val j = sfx.jitter
        val p = if (j > 0f) pitch * (1f + (Random.nextFloat() - 0.5f) * j) else pitch
        val v = if (j > 0f) volume * (1f - Random.nextFloat() * j * 0.6f) else volume
        mixer.play(sfx, v, p)
    }

    fun play(cues: List<Cue>) {
        for (c in cues) play(c.sfx, c.volume, c.pitch)
    }

    fun setNight(night: Boolean) {
        val song = if (night) Song.NIGHT else Song.DAY
        if (mixer.song != song) {
            mixer.song = song
            mixer.wake()
        }
    }

    fun toggleSound() {
        soundOn = !soundOn
        mixer.sfxOn = soundOn
        prefs.edit().putBoolean(KEY_SOUND, soundOn).apply()
        if (soundOn) play(Sfx.TAP)
    }

    fun toggleMusic() {
        musicOn = !musicOn
        mixer.musicOn = musicOn
        mixer.wake()
        prefs.edit().putBoolean(KEY_MUSIC, musicOn).apply()
    }

    fun resume() = mixer.start()

    fun pause() = mixer.stop()

    private companion object {
        const val TAG = "Audio"
        const val KEY_SOUND = "sound"
        const val KEY_MUSIC = "music"
    }
}
