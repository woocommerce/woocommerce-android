package com.woocommerce.android.di

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.woocommerce.android.ui.orders.creation.CodeScanner
import com.woocommerce.android.ui.orders.creation.GoogleCodeScanner
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
    fun provideGoogleCodeScanner(context: Context): CodeScanner {
        return GoogleCodeScanner(
            GmsBarcodeScanning.getClient(context)
        )
    }
}
