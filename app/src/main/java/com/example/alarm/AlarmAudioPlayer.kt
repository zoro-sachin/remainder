package com.example.alarm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.AlarmSoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object AlarmAudioPlayer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var isPlaying: Boolean = false
        private set

    fun playSound(soundType: AlarmSoundType, loop: Boolean = false) {
        stopSound()
        isPlaying = true

        playbackJob = scope.launch {
            try {
                do {
                    for (i in soundType.frequencies.indices) {
                        if (!isActive || !isPlaying) break
                        val freq = soundType.frequencies[i]
                        val durationMs = soundType.noteDurationsMs.getOrElse(i) { 250 }
                        playTone(freq, durationMs)
                        delay(40) // Inter-note gap
                    }
                    if (loop && isPlaying && isActive) {
                        delay(600) // Inter-cycle delay
                    }
                } while (loop && isPlaying && isActive)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (!loop) {
                    isPlaying = false
                }
            }
        }
    }

    private fun playTone(frequency: Double, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        
        val attackSamples = (numSamples * 0.1).toInt().coerceAtLeast(1)
        val decaySamples = (numSamples * 0.2).toInt().coerceAtLeast(1)
        val sustainLevel = 0.85

        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i / (sampleRate / frequency)
            // Primary tone + harmonic for warmth
            val sineVal = sin(angle) * 0.8 + sin(angle * 2.0) * 0.15 + sin(angle * 3.0) * 0.05
            
            // Envelope (ADSR)
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > numSamples - decaySamples -> (numSamples - i).toDouble() / decaySamples * sustainLevel
                else -> sustainLevel
            }
            
            val sample = (sineVal * envelope * Short.MAX_VALUE * 0.7).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong())
            track.stop()
            track.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSound() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }
}
