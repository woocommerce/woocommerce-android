package com.woocommerce.android.ui.products

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsAIProductDescriptionEnabled @Inject constructor() {
    operator fun invoke(): Boolean = FeatureFlag.PRODUCT_DESCRIPTION_AI_GENERATOR.isEnabled()
}
