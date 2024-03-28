package com.woocommerce.android.ui.blaze.creation.payment

import com.woocommerce.android.model.CreditCardType
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BlazeCampaignPaymentMethodsListViewModelTests : BaseUnitTest() {

    private val accountRepository: AccountRepository = mock()
    private val blazeRepository: BlazeRepository = mock()
    private lateinit var viewModel: BlazeCampaignPaymentMethodsListViewModel

    suspend fun setup(
        paymentMethodsData: BlazeRepository.PaymentMethodsData = BlazePaymentSampleData.paymentMethodsData,
        selectedMethodId: String = BlazePaymentSampleData.userPaymentMethods.first().id,
        prepareMocks: suspend () -> Unit = {}
    ) {
        prepareMocks()

        viewModel = BlazeCampaignPaymentMethodsListViewModel(
            savedStateHandle = BlazeCampaignPaymentMethodsListFragmentArgs(
                paymentMethodsData = paymentMethodsData,
                selectedPaymentMethodId = selectedMethodId
            ).toSavedStateHandle(),
            accountRepository = accountRepository,
            userAgent = mock(),
            wpComWebViewAuthenticator = mock(),
            analyticsTrackerWrapper = mock(),
            blazeRepository = blazeRepository
        )
    }

    @Test
    fun `given payment methods not empty, when screen is opened, then show payment methods list`() = testBlocking {
        setup(
            paymentMethodsData = BlazePaymentSampleData.paymentMethodsData,
            selectedMethodId = BlazePaymentSampleData.userPaymentMethods.first().id
        )

        val viewState = viewModel.viewState.getOrAwaitValue()

        assertThat(viewState)
            .isInstanceOf(BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList::class.java)
        assertThat((viewState as BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList).paymentMethods)
            .isEqualTo(BlazePaymentSampleData.userPaymentMethods)
        assertThat(viewState.selectedPaymentMethod)
            .isEqualTo(BlazePaymentSampleData.userPaymentMethods.first())
    }

    @Test
    fun `given payment methods empty, when screen is opened, then show add payment method web view`() = testBlocking {
        setup(
            paymentMethodsData = BlazeRepository.PaymentMethodsData(
                savedPaymentMethods = emptyList(),
                addPaymentMethodUrls = BlazePaymentSampleData.paymentMethodsUrls
            )
        )

        val viewState = viewModel.viewState.getOrAwaitValue()

        assertThat(viewState)
            .isInstanceOf(BlazeCampaignPaymentMethodsListViewModel.ViewState.AddPaymentMethodWebView::class.java)
    }

    @Test
    fun `when payment method is selected, then exit with result`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.getOrAwaitValue()
            as BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList
        val event = viewModel.event.runAndCaptureValues {
            viewState.onPaymentMethodClicked(viewState.paymentMethods.last())
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.ExitWithResult(viewState.paymentMethods.last().id))
    }

    @Test
    fun `when add payment method is clicked, then show add payment method web view`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.getOrAwaitValue()
            as BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList
        val state = viewModel.viewState.runAndCaptureValues {
            viewState.onAddPaymentMethodClicked()
        }.last()

        assertThat(state)
            .isInstanceOf(BlazeCampaignPaymentMethodsListViewModel.ViewState.AddPaymentMethodWebView::class.java)
    }

    @Test
    fun `when payment success URL is detected, then select the added payment method`() = testBlocking {
        val newPayment = BlazeRepository.PaymentMethod(
            id = "3",
            name = "MasterCard 5678",
            info = BlazeRepository.PaymentMethod.PaymentMethodInfo.CreditCard(
                cardHolderName = "Jane Doe",
                creditCardType = CreditCardType.MASTERCARD
            )
        )
        setup {
            whenever(blazeRepository.fetchPaymentMethods()).thenReturn(
                Result.success(
                    BlazeRepository.PaymentMethodsData(
                        savedPaymentMethods = BlazePaymentSampleData.userPaymentMethods + newPayment,
                        addPaymentMethodUrls = BlazePaymentSampleData.paymentMethodsUrls
                    )
                )
            )
        }
        val urls = BlazePaymentSampleData.paymentMethodsUrls

        val viewState = viewModel.viewState.captureValues()
        val event = viewModel.event.runAndCaptureValues {
            (viewState.first() as BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList)
                .onAddPaymentMethodClicked()
            val webViewState = viewState.last()
                as BlazeCampaignPaymentMethodsListViewModel.ViewState.AddPaymentMethodWebView

            webViewState.onUrlLoaded(urls.successUrl)
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.ExitWithResult(newPayment.id))
    }
}
