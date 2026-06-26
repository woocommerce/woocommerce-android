package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.models.WooPosWCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.EMPTY_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_COUPON
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_ID
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_PARAM
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_VARIATION_ID
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.RESOURCE_ALREADY_EXISTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.REST_INVALID_SIGNATURE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.TIMEOUT
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierGlobalUniqueSearch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val wooPosLogWrapper: WooPosLogWrapper,
    private val posProductMapper: WooPosWCProductToWooPosProductModelMapper,
) {
    suspend operator fun invoke(globalUniqueId: String): WooPosSearchByIdentifierResult {
        val result = productStore.searchProducts(
            site = selectedSite.get(),
            searchString = null,
            globalUniqueIdSearchQuery = globalUniqueId,
            offset = 0,
            pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
            filterOptions = emptyMap(),
            posProductsOnly = true,
        )

        return when {
            result.isError -> WooPosSearchByIdentifierResult.Failure(result.error.mapError())

            result.model != null -> {
                val productSearchResult = result.model!!
                val products = productSearchResult.products.map { posProductMapper.map(it) }
                if (products.isEmpty()) {
                    WooPosSearchByIdentifierResult.Failure(
                        WooPosSearchByIdentifierResult.Error.NotFound
                    )
                } else {
                    WooPosSearchByIdentifierResult.Success(products.first())
                }
            }

            else -> {
                wooPosLogWrapper.e("Result.isError == false but the model is missing.")
                WooPosSearchByIdentifierResult.Failure(
                    WooPosSearchByIdentifierResult.Error.ServerError(
                        "Empty response from server for global unique ID: $globalUniqueId"
                    )
                )
            }
        }
    }

    private fun WooError.mapError(): WooPosSearchByIdentifierResult.Error =
        when (type) {
            TIMEOUT, NO_CONNECTION -> WooPosSearchByIdentifierResult.Error.NetworkError
            API_ERROR,
            REST_INVALID_SIGNATURE -> WooPosSearchByIdentifierResult.Error.ServerError(
                message?.ifEmpty { null } ?: "API error occurred"
            )

            INVALID_ID,
            API_NOT_FOUND,
            EMPTY_RESPONSE,
            INVALID_VARIATION_ID -> WooPosSearchByIdentifierResult.Error.NotFound

            GENERIC_ERROR -> WooPosSearchByIdentifierResult.Error.UnknownError(
                message?.ifEmpty { null } ?: "Generic error occurred"
            )

            INVALID_RESPONSE -> WooPosSearchByIdentifierResult.Error.ServerError(
                message ?: "Invalid response from server"
            )

            AUTHORIZATION_REQUIRED -> WooPosSearchByIdentifierResult.Error.ServerError("Authorization required")
            INVALID_PARAM -> WooPosSearchByIdentifierResult.Error.ServerError(
                message?.ifEmpty { null } ?: "Invalid parameter"
            )

            INVALID_COUPON -> WooPosSearchByIdentifierResult.Error.ServerError("Invalid coupon")
            RESOURCE_ALREADY_EXISTS -> WooPosSearchByIdentifierResult.Error.ServerError("Resource already exists")
        }
}
