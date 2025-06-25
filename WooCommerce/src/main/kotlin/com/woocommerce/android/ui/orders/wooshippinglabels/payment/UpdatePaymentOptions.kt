package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import jakarta.inject.Inject

class UpdatePaymentOptions @Inject constructor(
    private val selectedSite: SelectedSite,
    private val repository: WooShippingLabelRepository
) {
    suspend operator fun invoke(
        selectedPaymentMethodId: Long?,
        emailReceipts: Boolean,
    ): Result<Unit> {
        val result = repository.updateAccountSettings(
            site = selectedSite.get(),
            selectedPaymentMethodId = selectedPaymentMethodId,
            emailReceipts = emailReceipts
        )

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }
}
