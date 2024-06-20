package com.woocommerce.commons

data class WearOrder(
    val localSiteId: Int,
    val id: Long,
    val date: String,
    val number: String,
    val total: String,
    val status: String,
    val billingFirstName: String,
    val billingLastName: String,
    val address: WearOrderAddress,
    val lineItemsJson: String
)

data class WearOrderedProduct(
    val amount: String,
    val total: String,
    val name: String
)

data class WearOrderAddress(
    val email: String,
    val firstName: String,
    val lastName: String,
    val company: String,
    val address1: String,
    val address2: String,
    val city: String,
    val state: String,
    val postcode: String,
    val country: String,
    val phone: String
)
