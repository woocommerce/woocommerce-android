package com.woocommerce.android.ui.prefs.cardreader

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.prefs.cardreader.CardReaderModule.CardReaderConnectFragmentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(
    includes = [
        CardReaderConnectFragmentModule::class
    ]
)
object CardReaderModule {
    @Module
    abstract class CardReaderConnectFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [CardReaderConnectModule::class])
        abstract fun cardReaderConnectFragment(): CardReaderConnectFragment
    }
}
