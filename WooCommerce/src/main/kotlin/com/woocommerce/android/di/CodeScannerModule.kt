package com.woocommerce.android.di

import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.woocommerce.android.ui.barcodescanner.MediaImageProvider
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
    fun provideGoogleCodeScanner(
        barcodeScanner: BarcodeScanner,
        googleCodeScannerErrorMapper: GoogleCodeScannerErrorMapper,
        barcodeFormatMapper: GoogleBarcodeFormatMapper,
        inputImageProvider: MediaImageProvider,
    ): CodeScanner {
        return GoogleMLKitCodeScanner(
            barcodeScanner,
            googleCodeScannerErrorMapper,
            barcodeFormatMapper,
            inputImageProvider,
        )
    }

    @Provides
    @Reusable
    fun providesGoogleBarcodeScanner() = BarcodeScanning.getClient()

    @Provides
    @Reusable
    fun provideInputImageProvider() = MediaImageProvider()
}
