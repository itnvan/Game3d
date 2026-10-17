package com.cozyhome.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import android.util.Log
import com.cozyhome.app.BuildConfig
import com.cozyhome.game.audio.Sfx
import com.cozyhome.game.audio.Song
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Mixer(val rate: Int, private val musicRate: Int) {
    private class Voice(val data: FloatArray, val step: Float, val gain: Float, val sfx: Int) {
        var pos = 0f
    }

    private val incoming = ConcurrentLinkedQueue<Voice>()
    private val voices = ArrayList<Voice>()
    private val lock = ReentrantLock()
    private val work = lock.newCondition()
    private val bank = AtomicReferenceArray<FloatArray>(Sfx.entries.size)
    private val songs = AtomicReferenceArray<FloatArray>(Song.entries.size)
    private val songPos = DoubleArray(Song.entries.size)
    private val songGain = FloatArray(Song.entries.size)
    private var duck = 1f
    private var duckFrames = 0
    private var limiter = 1f
    private var meterPeak = 0f
    private var meterFrames = 0
    private var worker: Thread? = null

    @Volatile private var generation = 0
    @Volatile var song = Song.DAY
    @Volatile var musicOn = true
    @Volatile var sfxOn = true

    fun setSfx(sfx: Sfx, pcm: FloatArray) {
        bank.set(sfx.ordinal, pcm)
    }

    fun setSong(s: Song, pcm: FloatArray) {
        songs.set(s.ordinal, pcm)
        wake()
    }

    fun play(sfx: Sfx, volume: Float, pitch: Float) {
        if (!sfxOn) return
        val data = bank.get(sfx.ordinal) ?: return
        incoming += Voice(data, pitch.coerceIn(0.25f, 4f), volume * SFX_GAIN, sfx.ordinal)
        wake()
    }

    fun wake() {
        lock.withLock { work.signalAll() }
    }

    @Synchronized
    fun start() {
        val gen = ++generation
        val previous = worker
        worker = thread(name = "audio-mixer", isDaemon = true) {
            previous?.join()
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            songGain.fill(0f)
            voices.clear()
            while (generation == gen) {
                val track = createTrack() ?: break
                try {
                    track.play()
                    loop(track, gen)
                } catch (e: Exception) {
                    Log.e(TAG, "mixer stopped", e)
                } finally {
                    runCatching { track.pause(); track.flush() }
                    track.release()
                }
                if (generation == gen) Thread.sleep(RETRY_MS)
            }
        }
    }

    @Synchronized
    fun stop() {
        generation++
        wake()
    }

    private fun createTrack(): AudioTrack? = runCatching {
        val minBytes = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(minBytes, FRAMES * 2 * 3))
        if (Build.VERSION.SDK_INT >= 26) builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        builder.build().takeIf { it.state == AudioTrack.STATE_INITIALIZED }.also {
            Log.d(TAG, "track rate=$rate minBytes=$minBytes buffer=${it?.bufferSizeInFrames}")
        }
    }.onFailure { Log.e(TAG, "AudioTrack failed", it) }.getOrNull()

    private fun loop(track: AudioTrack, gen: Int) {
        val mix = FloatArray(FRAMES)
        val out = ShortArray(FRAMES)
        var silentFrames = 0
        while (generation == gen) {
            drainIncoming()
            val audible = render(mix)
            if (!audible) {
                silentFrames += FRAMES
                if (silentFrames > rate) {
                    track.pause()
                    waitForWork(gen)
                    if (generation != gen) break
                    track.play()
                    silentFrames = 0
                    continue
                }
            } else {
                silentFrames = 0
            }
            if (BuildConfig.DEBUG) meter(mix)
            for (i in 0 until FRAMES) out[i] = (mix[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            if (track.write(out, 0, FRAMES) < 0) return
        }
    }

    private fun meter(mix: FloatArray) {
        for (x in mix) meterPeak = max(meterPeak, abs(x))
        meterFrames += FRAMES
        if (meterFrames < rate * 2) return
        Log.d(TAG, "out peak=%.3f voices=%d song=%s music=%s".format(meterPeak, voices.size, song, songGain.joinToString { "%.2f".format(it) }))
        meterFrames = 0
        meterPeak = 0f
    }

    private fun waitForWork(gen: Int) {
        lock.withLock {
            while (generation == gen && incoming.isEmpty() && !musicWanted()) work.await(1, TimeUnit.SECONDS)
        }
    }

    private fun musicWanted() = musicOn && songs.get(song.ordinal) != null

    private fun drainIncoming() {
        if (!sfxOn) {
            incoming.clear()
            voices.clear()
            return
        }
        while (true) {
            val v = incoming.poll() ?: break
            if (voices.count { it.sfx == v.sfx } >= SAME_SFX_LIMIT) voices.removeAt(voices.indexOfFirst { it.sfx == v.sfx })
            if (voices.size >= MAX_VOICES) voices.removeAt(0)
            voices += v
            if (BuildConfig.DEBUG) Log.d(TAG, "play ${Sfx.entries[v.sfx]} gain=%.2f step=%.2f".format(v.gain, v.step))
            if (Sfx.entries[v.sfx].ducksMusic) duckFrames = max(duckFrames, (v.data.size / v.step).toInt())
        }
    }

    private fun render(mix: FloatArray): Boolean {
        mix.fill(0f)
        val duckFrom = duck
        duck = approach(duck, if (duckFrames > 0) DUCK_LEVEL else 1f, FRAMES.toFloat() / (rate * DUCK_SECONDS))
        duckFrames = max(0, duckFrames - FRAMES)
        var audible = false
        val step = musicRate.toDouble() / rate
        for (s in 0 until songs.length()) {
            val data = songs.get(s)
            val target = if (musicOn && data != null && s == song.ordinal) MUSIC_GAIN else 0f
            val fade = if (musicOn) CROSSFADE_SECONDS else MUTE_SECONDS
            val g0 = songGain[s]
            val g1 = approach(g0, target, MUSIC_GAIN * FRAMES / (rate * fade))
            songGain[s] = g1
            if (data == null || (g0 == 0f && g1 == 0f)) {
                songPos[s] = 0.0
                continue
            }
            audible = true
            var pos = songPos[s]
            val len = data.size
            for (i in 0 until FRAMES) {
                val k = i.toFloat() / FRAMES
                val g = (g0 + (g1 - g0) * k) * (duckFrom + (duck - duckFrom) * k)
                val idx = pos.toInt()
                val f = (pos - idx).toFloat()
                val a = data[idx]
                val b = data[if (idx + 1 == len) 0 else idx + 1]
                mix[i] += (a + (b - a) * f) * g
                pos += step
                if (pos >= len) pos -= len
            }
            songPos[s] = pos
        }
        var v = voices.size - 1
        while (v >= 0) {
            val voice = voices[v]
            val d = voice.data
            val last = d.size - 1
            var pos = voice.pos
            var i = 0
            while (i < FRAMES && pos < last) {
                val idx = pos.toInt()
                val f = pos - idx
                mix[i] += (d[idx] + (d[idx + 1] - d[idx]) * f) * voice.gain
                pos += voice.step
                i++
            }
            voice.pos = pos
            if (pos >= last) voices.removeAt(v)
            v--
        }
        if (voices.isNotEmpty()) audible = true
        limit(mix)
        return audible
    }

    private fun limit(mix: FloatArray) {
        var peak = 0f
        for (x in mix) peak = max(peak, abs(x))
        val from = limiter
        val wanted = if (peak > CEILING) CEILING / peak else 1f
        limiter = if (wanted < limiter) wanted else min(1f, limiter + LIMITER_RELEASE)
        if (from == 1f && limiter == 1f) return
        for (i in mix.indices) mix[i] *= from + (limiter - from) * i / FRAMES
    }

    private fun approach(value: Float, target: Float, maxStep: Float): Float =
        if (abs(target - value) <= maxStep) target else value + if (target > value) maxStep else -maxStep

    companion object {
        private const val TAG = "Audio"
        const val FRAMES = 256
        private const val MAX_VOICES = 20
        private const val SAME_SFX_LIMIT = 3
        private const val SFX_GAIN = 0.95f
        private const val MUSIC_GAIN = 0.34f
        private const val DUCK_LEVEL = 0.35f
        private const val DUCK_SECONDS = 0.25f
        private const val CROSSFADE_SECONDS = 3f
        private const val MUTE_SECONDS = 0.4f
        private const val CEILING = 0.97f
        private const val LIMITER_RELEASE = 0.004f
        private const val RETRY_MS = 300L
    }
}
