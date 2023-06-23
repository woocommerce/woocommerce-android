package com.woocommerce.android.di

import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.woocommerce.android.ui.orders.creation.CodeScanner
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.orders.creation.GoogleCodeScannerErrorMapper
import com.woocommerce.android.ui.orders.creation.GoogleMLKitCodeScanner
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
    @ExperimentalGetImage
    fun provideGoogleCodeScanner(
        googleCodeScannerErrorMapper: GoogleCodeScannerErrorMapper,
        barcodeFormatMapper: GoogleBarcodeFormatMapper,
    ): CodeScanner {
//        val options = GmsBarcodeScannerOptions.Builder().allowManualInput().build()
        return GoogleMLKitCodeScanner(
            BarcodeScanning.getClient(),
            googleCodeScannerErrorMapper,
            barcodeFormatMapper
        )
    }
}
