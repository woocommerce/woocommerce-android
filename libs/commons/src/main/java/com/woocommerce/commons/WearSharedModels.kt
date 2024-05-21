package com.woocommerce.commons

data class WearOrder(
    val id: Long,
    val date: String,
    val number: String,
    val total: String,
    val status: String,
    val billingFirstName: String,
    val billingLastName: String
)

data class WearOrderedProduct(
    val amount: String,
    val total: String,
    val name: String
)
