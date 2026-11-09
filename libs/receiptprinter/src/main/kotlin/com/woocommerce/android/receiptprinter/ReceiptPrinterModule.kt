package com.woocommerce.android.receiptprinter

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module of the receipt printer library. It owns the StarIO10 SDK so no other module imports it directly.
 */
@Module
@InstallIn(SingletonComponent::class)
object ReceiptPrinterModule
