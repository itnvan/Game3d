package com.cozyhome.game

import com.cozyhome.game.audio.Cue
import com.cozyhome.game.audio.MusicSynth
import com.cozyhome.game.audio.Score
import com.cozyhome.game.audio.Sfx
import com.cozyhome.game.audio.SfxSynth
import com.cozyhome.game.audio.Song
import com.cozyhome.game.audio.SoundCues
import com.cozyhome.game.game.FrameInput
import com.cozyhome.game.game.GameWorld
import com.cozyhome.game.game.Msg
import com.cozyhome.game.game.SaveAccess
import com.cozyhome.game.game.WorldEvent
import com.cozyhome.game.math.V2
import com.cozyhome.game.sports.SportEvent
import com.cozyhome.game.sports.SportKind
import com.cozyhome.game.state.Rules
import com.cozyhome.game.state.SaveData
import com.cozyhome.game.world.Lot
import com.cozyhome.game.world.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

class AudioTest {
    private val outDir = File("build/sounds").apply { mkdirs() }

    @Test
    fun everySfxRendersCleanly() {
        val rate = 48_000
        val bank = SfxSynth.renderAll(rate)
        for (sfx in Sfx.entries) {
            val pcm = bank[sfx.ordinal]
            val seconds = pcm.size.toFloat() / rate
            assertTrue("$sfx too short/long: $seconds", seconds in 0.03f..3f)
            assertTrue("$sfx has NaN", pcm.none { it.isNaN() || it.isInfinite() })
            val peak = pcm.maxOf { abs(it) }
            assertEquals("$sfx peak", sfx.level, peak, 1e-3f)
            assertTrue("$sfx starts with a click: ${pcm[0]}", abs(pcm[0]) < 0.02f)
            assertTrue("$sfx ends abruptly: ${pcm.last()}", abs(pcm.last()) < 0.005f)
            assertTrue("$sfx has DC offset", abs(pcm.average()) < 0.02f)
        }
    }

    @Test
    fun scoresHaveFullBars() {
        for (song in Song.entries) {
            val score = Score.of(song)
            assertEquals(song.name, score.chords.size, score.bars.size)
            score.bars.indices.forEach { assertEquals("$song bar ${it + 1}", song.beatsPerBar.toFloat(), score.barLength(it), 1e-4f) }
        }
        assertEquals(60, Score.midi("C4"))
        assertEquals(70, Score.midi("Bb4"))
        assertEquals(listOf(57, 60, 64), Score.chord("Am").voiced(55))
    }

    @Test
    fun songsLoopSeamlessly() {
        val rate = 24_000
        for (song in Song.entries) {
            val t0 = System.nanoTime()
            val pcm = MusicSynth.render(song, rate)
            println("$song rendered ${pcm.size / rate}s in ${(System.nanoTime() - t0) / 1_000_000} ms")
            assertEquals(song.name, (Score.of(song).loopSeconds * rate).toInt().toFloat(), pcm.size.toFloat(), 1f)
            val deltas = FloatArray(pcm.size - 1) { abs(pcm[it + 1] - pcm[it]) }.sorted()
            val p999 = deltas[(deltas.size * 0.999).toInt()]
            val seam = abs(pcm[0] - pcm.last())
            assertTrue("$song seam $seam vs p99.9 $p999", seam <= p999)
            val rms = sqrt(pcm.sumOf { (it * it).toDouble() } / pcm.size)
            assertTrue("$song rms $rms", rms in 0.05..0.2)
            assertTrue("$song clips", pcm.maxOf { abs(it) } <= 0.95f)
            writeWav(File(outDir, "song_${song.name.lowercase()}.wav"), pcm, rate)
        }
    }

