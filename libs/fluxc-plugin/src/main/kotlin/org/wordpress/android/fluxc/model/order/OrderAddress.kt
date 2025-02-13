package org.wordpress.android.fluxc.model.order

import org.wordpress.android.fluxc.model.OrderEntity

sealed class OrderAddress {
    abstract val firstName: String
    abstract val lastName: String
    abstract val company: String
    abstract val address1: String
    abstract val address2: String
    abstract val city: String
    abstract val state: String
    abstract val postcode: String
    abstract val country: String
    abstract val phone: String

    data class Shipping(
        override val firstName: String,
        override val lastName: String,
        override val company: String,
        override val address1: String,
        override val address2: String,
        override val city: String,
        override val state: String,
        override val postcode: String,
        override val country: String,
        override val phone: String
    ) : OrderAddress() {
        constructor(orderEntity: OrderEntity) : this(
                firstName = orderEntity.shippingFirstName,
                lastName = orderEntity.shippingLastName,
                company = orderEntity.shippingCompany,
                address1 = orderEntity.shippingAddress1,
                address2 = orderEntity.shippingAddress2,
                city = orderEntity.shippingCity,
                state = orderEntity.shippingState,
                postcode = orderEntity.shippingPostcode,
                country = orderEntity.shippingCountry,
                phone = orderEntity.shippingPhone
        )
    }

    data class Billing(
        val email: String,
        override val firstName: String,
        override val lastName: String,
        override val company: String,
        override val address1: String,
        override val address2: String,
        override val city: String,
        override val state: String,
        override val postcode: String,
        override val country: String,
        override val phone: String
    ) : OrderAddress() {
        constructor(orderEntity: OrderEntity) : this(
                email = orderEntity.billingEmail,
                firstName = orderEntity.billingFirstName,
                lastName = orderEntity.billingLastName,
                company = orderEntity.billingCompany,
                address1 = orderEntity.billingAddress1,
                address2 = orderEntity.billingAddress2,
                city = orderEntity.billingCity,
                state = orderEntity.billingState,
                postcode = orderEntity.billingPostcode,
                country = orderEntity.billingCountry,
                phone = orderEntity.billingPhone
        )
    }
}
