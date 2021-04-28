package com.woocommerce.android.ui.orders.cardreader.di

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentDialog
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class CardReaderPaymentModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: CardReaderPaymentDialog): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: CardReaderPaymentDialog): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(CardReaderPaymentViewModel::class)
    abstract fun bindFactory(factory: CardReaderPaymentViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
