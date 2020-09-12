package com.woocommerce.android.ui.wpmediapicker

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerViewModel.Factory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class WPMediaPickerModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: WPMediaPickerFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(WPMediaPickerViewModel::class)
    abstract fun bindFactory(factory: Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: WPMediaPickerFragment): SavedStateRegistryOwner
}
