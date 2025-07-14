package com.woocommerce.android.ui.dashboard.topperformers

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers
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
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import java.util.Calendar
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
class GetTopPerformersTest : BaseUnitTest() {
    private val statsRepository: StatsRepository = mock()
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore = mock()

    private val sut = GetTopPerformers(
        statsRepository,
        coroutinesTestRule.testDispatchers,
        analyticsUpdateDataStore
    )

    @Test
    fun `Given fetch top performers success without cached data, when get top performers, then return successful result`() =
        testBlocking {
            givenGetPerformerResult(emptyList())
            givenFetchTopPerformersResult(Result.success(Unit))
            givenShouldUpdateAnalyticsReturns(true)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()
            // Returns loading because data could not be fetched from the DB
            assertThat(results).contains(GetTopPerformers.TopPerformerResult.Loading)
            // Then returns the empty list
            assertThat(results).contains(
                GetTopPerformers.TopPerformerResult.Success(
                    ResultWithOutdatedFlag(emptyList(), false)
                )
            )
        }

    @Test
    fun `Given fetch top performers success with cached data, when get top performers, then return successful result`() =
        testBlocking {
            givenGetPerformerResult(EXPECTED_TOP_PERFORMERS_ENTITY_LIST)
            givenFetchTopPerformersResult(Result.success(Unit))
            givenShouldUpdateAnalyticsReturns(true)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()
            // Returns outdated data
            assertThat(results).contains(
                GetTopPerformers.TopPerformerResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_PERFORMER_PRODUCT_LIST, true)
                )
            )
            // Then returns the up-to-date data
            assertThat(results).contains(
                GetTopPerformers.TopPerformerResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_PERFORMER_PRODUCT_LIST, false)
                )
            )
        }

    @Test
    fun `Given fetch top performers fails with cached data, when get top performers, then return error`() =
        testBlocking {
            val error = Exception("Something wrong")
            givenGetPerformerResult(EXPECTED_TOP_PERFORMERS_ENTITY_LIST)
            givenFetchTopPerformersResult(Result.failure(error))
            givenShouldUpdateAnalyticsReturns(true)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()
            // Returns outdated data
            assertThat(results).contains(
                GetTopPerformers.TopPerformerResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_PERFORMER_PRODUCT_LIST, true)
                )
            )
            // Then returns error
            assertThat(results).contains(
                GetTopPerformers.TopPerformerResult.Error(error)
            )
        }

    @Test
    fun `Given cached data is up-to-date, when get top performers, then return cached data`() =
        testBlocking {
            givenGetPerformerResult(EXPECTED_TOP_PERFORMERS_ENTITY_LIST)
            givenShouldUpdateAnalyticsReturns(false)

            val results = sut(selectedRange = ANY_STATS_RANGE_SELECTION, refresh = false).toList()
            // Returns up-to-date data
            assertThat(results.size).isEqualTo(1)
            assertThat(results).contains(
                GetTopPerformers.TopPerformerResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_PERFORMER_PRODUCT_LIST, false)
                )
            )
            // Then returns up-to-date data
            assertThat(results).contains(
                GetTopPerformers.TopPerformerResult.Success(
                    ResultWithOutdatedFlag(EXPECTED_TOP_PERFORMER_PRODUCT_LIST, false)
                )
            )
            verify(statsRepository, never()).fetchTopPerformerProducts(
                anyBoolean(),
                any(),
                anyInt(),
            )
        }

    private suspend fun givenGetPerformerResult(result: List<TopPerformerProductEntity>) {
        whenever(statsRepository.getTopPerformers(any(), any())).thenReturn(result)
    }

    private suspend fun givenFetchTopPerformersResult(result: Result<Unit>) {
        whenever(
            statsRepository.fetchTopPerformerProducts(
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
        )
            .thenReturn(flowOf(shouldUpdateAnalytics))
    }

    private companion object {
        val EXPECTED_TOP_PERFORMERS_ENTITY_LIST = listOf(
            TopPerformerProductEntity(
                localSiteId = LocalId(1234),
                datePeriod = "2021-01-01-2021-01-02",
                productId = RemoteId(134),
                name = "Shirt",
                imageUrl = "",
                quantity = 4,
                currency = "USD",
                total = 10.50,
                millisSinceLastUpdated = 0
            )
        )
        val EXPECTED_TOP_PERFORMER_PRODUCT_LIST = listOf(
            GetTopPerformers.TopPerformerProduct(
                productId = 134,
                name = "Shirt",
                imageUrl = "",
                quantity = 4,
                currency = "USD",
                total = 10.50,
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
