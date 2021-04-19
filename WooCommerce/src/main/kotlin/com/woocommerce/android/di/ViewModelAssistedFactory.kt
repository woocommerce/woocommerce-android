package com.woocommerce.android.di

import com.woocommerce.android.viewmodel.SavedStateWithArgs
import androidx.lifecycle.ViewModel

/**
 *  A factory interface that takes the [SavedStateHandle] argument that we want to assist with.
 *  Every ViewModel class must include a concrete interface inside it:
 *
 *  @AssistedFactory
 *  interface Factory : ViewModelAssistedFactory<SpecificViewModel>
 *
 *  Returns an instance of the ViewModel.
 */
interface ViewModelAssistedFactory<T : ViewModel> {
    fun create(savedState: SavedStateWithArgs): T
}
