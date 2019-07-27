package com.woocommerce.android.viewmodel

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory
@Inject constructor(
    private val mViewModelsMap: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(viewModelClass: Class<T>): T {
        var creator: Provider<out ViewModel>? = mViewModelsMap[viewModelClass]
        if (creator == null) {
            for ((key, value) in mViewModelsMap) {
                if (viewModelClass.isAssignableFrom(key)) {
                    creator = value
                    break
                }
            }
        }
        if (creator == null) {
            throw IllegalArgumentException(
                    "View model not found [" + viewModelClass
                            + "]. Have you added corresponding method into the ViewModelModule."
            )
        }
        return creator.get() as T
    }
}
