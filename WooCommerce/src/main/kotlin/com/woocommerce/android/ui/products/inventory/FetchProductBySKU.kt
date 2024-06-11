package com.woocommerce.android.ui.products.inventory

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.CheckDigitRemoverFactory
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.products.list.ProductListRepository
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class FetchProductBySKU @Inject constructor(
    private val productRepository: ProductListRepository,
    private val checkDigitRemoverFactory: CheckDigitRemoverFactory,
) {
    suspend operator fun invoke(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): Result<Product> {
        val product = productRepository.searchProductList(
            searchQuery = codeScannerResultCode,
            skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
        )?.firstOrNull()
            ?: if (codeScannerResultFormat.isEAN() || codeScannerResultFormat.isUPC()) {
                val sku = checkDigitRemoverFactory.getCheckDigitRemoverFor(codeScannerResultFormat)
                    .getSKUWithoutCheckDigit(codeScannerResultCode)
                productRepository.searchProductList(
                    searchQuery = sku,
                    skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch
                )?.firstOrNull()
            } else {
                null
            }
        return if (product != null) {
            Result.success(product)
        } else {
            Result.failure(Exception("Product not found"))
        }
    }

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isUPC() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE

    private fun GoogleBarcodeFormatMapper.BarcodeFormat.isEAN() =
        this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13 ||
            this == GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
}
