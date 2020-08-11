package com.woocommerce.android.ui.products

import android.content.DialogInterface
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
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

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductAddTypesBottomSheetViewModel>
}
