package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiers
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiersRequirement
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiersRequirement.NOT_REQUIRED
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.EuProductIdentifiersRequirement.REQUIRED
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class CheckEuProductIdentifiersRequirement @Inject constructor(
    private val repository: WooShippingLabelRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(orderId: Long, originId: String): CheckResult {
        val site = selectedSite.get()
        val response = repository.fetchCustomsRequirements(site, orderId, originId)
        val euProductIdsRequired = response.model?.euProductIdsRequired
        if (euProductIdsRequired == null) {
            WooLog.e(WooLog.T.SHIPPING_LABELS, "Could not load customs requirements: ${response.error?.message}")
        }

        return if (euProductIdsRequired == true) {
            CheckResult(REQUIRED, repository.fetchEuProductIdentifiers(site, orderId).model.orEmpty())
        } else {
            CheckResult(NOT_REQUIRED, emptyMap())
        }
    }

    data class CheckResult(
        val requirement: EuProductIdentifiersRequirement,
        val productIdentifiers: Map<Long, EuProductIdentifiers>
    )
}
