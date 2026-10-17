package com.cozyhome.game.audio

import com.cozyhome.game.math.Rng
import kotlin.math.min
import kotlin.math.roundToInt

enum class Song(val bpm: Float, val beatsPerBar: Int) { DAY(96f, 4), NIGHT(66f, 3) }

data class Note(val beat: Float, val beats: Float, val midi: Int)

data class Chord(val root: Int, val minor: Boolean) {
    val pitchClasses: List<Int> get() = listOf(root, root + if (minor) 3 else 4, root + 7).map { it % 12 }

    fun voiced(low: Int): List<Int> = pitchClasses.map { pc -> low + Math.floorMod(pc - low, 12) }.sorted()

    fun rootIn(low: Int): Int = low + Math.floorMod(root - low, 12)
}

class Score(val song: Song, chordLine: String, melodyLine: String) {
    val chords: List<Chord> = chordLine.trim().split(Regex("\\s+")).map(::chord)
    private val parsed: List<Pair<List<Note>, Float>> = melodyLine.split('|').map(::bar)
    val bars: List<List<Note>> = parsed.map { it.first }
    val beatSeconds: Float get() = 60f / song.bpm
    val loopSeconds: Float get() = chords.size * song.beatsPerBar * beatSeconds

    private fun bar(text: String): Pair<List<Note>, Float> {
        val notes = ArrayList<Note>()
        var beat = 0f
        for (token in text.trim().split(Regex("\\s+"))) {
            val (name, len) = token.split(':')
            val beats = len.toFloat()
            if (name != "r") notes += Note(beat, beats, midi(name))
            beat += beats
        }
        return notes to beat
    }

    fun barLength(index: Int): Float = parsed[index].second

    companion object {
        private val LETTERS = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

        fun midi(name: String): Int {
            val pc = LETTERS.getValue(name[0])
            val accidental = when (name.getOrNull(1)) { '#' -> 1; 'b' -> -1; else -> 0 }
            val octave = name.substring(if (accidental != 0) 2 else 1).toInt()
            return 12 * (octave + 1) + pc + accidental
        }

        fun chord(symbol: String): Chord {
            val accidental = when (symbol.getOrNull(1)) { '#' -> 1; 'b' -> -1; else -> 0 }
            val rest = symbol.substring(if (accidental != 0) 2 else 1)
            return Chord(Math.floorMod(LETTERS.getValue(symbol[0]) + accidental, 12), rest.startsWith("m"))
        }

        val DAY = Score(
            Song.DAY,
            "C Am F G C Am Dm G F G Em Am F G C G",
            "E5:1 G5:.5 A5:.5 G5:1 E5:1 | C5:1 E5:1 D5:1.5 r:.5 | C5:.5 D5:.5 E5:1 A5:1 G5:1 | D5:1.5 E5:.5 D5:1 B4:1 |" +
                "E5:1 G5:.5 A5:.5 C6:1 A5:1 | G5:1 E5:1 A5:1.5 r:.5 | F5:1 E5:.5 D5:.5 A5:1 F5:1 | G5:2 D5:1 r:1 |" +
                "A5:1 C6:1 A5:1 G5:1 | G5:1 D5:1 G5:1.5 r:.5 | E5:1 G5:1 B5:1 G5:1 | A5:2 E5:1 C5:1 |" +
                "F5:1 A5:1 C6:1.5 A5:.5 | G5:1 B5:.5 A5:.5 G5:1 D5:1 | E5:1 D5:.5 C5:.5 E5:1 G5:1 | D5:2 B4:1 r:1",
        )

        val NIGHT = Score(
            Song.NIGHT,
            "C Am F G C Am F G F C Dm Am F C G C",
            "G5:1 E6:1.5 D6:.5 | C6:2 A5:1 | A5:1.5 G5:.5 A5:1 | G5:2 B5:1 |" +
                "G5:1 E6:1.5 D6:.5 | C6:2 E6:1 | D6:1.5 C6:.5 A5:1 | G5:3 |" +
                "A5:1 C6:1 A5:1 | G5:1.5 E5:.5 G5:1 | F5:1 A5:1 D6:1 | C6:2 A5:1 |" +
                "A5:1.5 G5:.5 F5:1 | E5:1 G5:1 C6:1 | D6:1.5 C6:.5 B5:1 | C6:3",
        )

        fun of(song: Song): Score = when (song) {
            Song.DAY -> DAY
            Song.NIGHT -> NIGHT
        }
    }
}

object MusicSynth {
    private const val TAIL_SECONDS = 4f
    private const val TARGET_RMS = 0.11f
    private const val PEAK_CAP = 0.92f

    fun render(song: Song, rate: Int): FloatArray {
        val score = Score.of(song)
        val loopN = (score.loopSeconds * rate).roundToInt()
        val dry = Pcm(rate, score.loopSeconds + TAIL_SECONDS)
        val rng = Rng(song.ordinal + 101L)
        val wet = when (song) {
            Song.DAY -> {
                arrangeDay(dry, score, rng)
                0.22f
            }
            Song.NIGHT -> {
                arrangeNight(dry, score)
                0.38f
            }
        }
        val out = dry.data.copyOf(loopN)
        for (i in loopN until dry.size) out[(i - loopN) % loopN] += dry.data[i]
        addReverb(out, rate, wet, room = if (song == Song.DAY) 0.8f else 0.86f)
        normalize(out)
        return out
    }

    private fun addReverb(out: FloatArray, rate: Int, wet: Float, room: Float) {
        val reverb = Reverb(rate, room, 0.4f)
        for (x in out) reverb.process(x)
        val tail = FloatArray(out.size) { reverb.process(out[it]) }
        for (i in out.indices) out[i] += tail[i] * wet
    }