    @Test
    fun cuesFollowGameEvents() {
        fun sfx(e: WorldEvent) = SoundCues.of(e).map { it.sfx }
        fun sport(e: SportEvent) = sfx(WorldEvent.Sport(e))
        assertEquals(listOf(Sfx.SWISH, Sfx.SCORE), sport(SportEvent.Basket(2, swish = true, streak = 1)))
        assertTrue(Sfx.SPARKLE in sport(SportEvent.Basket(3, swish = false, streak = 3)))
        assertEquals(listOf(Sfx.BEEP), sport(SportEvent.Countdown(2)))
        assertEquals(listOf(Sfx.GO), sport(SportEvent.Go))
        assertEquals(listOf(Sfx.SAVE, Sfx.MISS), sport(SportEvent.KeeperSave))
        assertEquals(listOf(Sfx.BALL_BOUNCE), sport(SportEvent.Bounce(SportKind.BASKETBALL, 4f)))
        assertEquals(listOf(Sfx.BALL_THUD), sport(SportEvent.Bounce(SportKind.SOCCER, 4f)))
        val soft = SoundCues.of(SportEvent.Bounce(SportKind.BASKETBALL, 1f)).single().volume
        val hard = SoundCues.of(SportEvent.Bounce(SportKind.BASKETBALL, 6f)).single().volume
        assertTrue(soft < hard)
        val gates = (0 until 8).map { SoundCues.of(SportEvent.Gate(1, it)).single().pitch }
        assertEquals(gates.sorted(), gates)
        assertEquals(listOf(Sfx.STEP_WOOD), sfx(WorldEvent.Footstep(Surface.WOOD, running = false)))
        assertEquals(listOf(Sfx.COIN), sfx(WorldEvent.Reward(10, 1, null)))
        assertEquals(listOf(Sfx.SPEND), sfx(WorldEvent.Reward(-10, 0, null)))
        assertTrue(sfx(WorldEvent.Reward(0, 3, null)).isEmpty())
        assertTrue(sfx(WorldEvent.Toast(Msg.HARVESTED)).isEmpty())
        assertEquals(listOf(Sfx.HARVEST), SoundCues.of(Msg.HARVESTED).map { it.sfx })
        assertEquals(listOf(Sfx.ERROR), SoundCues.of(Msg.NOT_ENOUGH_COINS).map { it.sfx })
        val combo = SoundCues.of(WorldEvent.Bounced(6)).single()
        assertTrue(combo.pitch > SoundCues.of(WorldEvent.Bounced(1)).single().pitch)
    }

    @Test
    fun batchRulesAvoidDoubleJingles() {
        val result = WorldEvent.SportResult(SportKind.SOCCER, 3, 5, 30, best = false, millis = 0L)
        val withLevel = SoundCues.collect(listOf(result, WorldEvent.Reward(30, 11, null), WorldEvent.LevelUp(3)))
        assertEquals(listOf(Sfx.COIN, Sfx.LEVEL_UP), withLevel.map { it.sfx })
        val lap = SoundCues.collect(listOf(WorldEvent.Sport(SportEvent.Gate(1, 7)), WorldEvent.Sport(SportEvent.Lap(1, 9000L))))
        assertEquals(listOf(Cue(Sfx.LAP)), lap)
        val twice = SoundCues.collect(listOf(WorldEvent.Reward(5, 0, null), WorldEvent.Reward(8, 0, null)))
        assertEquals(1, twice.size)
    }

    @Test
    fun walkingEmitsFootstepsForTheGround() {
        assertEquals(Surface.WOOD, Lot.surfaceAt(V2(0f, -11f)))
        assertEquals(Surface.HARD, Lot.surfaceAt(Lot.SPAWN))
        assertEquals(Surface.HARD, Lot.surfaceAt(Lot.TRACK_START))
        assertEquals(Surface.HARD, Lot.surfaceAt(V2(-15f, -10f)))
        assertEquals(Surface.GRASS, Lot.surfaceAt(V2(16f, 2f)))
        val steps = walk(V2(16f, 3f))
        assertTrue("steps ${steps.size}", steps.size in 4..14)
        assertTrue(steps.all { it.surface == Surface.GRASS })
        assertTrue(steps.last().running)
    }

    private fun walk(from: V2): List<WorldEvent.Footstep> {
        var data = Rules.starter()
        val access = object : SaveAccess {
            override val current: SaveData get() = data
            override fun update(transform: (SaveData) -> SaveData): SaveData { data = transform(data); return data }
        }
        val world = GameWorld(access, 0L, seed = 3)
        world.restore(from, 180f)
        val input = FrameInput().apply { stickY = 1f }
        val events = ArrayList<WorldEvent>()
        val steps = ArrayList<WorldEvent.Footstep>()
        repeat(60) {
            events.clear()
            world.step(input, 1 / 60f, 1000L, events)
            steps += events.filterIsInstance<WorldEvent.Footstep>()
        }
        return steps
    }

