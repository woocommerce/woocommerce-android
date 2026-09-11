package com.woocommerce.android.ui.products.filter

import androidx.annotation.StringRes
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.filters.FilterHistoryRepository
import com.woocommerce.android.ui.filters.FilterHistoryType
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Persists the given product filter selection to the filter history.
 *
 * Fire-and-forget on the application scope so it survives the filter screen being dismissed, letting
 * callers navigate away immediately without awaiting the DB write. Failures are logged rather than
 * propagated — persisting history is best-effort and must never crash the app.
 *
 * Owns the human-readable label resolution (slug → localized label) so the [ProductFilterHistoryMapper]
 * stays a pure payload codec, matching the order-list use case.
 */
class SaveProductFilterToHistory @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val filterHistoryRepository: FilterHistoryRepository,
    private val productFilterHistoryMapper: ProductFilterHistoryMapper,
    private val resourceProvider: ResourceProvider,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    operator fun invoke(filter: ProductFilterResult) {
        if (!featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)) return
        if (!filter.hasSelection()) return
        appCoroutineScope.launch {
            runCatching {
                filterHistoryRepository.save(
                    type = FilterHistoryType.PRODUCTS,
                    payload = productFilterHistoryMapper.toPayload(filter),
                    readableString = buildReadableString(filter)
                )
            }.onFailure { WooLog.e(WooLog.T.PRODUCTS, "Failed to save product filter to history", it) }
        }
    }

    private fun buildReadableString(filter: ProductFilterResult): String =
        listOfNotNull(
            filter.stockStatus?.let { label(ProductStockStatus.fromString(it).stringResource) },
            filter.productStatus?.let { status -> ProductStatus.fromString(status)?.let { label(it.stringResource) } },
            filter.productType?.let { label(ProductType.fromString(it).stringResource) },
            filter.productCategoryName?.takeIf { filter.productCategory != null }
        ).joinToString(separator = ", ")

    private fun label(@StringRes resId: Int): String? =
        resId.takeIf { it != 0 }?.let { resourceProvider.getString(it) }

    private fun ProductFilterResult.hasSelection(): Boolean =
        stockStatus != null || productStatus != null || productType != null || productCategory != null
}
