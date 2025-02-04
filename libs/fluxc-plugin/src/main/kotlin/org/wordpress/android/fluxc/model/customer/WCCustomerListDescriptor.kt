package org.wordpress.android.fluxc.model.customer

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListDescriptorUniqueIdentifier

open class WCCustomerListDescriptor(
    val site: SiteModel,
    val searchQuery: String? = null,
    val email: String? = null,
    val role: String? = null,
    val remoteCustomerIds: List<Long>? = null,
    val excludedCustomerIds: List<Long>? = null
) : ListDescriptor {
    override val config: ListConfig = ListConfig.default

    override val uniqueIdentifier: ListDescriptorUniqueIdentifier by lazy {
        ListDescriptorUniqueIdentifier(calculateListTypeHash(site.id))
    }

    override val typeIdentifier: ListDescriptorTypeIdentifier by lazy {
        calculateTypeIdentifier(site.id)
    }

    companion object {
        @JvmStatic
        fun calculateTypeIdentifier(localSiteId: Int) =
                ListDescriptorTypeIdentifier(calculateListTypeHash(localSiteId))

        private fun calculateListTypeHash(localSiteId: Int) = "woo-site-customer-list$localSiteId".hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as WCCustomerListDescriptor
        return uniqueIdentifier == that.uniqueIdentifier
    }

    override fun hashCode() = uniqueIdentifier.value
}
