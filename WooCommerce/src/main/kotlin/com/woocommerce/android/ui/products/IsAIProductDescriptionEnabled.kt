package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class IsAIProductDescriptionEnabled @Inject constructor(
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Boolean = selectedSite.getIfExists()?.isWpComStore ?: false
}
