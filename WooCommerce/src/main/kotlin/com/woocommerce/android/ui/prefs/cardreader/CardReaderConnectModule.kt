package com.woocommerce.android.ui.prefs.cardreader

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class CardReaderConnectModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: CardReaderConnectFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: CardReaderConnectFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(CardReaderConnectViewModel::class)
    abstract fun bindFactory(factory: CardReaderConnectViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
