package com.cozyhome.game.audio

import com.cozyhome.game.math.Rng

enum class Sfx(val level: Float, val jitter: Float = 0f, val ducksMusic: Boolean = false) {
    STEP_GRASS(0.2f, 0.16f),
    STEP_WOOD(0.22f, 0.12f),
    STEP_HARD(0.26f, 0.14f),
    TAP(0.32f, 0.04f),
    SWITCH(0.45f),
    BALL_BOUNCE(0.55f, 0.06f),
    RIM(0.42f, 0.04f),
    BOARD(0.5f, 0.05f),
    SWISH(0.5f, 0.05f),
    SCORE(0.36f),
    THROW(0.3f, 0.08f),
    KICK(0.6f, 0.05f),
    BALL_THUD(0.45f, 0.08f),
    POST(0.42f),
    SAVE(0.55f),
    MISS(0.36f),
    GOAL(0.6f, ducksMusic = true),
    LEVEL_UP(0.55f, ducksMusic = true),
    RESULT(0.5f, ducksMusic = true),
    SPARKLE(0.3f),
    COIN(0.38f, 0.03f),
    SPEND(0.36f),
    WATER(0.3f),
    HOSE(0.3f),
    DIG(0.36f, 0.08f),
    HARVEST(0.45f, 0.06f),
    ERROR(0.34f),
    PICK(0.36f, 0.05f),
    PLACE(0.45f, 0.04f),
    STORE(0.38f),
    SIT(0.4f, 0.06f),
    BOING(0.4f),
    WHOOSH(0.26f, 0.1f),
    WHISTLE(0.22f),
    BEEP(0.28f),
    GO(0.32f),
    GATE(0.38f),
    LAP(0.4f),
    BUZZ(0.32f),
    LULLABY(0.4f, ducksMusic = true),
}

object SfxSynth {
    fun renderAll(rate: Int): Array<FloatArray> = Array(Sfx.entries.size) { render(Sfx.entries[it], rate) }

    fun render(sfx: Sfx, rate: Int): FloatArray {
        val rng = Rng(sfx.ordinal * 7919L + 17L)
        val p = Pcm(rate, MAX_SECONDS)
        p.compose(sfx, rng)
        return p.finish(sfx.level)
    }

    private const val MAX_SECONDS = 3f

