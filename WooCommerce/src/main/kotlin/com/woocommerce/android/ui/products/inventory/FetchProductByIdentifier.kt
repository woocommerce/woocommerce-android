package com.woocommerce.android.ui.products.inventory

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.CheckDigitRemoverFactory
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.products.list.ProductListRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class FetchProductByIdentifier @Inject constructor(
    private val productRepository: ProductListRepository,
    private val checkDigitRemoverFactory: CheckDigitRemoverFactory,
    private val tracker: AnalyticsTrackerWrapper
) {
    suspend operator fun invoke(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): Result<Product> = coroutineScope {
        val globalUniqueIdentifierSearch = async {
            searchProductByGlobalUniqueIdentifier(
                codeScannerResultCode,
                codeScannerResultFormat
            )
        }
        val skuSearch = async { searchProductBySku(codeScannerResultCode, codeScannerResultFormat) }

        val product = globalUniqueIdentifierSearch.await() ?: skuSearch.await()

        if (product != null) {
            Result.success(product)
        } else {
            Result.failure(Exception("Product not found"))
        }
    }

    private suspend fun searchProductBySku(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): Product? {
        val product = productRepository.searchProductList(
            searchQuery = codeScannerResultCode,
            skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
        )?.firstOrNull()
            ?: removeCheckDigitIfPossible(
                codeScannerResultCode = codeScannerResultCode,
                codeScannerResultFormat = codeScannerResultFormat
            )?.let {
                productRepository.searchProductList(
                    searchQuery = it,
                    skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
                )?.firstOrNull()
            }
        return product
    }

    private suspend fun searchProductByGlobalUniqueIdentifier(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): Product? {
        val product = productRepository.searchProductListByGlobalUniqueId(
            globalUniqueId = codeScannerResultCode
        )?.firstOrNull() ?: removeCheckDigitIfPossible(
            codeScannerResultCode = codeScannerResultCode,
            codeScannerResultFormat = codeScannerResultFormat
        )?.let {
            productRepository.searchProductListByGlobalUniqueId(
                globalUniqueId = codeScannerResultCode
            )?.firstOrNull()
        }

        if (product != null) {
            tracker.track(
                AnalyticsEvent.PRODUCT_SEARCH_VIA_GLOBAL_UNIQUE_IDENTIFIER_SUCCESS
            )
        }

        return product
    }

    private fun removeCheckDigitIfPossible(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): String? {
        if (codeScannerResultFormat.isEAN() || codeScannerResultFormat.isUPC()) {
            return checkDigitRemoverFactory.getCheckDigitRemoverFor(codeScannerResultFormat)
                .getSKUWithoutCheckDigit(codeScannerResultCode)
        }

        return null
    }

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isUPC() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isEAN() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13 ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
}
