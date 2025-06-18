package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import javax.inject.Inject

class WooPosSearchByIdentifierRemote @Inject constructor(
    private val globalUniqueIdSearch: WooPosSearchByIdentifierGlobalUniqueSearch,
    private val resultConverter: WooPosSearchByIdentifierResultConverter,
) {
    suspend operator fun invoke(identifier: String): WooPosSearchByIdentifierResult =
        resultConverter { globalUniqueIdSearch(identifier) }
}
