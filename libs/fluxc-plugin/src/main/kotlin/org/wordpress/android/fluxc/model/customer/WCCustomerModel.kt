package org.wordpress.android.fluxc.model.customer

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

/**
 * Single Woo customer - see https://woocommerce.github.io/woocommerce-rest-api-docs/#customer-properties
 */
@Entity(tableName = "CustomerEntity",)
data class WCCustomerModel(
    /**
     * Synthetic primary key used to uniquely identify a customer.
     *
     * Some customers, such as guest customers from the Analytics API (represented by
     * [org.wordpress.android.fluxc.persistence.entity.CustomerFromAnalyticsEntity]), have `remoteCustomerId = 0`,
     * which means they cannot be uniquely identified by (remote id x local site id) pair.
     *
     * To ensure uniqueness across both registered and guest customers, this synthetic ID is used.
     */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
)
