package com.woocommerce.android.ui.products

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
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
        saveUserSelection(productTypeUiItem.type)
        triggerEvent(ExitWithResult(productTypeUiItem = productTypeUiItem))
    }

    private fun saveUserSelection(type: ProductType) = AppPrefs.setSelectedProductType(type)

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductAddTypesBottomSheetViewModel>
}
