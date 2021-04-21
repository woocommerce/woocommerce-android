package com.woocommerce.android.ui.prefs.cardreader

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentDialog
import com.woocommerce.android.ui.orders.cardreader.di.CardReaderPaymentModule
import com.woocommerce.android.ui.prefs.cardreader.CardReaderModule.CardReaderConnectFragmentModule
import com.woocommerce.android.ui.prefs.cardreader.CardReaderModule.CardReaderPaymentFragmentModule
import com.woocommerce.android.ui.prefs.cardreader.CardReaderModule.CardReaderScanFragmentModule
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectFragment
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectModule
import com.woocommerce.android.ui.prefs.cardreader.scan.CardReaderScanFragment
import com.woocommerce.android.ui.prefs.cardreader.scan.CardReaderScanModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(
    includes = [
        CardReaderConnectFragmentModule::class,
        CardReaderScanFragmentModule::class,
        CardReaderPaymentFragmentModule::class
    ]
)
object CardReaderModule {
    @Module
    abstract class CardReaderConnectFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [CardReaderConnectModule::class])
        abstract fun cardReaderConnectFragment(): CardReaderConnectFragment
    }

    @Module
    abstract class CardReaderScanFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [CardReaderScanModule::class])
        abstract fun cardReaderScanFragment(): CardReaderScanFragment
    }

    @Module
    abstract class CardReaderPaymentFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [CardReaderPaymentModule::class])
        abstract fun cardReaderPaymentFragment(): CardReaderPaymentDialog
    }
}
