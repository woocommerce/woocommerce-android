package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.WooException
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCStatsStore

@ExperimentalCoroutinesApi
class GetTopPerformersTest : BaseUnitTest() {
    private val statsRepository: StatsRepository = mock()
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore = mock()

    private val sut = GetTopPerformers(
        statsRepository,
        coroutinesTestRule.testDispatchers,
        analyticsUpdateDataStore,
        mock(),
        mock()
    )

    @Test
    fun `Given fetch top performers success, when get top performers, then returns successful result`() =
        testBlocking {
            givenFetchTopPerformersResult(Result.success(Unit))
            givenShouldUpdateAnalyticsReturns(true)

            val result = sut.fetchTopPerformers(
                granularity = WCStatsStore.StatsGranularity.DAYS,
                refresh = false,
                topPerformersCount = ANY_TOP_PERFORMERS_NUMBER
            )

            assertThat(result).isEqualTo(Result.success(Unit))
        }

    @Test
    fun `Given fetch top performers error, when get top performers, then returns error`() =
        testBlocking {
            val wooException = WooException(WOO_GENERIC_ERROR)
            givenShouldUpdateAnalyticsReturns(true)
            givenFetchTopPerformersResult(Result.failure(wooException))

            val result = sut.fetchTopPerformers(
                granularity = WCStatsStore.StatsGranularity.DAYS,
                refresh = false,
                topPerformersCount = ANY_TOP_PERFORMERS_NUMBER
            )

            assertThat(result.exceptionOrNull()).isEqualTo(wooException)
        }

    @Test
    fun `observing top performer updates should return the right data`() {
        testBlocking {
            val emittedEntity = EXPECTED_TOP_PERFORMERS_ENTITY_LIST
            givenTopPerformerEntityIsEmitted(emittedEntity)

            val observedDataModel = sut
                .observeTopPerformers(WCStatsStore.StatsGranularity.DAYS)
                .first()

            assertThat(observedDataModel).isEqualTo(EXPECTED_TOP_PERFORMER_PRODUCT_LIST)
        }
    }

    private fun givenTopPerformerEntityIsEmitted(emittedEntity: List<TopPerformerProductEntity>) {
        whenever(statsRepository.observeTopPerformers(any())).thenReturn(
            flowOf(emittedEntity)
        )
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
                maxOutdatedTime = any(),
                analyticData = eq(AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS)
            )
        )
            .thenReturn(flowOf(shouldUpdateAnalytics))
    }

    private companion object {
        const val ANY_TOP_PERFORMERS_NUMBER = 3
        val WOO_GENERIC_ERROR = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
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
    }
}
