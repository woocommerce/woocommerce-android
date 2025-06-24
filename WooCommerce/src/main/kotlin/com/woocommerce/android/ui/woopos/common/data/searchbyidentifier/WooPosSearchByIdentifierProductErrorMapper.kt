package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.DUPLICATE_SKU
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_IMAGE_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_MIN_MAX_QUANTITY
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_PARAM
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_PRODUCT_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_REVIEW_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.INVALID_VARIATION_IMAGE_ID
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.TERM_EXISTS
import javax.inject.Inject

class WooPosSearchByIdentifierProductErrorMapper @Inject constructor() {
    operator fun invoke(error: ProductError): WooPosSearchByIdentifierResult.Error =
        when (error.type) {
            INVALID_PRODUCT_ID -> WooPosSearchByIdentifierResult.Error.NotFound

            INVALID_PARAM -> WooPosSearchByIdentifierResult.Error.ServerError(
                error.message.ifEmpty { "Invalid parameter" }
            )

            INVALID_REVIEW_ID -> WooPosSearchByIdentifierResult.Error.NotFound

            INVALID_IMAGE_ID -> WooPosSearchByIdentifierResult.Error.NotFound

            DUPLICATE_SKU -> WooPosSearchByIdentifierResult.Error.ServerError(
                error.message.ifEmpty { "Duplicate SKU" }
            )

            TERM_EXISTS -> WooPosSearchByIdentifierResult.Error.ServerError(
                error.message.ifEmpty { "Term already exists" }
            )

            INVALID_VARIATION_IMAGE_ID -> WooPosSearchByIdentifierResult.Error.ServerError(
                error.message.ifEmpty { "Invalid variation image ID" }
            )

            INVALID_MIN_MAX_QUANTITY -> WooPosSearchByIdentifierResult.Error.ServerError(
                error.message.ifEmpty { "Invalid min/max quantity" }
            )

            PARSE_ERROR -> WooPosSearchByIdentifierResult.Error.ServerError(
                error.message.ifEmpty { "Parse error occurred" }
            )

            GENERIC_ERROR -> WooPosSearchByIdentifierResult.Error.UnknownError(
                error.message.ifEmpty { "Generic error occurred" }
            )
        }
}
