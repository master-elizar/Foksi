package com.foksi.app.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/** Plays the looping alarm tone and vibration used by alarm-style reminders. */
object AlarmSoundPlayer {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    /** True while a ringtone or vibration is active, so a second caller does not restart it. */
    val isRinging: Boolean
        @Synchronized get() = player != null || vibrator != null

    @Synchronized
    fun start(context: Context, uri: Uri, vibrate: Boolean, sound: Boolean) {
        stop()
        if (sound) {
            runCatching {
                player = MediaPlayer().apply {
                    setDataSource(context, uri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    setOnErrorListener { _, _, _ -> true }
                    prepare()
                    start()
                }
            }.onFailure { Log.w("FoksiAlarmSound", "Cannot play alarm tone", it) }
            runCatching {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audio?.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                    audio.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        audio.getStreamMaxVolume(AudioManager.STREAM_ALARM) / 2,
                        0
                    )
                }
            }
        }
        if (vibrate) {
            vibrator = resolveVibrator(context)
            runCatching {
                val pattern = longArrayOf(0, 700, 500, 700, 500)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
        }
    }

    @Synchronized
    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun resolveVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
