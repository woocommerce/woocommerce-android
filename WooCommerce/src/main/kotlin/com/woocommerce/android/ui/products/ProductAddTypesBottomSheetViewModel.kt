package com.woocommerce.android.ui.products

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.SavedStateWithArgs

@OpenClassOnDebug
class ProductAddTypesBottomSheetViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ProductDetailTypesBottomSheetViewModel(savedState, dispatchers) {
    override val productListBuilder: ProductTypeBottomSheetBuilder by lazy {
        ProductAddTypeBottomSheetBuilder()
    }

    override fun onProductTypeSelected(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        triggerEvent(Exit)
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductAddTypesBottomSheetViewModel>
}
