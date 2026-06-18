package com.undy.startrobot3.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.undy.startrobot3.data.model.BeepTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sin

object BeepGenerator {

    // Trailing silence appended after the tone so that stopping playback at exactly
    // durationMs never clips the audible tone itself, keeping elapsed time deterministic.
    private const val TAIL_SILENCE_MS = 50

    suspend fun playBeep(frequencyHz: Float = 880f, durationMs: Int = 150) =
        withContext(Dispatchers.IO) {
            val sampleRate = 44100
            val toneSamples = sampleRate * durationMs / 1000
            val tailSamples = sampleRate * TAIL_SILENCE_MS / 1000
            val totalSamples = toneSamples + tailSamples
            val samples = ShortArray(totalSamples) { i ->
                if (i >= toneSamples) 0
                else {
                    val angle = 2.0 * Math.PI * i * frequencyHz / sampleRate
                    (sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
                }
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(totalSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                track.write(samples, 0, totalSamples)
                track.play()
                delay(durationMs.toLong())
            } finally {
                track.stop()
                track.release()
            }
        }

    suspend fun playStartBeep() = playBeep(frequencyHz = 1320f, durationMs = BeepTiming.START_BEEP_MS.toInt())

    suspend fun playCountdownBeeps(count: Int) {
        repeat(count) { i ->
            playBeep(frequencyHz = 880f, durationMs = BeepTiming.COUNTDOWN_TONE_MS.toInt())
            if (i < count - 1) delay(BeepTiming.COUNTDOWN_GAP_MS)
        }
    }
}
