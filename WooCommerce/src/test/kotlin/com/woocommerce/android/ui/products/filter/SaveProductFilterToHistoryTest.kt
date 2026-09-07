package com.woocommerce.android.ui.products.filter

import com.woocommerce.android.ui.filters.FilterHistoryRepository
import com.woocommerce.android.ui.filters.FilterHistoryType
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SaveProductFilterToHistoryTest : BaseUnitTest() {
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val filterHistoryRepository: FilterHistoryRepository = mock()
    private val productFilterHistoryMapper: ProductFilterHistoryMapper = mock {
        on { toPayload(any()) } doReturn PAYLOAD
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doReturn "Label"
    }

    private val sut = SaveProductFilterToHistory(
        featureFlagRepository = featureFlagRepository,
        filterHistoryRepository = filterHistoryRepository,
        productFilterHistoryMapper = productFilterHistoryMapper,
        resourceProvider = resourceProvider,
        appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher)
    )

    @Test
    fun `given feature flag disabled, when invoked, then nothing is saved`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(false)

        sut(ProductFilterResult("instock", null, null, null, null))

        verify(filterHistoryRepository, never()).save(any(), any(), any())
    }

    @Test
    fun `given no filter is selected, when invoked, then nothing is saved`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)

        sut(ProductFilterResult(null, null, null, null, null))

        verify(filterHistoryRepository, never()).save(any(), any(), any())
    }

    @Test
    fun `given only a category name is set, when invoked, then nothing is saved`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)

        sut(ProductFilterResult(null, null, null, null, "Shoes"))

        verify(filterHistoryRepository, never()).save(any(), any(), any())
    }

    @Test
    fun `given a filter is selected, when invoked, then it is saved with the resolved label`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)

        sut(ProductFilterResult("instock", null, null, null, null))

        verify(filterHistoryRepository).save(FilterHistoryType.PRODUCTS, PAYLOAD, "Label")
    }

    @Test
    fun `given an unresolvable slug, when building the label, then it is dropped from the readable string`() =
        testBlocking {
            whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)

            // "weirdstatus" maps to ProductStockStatus.Custom (stringResource == 0) → dropped;
            // only the valid product type resolves, so the readable string is a single "Label".
            sut(ProductFilterResult(stockStatus = "weirdstatus", productType = "simple", null, null, null))

            verify(filterHistoryRepository).save(eq(FilterHistoryType.PRODUCTS), eq(PAYLOAD), eq("Label"))
        }

    @Test
    fun `given a category name without a category id, when building the label, then the name is dropped`() =
        testBlocking {
            whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)

            sut(ProductFilterResult(stockStatus = "instock", null, null, null, productCategoryName = "Any"))

            // Readable is just the stock label, not "Label, Any".
            verify(filterHistoryRepository).save(eq(FilterHistoryType.PRODUCTS), eq(PAYLOAD), eq("Label"))
        }

    private companion object {
        const val PAYLOAD = "payload"
    }
}
