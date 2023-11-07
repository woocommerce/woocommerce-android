@file:OptIn(ExperimentalCoroutinesApi::class)

package com.woocommerce.android.ui.payments.hub.depositsummary

import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

@ExperimentalCoroutinesApi
class PaymentsHubDepositSummaryViewModelTest : BaseUnitTest() {
    private val repository: PaymentsHubDepositSummaryRepository = mock()
    private val mapper: PaymentsHubDepositSummaryStateMapper = mock()

    @Test
    fun `given repository returns error, when viewmodel init, then error state emitted`() = testBlocking {
        // GIVEN
        whenever(repository.retrieveDepositOverview()).thenAnswer {
            flow {
                emit(
                    RetrieveDepositOverviewResult.Error(
                        WooError(
                            type = WooErrorType.API_ERROR,
                            original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                            message = "message"
                        )
                    )
                )
            }
        }

        // WHEN
        val viewModel = initViewModel()
        advanceUntilIdle()

        // THEN
        val values = viewModel.viewState.captureValues()
        assertThat((values[0] as PaymentsHubDepositSummaryState.Error).errorMessage).isEqualTo(
            "message"
        )
    }

    @Test
    fun `given repository returns cache, when viewmodel init, then success state emitted`() = testBlocking {
        // GIVEN
        val overview: WooPaymentsDepositsOverview = mock()
        val mappedOverview: PaymentsHubDepositSummaryState.Overview = mock()
        whenever(mapper.mapDepositOverviewToViewModelOverviews(overview)).thenReturn(
            mappedOverview
        )
        whenever(repository.retrieveDepositOverview()).thenAnswer {
            flow {
                emit(
                    RetrieveDepositOverviewResult.Cache(
                        overview
                    )
                )
            }
        }

        // WHEN
        val viewModel = initViewModel()
        advanceUntilIdle()

        // THEN
        val values = viewModel.viewState.captureValues()
        assertThat((values[0] as PaymentsHubDepositSummaryState.Success).overview).isEqualTo(
            mappedOverview
        )
    }

    @Test
    fun `given repository returns remote, when viewmodel init, then success state emitted`() = testBlocking {
        // GIVEN
        val overview: WooPaymentsDepositsOverview = mock()
        val mappedOverview: PaymentsHubDepositSummaryState.Overview = mock()
        whenever(mapper.mapDepositOverviewToViewModelOverviews(overview)).thenReturn(
            mappedOverview
        )
        whenever(repository.retrieveDepositOverview()).thenAnswer {
            flow {
                emit(
                    RetrieveDepositOverviewResult.Remote(
                        overview
                    )
                )
            }
        }

        // WHEN
        val viewModel = initViewModel()
        advanceUntilIdle()

        // THEN
        val values = viewModel.viewState.captureValues()
        assertThat((values[0] as PaymentsHubDepositSummaryState.Success).overview).isEqualTo(
            mappedOverview
        )
    }

    @Test
    fun `given repository returns remote with data that maps to null, when viewmodel init, then error state emitted`() =
        testBlocking {
            // GIVEN
            val overview: WooPaymentsDepositsOverview = mock()
            whenever(mapper.mapDepositOverviewToViewModelOverviews(overview)).thenReturn(
                null
            )
            whenever(repository.retrieveDepositOverview()).thenAnswer {
                flow {
                    emit(
                        RetrieveDepositOverviewResult.Remote(
                            overview
                        )
                    )
                }
            }

            // WHEN
            val viewModel = initViewModel()
            advanceUntilIdle()

            // THEN
            val values = viewModel.viewState.captureValues()
            assertThat((values[0] as PaymentsHubDepositSummaryState.Error).errorMessage).isEqualTo(
                "Invalid data"
            )
        }

    @Test
    fun `given repository returns cache with data that maps to null, when viewmodel init, then error state emitted`() =
        testBlocking {
            // GIVEN
            val overview: WooPaymentsDepositsOverview = mock()
            whenever(mapper.mapDepositOverviewToViewModelOverviews(overview)).thenReturn(
                null
            )
            whenever(repository.retrieveDepositOverview()).thenAnswer {
                flow {
                    emit(
                        RetrieveDepositOverviewResult.Cache(
                            overview
                        )
                    )
                }
            }

            // WHEN
            val viewModel = initViewModel()
            advanceUntilIdle()

            // THEN
            val values = viewModel.viewState.captureValues()
            assertThat((values[0] as PaymentsHubDepositSummaryState.Error).errorMessage).isEqualTo(
                "Invalid data"
            )
        }

    private fun initViewModel() = PaymentsHubDepositSummaryViewModel(
        savedState = mock(),
        repository = repository,
        mapper = mapper
    )
}
