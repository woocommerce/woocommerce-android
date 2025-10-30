package org.wordpress.android.fluxc.model.customer

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * Single Woo customer - see https://woocommerce.github.io/woocommerce-rest-api-docs/#customer-properties
 */
@ConsistentCopyVisibility
@Entity(tableName = "CustomerEntity")
data class WCCustomerModel internal constructor(
    /**
     * Deterministic primary key used to uniquely identify a customer across refreshes.
     *
     * Format examples:
     * - "site:<localSiteId>|wp:<remoteCustomerId>" for registered users (remote > 0)
     * - "site:<localSiteId>|analytics:<analyticsCustomerId>" for analytics guests
     */
    @PrimaryKey val stableId: String = "",
    val localSiteId: LocalId = LocalId(0),
    val remoteCustomerId: RemoteId = RemoteId(0),
    val avatarUrl: String = "",
    val dateCreated: String = "",
    val dateCreatedGmt: String = "",
    val dateModified: String = "",
    val dateModifiedGmt: String = "",
    val email: String = "",
    val firstName: String = "",
    val isPayingCustomer: Boolean = false,
    val lastName: String = "",
    val role: String = "",
    val username: String = "",
    val billingAddress1: String = "",
    val billingAddress2: String = "",
    val billingCity: String = "",
    val billingCompany: String = "",
    val billingCountry: String = "",
    val billingEmail: String = "",
    val billingFirstName: String = "",
    val billingLastName: String = "",
    val billingPhone: String = "",
    val billingPostcode: String = "",
    val billingState: String = "",
    val shippingAddress1: String = "",
    val shippingAddress2: String = "",
    val shippingCity: String = "",
    val shippingCompany: String = "",
    val shippingCountry: String = "",
    val shippingFirstName: String = "",
    val shippingLastName: String = "",
    val shippingPostcode: String = "",
    val shippingState: String = "",
    val analyticsCustomerId: Long? = 0
) {
    /**
     * A secondary constructor used to easily create a WCCustomerModel with stableId generated.
     */
    constructor(
        localSiteId: LocalId = LocalId(0),
        remoteCustomerId: RemoteId = RemoteId(0),
        avatarUrl: String = "",
        dateCreated: String = "",
        dateCreatedGmt: String = "",
        dateModified: String = "",
        dateModifiedGmt: String = "",
        email: String = "",
        firstName: String = "",
        isPayingCustomer: Boolean = false,
        lastName: String = "",
        role: String = "",
        username: String = "",
        billingAddress1: String = "",
        billingAddress2: String = "",
        billingCity: String = "",
        billingCompany: String = "",
        billingCountry: String = "",
        billingEmail: String = "",
        billingFirstName: String = "",
        billingLastName: String = "",
        billingPhone: String = "",
        billingPostcode: String = "",
        billingState: String = "",
        shippingAddress1: String = "",
        shippingAddress2: String = "",
        shippingCity: String = "",
        shippingCompany: String = "",
        shippingCountry: String = "",
        shippingFirstName: String = "",
        shippingLastName: String = "",
        shippingPostcode: String = "",
        shippingState: String = "",
        analyticsCustomerId: Long? = 0,
    ) : this(
        stableId = buildStableId(localSiteId.value, remoteCustomerId.value, analyticsCustomerId),
        localSiteId = localSiteId,
        remoteCustomerId = remoteCustomerId,
        avatarUrl = avatarUrl,
        dateCreated = dateCreated,
        dateCreatedGmt = dateCreatedGmt,
        dateModified = dateModified,
        dateModifiedGmt = dateModifiedGmt,
        email = email,
        firstName = firstName,
        isPayingCustomer = isPayingCustomer,
        lastName = lastName,
        role = role,
        username = username,
        billingAddress1 = billingAddress1,
        billingAddress2 = billingAddress2,
        billingCity = billingCity,
        billingCompany = billingCompany,
        billingCountry = billingCountry,
        billingEmail = billingEmail,
        billingFirstName = billingFirstName,
        billingLastName = billingLastName,
        billingPhone = billingPhone,
        billingPostcode = billingPostcode,
        billingState = billingState,
        shippingAddress1 = shippingAddress1,
        shippingAddress2 = shippingAddress2,
        shippingCity = shippingCity,
        shippingCompany = shippingCompany,
        shippingCountry = shippingCountry,
        shippingFirstName = shippingFirstName,
        shippingLastName = shippingLastName,
        shippingPostcode = shippingPostcode,
        shippingState = shippingState,
        analyticsCustomerId = analyticsCustomerId,
    )

    companion object {
        /**
         * Builds deterministic primary key used to uniquely identify a customer across refreshes.
         *
         * Format examples:
         * - "site:<localSiteId>|wp:<remoteCustomerId>" for registered users (remote > 0)
         * - "site:<localSiteId>|analytics:<analyticsCustomerId>" for analytics guests
         *
         * @param siteId local site id
         * @param remoteId remote customer id
         * @param analyticsId analytics customer id
         * @return stable id
         */
        private fun buildStableId(siteId: Int, remoteId: Long?, analyticsId: Long?): String {
            val rid = remoteId ?: 0L
            return if (rid > 0L) {
                "site:$siteId|wp:$rid"
            } else {
                val aid = analyticsId ?: 0L
                if (aid > 0L) "site:$siteId|analytics:$aid" else ""
            }
        }
    }
}
