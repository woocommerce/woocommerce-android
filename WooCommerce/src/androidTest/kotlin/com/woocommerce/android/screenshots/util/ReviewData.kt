package com.woocommerce.android.screenshots.util

val productsMap = mapOf(
    2132 to "Rose Gold shades",
    2131 to "Colorado shades",
    2130 to "Black Coral shades"
)

data class ReviewData(
    val productID: Int,
    val status: String,
    val reviewer: String,
    val review: String,
    val rating: Int
) {
    val product = productsMap[productID]
    val title = "$reviewer left a review on $product"
    val content = if (status == "hold") "Pending Review • $review" else review
    val approveButtonTitle = if (status == "hold") "Approve" else "Approved"
}
