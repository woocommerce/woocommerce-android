package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListDescriptorUniqueIdentifier

data class WCOrderListDescriptor(
    val site: SiteModel,
    val statusFilter: String? = null,
    val searchQuery: String? = null,
    val excludeFutureOrders: Boolean = false,
    val beforeFilter: String? = null,
    val afterFilter: String? = null,
    val productId: Long? = null,
    val customerId: Long? = null,
    val excludedIds: List<Long>? = null
) : ListDescriptor {
    override val config: ListConfig = ListConfig.default

    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        ListDescriptorUniqueIdentifier(
            ("woo-site-order-list-${site.id}" +
                    "-sf${statusFilter.orEmpty()}" +
                    "-sq${searchQuery.orEmpty()}" +
                    "-bf${beforeFilter.orEmpty()}" +
                    "-af${afterFilter.orEmpty()}" +
                    "-p${productId ?: ""}" +
                    "-c${customerId ?: ""}" +
                    "-efo$excludeFutureOrders"
                    ).hashCode()
        )
    }

    override val typeIdentifier: ListDescriptorTypeIdentifier by lazy {
        calculateTypeIdentifier(site.id)
    }

    companion object {
        @JvmStatic
        fun calculateTypeIdentifier(localSiteId: Int) =
                ListDescriptorTypeIdentifier("woo-site-order-list$localSiteId".hashCode())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as WCOrderListDescriptor
        return uniqueIdentifier == that.uniqueIdentifier
    }

    override fun hashCode() = uniqueIdentifier.value
}
