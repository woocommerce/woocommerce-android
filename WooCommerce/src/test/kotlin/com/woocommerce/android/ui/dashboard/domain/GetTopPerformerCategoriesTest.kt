package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformerCategories.TopPerformerCategory
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformerCategories.TopPerformerCategoryResult
import com.woocommerce.android.util.ResultWithOutdatedFlag
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.TopPerformerCategoryEntity
import java.util.Calendar
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
class GetTopPerformerCategoriesTest : BaseUnitTest() {
    private val statsRepository: StatsRepository = mock()
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore = mock()

    private val sut = GetTopPerformerCategories(
        statsRepository,
        coroutinesTestRule.testDispatchers,
        analyticsUpdateDataStore
    )

    @Test
    fun `given fetch succeeds without cached data, when get top categories, then return loading then success`() =
        testBlocking {
            givenGetCategoriesResult(emptyList())
            givenFetchTopCategoriesResult(Result.success(Unit))
            givenShouldUpdateAnalyticsReturns(true)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()

            assertThat(results).contains(TopPerformerCategoryResult.Loading)
            assertThat(results).contains(
                TopPerformerCategoryResult.Success(
                    ResultWithOutdatedFlag(emptyList(), false)
                )
            )
        }

    @Test
    fun `given fetch succeeds with cached data, when get top categories, then return outdated then fresh data`() =
        testBlocking {
            givenGetCategoriesResult(EXPECTED_TOP_CATEGORIES_ENTITY_LIST)
            givenFetchTopCategoriesResult(Result.success(Unit))
            givenShouldUpdateAnalyticsReturns(true)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()

            assertThat(results).contains(
                TopPerformerCategoryResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_CATEGORY_LIST, true)
                )
            )
            assertThat(results).contains(
                TopPerformerCategoryResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_CATEGORY_LIST, false)
                )
            )
        }

    @Test
    fun `given fetch fails with cached data, when get top categories, then return outdated data then error`() =
        testBlocking {
            val error = Exception("Network error")
            givenGetCategoriesResult(EXPECTED_TOP_CATEGORIES_ENTITY_LIST)
            givenFetchTopCategoriesResult(Result.failure(error))
            givenShouldUpdateAnalyticsReturns(true)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()

            assertThat(results).contains(
                TopPerformerCategoryResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_CATEGORY_LIST, true)
                )
            )
            assertThat(results).contains(
                TopPerformerCategoryResult.Error(error)
            )
        }

    @Test
    fun `given cached data is up-to-date, when get top categories, then return cached data without fetching`() =
        testBlocking {
            givenGetCategoriesResult(EXPECTED_TOP_CATEGORIES_ENTITY_LIST)
            givenShouldUpdateAnalyticsReturns(false)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()

            assertThat(results.size).isEqualTo(1)
            assertThat(results).contains(
                TopPerformerCategoryResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_CATEGORY_LIST, false)
                )
            )
            verify(statsRepository, never()).fetchTopPerformerCategories(
                anyBoolean(),
                any(),
                anyInt(),
            )
        }

    @Test
    fun `given no cached data and no refresh needed, when get top categories, then return empty success`() =
        testBlocking {
            givenGetCategoriesResult(emptyList())
            givenShouldUpdateAnalyticsReturns(false)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()

            assertThat(results.size).isEqualTo(1)
            assertThat(results).contains(
                TopPerformerCategoryResult.Success(
                    ResultWithOutdatedFlag(emptyList(), false)
                )
            )
        }

    @Test
    fun `given multiple categories, when get top categories, then results are sorted by quantity desc then total desc`() =
        testBlocking {
            val unsortedEntities = listOf(
                createCategoryEntity(categoryId = 1, name = "Low", quantity = 2, total = 5.0),
                createCategoryEntity(categoryId = 2, name = "High", quantity = 10, total = 100.0),
                createCategoryEntity(categoryId = 3, name = "Mid", quantity = 5, total = 50.0),
                createCategoryEntity(categoryId = 4, name = "MidSameQty", quantity = 5, total = 80.0),
            )
            givenGetCategoriesResult(unsortedEntities)
            givenShouldUpdateAnalyticsReturns(false)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()

            val successResult = results.first() as TopPerformerCategoryResult.Success
            val categories = successResult.topCategories.value
            assertThat(categories.map { it.name })
                .containsExactly("High", "MidSameQty", "Mid", "Low")
        }

    private suspend fun givenGetCategoriesResult(result: List<TopPerformerCategoryEntity>) {
        whenever(statsRepository.getTopPerformerCategories(any(), any())).thenReturn(result)
    }

    private suspend fun givenFetchTopCategoriesResult(result: Result<Unit>) {
        whenever(
            statsRepository.fetchTopPerformerCategories(
                anyBoolean(),
                any(),
                anyInt(),
            )
        ).thenReturn(result)
    }

    private fun givenShouldUpdateAnalyticsReturns(shouldUpdateAnalytics: Boolean) {
        whenever(
            analyticsUpdateDataStore.shouldUpdateAnalytics(
                rangeSelection = any(),
                analyticData = any(),
                maxOutdatedTime = any()
            )
        ).thenReturn(flowOf(shouldUpdateAnalytics))
    }

    private fun createCategoryEntity(
        categoryId: Long = 1,
        name: String = "Category",
        quantity: Int = 4,
        total: Double = 10.50
    ) = TopPerformerCategoryEntity(
        localSiteId = LocalId(1234),
        datePeriod = "2021-01-01-2021-01-02",
        categoryId = RemoteId(categoryId),
        name = name,
        quantity = quantity,
        currency = "USD",
        total = total,
        millisSinceLastUpdated = 0
    )

    private companion object {
        val EXPECTED_TOP_CATEGORIES_ENTITY_LIST = listOf(
            TopPerformerCategoryEntity(
                localSiteId = LocalId(1234),
                datePeriod = "2021-01-01-2021-01-02",
                categoryId = RemoteId(42),
                name = "Clothing",
                quantity = 10,
                currency = "USD",
                total = 250.00,
                millisSinceLastUpdated = 0
            )
        )
        val EXPECTED_TOP_CATEGORY_LIST = listOf(
            TopPerformerCategory(
                categoryId = 42,
                name = "Clothing",
                quantity = 10,
                currency = "USD",
                total = 250.00
            )
        )
        val ANY_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
        val ANY_STATS_RANGE_SELECTION = StatsTimeRangeSelection.build(
            selectionType = ANY_SELECTION_TYPE,
            referenceDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
    }
}