    private fun normalize(out: FloatArray) {
        var sum = 0.0
        var peak = 0f
        for (x in out) {
            sum += x * x
            peak = maxOf(peak, kotlin.math.abs(x))
        }
        val rms = kotlin.math.sqrt(sum / out.size).toFloat()
        if (rms < 1e-6f) return
        val k = min(TARGET_RMS / rms, PEAK_CAP / peak)
        for (i in out.indices) out[i] *= k
    }

    private fun arrangeDay(dry: Pcm, score: Score, rng: Rng) {
        val beat = score.beatSeconds
        val bar = beat * score.song.beatsPerBar
        val pad = Pcm(dry.rate, dry.size.toFloat() / dry.rate)
        score.chords.forEachIndexed { b, chord ->
            val t0 = b * bar
            for (m in chord.voiced(55)) {
                val f = midiToHz(m.toFloat())
                val env = Env(0.35f, 3f, 0.8f, 0.7f)
                pad.tone(t0, bar, f * 1.0025f, 0.06f, env, Wave.TRIANGLE)
                pad.tone(t0, bar, f * 0.9975f, 0.06f, env, Wave.TRIANGLE)
            }
            val root = chord.rootIn(48)
            val fifth = if (root + 7 < 60) root + 7 else root - 5
            bass(dry, t0, beat * 1.5f, root)
            bass(dry, t0 + beat * 2f, beat, fifth)
            bass(dry, t0 + beat * 3.5f, beat * 0.5f, root)
            val arp = chord.voiced(60)
            val pattern = intArrayOf(0, 1, 2, 1, 0, 1, 2, 1)
            pattern.forEachIndexed { i, k ->
                val accent = if (i % 2 == 0) 1.3f else 1f
                marimba(dry, t0 + i * beat * 0.5f, midiToHz(arp[k].toFloat()), 0.045f * accent)
            }
            for (i in 0 until 4) {
                dry.noise(t0 + (i + 0.5f) * beat, 0.03f, 0.03f + rng.range(0f, 0.01f), Env(0.002f, 0.015f), rng, Pass.HIGH, 6000f)
            }
            for (i in intArrayOf(1, 3)) {
                dry.tone(t0 + i * beat, 0.02f, 1100f, 0.05f, Env(0.0005f, 0.012f))
                dry.tone(t0 + i * beat, 0.02f, 2200f, 0.02f, Env(0.0005f, 0.008f))
            }
            for (i in intArrayOf(0, 2)) dry.tone(t0 + i * beat, 0.1f, 120f, 0.18f, Env(0.001f, 0.07f), to = 50f, glide = 0.05f)
            for (n in score.bars[b]) {
                val t = t0 + n.beat * beat
                val f = midiToHz(n.midi.toFloat())
                val dur = n.beats * beat * 0.9f
                dry.tone(t, dur, f, 0.2f, Env(0.004f, 0.5f, 0.25f, 0.12f))
                dry.tone(t, dur, f * 2f, 0.06f, Env(0.004f, 0.25f, 0f, 0.1f))
                dry.tone(t, 0.1f, f * 3f, 0.03f, Env(0.002f, 0.05f))
            }
        }
        pad.lowpass(1600f)
        dry.add(pad, 1f)
    }

    private fun arrangeNight(dry: Pcm, score: Score) {
        val beat = score.beatSeconds
        val bar = beat * score.song.beatsPerBar
        val pad = Pcm(dry.rate, dry.size.toFloat() / dry.rate)
        score.chords.forEachIndexed { b, chord ->
            val t0 = b * bar
            for (m in chord.voiced(52)) {
                val f = midiToHz(m.toFloat())
                val env = Env(0.9f, 4f, 0.85f, 1.4f)
                pad.tone(t0, bar, f * 1.002f, 0.06f, env)
                pad.tone(t0, bar, f * 0.998f, 0.05f, env, Wave.TRIANGLE)
            }
            val root = chord.rootIn(48)
            val third = root + (if (chord.minor) 3 else 4) + 12
            intArrayOf(root, root + 7, third).forEachIndexed { i, m -> harp(dry, t0 + i * beat, midiToHz(m.toFloat()), beat * 1.6f) }
            dry.tone(t0, bar * 0.9f, midiToHz(chord.rootIn(43).toFloat()), 0.1f, Env(0.05f, 2f, 0.5f, 0.6f))
            for (n in score.bars[b]) {
                dry.musicBox(t0 + n.beat * beat, midiToHz(n.midi.toFloat()), 0.2f, 1.1f)
            }
        }
        pad.lowpass(1100f)
        dry.add(pad, 1f)
    }

    private fun bass(p: Pcm, at: Float, dur: Float, midi: Int) {
        val f = midiToHz(midi.toFloat())
        val env = Env(0.008f, 0.45f, 0.3f, 0.08f)
        p.tone(at, dur, f, 0.22f, env)
        p.tone(at, dur, f * 2f, 0.09f, env)
        p.tone(at, dur, f * 3f, 0.03f, env)
    }

    private fun marimba(p: Pcm, at: Float, f: Float, gain: Float) {
        p.tone(at, 0.6f, f, gain, Env(0.002f, 0.25f, 0f, 0.05f))
        p.tone(at, 0.1f, f * 3.9f, gain * 0.3f, Env(0.001f, 0.03f))
    }

    private fun harp(p: Pcm, at: Float, f: Float, dur: Float) {
        p.tone(at, dur, f, 0.07f, Env(0.006f, 0.9f, 0f, 0.3f))
        p.tone(at, dur, f * 2f, 0.02f, Env(0.006f, 0.4f, 0f, 0.2f))
    }
}
