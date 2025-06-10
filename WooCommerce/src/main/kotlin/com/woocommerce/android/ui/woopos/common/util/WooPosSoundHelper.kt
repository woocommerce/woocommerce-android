package com.woocommerce.android.ui.woopos.common.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.woocommerce.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosSoundHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mp: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var failureSoundId: Int = 0
    private var chaChingSoundId: Int = 0

    suspend fun preloadPlayChaChing() = withContext(Dispatchers.IO) {
        if (soundPool == null) {
            init()
        }
        soundPool?.load(context, R.raw.cha_ching, 1)?.let { soundId ->
            chaChingSoundId = soundId
        }
    }

    suspend fun playChaChing() = withContext(Dispatchers.IO) {
        soundPool?.play(chaChingSoundId, 1.0f, 1.0f, 5, 0, 1.0f)
    }

    suspend fun preloadBarcodeScanFailure() = withContext(Dispatchers.IO) {
        if (soundPool == null) {
            init()
        }
        soundPool?.load(context, R.raw.pos_scan_failure, 1)?.let { soundId ->
            failureSoundId = soundId
        }
    }

    fun playBarcodeScanFailure() {
        soundPool?.play(failureSoundId, 1.0f, 1.0f, 5, 0, 1.0f)
    }

    private suspend fun init() = withContext(Dispatchers.IO) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()
    }

    fun onCleanup() {
        mp?.release()
        mp = null
        soundPool?.release()
        soundPool = null
    }
}
