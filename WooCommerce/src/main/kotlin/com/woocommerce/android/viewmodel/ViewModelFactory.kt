package com.woocommerce.android.viewmodel

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory

/**
 * {@link Factory} implementation, which creates a {@link ViewModel}, which has an empty constructor. The factory instance
 * can be supplied to a {@link ViewModelProvider} to create a ViewModel instance scoped to a lifecycle of a fragment or
 * an activity.
 *
 * Ex: ViewModelProviders.of(view, factoryInstance).get(SampleViewModel::class.java)
 */
class ViewModelFactory
@Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards ViewModelAssistedFactory<out ViewModel>>,
    owner: SavedStateRegistryOwner,
    private val defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(key: String, viewModelClass: Class<T>, savedState: SavedStateHandle): T {
        return creators[viewModelClass]?.create(SavedStateWithArgs(savedState, defaultArgs)) as? T
                ?: throw IllegalArgumentException("[$viewModelClass] not found. Did you add it to a module?")
    }
}
