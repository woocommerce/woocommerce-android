package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.tools.ResolveSiteBySiteId
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFiltersBySiteId @Inject constructor(
    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters,
    private val resolveSiteBySiteId: ResolveSiteBySiteId
) {
    operator fun invoke(siteId: Long): WCOrderListDescriptor? =
        resolveSiteBySiteId(siteId)?.let { site -> getWCOrderListDescriptorWithFilters(site) }
}
