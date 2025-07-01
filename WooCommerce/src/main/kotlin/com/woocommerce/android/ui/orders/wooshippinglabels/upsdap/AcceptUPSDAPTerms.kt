package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRestClient
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingNetworkingMapper
import javax.inject.Inject

class AcceptUPSDAPTerms @Inject constructor(
    private val selectedSite: SelectedSite,
    private val restClient: WooShippingLabelRestClient,
    private val mapper: WooShippingNetworkingMapper
) {
    suspend operator fun invoke(originAddressDTO: OriginShippingAddress): Result<Unit> {
        val response = restClient.updateUPSDAPAgreement(
            site = selectedSite.get(),
            originAddress = mapper.toOriginAddressPurchaseDTO(originAddressDTO),
            agreementAccepted = true
        )

        return if (response.isError) {
            Result.failure(WooException(response.error))
        } else {
            Result.success(Unit)
        }
    }
}
