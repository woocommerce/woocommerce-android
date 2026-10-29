package com.woocommerce.android.ui.products.variations

import org.wordpress.android.fluxc.store.WCProductStore.VariationSorting

/**
 * Paging used by the variation lists. Pages are requested by ID because variations sharing the same
 * menu_order can repeat across offset pages when requested by menu order.
 */
object VariationListPaging {
    const val PAGE_SIZE = 100
    val SORTING = VariationSorting.ID_DESC
}
