package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for the product images service - this allows clients to control the service without having a reference
 * to the [Context]
 */
@Singleton
class ProductImagesServiceWrapper
@Inject constructor(
    private val context: Context
) {
    fun startService() {
        ContextCompat.startForegroundService(context, Intent(context, ProductImagesService::class.java))
    }

    fun stopService() {
        context.stopService(Intent(context, ProductImagesService::class.java))
    }
}
