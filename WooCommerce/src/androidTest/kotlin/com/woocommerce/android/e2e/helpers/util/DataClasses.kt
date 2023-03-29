package com.woocommerce.android.e2e.helpers.util

// While the test data for reviews is obtained from
// "mappings/jetpack-blogs/wc/reviews/products_reviews_all.json",
// that file does not contain human-readable product descriptions,
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

val productTypesMap = mapOf(
    "simple" to "Physical product"
)

val orderStatusMap = mapOf(
    "pending" to "Pending payment",
    "processing" to "Processing"
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
    val id: Int = -1,
    val name: String,
    val stockStatusRaw: String,
    val priceDiscountedRaw: String,
    val priceRegularRaw: String = "",
    val typeRaw: String = "",
    val rating: Int = -1,
    val reviewsCount: Int = -1,
    val sku: String = "",
    val variations: String = ""
) {
    val stockStatus = productStatusesMap[stockStatusRaw]
    val price = getPriceDescription()
    val type = productTypesMap[typeRaw]

    private fun getPriceDescription(): String {
        var price = "Regular price: \$$priceRegularRaw.00"

        // Every product has a sale price and a regular price in JSON.
        // If there is no sale, these prices will be the same,
        // and the app will show a regular price only.
        // If sale takes place, these two prices will be different,
        // and there will be an additional sale price shown in the app.
        if (priceDiscountedRaw != priceRegularRaw) {
            price += "\nSale price: \$$priceDiscountedRaw.00"
        }

        return price
    }
}

data class OrderData(
    val customer: String,
    val customerNoteRaw: String,
    val feeRaw: String,
    val id: Int,
    val productName: String,
    val shippingRaw: String,
    val statusRaw: String,
    val totalRaw: String
) {
    // there is a white space in the text value returned by the element, adjusting here to match that
    val customerName = "$customer "
    val customerNote = "\"$customerNoteRaw\""
    val feeAmount = "\$$feeRaw"
    val shippingAmount = "\$$shippingRaw"
    val status = orderStatusMap[statusRaw]
    val total = "\$$totalRaw"
}
