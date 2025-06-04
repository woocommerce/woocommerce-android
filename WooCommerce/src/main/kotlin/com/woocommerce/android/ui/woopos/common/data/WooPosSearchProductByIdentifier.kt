package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.CheckDigitRemoverFactory
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchProductByIdentifier @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val productsCache: WooPosProductsCache,
    private val checkDigitRemoverFactory: CheckDigitRemoverFactory
) {
    private var searchBySKUContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)
    private var searchByGlobalUniqueIdContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

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
        val cacheResult = searchInCache { product ->
            product.sku.equals(codeScannerResultCode, ignoreCase = true)
        }
        if (cacheResult != null) {
            return cacheResult
        }

        val product = searchProductsBySKURemotely(codeScannerResultCode)?.firstOrNull()
            ?: removeCheckDigitIfPossible(
                codeScannerResultCode = codeScannerResultCode,
                codeScannerResultFormat = codeScannerResultFormat
            )?.let { skuWithoutCheckDigit ->
                val cacheResultWithoutDigit = searchInCache { product ->
                    product.sku.equals(skuWithoutCheckDigit, ignoreCase = true)
                }
                if (cacheResultWithoutDigit != null) {
                    return cacheResultWithoutDigit
                }

                searchProductsBySKURemotely(skuWithoutCheckDigit)?.firstOrNull()
            }

        product?.let { foundProduct ->
            productsCache.addAll(listOf(foundProduct))
        }

        return product
    }

    private suspend fun searchProductByGlobalUniqueIdentifier(
        codeScannerResultCode: String,
        codeScannerResultFormat: GoogleBarcodeFormatMapper.BarcodeFormat
    ): Product? {
        val cacheResult = searchInCache { product ->
            product.globalUniqueId.equals(codeScannerResultCode, ignoreCase = true)
        }
        if (cacheResult != null) {
            return cacheResult
        }

        val product = searchProductsByGlobalUniqueIdRemotely(codeScannerResultCode)?.firstOrNull()
            ?: removeCheckDigitIfPossible(
                codeScannerResultCode = codeScannerResultCode,
                codeScannerResultFormat = codeScannerResultFormat
            )?.let { identifierWithoutCheckDigit ->
                val cacheResultWithoutDigit = searchInCache { product ->
                    product.globalUniqueId.equals(identifierWithoutCheckDigit, ignoreCase = true)
                }
                if (cacheResultWithoutDigit != null) {
                    return cacheResultWithoutDigit
                }

                searchProductsByGlobalUniqueIdRemotely(identifierWithoutCheckDigit)?.firstOrNull()
            }

        product?.let { foundProduct ->
            productsCache.addAll(listOf(foundProduct))
        }

        return product
    }

    private suspend fun searchInCache(predicate: (Product) -> Boolean): Product? {
        return productsCache.getAll().firstOrNull(predicate)
    }

    private suspend fun searchProductsBySKURemotely(searchQuery: String): List<Product>? {
        val result = searchBySKUContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.SearchProductsPayload(
                site = selectedSite.get(),
                searchQuery = searchQuery,
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch,
                pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                offset = 0,
                sorting = WCProductStore.ProductSorting.TITLE_ASC,
                excludedProductIds = null,
                filterOptions = emptyMap()
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation -> null
            is ContinuationWrapper.ContinuationResult.Success -> result.value
        }
    }

    private suspend fun searchProductsByGlobalUniqueIdRemotely(globalUniqueId: String): List<Product>? {
        val result = searchByGlobalUniqueIdContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.SearchProductsByGlobalUniqueIdPayload(
                site = selectedSite.get(),
                globalUniqueId = globalUniqueId,
                pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                offset = 0,
                sorting = WCProductStore.ProductSorting.TITLE_ASC,
                excludedProductIds = null,
                filterOptions = emptyMap()
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsByGlobalUniqueIdAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation -> null
            is ContinuationWrapper.ContinuationResult.Success -> result.value
        }
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: WCProductStore.OnProductsSearched) {
        val continuation = if (event.globalUniqueIdSearchQuery != null) {
            searchByGlobalUniqueIdContinuation
        } else {
            searchBySKUContinuation
        }

        if (event.isError) {
            continuation.continueWith(emptyList())
        } else {
            val products = event.searchResults.map { it.toAppModel() }
            continuation.continueWith(products)
        }
    }
}
