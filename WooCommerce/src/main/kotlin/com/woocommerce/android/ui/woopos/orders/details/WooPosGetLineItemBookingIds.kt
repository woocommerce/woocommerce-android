package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosGetLineItemBookingIds @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
) {
    suspend operator fun invoke(orderId: Long): Map<Long, Long> {
        val entity = orderStore.getOrderByIdAndSite(orderId, selectedSite.get()) ?: return emptyMap()
        return entity.getLineItemList().mapNotNull { lineItem ->
            val bookingId = lineItem.metaData
                ?.firstOrNull { it.key == BOOKING_ID_META_KEY }
                ?.value?.stringValue?.toLongOrNull()
            if (bookingId != null && lineItem.id != null) lineItem.id!! to bookingId else null
        }.toMap()
    }

    companion object {
        private const val BOOKING_ID_META_KEY = "_booking_id"
    }
}
