package com.woocommerce.android.ui.orders.wooshippinglabels.fedex

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRestClient
import javax.inject.Inject

class AcceptFedExTerms @Inject constructor(
    private val selectedSite: SelectedSite,
    private val restClient: WooShippingLabelRestClient
) {
    suspend operator fun invoke(): Result<Unit> {
        val response = restClient.updateFedExAgreement(
            site = selectedSite.get(),
            agreementAccepted = true
        )

        return if (response.isError) {
            Result.failure(WooException(response.error))
        } else {
            Result.success(Unit)
        }
    }
}
