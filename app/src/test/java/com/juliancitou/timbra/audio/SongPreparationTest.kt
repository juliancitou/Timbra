package com.juliancitou.timbra.audio

import com.juliancitou.timbra.audio.music.MUSIC_TARGET_LUFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val RATE = 48_000

class SongPreparationTest {

    private fun tone(seconds: Int, amplitude: Float) =
        FloatArray(RATE * seconds) { amplitude * sin(2 * PI * 1_000 * it / RATE).toFloat() }

    @Test
    fun loudSongEndsAtTheLevelOfTheAppMusicWithoutReachingTheLimiter() {
        val loud = tone(6, 0.9f)
        val song = prepareSong(loud, loud.copyOf(), RATE)
        assertEquals(MUSIC_TARGET_LUFS, loudnessLufs(song.reference, RATE), 0.5)
        assertTrue(song.left.all { abs(it) < 0.4f })
    }

    @Test
    fun referenceIsTheLoudestPart() {
        val quietThenLoud = tone(10, 0.02f) + tone(5, 0.8f)
        val song = prepareSong(quietThenLoud, quietThenLoud.copyOf(), RATE)
        val endOfLoudPart = song.left.copyOfRange(song.left.size - RATE * 2, song.left.size - RATE)
        assertEquals(
            endOfLoudPart.maxOf { abs(it) }.toDouble(),
            song.reference.maxOf { abs(it) },
            0.01,
        )
    }

    @Test
    fun channelsStayDifferent() {
        val song = prepareSong(tone(3, 0.5f), tone(3, 0.1f), RATE)
        val middle = RATE + RATE / 8 + 3
        assertEquals(5.0, (song.left[middle] / song.right[middle]).toDouble(), 0.01)
    }

    @Test
    fun endsFadeSoTheLoopHasNoClick() {
        val song = prepareSong(FloatArray(RATE * 2) { 0.5f }, FloatArray(RATE * 2) { 0.5f }, RATE)
        assertEquals(0f, song.left.first(), 1e-4f)
        assertEquals(0f, song.left.last(), 1e-4f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSilence() {
        prepareSong(FloatArray(RATE * 2), FloatArray(RATE * 2), RATE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAFileTooShortToLoop() {
        prepareSong(FloatArray(100), FloatArray(100), RATE)
    }
}
