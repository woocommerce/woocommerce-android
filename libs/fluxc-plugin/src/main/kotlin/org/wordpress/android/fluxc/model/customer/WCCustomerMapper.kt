package org.wordpress.android.fluxc.model.customer

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerFromAnalyticsDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerMapper @Inject constructor() {
    @Suppress("ComplexMethod")
    fun mapToModel(site: SiteModel, dto: CustomerDTO): WCCustomerModel {
        return WCCustomerModel().apply {
            localSiteId = site.id
            avatarUrl = dto.avatarUrl ?: ""
            dateCreated = dto.dateCreated ?: ""
            dateCreatedGmt = dto.dateCreatedGmt ?: ""
            dateModified = dto.dateModified ?: ""
            dateModifiedGmt = dto.dateModifiedGmt ?: ""
            email = dto.email ?: ""
            firstName = dto.firstName ?: ""
            remoteCustomerId = dto.id ?: 0
            isPayingCustomer = dto.isPayingCustomer
            lastName = dto.lastName ?: ""
            role = dto.role ?: ""
            username = dto.username ?: ""

            billingAddress1 = dto.billing?.address1 ?: ""
            billingAddress2 = dto.billing?.address2 ?: ""
            billingCompany = dto.billing?.company ?: ""
            billingCountry = dto.billing?.country ?: ""
            billingCity = dto.billing?.city ?: ""
            billingEmail = dto.billing?.email ?: ""
            billingFirstName = dto.billing?.firstName ?: ""
            billingLastName = dto.billing?.lastName ?: ""
            billingPhone = dto.billing?.phone ?: ""
            billingPostcode = dto.billing?.postcode ?: ""
            billingState = dto.billing?.state ?: ""

            shippingAddress1 = dto.billing?.address1 ?: ""
            shippingAddress2 = dto.billing?.address2 ?: ""
            shippingCity = dto.billing?.city ?: ""
            shippingCompany = dto.billing?.company ?: ""
            shippingCountry = dto.billing?.country ?: ""
            shippingFirstName = dto.billing?.firstName ?: ""
            shippingLastName = dto.billing?.lastName ?: ""
            shippingPostcode = dto.billing?.postcode ?: ""
            shippingState = dto.billing?.state ?: ""
        }
    }

    fun mapToDTO(model: WCCustomerModel): CustomerDTO {
        val billing = CustomerDTO.Billing(
            firstName = model.billingFirstName,
            lastName = model.billingLastName,
            company = model.billingCompany,
            address1 = model.billingAddress1,
            address2 = model.billingAddress2,
            city = model.billingCity,
            state = model.billingState,
            postcode = model.billingPostcode,
            country = model.billingCountry,
            email = model.billingEmail,
            phone = model.billingPhone
        )
        val shipping = CustomerDTO.Shipping(
            firstName = model.billingFirstName,
            lastName = model.billingLastName,
            company = model.billingCompany,
            address1 = model.billingAddress1,
            address2 = model.billingAddress2,
            city = model.billingCity,
            state = model.billingState,
            postcode = model.billingPostcode,
            country = model.billingCountry
        )
        return CustomerDTO(
            email = model.email,
            firstName = model.firstName,
            lastName = model.lastName,
            username = model.username,
            billing = billing,
            shipping = shipping
        )
    }

    @Suppress("ComplexMethod")
    fun mapToModel(site: SiteModel, dto: CustomerFromAnalyticsDTO): WCCustomerModel {
        return WCCustomerModel().apply {
            localSiteId = site.id
            remoteCustomerId = dto.userId ?: 0
            email = dto.email ?: ""
            firstName = dto.name.firstNameFromName()
            lastName = dto.name.lastNameFromName()
            username = dto.username ?: ""
            dateCreated = dto.dateRegistered ?: ""
            dateCreatedGmt = dto.dateRegisteredGmt ?: ""
            dateModified = dto.dateLastActive ?: ""
            dateModifiedGmt = dto.dateLastActiveGmt ?: ""
            isPayingCustomer = (dto.totalSpend ?: 0.0) > 0

            billingCountry = dto.country ?: ""
            billingCity = dto.city ?: ""
            billingEmail = dto.email ?: ""
            billingFirstName = dto.name?.firstNameFromName() ?: ""
            billingLastName = dto.name?.lastNameFromName() ?: ""
            billingPostcode = dto.postcode ?: ""
            billingState = dto.state ?: ""

            shippingCountry = dto.country ?: ""
            shippingCity = dto.city ?: ""
            shippingFirstName = dto.name?.firstNameFromName() ?: ""
            shippingLastName = dto.name?.lastNameFromName() ?: ""
            shippingPostcode = dto.postcode ?: ""
            shippingState = dto.state ?: ""
            analyticsCustomerId = dto.id
        }
    }

    // Please refer WCCustomerMapperTest file which serves as documentation of how this function behaves.
    private fun String?.firstNameFromName(): String =
        this?.trim()?.substringBefore(' ')?.trim().orEmpty()

    private fun String?.lastNameFromName(): String =
        this?.trim()?.substringAfter(' ', "")?.trim().orEmpty()
}
