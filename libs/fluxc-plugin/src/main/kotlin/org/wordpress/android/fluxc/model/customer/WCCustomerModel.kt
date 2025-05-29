package org.wordpress.android.fluxc.model.customer

import androidx.room.Entity
import androidx.room.Ignore
import org.wordpress.android.fluxc.model.LocalOrRemoteId

/**
 * Single Woo customer - see https://woocommerce.github.io/woocommerce-rest-api-docs/#customer-properties
 */
@Entity(
    tableName = "CustomerEntity",
    primaryKeys = ["localSiteId", "remoteCustomerId"]
)
data class WCCustomerModel(
    val localSiteId: LocalOrRemoteId.LocalId = LocalOrRemoteId.LocalId(0),
    val remoteCustomerId: Long = 0L,
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
    @Ignore
    val analyticsCustomerId: Long? = null
) {
    override fun toString(): String {
        return "WCCustomerModel(" +
                "localSiteId=${localSiteId.value}, " +
                "avatarUrl='$avatarUrl', " +
                "dateCreated='$dateCreated', " +
                "dateCreatedGmt='$dateCreatedGmt', " +
                "dateModified='$dateModified', " +
                "dateModifiedGmt='$dateModifiedGmt', " +
                "email='$email', " +
                "firstName='$firstName', " +
                "remoteCustomerId=$remoteCustomerId, " +
                "isPayingCustomer=$isPayingCustomer, " +
                "lastName='$lastName', " +
                "role='$role', " +
                "username='$username', " +
                "billingAddress1='$billingAddress1', " +
                "billingAddress2='$billingAddress2', " +
                "billingCity='$billingCity', " +
                "billingCompany='$billingCompany', " +
                "billingCountry='$billingCountry', " +
                "billingEmail='$billingEmail', " +
                "billingFirstName='$billingFirstName', " +
                "billingLastName='$billingLastName', " +
                "billingPhone='$billingPhone', " +
                "billingPostcode='$billingPostcode', " +
                "billingState='$billingState', " +
                "shippingAddress1='$shippingAddress1', " +
                "shippingAddress2='$shippingAddress2', " +
                "shippingCity='$shippingCity', " +
                "shippingCompany='$shippingCompany', " +
                "shippingCountry='$shippingCountry', " +
                "shippingFirstName='$shippingFirstName', " +
                "shippingLastName='$shippingLastName', " +
                "shippingPostcode='$shippingPostcode', " +
                "shippingState='$shippingState'" +
                ")"
    }
}
