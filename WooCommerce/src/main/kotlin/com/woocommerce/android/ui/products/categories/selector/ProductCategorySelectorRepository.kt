package com.woocommerce.android.ui.products.categories.selector

import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragment
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

/**
 * This class seems like a duplicate of [ProductCategoriesRepository].
 * The goal here is to make this as the main repository after adding having features parity between
 * [ProductCategorySelectorFragment] and [ProductCategoriesFragment], and then remove the old one that depends
 * on EventBus.
 */
class ProductCategorySelectorRepository @Inject constructor(private val store: WCProductStore) {
    
}
