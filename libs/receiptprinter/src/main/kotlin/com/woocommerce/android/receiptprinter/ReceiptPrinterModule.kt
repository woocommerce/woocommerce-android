package com.woocommerce.android.receiptprinter

import com.woocommerce.android.receiptprinter.star.StarReceiptPrinterManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ReceiptPrinterModule {
    @Binds
    @Singleton
    fun bindReceiptPrinterManager(manager: StarReceiptPrinterManager): ReceiptPrinterManager
}
