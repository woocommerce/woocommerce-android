package com.woocommerce.android.ui.woopos.common.util

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import com.woocommerce.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosSoundHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun playChaChing() = withContext(Dispatchers.IO) {
        val chaChingUri = "android.resource://${context.packageName}/${R.raw.cha_ching}".toUri()
        val mp = MediaPlayer.create(context, chaChingUri)
        mp.setOnCompletionListener { it.release() }
        mp.start()
    }
}
