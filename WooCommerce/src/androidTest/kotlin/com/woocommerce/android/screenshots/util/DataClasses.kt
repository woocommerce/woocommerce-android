package com.woocommerce.android.screenshots.util

// While the test data for reviews is obtained from
// "mappings/jetpack-blogs/wc/reviews/products_reviews_all.json",
// this file does not contain human-readable product descriptions,
// but only products ID. The perfect solution would be to
// retrieve the human readable descriptions by product IDs from
// "mappings/jetpack-blogs/wc/products/products.json",
// I honestly had no time to finish this before project ETA.
// This is something TODO in case of project extension.
// Instead, I'm using this map.
val productsMap = mapOf(
    2132 to "Rose Gold shades",
    2131 to "Colorado shades",
    2130 to "Black Coral shades"
)

val productStatusesMap = mapOf(
    "instock" to "In stock",
    "onbackorder" to "On backorder",
    "outofstock" to "Out of stock"
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

data class ProductData(
    val id: Int,
    val name: String,
    val stockStatusRaw: String
) {
    val stockStatus = productStatusesMap[stockStatusRaw]
}
