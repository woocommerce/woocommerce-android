package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialog.Companion.CUSTOM_AMOUNT
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.TaxStatus
import javax.inject.Inject

class MapFeeLineToCustomAmountUiModel @Inject constructor() {

    operator fun invoke(feeLine: Order.FeeLine): CustomAmountUIModel {
        return CustomAmountUIModel(
            id = feeLine.id,
            amount = feeLine.total,
            name = feeLine.name ?: CUSTOM_AMOUNT,
            taxStatus = when (feeLine.taxStatus) {
                Order.FeeLine.FeeLineTaxStatus.TAXABLE -> TaxStatus(isTaxable = true)
                Order.FeeLine.FeeLineTaxStatus.NONE -> TaxStatus(isTaxable = false)
                Order.FeeLine.FeeLineTaxStatus.UNKNOWN -> TaxStatus(isTaxable = false)
            }
        )
    }
}
