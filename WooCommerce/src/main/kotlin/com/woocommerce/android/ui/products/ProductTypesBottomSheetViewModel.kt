package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAdd
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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

    private val arguments: ProductTypesBottomSheetFragmentArgs by savedState.navArgs()

    fun loadProductTypes(builder: ProductTypeBottomSheetBuilder) {
        _productTypesBottomSheetList.value = builder.buildBottomSheetList()
    }

    fun onProductTypeSelected(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        triggerSelectionHandler(productTypeUiItem)
    }

    private fun triggerSelectionHandler(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        when (arguments.isAddProduct) {
            true -> {
                saveUserSelection(productTypeUiItem.type)
                triggerEvent(ViewProductAdd)
                triggerEvent(ExitWithResult(productTypeUiItem = productTypeUiItem))
            }
            else -> {
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
        }
    }

    private fun saveUserSelection(type: ProductType) = AppPrefs.setSelectedProductType(type)

    data class ExitWithResult(val productTypeUiItem: ProductTypesBottomSheetUiItem) : Event()

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
