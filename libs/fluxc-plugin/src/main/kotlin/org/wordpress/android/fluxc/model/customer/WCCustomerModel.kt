package org.wordpress.android.fluxc.model.customer

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

/**
 * Single Woo customer - see https://woocommerce.github.io/woocommerce-rest-api-docs/#customer-properties
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCCustomerModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var avatarUrl: String = ""
    @Column var dateCreated: String = ""
    @Column var dateCreatedGmt: String = ""
    @Column var dateModified: String = ""
    @Column var dateModifiedGmt: String = ""
    @Column var email: String = ""
    @Column var firstName: String = ""
    @Column var remoteCustomerId: Long = 0L
    @Column var isPayingCustomer: Boolean = false
        @JvmName("setIsPayingCustomer")
        set
    @Column var lastName: String = ""
    @Column var role: String = ""
    @Column var username: String = ""

    @Column var localSiteId = 0

    @Column var billingAddress1: String = ""
    @Column var billingAddress2: String = ""
    @Column var billingCity: String = ""
    @Column var billingCompany: String = ""
    @Column var billingCountry: String = ""
    @Column var billingEmail: String = ""
    @Column var billingFirstName: String = ""
    @Column var billingLastName: String = ""
    @Column var billingPhone: String = ""
    @Column var billingPostcode: String = ""
    @Column var billingState: String = ""

    @Column var shippingAddress1: String = ""
    @Column var shippingAddress2: String = ""
    @Column var shippingCity: String = ""
    @Column var shippingCompany: String = ""
    @Column var shippingCountry: String = ""
    @Column var shippingFirstName: String = ""
    @Column var shippingLastName: String = ""
    @Column var shippingPostcode: String = ""
    @Column var shippingState: String = ""
    var analyticsCustomerId: Long? = null

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    override fun toString(): String {
        return "WCCustomerModel(" +
                "id=$id, " +
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
                "localSiteId=$localSiteId, " +
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