    @Test
    fun exportSoundSheet() {
        val rate = 48_000
        val bank = SfxSynth.renderAll(rate)
        val cols = 4
        val tileW = 300
        val tileH = 150
        val rows = (Sfx.entries.size + cols - 1) / cols
        val img = BufferedImage(cols * tileW, rows * tileH, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color(0x16, 0x18, 0x24)
        g.fillRect(0, 0, img.width, img.height)
        for (sfx in Sfx.entries) {
            val pcm = bank[sfx.ordinal]
            writeWav(File(outDir, "${sfx.name.lowercase()}.wav"), pcm, rate)
            val x0 = (sfx.ordinal % cols) * tileW
            val y0 = (sfx.ordinal / cols) * tileH
            drawSpectrogram(img, pcm, rate, x0, y0 + 50, tileW - 4, tileH - 54)
            g.color = Color(0x7F, 0xD8, 0xBE)
            val seconds = pcm.size.toFloat() / rate
            for (px in 0 until tileW - 4) {
                val a = (px.toFloat() / (tileW - 4) * pcm.size).toInt()
                val b = ((px + 1f) / (tileW - 4) * pcm.size).toInt().coerceAtMost(pcm.size)
                var m = 0f
                for (i in a until b) m = maxOf(m, abs(pcm[i]))
                val h = (m * 34).toInt()
                g.drawLine(x0 + px, y0 + 32 - h, x0 + px, y0 + 32 + h)
            }
            val win = rate / 20
            val loud = (0 until maxOf(1, pcm.size - win) step win / 4).maxOf { s -> sqrt((s until minOf(pcm.size, s + win)).sumOf { (pcm[it] * pcm[it]).toDouble() } / win) }
            println("%-12s %.2fs peak %.2f loud %.3f".format(sfx.name, seconds, sfx.level, loud))
            g.color = Color.WHITE
            g.drawString("${sfx.name} %.2fs".format(seconds), x0 + 4, y0 + 12)
        }
        g.dispose()
        ImageIO.write(img, "png", File(outDir, "sfx_sheet.png"))
    }

    private fun drawSpectrogram(img: BufferedImage, pcm: FloatArray, rate: Int, x0: Int, y0: Int, w: Int, h: Int) {
        val n = 512
        val window = FloatArray(n) { (0.5 - 0.5 * cos(2 * Math.PI * it / (n - 1))).toFloat() }
        val maxHz = 12_000f
        for (px in 0 until w) {
            val center = (px.toFloat() / w * pcm.size).toInt()
            val re = DoubleArray(n) { val j = center - n / 2 + it; if (j in pcm.indices) (pcm[j] * window[it]).toDouble() else 0.0 }
            val im = DoubleArray(n)
            fft(re, im)
            for (py in 0 until h) {
                val hz = 60f * Math.pow((maxHz / 60f).toDouble(), (h - 1 - py).toDouble() / (h - 1)).toFloat()
                val bin = (hz / rate * n).toInt().coerceIn(1, n / 2 - 1)
                val mag = sqrt(re[bin] * re[bin] + im[bin] * im[bin])
                val db = (20 * ln(mag + 1e-9) / ln(10.0)).coerceIn(-70.0, 10.0)
                val v = ((db + 70) / 80).toFloat()
                img.setRGB(x0 + px, y0 + py, Color(v, v * v, (1 - v) * 0.4f + v * 0.2f).rgb)
            }
        }
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { re[i] = re[j].also { re[j] = re[i] }; im[i] = im[j].also { im[j] = im[i] } }
        }
        var len = 2
        while (len <= n) {
            val ang = -2 * Math.PI / len
            for (i in 0 until n step len) {
                for (k in 0 until len / 2) {
                    val wr = cos(ang * k)
                    val wi = sin(ang * k)
                    val a = i + k
                    val b = a + len / 2
                    val tr = re[b] * wr - im[b] * wi
                    val ti = re[b] * wi + im[b] * wr
                    re[b] = re[a] - tr; im[b] = im[a] - ti
                    re[a] += tr; im[a] += ti
                }
            }
            len = len shl 1
        }
    }

    private fun writeWav(file: File, pcm: FloatArray, rate: Int) {
        DataOutputStream(FileOutputStream(file).buffered()).use { out ->
            fun le32(v: Int) = out.writeInt(Integer.reverseBytes(v))
            fun le16(v: Int) = out.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
            out.writeBytes("RIFF"); le32(36 + pcm.size * 2); out.writeBytes("WAVE")
            out.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            out.writeBytes("data"); le32(pcm.size * 2)
            for (x in pcm) le16((x.coerceIn(-1f, 1f) * 32767f).toInt())
        }
    }
}
