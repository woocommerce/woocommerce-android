package com.woocommerce.android.ui.products.typesbottomsheet

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.subscriptions.IsEligibleForSubscriptions
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProductTypesBottomSheetViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productTypeBottomSheetBuilder: ProductTypeBottomSheetBuilder,
    private val isEligibleForSubscriptions: IsEligibleForSubscriptions
) : ScopedViewModel(savedState) {
    private val navArgs: ProductTypesBottomSheetFragmentArgs by savedState.navArgs()

    private val _productTypesBottomSheetList = MutableLiveData<List<ProductTypesBottomSheetUiItem>>()
    val productTypesBottomSheetList: LiveData<List<ProductTypesBottomSheetUiItem>> = _productTypesBottomSheetList

    init {
        viewModelScope.launch {
            loadProductTypes()
        }
    }

    private suspend fun loadProductTypes() {
        val areSubscriptionsSupported = isEligibleForSubscriptions()
        _productTypesBottomSheetList.value = if (navArgs.isAddProduct) {
            productTypeBottomSheetBuilder.buildBottomSheetList(areSubscriptionsSupported)
                .filter { it.isVisible }
        } else {
            productTypeBottomSheetBuilder.buildBottomSheetList(areSubscriptionsSupported)
                .filter {
                    val currentProductType = navArgs.currentProductType
                        ?.let { nonNullProductType -> ProductType.fromString(nonNullProductType) }

                    return@filter it.isVisible &&
                        (it.type != currentProductType || it.isVirtual != navArgs.isCurrentProductVirtual)
                }
        }
    }

    fun onProductTypeSelected(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        if (navArgs.isAddProduct) {
            val properties = mapOf("product_type" to productTypeUiItem.type.value.lowercase(Locale.ROOT))
            AnalyticsTracker.track(AnalyticsEvent.ADD_PRODUCT_PRODUCT_TYPE_SELECTED, properties)

            saveUserSelection(productTypeUiItem)
            triggerEvent(ProductNavigationTarget.ViewProductAdd(navArgs.source))
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(productTypeUiItem))
        } else {
            triggerEvent(
                MultiLiveEvent.Event.ShowDialog(
                    titleId = R.string.product_type_confirm_dialog_title,
                    messageId = R.string.product_type_confirm_dialog_message,
                    positiveButtonId = R.string.product_type_confirm_button,
                    negativeButtonId = R.string.cancel,
                    positiveBtnAction = { _, _ ->
                        triggerEvent(MultiLiveEvent.Event.ExitWithResult(productTypeUiItem))
                    }
                )
            )
        }
    }

    private fun saveUserSelection(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        AppPrefs.setSelectedProductType(productTypeUiItem.type)
        AppPrefs.setSelectedProductIsVirtual(productTypeUiItem.isVirtual)
    }

    @Parcelize
    data class ProductTypesBottomSheetUiItem(
        val type: ProductType,
        @StringRes val titleResource: Int,
        @StringRes val descResource: Int,
        @DrawableRes val iconResource: Int,
        val isVirtual: Boolean = false,
        val isVisible: Boolean = true
    ) : Parcelable
}
