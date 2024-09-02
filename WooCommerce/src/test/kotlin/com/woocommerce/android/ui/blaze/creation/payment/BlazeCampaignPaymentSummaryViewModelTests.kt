package com.woocommerce.android.ui.blaze.creation.payment

import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.Budget
import com.woocommerce.android.ui.blaze.BlazeRepository.TargetingParameters
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignPaymentSummaryViewModelTests : BaseUnitTest() {
    companion object {
        private val campaignDetails = BlazeRepository.CampaignDetails(
            productId = 0L,
            tagLine = "",
            description = "",
            campaignImage = BlazeRepository.BlazeCampaignImage.LocalImage("test"),
            budget = Budget(
                totalBudget = 10f,
                spentBudget = 0f,
                currencyCode = "$",
                durationInDays = 7,
                startDate = Date(),
                isEndlessCampaign = false
            ),
            targetingParameters = TargetingParameters(),
            destinationParameters = BlazeRepository.DestinationParameters(
                targetUrl = "https://test.com",
                parameters = emptyMap()
            )
        )
    }

    private val blazeRepository: BlazeRepository = mock {
        onBlocking { fetchPaymentMethods() } doReturn Result.success(BlazePaymentSampleData.paymentMethodsData)
    }
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(amount = any(), any(), any()) }.doAnswer { it.getArgument<BigDecimal>(0).toString() }
    }
    private lateinit var viewModel: BlazeCampaignPaymentSummaryViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = BlazeCampaignPaymentSummaryViewModel(
            savedStateHandle = BlazeCampaignPaymentSummaryFragmentArgs(
                campaignDetails = campaignDetails
            ).toSavedStateHandle(),
            blazeRepository = blazeRepository,
            abandonedCampaignReminder = mock(),
            currencyFormatter = currencyFormatter,
            analyticsTrackerWrapper = mock(),
            dashboardRepository = mock(),
            resourceProvider = mock(),
        )
    }

    @Test
    fun `when screen is opened, then fetch payment method`() = testBlocking {
        setup()

        verify(blazeRepository).fetchPaymentMethods()
    }

    @Test
    fun `when payment methods are fetched, auto-select the first one`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.selectedPaymentMethod).isEqualTo(BlazePaymentSampleData.userPaymentMethods.first())
    }

    @Test
    fun `when screen is created, then format budget correctly`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.displayBudget).isEqualTo(
            currencyFormatter.formatCurrency(
                amount = campaignDetails.budget.totalBudget.toBigDecimal(),
                currencyCode = campaignDetails.budget.currencyCode
            )
        )
    }

    @Test
    fun `when payment methods fetching fails, then show an error`() = testBlocking {
        setup {
            whenever(blazeRepository.fetchPaymentMethods()).thenReturn(Result.failure(Exception()))
        }

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.paymentMethodsState)
            .isInstanceOf(BlazeCampaignPaymentSummaryViewModel.PaymentMethodsState.Error::class.java)
    }

    @Test
    fun `when payment method is tapped, then open payment method selection screen`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        val event = viewModel.event.runAndCaptureValues {
            val paymentState = state.paymentMethodsState
                as BlazeCampaignPaymentSummaryViewModel.PaymentMethodsState.Success
            paymentState.onClick()
        }.last()

        assertThat(event).isEqualTo(
            BlazeCampaignPaymentSummaryViewModel.NavigateToPaymentsListScreen(
                BlazePaymentSampleData.paymentMethodsData,
                state.selectedPaymentMethod?.id
            )
        )
    }

    @Test
    fun `when payment method is selected, then update the selected payment method`() = testBlocking {
        setup()

        val paymentMethod = BlazePaymentSampleData.userPaymentMethods.last()
        viewModel.onPaymentMethodSelected(paymentMethod.id)

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.selectedPaymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `when back button is clicked, then exit the screen`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onBackClicked()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when help button is clicked, then navigate to help screen`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onHelpClicked()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.BLAZE_CAMPAIGN_CREATION))
    }
}
