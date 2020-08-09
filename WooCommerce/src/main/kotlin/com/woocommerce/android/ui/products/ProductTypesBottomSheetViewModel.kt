package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel

@OpenClassOnDebug
class ProductTypesBottomSheetViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    private val _productTypesBottomSheetList = MutableLiveData<List<ProductTypesBottomSheetUiItem>>()
    val productTypesBottomSheetList: LiveData<List<ProductTypesBottomSheetUiItem>> = _productTypesBottomSheetList

    fun loadProductTypes() {
        _productTypesBottomSheetList.value = buildProductTypeList()
    }

    fun onProductTypeSelected(type: ProductType) {
        // TODO: display confirmation dialog
    }

    private fun buildProductTypeList(): List<ProductTypesBottomSheetUiItem> {
        return listOf(
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                titleResource = R.string.product_type_physical,
                descResource = R.string.product_type_physical_desc
            ),
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                isVirtual = true,
                titleResource = R.string.product_type_virtual,
                descResource = R.string.product_type_virtual_desc
            ),
            ProductTypesBottomSheetUiItem(
                type = GROUPED,
                titleResource = R.string.product_type_grouped,
                descResource = R.string.product_type_grouped_desc
            ),
            ProductTypesBottomSheetUiItem(
                type = EXTERNAL,
                titleResource = R.string.product_type_external,
                descResource = R.string.product_type_external_desc
            )
        )
    }

    data class ProductTypesBottomSheetUiItem(
        val type: ProductType,
        val isVirtual: Boolean = false,
        @StringRes val titleResource: Int,
        @StringRes val descResource: Int
    )

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductTypesBottomSheetViewModel>
}