    private fun Pcm.compose(sfx: Sfx, rng: Rng) {
        when (sfx) {
            Sfx.STEP_GRASS -> {
                noise(0f, 0.09f, 1f, Env(0.004f, 0.035f), rng, Pass.BAND, 3400f, 2300f, q = 0.9f, grains = 900f)
                noise(0f, 0.05f, 0.25f, Env(0.002f, 0.02f), rng, Pass.LOW, 900f)
                tone(0f, 0.05f, 95f, 0.35f, Env(0.002f, 0.025f), to = 60f)
            }
            Sfx.STEP_WOOD -> {
                tone(0f, 0.08f, 210f, 0.8f, Env(0.001f, 0.035f), to = 150f)
                tone(0f, 0.05f, 520f, 0.35f, Env(0.001f, 0.018f), to = 470f)
                noise(0f, 0.015f, 0.4f, Env(0.0005f, 0.004f), rng, Pass.HIGH, 1800f)
                tone(0f, 0.06f, 95f, 0.3f, Env(0.002f, 0.03f))
            }
            Sfx.STEP_HARD -> {
                noise(0f, 0.05f, 0.7f, Env(0.001f, 0.018f), rng, Pass.BAND, 2600f, q = 1.2f, grains = 1500f)
                noise(0f, 0.012f, 0.5f, Env(0.0005f, 0.003f), rng, Pass.HIGH, 2500f)
                tone(0f, 0.04f, 160f, 0.4f, Env(0.001f, 0.015f), to = 110f)
            }
            Sfx.TAP -> {
                tone(0f, 0.05f, 620f, 1f, Env(0.001f, 0.018f), to = 1050f, glide = 0.03f)
                noise(0f, 0.004f, 0.15f, Env(0.0003f, 0.0015f), rng, Pass.HIGH, 4000f)
            }
            Sfx.SWITCH -> {
                noise(0f, 0.006f, 1f, Env(0.0003f, 0.0018f), rng, Pass.BAND, 3000f, q = 1.5f)
                noise(0.028f, 0.006f, 0.7f, Env(0.0003f, 0.0018f), rng, Pass.BAND, 3400f, q = 1.5f)
                tone(0f, 0.02f, 1900f, 0.15f, Env(0.0005f, 0.006f))
            }
            Sfx.BALL_BOUNCE -> {
                tone(0f, 0.12f, 150f, 1f, Env(0.001f, 0.06f), to = 108f, glide = 0.06f)
                tone(0f, 0.06f, 310f, 0.5f, Env(0.001f, 0.025f), to = 250f)
                tone(0f, 0.12f, 1150f, 0.12f, Env(0.001f, 0.05f))
                noise(0f, 0.01f, 0.35f, Env(0.0005f, 0.003f), rng, Pass.LOW, 2500f)
            }
            Sfx.RIM -> {
                partials(0f, 540f, 0.8f, floatArrayOf(1f, 2.32f, 4.25f, 6.63f, 9.38f), floatArrayOf(1f, 0.55f, 0.35f, 0.2f, 0.12f), floatArrayOf(0.3f, 0.18f, 0.1f, 0.07f, 0.045f))
                noise(0f, 0.01f, 0.6f, Env(0.0003f, 0.003f), rng, Pass.HIGH, 3000f)
                tone(0f, 0.06f, 140f, 0.5f, Env(0.001f, 0.04f), to = 110f)
            }
            Sfx.BOARD -> {
                tone(0f, 0.1f, 120f, 0.9f, Env(0.001f, 0.06f), to = 95f)
                tone(0f, 0.06f, 250f, 0.4f, Env(0.001f, 0.025f), to = 200f)
                tone(0f, 0.05f, 640f, 0.3f, Env(0.0005f, 0.02f), to = 560f)
                noise(0f, 0.03f, 0.5f, Env(0.0005f, 0.01f), rng, Pass.LOW, 1800f)
                partials(0f, 260f, 0.3f, floatArrayOf(1f, 2.7f), floatArrayOf(0.6f, 0.3f), floatArrayOf(0.08f, 0.05f))
            }
            Sfx.SWISH -> {
                noise(0f, 0.38f, 1f, Env(0.03f, 0.16f), rng, Pass.BAND, 2200f, 4800f, q = 0.9f)
                noise(0.02f, 0.3f, 0.35f, Env(0.01f, 0.12f), rng, Pass.BAND, 5500f, q = 1.2f, grains = 300f)
            }
            Sfx.SCORE -> {
                bell(0f, 1318.5f, 0.8f, 0.4f)
                bell(0.09f, 1760f, 0.8f, 0.4f)
            }
            Sfx.THROW -> noise(0f, 0.22f, 1f, Env(0.06f, 0.1f), rng, Pass.BAND, 500f, 1400f, q = 1.2f)
            Sfx.KICK -> {
                tone(0f, 0.1f, 170f, 1f, Env(0.001f, 0.06f), to = 55f, glide = 0.07f)
                tone(0f, 0.06f, 340f, 0.45f, Env(0.001f, 0.025f), to = 180f)
                noise(0f, 0.02f, 0.6f, Env(0.0005f, 0.006f), rng, Pass.LOW, 3500f)
                tone(0f, 0.03f, 900f, 0.15f, Env(0.0005f, 0.012f), to = 600f)
            }
            Sfx.BALL_THUD -> {
                tone(0f, 0.07f, 120f, 0.9f, Env(0.001f, 0.04f), to = 80f)
                tone(0f, 0.05f, 260f, 0.4f, Env(0.001f, 0.02f), to = 170f)
                noise(0f, 0.05f, 0.4f, Env(0.002f, 0.02f), rng, Pass.BAND, 2500f, grains = 800f)
            }
            Sfx.POST -> {
                partials(0f, 760f, 0.8f, floatArrayOf(1f, 2.76f, 5.4f, 8.93f), floatArrayOf(1f, 0.5f, 0.25f, 0.12f), floatArrayOf(0.5f, 0.25f, 0.12f, 0.06f))
                noise(0f, 0.008f, 0.6f, Env(0.0003f, 0.0025f), rng, Pass.HIGH, 3500f)
                tone(0f, 0.05f, 140f, 0.45f, Env(0.001f, 0.03f), to = 100f)
            }
            Sfx.SAVE -> {
                noise(0f, 0.05f, 1f, Env(0.0005f, 0.018f), rng, Pass.BAND, 1500f, q = 0.8f)
                tone(0f, 0.08f, 130f, 0.8f, Env(0.001f, 0.05f), to = 85f)
                noise(0.02f, 0.25f, 0.15f, Env(0.02f, 0.1f), rng, Pass.LOW, 700f)
            }
            Sfx.MISS -> {
                tone(0f, 0.2f, 392f, 0.8f, Env(0.01f, 0.5f, 0.7f, 0.04f), Wave.TRIANGLE, to = 370f)
                tone(0.22f, 0.42f, 349.2f, 0.8f, Env(0.01f, 0.6f, 0.7f, 0.12f), Wave.TRIANGLE, to = 311f, glide = 0.4f, vibrato = 0.012f, vibratoHz = 6f)
                lowpass(1800f)
            }
            Sfx.GOAL -> {
                noise(0f, 0.45f, 0.55f, Env(0.02f, 0.2f), rng, Pass.BAND, 900f, 2600f, q = 0.8f)
                val fanfare = floatArrayOf(523.3f, 659.3f, 784f, 1046.5f)
                fanfare.forEachIndexed { i, f ->
                    val last = i == fanfare.lastIndex
                    val dur = if (last) 0.55f else 0.1f
                    val env = Env(0.008f, 0.4f, 0.6f, 0.12f)
                    tone(0.05f + i * 0.11f, dur, f, 0.5f, env, Wave.TRIANGLE, vibrato = if (last) 0.006f else 0f)
                    tone(0.05f + i * 0.11f, dur, f, 0.12f, env, Wave.SQUARE)
                }
                noise(0.05f, 1.4f, 0.35f, Env(0.25f, 0.9f, 0f, 0.3f), rng, Pass.BAND, 1100f, q = 0.5f, grains = 2500f)
                floatArrayOf(2093f, 2637f, 3136f).forEachIndexed { i, f -> bell(0.4f + i * 0.08f, f, 0.2f, 0.25f) }
            }
            Sfx.LEVEL_UP -> {
                floatArrayOf(523.3f, 659.3f, 784f, 1046.5f, 1318.5f).forEachIndexed { i, f -> bell(i * 0.08f, f, 0.5f, 0.35f) }
                floatArrayOf(1046.5f, 1318.5f, 1568f).forEach { bell(0.42f, it, 0.35f, 0.6f) }
                tone(0.42f, 0.8f, 523.3f, 0.2f, Env(0.02f, 0.6f, 0.5f, 0.3f), Wave.TRIANGLE)
                tone(0.42f, 0.8f, 784f, 0.2f, Env(0.02f, 0.6f, 0.5f, 0.3f), Wave.TRIANGLE)
                repeat(8) { tone(rng.range(0.45f, 1.2f), 0.02f, rng.range(2500f, 5000f), 0.12f, Env(0.001f, 0.06f, 0f, 0.05f)) }
            }
            Sfx.RESULT -> {
                floatArrayOf(784f, 1046.5f, 1318.5f).forEachIndexed { i, f ->
                    bell(i * 0.1f, f, 0.5f, 0.25f)
                    tone(i * 0.1f, 0.08f, f, 0.18f, Env(0.004f, 0.2f, 0.4f, 0.03f), Wave.TRIANGLE)
                }
                bell(0.3f, 1568f, 0.55f, 0.7f)
                tone(0.3f, 0.6f, 1568f, 0.15f, Env(0.01f, 0.5f, 0.5f, 0.3f), Wave.TRIANGLE, vibrato = 0.005f)
                floatArrayOf(523.3f, 659.3f, 784f).forEach { tone(0.3f, 0.6f, it, 0.15f, Env(0.02f, 0.5f, 0.5f, 0.3f), Wave.TRIANGLE) }
            }
            Sfx.SPARKLE -> floatArrayOf(2093f, 2349.3f, 2637f, 3136f, 3520f, 4186f).forEachIndexed { i, f ->
                bell(i * 0.05f, f, 0.5f * (1f - i * 0.1f), 0.18f)
            }
            Sfx.COIN -> {
                val first = Env(0.001f, 0.5f, 0.8f, 0.01f)
                tone(0f, 0.06f, 987.8f, 0.35f, first, Wave.SQUARE)
                tone(0f, 0.06f, 987.8f, 0.5f, first)
                tone(0.06f, 0.32f, 1318.5f, 0.3f, Env(0.001f, 0.18f), Wave.SQUARE)
                tone(0.06f, 0.32f, 1318.5f, 0.5f, Env(0.001f, 0.18f))
                lowpass(6000f)
            }
            Sfx.SPEND -> {
                noise(0f, 0.01f, 0.6f, Env(0.0005f, 0.003f), rng, Pass.BAND, 2500f, q = 1.4f)
                noise(0.05f, 0.01f, 0.5f, Env(0.0005f, 0.003f), rng, Pass.BAND, 2800f, q = 1.4f)
                partials(0.07f, 1568f, 0.6f, floatArrayOf(1f, 2.76f, 5.4f), floatArrayOf(1f, 0.4f, 0.15f), floatArrayOf(0.3f, 0.15f, 0.07f))
                bell(0.07f, 2093f, 0.35f, 0.3f)
            }
            Sfx.WATER -> water(1.05f, rng, hose = false)
            Sfx.HOSE -> {
                tone(0f, 0.09f, 1300f, 0.1f, Env(0.01f, 0.1f, 0.6f, 0.02f), Wave.TRIANGLE, to = 1700f)
                water(1.5f, rng, hose = true)
            }
            Sfx.DIG -> {
                noise(0f, 0.2f, 0.9f, Env(0.005f, 0.07f), rng, Pass.BAND, 1100f, q = 0.7f, grains = 1200f)
                tone(0f, 0.06f, 160f, 0.35f, Env(0.002f, 0.03f), to = 90f)
            }
            Sfx.HARVEST -> {
                tone(0f, 0.07f, 260f, 1f, Env(0.001f, 0.04f), to = 820f, glide = 0.05f)
                noise(0f, 0.005f, 0.3f, Env(0.0003f, 0.0015f), rng, Pass.HIGH, 3000f)
                noise(0f, 0.12f, 0.3f, Env(0.004f, 0.04f), rng, Pass.BAND, 3500f, grains = 900f)
                bell(0.06f, 1568f, 0.25f, 0.15f)
            }
            Sfx.ERROR -> {
                tone(0f, 0.09f, 330f, 0.8f, Env(0.003f, 0.2f, 0.6f, 0.03f), Wave.TRIANGLE)
                tone(0.1f, 0.12f, 262f, 0.8f, Env(0.003f, 0.2f, 0.6f, 0.05f), Wave.TRIANGLE)
                lowpass(1500f)
            }
            Sfx.PICK -> {
                tone(0f, 0.08f, 440f, 1f, Env(0.002f, 0.05f), to = 880f, glide = 0.06f)
                tone(0f, 0.08f, 880f, 0.2f, Env(0.002f, 0.04f), to = 1760f, glide = 0.06f)
            }
            Sfx.PLACE -> {
                tone(0f, 0.08f, 140f, 0.9f, Env(0.001f, 0.05f), to = 95f)
                tone(0f, 0.05f, 290f, 0.35f, Env(0.001f, 0.02f), to = 220f)
                tone(0f, 0.04f, 600f, 0.3f, Env(0.0005f, 0.015f), to = 520f)
                noise(0f, 0.02f, 0.3f, Env(0.0005f, 0.006f), rng, Pass.LOW, 2000f)
                bell(0.05f, 1760f, 0.18f, 0.12f)
            }
            Sfx.STORE -> {
                noise(0f, 0.25f, 0.8f, Env(0.03f, 0.1f), rng, Pass.BAND, 3000f, 700f, q = 1f)
                tone(0f, 0.2f, 600f, 0.25f, Env(0.01f, 0.1f), to = 300f)
            }
            Sfx.SIT -> {
                noise(0f, 0.2f, 1f, Env(0.012f, 0.07f), rng, Pass.LOW, 650f)
                tone(0f, 0.1f, 90f, 0.4f, Env(0.005f, 0.05f), to = 70f)
            }
            Sfx.BOING -> {
                tone(0f, 0.4f, 150f, 1f, Env(0.002f, 0.22f), to = 380f, glide = 0.12f, vibrato = 0.05f, vibratoHz = 18f)
                tone(0f, 0.05f, 90f, 0.4f, Env(0.001f, 0.03f))
            }
            Sfx.WHOOSH -> noise(0f, 0.35f, 1f, Env(0.12f, 0.12f), rng, Pass.BAND, 400f, 900f, q = 1.4f)
            Sfx.WHISTLE -> {
                tone(0f, 0.35f, 2850f, 0.8f, Env(0.01f, 1f, 0.9f, 0.04f), vibrato = 0.035f, vibratoHz = 28f)
                noise(0f, 0.35f, 0.25f, Env(0.01f, 1f, 0.9f, 0.04f), rng, Pass.BAND, 2850f, q = 4f)
            }
            Sfx.BEEP -> {
                tone(0f, 0.13f, 880f, 0.5f, Env(0.003f, 1f, 1f, 0.02f), Wave.SQUARE)
                tone(0f, 0.13f, 880f, 0.5f, Env(0.003f, 1f, 1f, 0.02f))
                lowpass(4000f)
            }
            Sfx.GO -> {
                tone(0f, 0.4f, 1760f, 0.45f, Env(0.003f, 1.5f, 0.8f, 0.06f), Wave.SQUARE)
                tone(0f, 0.4f, 1760f, 0.5f, Env(0.003f, 1.5f, 0.8f, 0.06f))
                tone(0f, 0.4f, 880f, 0.3f, Env(0.003f, 1.5f, 0.8f, 0.06f))
                lowpass(5000f)
            }
            Sfx.GATE -> bell(0f, 1568f, 1f, 0.35f)
            Sfx.LAP -> {
                bell(0f, 1568f, 0.8f, 0.35f)
                bell(0.12f, 2093f, 0.8f, 0.45f)
            }
            Sfx.BUZZ -> {
                tone(0f, 0.28f, 120f, 1f, Env(0.01f, 1f, 1f, 0.05f), Wave.SAW)
                lowpass(900f)
            }
            Sfx.LULLABY -> floatArrayOf(784f, 659.3f, 523.3f).forEachIndexed { i, f -> musicBox(i * 0.28f, f, 0.5f, 0.5f) }
        }
    }

    private fun Pcm.water(dur: Float, rng: Rng, hose: Boolean) {
        val flow = Env(0.08f, 10f, 1f, 0.18f)
        noise(0f, dur, 0.35f, flow, rng, Pass.BAND, if (hose) 2000f else 2800f, q = if (hose) 0.4f else 0.6f)
        noise(0f, dur, 0.2f, flow, rng, Pass.LOW, 600f)
        repeat(if (hose) 60 else 40) {
            val f = rng.range(1400f, 3200f)
            tone(rng.range(0.05f, dur - 0.05f), 0.03f, f, 0.12f, Env(0.001f, 0.01f), to = f * 0.6f, glide = 0.02f)
        }
    }
}
