@file:OptIn(ExperimentalCoroutinesApi::class)

package com.woocommerce.android.ui.payments.hub.depositsummary

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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

    @Test
    fun `given repository returns error, when viewmodel init, then error state emitted and tracked`() = testBlocking {
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

        verify(trackerWrapper).track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_ERROR,
            errorContext = "PaymentsHubDepositSummaryViewModel",
            errorType = error.error.type.name,
            errorDescription = error.error.message
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
        assertThat((values[0] as PaymentsHubDepositSummaryState.Success).fromCache).isTrue()
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
        assertThat((values[0] as PaymentsHubDepositSummaryState.Success).fromCache).isFalse()
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
    fun `when learn more clicked, then openBrowserEvents emitted with correct url and tracked`() = testBlocking {
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

        verify(trackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_LEARN_MORE_CLICKED)

        job.cancel()
    }

    @Test
    fun `when currency selected, then selected currency event tracked`() = testBlocking {
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

        // WHEN
        (values[0] as PaymentsHubDepositSummaryState.Success).onCurrencySelected("USD")

        // THEN
        verify(trackerWrapper).track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_CURRENCY_SELECTED,
            properties = mapOf(
                "currency" to "usd"
            )
        )
    }

    @Test
    fun `when learn more clicked 3 times, then openBrowserEvents emitted only once`() =
        testBlocking {
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
            (values[0] as PaymentsHubDepositSummaryState.Success).onLearnMoreClicked()
            (values[0] as PaymentsHubDepositSummaryState.Success).onLearnMoreClicked()

            // THEN
            assertThat(emittedValues).hasSize(1)
            assertThat(emittedValues.last()).isEqualTo(
                "https://woocommerce.com/document/woopayments/deposits/deposit-schedule/"
            )

            job.cancel()
        }

    @Test
    fun `when expanded, then event tracked`() = testBlocking {
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

        // WHEN
        (values[0] as PaymentsHubDepositSummaryState.Success).onExpandCollapseClicked(true)

        // THEN
        verify(trackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_EXPANDED)
    }

    @Test
    fun `when collapsed, then event is not tracked`() = testBlocking {
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

        // WHEN
        (values[0] as PaymentsHubDepositSummaryState.Success).onExpandCollapseClicked(false)

        // THEN
        verify(trackerWrapper, never()).track(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_EXPANDED)
    }

    @Test
    fun `given success state with 2 currencies, when onSummaryDepositShown, then event tracked with 2 currencies`() = testBlocking {
        // GIVEN
        val overview: WooPaymentsDepositsOverview = mock()
        val mappedOverview: PaymentsHubDepositSummaryState.Overview = mock() {
            on { infoPerCurrency }.thenReturn(
                mapOf(
                    "USD" to mock(),
                    "EUR" to mock(),
                )
            )
        }
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
        advanceUntilIdle()

        // WHEN
        viewModel.onSummaryDepositShown()

        // THEN
        verify(trackerWrapper).track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_SHOWN,
            properties = mapOf(
                "number_of_currencies" to "2",
            )
        )
    }

    @Test
    fun `given success state with 0 currencies, when onSummaryDepositShown, then event tracked with 0 currencies`() = testBlocking {
        // GIVEN
        val overview: WooPaymentsDepositsOverview = mock()
        val mappedOverview: PaymentsHubDepositSummaryState.Overview = mock() {
            on { infoPerCurrency }.thenReturn(
                mapOf()
            )
        }
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
        advanceUntilIdle()

        // WHEN
        viewModel.onSummaryDepositShown()

        // THEN
        verify(trackerWrapper).track(
            AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_SHOWN,
            properties = mapOf(
                "number_of_currencies" to "0",
            )
        )
    }

    @Test
    fun `given error state, when onSummaryDepositShown, then event is not tracked`() = testBlocking {
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
        val viewModel = initViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSummaryDepositShown()

        // THEN
        verify(trackerWrapper, never()).track(
            eq(AnalyticsEvent.PAYMENTS_HUB_DEPOSIT_SUMMARY_SHOWN),
            any()
        )
    }

    private fun initViewModel() = PaymentsHubDepositSummaryViewModel(
        savedState = mock(),
        repository = repository,
        mapper = mapper,
        trackerWrapper = trackerWrapper,
    )
}
