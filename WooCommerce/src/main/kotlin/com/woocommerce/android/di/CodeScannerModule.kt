package com.woocommerce.android.di

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.woocommerce.android.ui.orders.creation.CodeScanner
import com.woocommerce.android.ui.orders.creation.GoogleCodeScanner
import com.woocommerce.android.ui.orders.creation.GoogleCodeScannerErrorMapper
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class CodeScannerModule {
    @Provides
    @Reusable
    fun provideGoogleCodeScanner(
        context: Context,
        googleCodeScannerErrorMapper: GoogleCodeScannerErrorMapper
    ): CodeScanner {
        val options = GmsBarcodeScannerOptions.Builder().allowManualInput().build()
        return GoogleCodeScanner(
            GmsBarcodeScanning.getClient(context, options),
            googleCodeScannerErrorMapper
        )
    }
}
