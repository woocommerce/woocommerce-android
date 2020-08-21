package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import androidx.annotation.DrawableRes
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
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

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

    fun onProductTypeSelected(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        triggerEvent(ShowDiscardDialog(
            titleId = R.string.product_type_confirm_dialog_title,
            messageId = R.string.product_type_confirm_dialog_message,
            positiveButtonId = R.string.product_type_confirm_button,
            negativeButtonId = R.string.cancel,
            positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                triggerEvent(ExitWithResult(productTypeUiItem))
            }
        ))
    }

    private fun buildProductTypeList(): List<ProductTypesBottomSheetUiItem> {
        return listOf(
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                titleResource = R.string.product_type_physical,
                descResource = R.string.product_type_physical_desc,
                iconResource = R.drawable.ic_gridicons_product
            ),
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                isVirtual = true,
                titleResource = R.string.product_type_virtual,
                descResource = R.string.product_type_virtual_desc,
                iconResource = R.drawable.ic_gridicons_cloud_outline
            ),
            ProductTypesBottomSheetUiItem(
                type = GROUPED,
                titleResource = R.string.product_type_grouped,
                descResource = R.string.product_type_grouped_desc,
                iconResource = R.drawable.ic_widgets
            ),
            ProductTypesBottomSheetUiItem(
                type = EXTERNAL,
                titleResource = R.string.product_type_external,
                descResource = R.string.product_type_external_desc,
                iconResource = R.drawable.ic_gridicons_up_right
            )
        )
    }

    @Parcelize
    data class ProductTypesBottomSheetUiItem(
        val type: ProductType,
        val isVirtual: Boolean = false,
        @StringRes val titleResource: Int,
        @StringRes val descResource: Int,
        @DrawableRes val iconResource: Int
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductTypesBottomSheetViewModel>
}
