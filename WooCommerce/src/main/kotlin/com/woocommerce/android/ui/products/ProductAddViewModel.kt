package com.woocommerce.android.ui.products

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import org.greenrobot.eventbus.EventBus

@OpenClassOnDebug
class ProductAddViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    init {
        start()
    }

    private fun start() {
        EventBus.getDefault().register(this)
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductAddViewModel>
}
