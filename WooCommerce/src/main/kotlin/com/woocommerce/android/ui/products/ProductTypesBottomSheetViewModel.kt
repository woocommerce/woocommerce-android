package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAdd
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.util.Locale.ROOT
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class ProductTypesBottomSheetViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val prefs: AppPrefs
) : ScopedViewModel(savedState) {
    private val navArgs: ProductTypesBottomSheetFragmentArgs by savedState.navArgs()

    private val _productTypesBottomSheetList = MutableLiveData<List<ProductTypesBottomSheetUiItem>>()
    val productTypesBottomSheetList: LiveData<List<ProductTypesBottomSheetUiItem>> = _productTypesBottomSheetList

    fun loadProductTypes() {
        _productTypesBottomSheetList.value = if (navArgs.isAddProduct) {
            ProductTypeBottomSheetBuilder().buildBottomSheetList()
                .filter { it.isEnabledForAddFlow }
        } else {
            ProductTypeBottomSheetBuilder().buildBottomSheetList()
        }
    }

    fun onProductTypeSelected(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        if (navArgs.isAddProduct) {
            val properties = mapOf("product_type" to productTypeUiItem.type.value.toLowerCase(ROOT))
            AnalyticsTracker.track(Stat.ADD_PRODUCT_PRODUCT_TYPE_SELECTED, properties)

            saveUserSelection(productTypeUiItem)
            triggerEvent(ViewProductAdd)
            triggerEvent(ExitWithResult(productTypeUiItem))
        } else {
            triggerEvent(ShowDialog(
                titleId = R.string.product_type_confirm_dialog_title,
                messageId = R.string.product_type_confirm_dialog_message,
                positiveButtonId = R.string.product_type_confirm_button,
                negativeButtonId = R.string.cancel,
                positiveBtnAction = { _, _ ->
                    triggerEvent(ExitWithResult(productTypeUiItem))
                }
            ))
        }
    }

    private fun saveUserSelection(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        prefs.setSelectedProductType(productTypeUiItem.type)
        prefs.setSelectedProductIsVirtual(productTypeUiItem.isVirtual)
    }

    @Parcelize
    data class ProductTypesBottomSheetUiItem(
        val type: ProductType,
        @StringRes val titleResource: Int,
        @StringRes val descResource: Int,
        @DrawableRes val iconResource: Int,
        val isEnabledForAddFlow: Boolean = true,
        val isVirtual: Boolean = false
    ) : Parcelable
}
