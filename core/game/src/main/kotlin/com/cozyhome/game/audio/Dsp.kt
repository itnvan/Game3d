package com.cozyhome.game.audio

import com.cozyhome.game.math.Rng
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

enum class Wave { SINE, TRIANGLE, SQUARE, SAW }

enum class Pass { LOW, BAND, HIGH }

data class Env(val attack: Float = 0.004f, val decay: Float = 0.3f, val sustain: Float = 0f, val release: Float = 0.02f)

object SineTable {
    private const val SIZE = 4096
    private val table = FloatArray(SIZE + 1) { kotlin.math.sin(2.0 * PI * it / SIZE).toFloat() }

    fun at(phase: Float): Float {
        val p = frac(phase) * SIZE
        val i = p.toInt()
        return table[i] + (table[i + 1] - table[i]) * (p - i)
    }
}

fun frac(x: Float): Float = x - kotlin.math.floor(x)

fun Wave.at(phase: Float): Float = when (this) {
    Wave.SINE -> SineTable.at(phase)
    Wave.TRIANGLE -> if (phase < 0.5f) 4f * phase - 1f else 3f - 4f * phase
    Wave.SQUARE -> 0.9f * (SineTable.at(phase) + SineTable.at(phase * 3f) / 3f + SineTable.at(phase * 5f) / 5f + SineTable.at(phase * 7f) / 7f)
    Wave.SAW -> {
        var s = 0f
        for (k in 1..7) s += SineTable.at(phase * k) / k
        0.55f * s
    }
}

fun midiToHz(midi: Float): Float = 440f * 2f.pow((midi - 69f) / 12f)

class EnvGen(env: Env, dur: Float, rate: Int) {
    private val attackN = max(1, (env.attack * rate).roundToInt())
    private val decayMul = if (env.decay > 0f) exp(-1.0 / (env.decay * rate)).toFloat() else 1f
    private val sustain = env.sustain
    private val holdN = (dur * rate).roundToInt()
    private val releaseN = max(1, (env.release * rate).roundToInt())
    private var i = 0
    private var level = 1f

    fun next(): Float {
        val a = if (i < attackN) i.toFloat() / attackN else 1f
        val body = sustain + (1f - sustain) * level
        if (i >= attackN) level = if (level > 1e-6f) level * decayMul else 0f
        val r = if (i >= holdN) max(0f, 1f - (i - holdN).toFloat() / releaseN) else 1f
        i++
        return a * body * r
    }
}

class Svf(private val mode: Pass, q: Float, private val rate: Int) {
    private val k = 1f / q
    private var ic1 = 0f
    private var ic2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var a3 = 0f

    fun tune(cutoff: Float) {
        val g = tan(PI * min(cutoff, rate * 0.45f) / rate).toFloat()
        a1 = 1f / (1f + g * (g + k))
        a2 = g * a1
        a3 = g * a2
    }

    fun process(v0: Float): Float {
        val v3 = v0 - ic2
        val v1 = a1 * ic1 + a2 * v3
        val v2 = ic2 + a2 * ic1 + a3 * v3
        ic1 = 2f * v1 - ic1
        ic2 = 2f * v2 - ic2
        return when (mode) {
            Pass.LOW -> v2
            Pass.BAND -> k * v1
            Pass.HIGH -> v0 - k * v1 - v2
        }
    }
}

class Pcm(val rate: Int, seconds: Float) {
    val data = FloatArray(max(1, (seconds * rate).roundToInt()))
    val size: Int get() = data.size
    var written = 0
        private set

    fun index(t: Float) = (t * rate).roundToInt()

    fun tone(
        at: Float,
        dur: Float,
        freq: Float,
        gain: Float,
        env: Env = Env(),
        wave: Wave = Wave.SINE,
        to: Float = freq,
        glide: Float = dur,
        vibrato: Float = 0f,
        vibratoHz: Float = 5.5f,
    ) {
        val start = index(at)
        val n = ((dur + env.release) * rate).roundToInt()
        val glideN = (glide * rate).roundToInt()
        val sweep = if (to != freq && glideN > 0) (to / freq).pow(1f / glideN) else 1f
        val gen = EnvGen(env, dur, rate)
        val vibStep = vibratoHz / rate
        var phase = 0f
        var vib = 0f
        var f = freq
        for (i in 0 until n) {
            val j = start + i
            if (j >= data.size) break
            val e = gen.next()
            if (j >= 0) data[j] += gain * e * wave.at(phase)
            val fi = if (vibrato != 0f) f * (1f + vibrato * SineTable.at(vib)) else f
            phase += fi / rate
            if (phase >= 1f) phase -= phase.toInt()
            if (i < glideN) f *= sweep
            vib += vibStep
            if (vib >= 1f) vib -= 1f
        }
        written = max(written, min(data.size, start + n))
    }

    fun partials(at: Float, base: Float, gain: Float, ratios: FloatArray, weights: FloatArray, decays: FloatArray) {
        for (k in ratios.indices) {
            tone(at, decays[k] * 3.5f, base * ratios[k], gain * weights[k], Env(0.0015f, decays[k], 0f, decays[k] * 0.5f))
        }
    }

