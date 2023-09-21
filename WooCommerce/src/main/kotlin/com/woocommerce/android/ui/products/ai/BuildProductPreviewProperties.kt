package com.woocommerce.android.ui.products.ai

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ai.ProductPreviewSubViewModel.ProductPropertyCard
import javax.inject.Inject

class BuildProductPreviewProperties @Inject constructor() {
    operator fun invoke(product: Product): List<List<ProductPropertyCard>> = buildList {
        TODO()
    }
}
