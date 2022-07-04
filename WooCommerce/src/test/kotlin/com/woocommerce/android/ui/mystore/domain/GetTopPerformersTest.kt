package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersError
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers.TopPerformersResult.TopPerformersSuccess
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore

@ExperimentalCoroutinesApi
class GetTopPerformersTest : BaseUnitTest() {
    private val statsRepository: StatsRepository = mock()

    private val getTopPerformers = GetTopPerformers(
        statsRepository,
        coroutinesTestRule.testDispatchers
    )

    @Test
    fun `Given fetch product leader boards success, when get top performers, then emits sorted top performers`() =
        testBlocking {
            givenFetchProductLeaderboardsResult(Result.success(UNORDERED_PERFORMERS_PRODUCT_LIST))

            val result = getTopPerformers(
                false, WCStatsStore.StatsGranularity.DAYS, ANY_TOP_PERFORMERS_NUMBER
            ).first()

            assertThat(result).isEqualTo(
                TopPerformersSuccess(SORTED_PERFORMERS_PRODUCT_LIST)
            )
        }

    @Test
    fun `Given fetch product leader boards error, when get top performers, then emits top performers error`() =
        testBlocking {
            givenFetchProductLeaderboardsResult(REPOSITORY_ERROR_RESULT)

            val result = getTopPerformers(
                false, WCStatsStore.StatsGranularity.DAYS, ANY_TOP_PERFORMERS_NUMBER
            ).first()

            assertThat(result).isEqualTo(TopPerformersError)
        }

    private suspend fun givenFetchProductLeaderboardsResult(result: Result<List<WCTopPerformerProductModel>>) {
        whenever(
            statsRepository.fetchProductLeaderboards(
                anyBoolean(),
                any(),
                anyInt()
            )
        ).thenReturn(flow { emit(result) })
    }

    private companion object {
        const val ANY_TOP_PERFORMERS_NUMBER = 3
        const val ANY_ERROR_MESSAGE = "Error message"
        val REPOSITORY_ERROR_RESULT: Result<List<WCTopPerformerProductModel>> =
            Result.failure(Exception(ANY_ERROR_MESSAGE))
        val UNORDERED_PERFORMERS_PRODUCT_LIST = listOf(
            WCTopPerformerProductModel(quantity = 3, total = 3 * 10.toDouble()),
            WCTopPerformerProductModel(quantity = 1, total = 15.toDouble()),
            WCTopPerformerProductModel(quantity = 5, total = 5 * 5.toDouble())
        )
        val SORTED_PERFORMERS_PRODUCT_LIST = listOf(
            WCTopPerformerProductModel(quantity = 5, total = 5 * 5.toDouble()),
            WCTopPerformerProductModel(quantity = 3, total = 3 * 10.toDouble()),
            WCTopPerformerProductModel(quantity = 1, total = 15.toDouble())
        )
    }
}