    fun bell(at: Float, freq: Float, gain: Float, decay: Float) =
        partials(at, freq, gain, BELL_RATIOS, BELL_WEIGHTS, FloatArray(BELL_RATIOS.size) { decay / (1f + it * 0.9f) })

    fun musicBox(at: Float, freq: Float, gain: Float, decay: Float) =
        partials(at, freq, gain, BOX_RATIOS, BOX_WEIGHTS, FloatArray(BOX_RATIOS.size) { decay / (1f + it * 2.5f) })

    fun noise(
        at: Float,
        dur: Float,
        gain: Float,
        env: Env,
        rng: Rng,
        pass: Pass = Pass.BAND,
        cutoff: Float = 1000f,
        to: Float = cutoff,
        q: Float = 0.7f,
        grains: Float = 0f,
    ) {
        val start = index(at)
        val n = ((dur + env.release) * rate).roundToInt()
        val gen = EnvGen(env, dur, rate)
        val filter = Svf(pass, q, rate)
        val sweep = if (to != cutoff && n > 0) (to / cutoff).pow(1f / n) else 1f
        val grainChance = grains / rate
        var c = cutoff
        var grainLeft = 0
        var grainAmp = 0f
        var grainDecay = 1f
        for (i in 0 until n) {
            val j = start + i
            if (j >= data.size) break
            if (i % 8 == 0) filter.tune(c)
            var src = rng.nextFloat() * 2f - 1f
            if (grains > 0f) {
                if (grainLeft <= 0 && rng.nextFloat() < grainChance) {
                    grainLeft = (rate * rng.range(0.0015f, 0.005f)).toInt()
                    grainAmp = rng.range(0.35f, 1f)
                    grainDecay = exp(-4.0 / grainLeft).toFloat()
                }
                src *= if (grainLeft > 0) grainAmp else 0f
                grainAmp *= grainDecay
                grainLeft--
            }
            val y = filter.process(src) * gen.next() * gain
            if (j >= 0) data[j] += y
            c *= sweep
        }
        written = max(written, min(data.size, start + n))
    }

    fun lowpass(cutoff: Float) {
        val a = 1f - exp(-2.0 * PI * cutoff / rate).toFloat()
        var y = 0f
        for (i in 0 until written) {
            y += a * (data[i] - y)
            data[i] = y
        }
    }

    fun highpass(cutoff: Float) {
        val a = exp(-2.0 * PI * cutoff / rate).toFloat()
        var y = 0f
        var prev = 0f
        for (i in 0 until written) {
            val x = data[i]
            y = a * (y + x - prev)
            prev = x
            data[i] = y
        }
    }

    fun add(other: Pcm, gain: Float) {
        val n = min(size, other.written)
        for (i in 0 until n) data[i] += other.data[i] * gain
        written = max(written, n)
    }

    fun peak(): Float {
        var p = 0f
        for (i in 0 until written) p = max(p, abs(data[i]))
        return p
    }

    fun finish(level: Float, fade: Float = 0.012f): FloatArray {
        highpass(25f)
        val p = peak()
        if (p < 1e-6f) return FloatArray(1)
        var end = written
        while (end > 1 && abs(data[end - 1]) < p * 0.003f) end--
        end = min(data.size, end + rate / 200)
        val out = data.copyOf(end)
        val fadeN = min(end, (fade * rate).roundToInt())
        for (i in 0 until fadeN) out[end - 1 - i] *= i.toFloat() / fadeN
        val k = level / p
        for (i in out.indices) out[i] *= k
        return out
    }

    companion object {
        val BELL_RATIOS = floatArrayOf(1f, 2f, 3f, 4.2f)
        val BELL_WEIGHTS = floatArrayOf(1f, 0.3f, 0.12f, 0.06f)
        val BOX_RATIOS = floatArrayOf(1f, 3f, 5.4f)
        val BOX_WEIGHTS = floatArrayOf(1f, 0.22f, 0.07f)
    }
}

class Reverb(rate: Int, room: Float, damp: Float) {
    private class Comb(size: Int, val feedback: Float, val damp: Float) {
        val buf = FloatArray(size)
        var i = 0
        var store = 0f
        fun process(x: Float): Float {
            val out = buf[i]
            store = out * (1f - damp) + store * damp
            buf[i] = x + store * feedback
            if (++i == buf.size) i = 0
            return out
        }
    }

    private class Allpass(size: Int) {
        val buf = FloatArray(size)
        var i = 0
        fun process(x: Float): Float {
            val b = buf[i]
            buf[i] = x + b * 0.5f
            if (++i == buf.size) i = 0
            return b - x
        }
    }

    private val combs = intArrayOf(1116, 1277, 1422, 1557).map { Comb(it * rate / 44100, room, damp) }
    private val allpasses = intArrayOf(556, 341).map { Allpass(it * rate / 44100) }

    fun process(x: Float): Float {
        var y = 0f
        for (c in combs) y += c.process(x)
        for (a in allpasses) y = a.process(y)
        return y * 0.25f
    }
}
