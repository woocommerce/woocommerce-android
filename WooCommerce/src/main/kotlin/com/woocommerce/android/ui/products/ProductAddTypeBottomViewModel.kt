package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductTypesBottomSheetUiItem(
    val type: ProductType,
    val isVirtual: Boolean = false,
    @StringRes val titleResource: Int,
    @StringRes val descResource: Int,
    @DrawableRes val iconResource: Int
) : Parcelable

class ProductAddTypeBottomViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    private val addTypeBottomBuilder: ProductTypeBottomSheetBuilder by lazy {
        ProductAddTypeBottomSheetBuilder()
    }

    fun getProductTypesList(): List<ProductTypesBottomSheetUiItem> = addTypeBottomBuilder.buildBottomSheetList()

    fun onProductSelected(productType: ProductType) {
        triggerEvent(Exit)
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductAddTypeBottomViewModel>
}

