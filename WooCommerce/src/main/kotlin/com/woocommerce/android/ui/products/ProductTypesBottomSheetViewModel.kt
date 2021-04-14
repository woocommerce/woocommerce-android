package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductAdd
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize
import java.util.Locale.ROOT

@OpenClassOnDebug
class ProductTypesBottomSheetViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val prefs: AppPrefs
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: ProductTypesBottomSheetFragmentArgs by savedState.navArgs()

    private val _productTypesBottomSheetList = MutableLiveData<List<ProductTypesBottomSheetUiItem>>()
    val productTypesBottomSheetList: LiveData<List<ProductTypesBottomSheetUiItem>> = _productTypesBottomSheetList

    fun loadProductTypes(builder: ProductTypeBottomSheetBuilder) {
        _productTypesBottomSheetList.value = builder.buildBottomSheetList().filter { it.isEnabled }
    }

    fun onProductTypeSelected(productTypeUiItem: ProductTypesBottomSheetUiItem) {
        if (navArgs.isAddProduct) {
            val properties = mapOf("product_type" to productTypeUiItem.type.value.toLowerCase(ROOT))
            AnalyticsTracker.track(Stat.ADD_PRODUCT_PRODUCT_TYPE_SELECTED, properties)

            saveUserSelection(productTypeUiItem.type)
            triggerEvent(ViewProductAdd)
            triggerEvent(ExitWithResult(productTypeUiItem.type))
        } else {
            triggerEvent(ShowDialog(
                titleId = R.string.product_type_confirm_dialog_title,
                messageId = R.string.product_type_confirm_dialog_message,
                positiveButtonId = R.string.product_type_confirm_button,
                negativeButtonId = R.string.cancel,
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(ExitWithResult(productTypeUiItem.type))
                }
            ))
        }
    }

    private fun saveUserSelection(type: ProductType) = prefs.setSelectedProductType(type)

    @Parcelize
    data class ProductTypesBottomSheetUiItem(
        val type: ProductType,
        @StringRes val titleResource: Int,
        @StringRes val descResource: Int,
        @DrawableRes val iconResource: Int,
        val isEnabled: Boolean
    ) : Parcelable

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<ProductTypesBottomSheetViewModel>
}
