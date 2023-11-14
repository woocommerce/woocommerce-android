@file:OptIn(ExperimentalCoroutinesApi::class)

package com.woocommerce.android.ui.payments.hub.depositsummary

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

@ExperimentalCoroutinesApi
class PaymentsHubDepositSummaryViewModelTest : BaseUnitTest() {
    private val repository: PaymentsHubDepositSummaryRepository = mock()
    private val mapper: PaymentsHubDepositSummaryStateMapper = mock()
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()
    private val isFeatureEnabled: IsFeatureEnabled = mock {
        on { invoke() }.thenReturn(true)
    }

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
        val error = values[0] as PaymentsHubDepositSummaryState.Error
        assertThat(error.error.message).isEqualTo("message")
        assertThat(error.error.type).isEqualTo(WooErrorType.API_ERROR)
        assertThat(error.error.original).isEqualTo(BaseRequest.GenericErrorType.NETWORK_ERROR)
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

            val error = values[0] as PaymentsHubDepositSummaryState.Error
            assertThat(error.error.message).isEqualTo("Invalid data")
            assertThat(error.error.type).isEqualTo(WooErrorType.API_ERROR)
            assertThat(error.error.original).isEqualTo(BaseRequest.GenericErrorType.UNKNOWN)
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
            val error = values[0] as PaymentsHubDepositSummaryState.Error
            assertThat(error.error.message).isEqualTo("Invalid data")
            assertThat(error.error.type).isEqualTo(WooErrorType.API_ERROR)
            assertThat(error.error.original).isEqualTo(BaseRequest.GenericErrorType.UNKNOWN)
        }

    @Test
    fun `given feature flag off, when viewmodel init, then error is returned and not interactions with repository`() =
        testBlocking {
            // GIVEN
            whenever(isFeatureEnabled()).thenReturn(false)

            // WHEN
            val viewModel = initViewModel()
            advanceUntilIdle()

            // THEN
            val values = viewModel.viewState.captureValues()
            val error = values[0] as PaymentsHubDepositSummaryState.Error
            assertThat(error.error.message).isEqualTo("Invalid data")
            assertThat(error.error.type).isEqualTo(WooErrorType.API_ERROR)
            assertThat(error.error.original).isEqualTo(BaseRequest.GenericErrorType.UNKNOWN)
            verifyNoInteractions(repository)
        }

    @Test
    fun `when learn more clicked, then openBrowserEvents emitted with correct url`() = testBlocking {
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
        val viewModel = initViewModel()
        val values = viewModel.viewState.captureValues()
        val emittedValues = mutableListOf<String>()
        val job = launch {
            viewModel.openBrowserEvents.collect {
                emittedValues.add(it)
            }
        }

        // WHEN
        (values[0] as PaymentsHubDepositSummaryState.Success).onLearnMoreClicked()

        // THEN
        assertThat(emittedValues).hasSize(1)
        assertThat(emittedValues.last()).isEqualTo(
            "https://woocommerce.com/document/woopayments/deposits/deposit-schedule/"
        )

        job.cancel()
    }

    private fun initViewModel() = PaymentsHubDepositSummaryViewModel(
        savedState = mock(),
        repository = repository,
        mapper = mapper,
        trackerWrapper = trackerWrapper,
        isFeatureEnabled
    )
}
