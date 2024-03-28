package com.woocommerce.android.model

data class BundleStat(
    val bundlesSold: Int,
    val bundlesSoldDelta: DeltaPercentage,
    val bundles: List<BundleItem>
) {
    companion object {
        val EMPTY = BundleStat(
            bundlesSold = 0,
            bundlesSoldDelta = DeltaPercentage.NotExist,
            bundles = emptyList()
        )
    }
}

data class BundleItem(
    val name: String,
    val netSales: Double,
    val image: String?,
    val quantity: Int,
    val currencyCode: String?
)
