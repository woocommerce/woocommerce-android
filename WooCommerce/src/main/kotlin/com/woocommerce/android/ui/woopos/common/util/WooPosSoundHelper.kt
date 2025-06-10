package com.woocommerce.android.ui.woopos.common.util

import android.content.Context
import android.media.MediaPlayer
import com.woocommerce.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosSoundHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun playChaChing() = withContext(Dispatchers.IO) {
        val mp = MediaPlayer.create(context, R.raw.cha_ching)
        mp.setOnCompletionListener { it.release() }
        mp.start()
    }

    suspend fun playBarcodeScanFailure() = withContext(Dispatchers.IO) {
        val mp = MediaPlayer.create(context, R.raw.pos_scan_failure)
        mp.setOnCompletionListener {
            it.release()
        }
        mp.start()
    }
}
