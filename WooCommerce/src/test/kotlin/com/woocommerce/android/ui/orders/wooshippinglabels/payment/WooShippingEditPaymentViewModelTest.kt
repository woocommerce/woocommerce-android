package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STARTED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.orders.wooshippinglabels.FetchAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.ObserveAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.UserAgent
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingEditPaymentViewModelTest : BaseUnitTest() {
    private val defaultAccountSettings = AccountSettingsModel(
        storeOptions = StoreOptionsModel(
            weightUnit = "kg",
            currencySymbol = "$",
            dimensionUnit = "cm",
            originCountry = "US"
        ),
        paymentMethodOptions = PaymentMethodOptions(
            selectedPaymentId = 1,
            paymentMethods = listOf(
                PaymentMethodModel(
                    paymentMethodId = 1,
                    cardType = "visa",
                    cardDigits = "1234",
                    expiry = "12/25",
                    name = "Visa *1234"
                ),
                PaymentMethodModel(
                    paymentMethodId = 2,
                    cardType = "mastercard",
                    cardDigits = "5678",
                    expiry = "10/24",
                    name = "Mastercard *5678"
                )
            ),
            addPaymentMethodUrl = "https://example.com/add-payment-method",
            emailReceipts = true
        ),
        canManagePayments = true,
        canEditSettings = true,
        storeOwnerName = "John Doe",
        storeOwnerUsername = "johndoe",
        paperSize = WooShippingLabelPaperSize.LABEL,
        lastOrderCompleted = false
    )

    private val observeAccountSettings: ObserveAccountSettings = mock {
        on { invoke() } doReturn flowOf(defaultAccountSettings)
    }
    private val fetchAccountSettings: FetchAccountSettings = mock {
        on { invoke() } doReturn Result.success(defaultAccountSettings)
    }
    private val updatePaymentOptions: UpdatePaymentOptions = mock()
    private val webViewAuthenticator: WebViewAuthenticator = mock()
    private val userAgent: UserAgent = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val savedStateHandle = SavedStateHandle()

    private lateinit var viewModel: WooShippingEditPaymentViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = WooShippingEditPaymentViewModel(
            savedStateHandle = savedStateHandle,
            observeAccountSettings = observeAccountSettings,
            fetchAccountSettings = fetchAccountSettings,
            updatePaymentOptions = updatePaymentOptions,
            webViewAuthenticator = webViewAuthenticator,
            userAgent = userAgent,
            tracker = tracker
        )
    }

    @Test
    fun `when screen is opened, then show loading state`() = testBlocking {
        whenever(observeAccountSettings()).doReturn(flowOf(null))

        setup()

        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state).isInstanceOf(WooShippingEditPaymentViewModel.ViewState.Loading::class.java)
    }

    @Test
    fun `when screen is opened, then track started event`() = testBlocking {
        setup()

        verify(tracker).track(AnalyticsEvent.WCS_PAYMENT_STEP, mapOf(KEY_STATE to VALUE_STARTED))
    }

    @Test
    fun `when account settings are loaded, then show content state`() = testBlocking {
        setup()

        // Capture all states emitted by the viewState LiveData
        val states = viewModel.viewState.runAndCaptureValues {
            // Need to advance time to allow the flow transformation to complete
            advanceUntilIdle()
        }

        // The first state might be Loading, but the last state should be Content
        assertThat(states.last()).isInstanceOf(WooShippingEditPaymentViewModel.ViewState.Content::class.java)

        val contentState = states.last() as WooShippingEditPaymentViewModel.ViewState.Content
        assertThat(contentState.selectedPaymentMethodId)
            .isEqualTo(defaultAccountSettings.paymentMethodOptions.selectedPaymentId)
        assertThat(contentState.paymentMethods).isEqualTo(defaultAccountSettings.paymentMethodOptions.paymentMethods)
        assertThat(contentState.canManagePaymentMethods).isEqualTo(defaultAccountSettings.canManagePayments)
        assertThat(contentState.canEditSettings).isEqualTo(defaultAccountSettings.canEditSettings)
        assertThat(contentState.emailReceipts).isEqualTo(defaultAccountSettings.paymentMethodOptions.emailReceipts)
        assertThat(contentState.storeOwnerName).isEqualTo(defaultAccountSettings.storeOwnerName)
        assertThat(contentState.storeOwnerUsername).isEqualTo(defaultAccountSettings.storeOwnerUsername)
    }

    @Test
    fun `when add new payment method is clicked, then show add payment method web view`() = testBlocking {
        setup()

        val initialState = viewModel.viewState.captureValues().last()
            as WooShippingEditPaymentViewModel.ViewState.Content
        val states = viewModel.viewState.runAndCaptureValues {
            initialState.onAddNewPaymentMethod()
        }

        assertThat(states.last())
            .isInstanceOf(WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView::class.java)

        val webViewState = states.last() as WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView
        assertThat(webViewState.url).isEqualTo(defaultAccountSettings.paymentMethodOptions.addPaymentMethodUrl)
        assertThat(webViewState.userAgent).isEqualTo(userAgent)
        assertThat(webViewState.authenticator).isEqualTo(webViewAuthenticator)
    }

    @Test
    fun `when add new payment method is clicked, then track add_payment_method_button_tapped event`() = testBlocking {
        setup()

        val initialState = viewModel.viewState.captureValues().last()
            as WooShippingEditPaymentViewModel.ViewState.Content
        viewModel.viewState.runAndCaptureValues { initialState.onAddNewPaymentMethod() }

        verify(tracker).track(AnalyticsEvent.WCS_PAYMENT_STEP, mapOf(KEY_STATE to "add_payment_method_button_tapped"))
    }

    @Test
    fun `when payment method is selected, then update selected payment method`() = testBlocking {
        setup()

        val newPaymentMethodId = 2L
        val initialState = viewModel.viewState.captureValues().last()
            as WooShippingEditPaymentViewModel.ViewState.Content
        val states = viewModel.viewState.runAndCaptureValues {
            initialState.onPaymentMethodSelected(newPaymentMethodId)
        }

        val contentState = states.last() as WooShippingEditPaymentViewModel.ViewState.Content
        assertThat(contentState.selectedPaymentMethodId).isEqualTo(newPaymentMethodId)
    }

    @Test
    fun `when payment method is added successfully, then update payment methods and show snackbar`() = testBlocking {
        val initialAccountSettings = defaultAccountSettings.copy(
            paymentMethodOptions = defaultAccountSettings.paymentMethodOptions.copy(
                paymentMethods = listOf(defaultAccountSettings.paymentMethodOptions.paymentMethods[0])
            )
        )

        val updatedAccountSettings = defaultAccountSettings.copy(
            paymentMethodOptions = defaultAccountSettings.paymentMethodOptions.copy(
                paymentMethods = defaultAccountSettings.paymentMethodOptions.paymentMethods
            )
        )

        whenever(observeAccountSettings()).doReturn(flowOf(initialAccountSettings))
        whenever(fetchAccountSettings()).doReturn(Result.success(updatedAccountSettings))

        setup()

        // Simulate adding a payment method
        val initialState = viewModel.viewState.captureValues().last()
            as WooShippingEditPaymentViewModel.ViewState.Content
        val webViewState = viewModel.viewState.runAndCaptureValues {
            initialState.onAddNewPaymentMethod()
        }.last() as WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView

        // Simulate successful URL load with payment method success URL
        val events = viewModel.event.runAndCaptureValues {
            webViewState.onUrlLoaded("https://wordpress.com/me/payment-methods")
            advanceUntilIdle()
        }

        // Verify that the snackbar is shown
        assertThat(events).anyMatch { it is ShowSnackbar && it.message == R.string.woo_shipping_payment_method_added }

        // Verify that the new payment method is selected
        val contentState = viewModel.viewState.getOrAwaitValue() as WooShippingEditPaymentViewModel.ViewState.Content
        assertThat(contentState.selectedPaymentMethodId).isEqualTo(2)

        // Verify that payment_method_added event is tracked
        verify(tracker).track(AnalyticsEvent.WCS_PAYMENT_STEP, mapOf(KEY_STATE to "payment_method_added"))
    }

    @Test
    fun `when payment method fetch fails, then show error dialog`() = testBlocking {
        whenever(fetchAccountSettings())
            .doReturn(Result.failure(Exception("Error fetching payment methods")))

        setup()

        // Simulate adding a payment method
        val initialState = viewModel.viewState.captureValues().last()
            as WooShippingEditPaymentViewModel.ViewState.Content
        val webViewState = viewModel.viewState.runAndCaptureValues {
            initialState.onAddNewPaymentMethod()
        }.last() as WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView

        // Simulate successful URL load with payment method success URL
        webViewState.onUrlLoaded("https://wordpress.com/me/payment-methods")
        advanceUntilIdle()

        // Verify that the error dialog is shown
        val contentState = viewModel.viewState.getOrAwaitValue() as WooShippingEditPaymentViewModel.ViewState.Content
        assertThat(contentState.dialogState).isNotNull

        // Check that the message is a UiString.UiStringRes with the correct resource ID
        val message = contentState.dialogState!!.message
        assertThat(message).isInstanceOf(com.woocommerce.android.model.UiString.UiStringRes::class.java)
        val uiStringRes = message as com.woocommerce.android.model.UiString.UiStringRes
        assertThat(uiStringRes.stringRes).isEqualTo(R.string.woo_shipping_payment_fetching_cards_failed)
    }

    @Test
    fun `when web view is dismissed, then return to content view`() = testBlocking {
        setup()

        // Simulate adding a payment method
        val webViewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onAddNewPaymentMethod()
        }.last() as WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView

        // Simulate dismissing the web view
        val states = viewModel.viewState.runAndCaptureValues {
            webViewState.onDismiss()
        }

        assertThat(states.last()).isInstanceOf(WooShippingEditPaymentViewModel.ViewState.Content::class.java)
    }

    @Test
    fun `when email receipts is toggled, then update email receipts state`() = testBlocking {
        setup()

        val initialState = viewModel.viewState.captureValues().last()
            as WooShippingEditPaymentViewModel.ViewState.Content
        val newState = viewModel.viewState.runAndCaptureValues {
            initialState.onEmailReceiptsChanged(!defaultAccountSettings.paymentMethodOptions.emailReceipts)
        }.last() as WooShippingEditPaymentViewModel.ViewState.Content

        assertThat(newState.emailReceipts).isEqualTo(!defaultAccountSettings.paymentMethodOptions.emailReceipts)
    }

    @Test
    fun `when save is clicked with no changes, then exit without calling updatePaymentOptions`() = testBlocking {
        setup()

        // Get the content state and call onSaveClicked
        val contentState =
            viewModel.viewState.captureValues().last() as WooShippingEditPaymentViewModel.ViewState.Content

        // Capture events
        val events = viewModel.event.runAndCaptureValues {
            contentState.onSaveClicked()
            advanceUntilIdle()
        }

        // Verify that Exit event is triggered and updatePaymentOptions is not called
        assertThat(events).anyMatch { it is Exit }
        verify(updatePaymentOptions, never()).invoke(
            any(),
            any()
        )
    }

    @Test
    fun `when save is clicked with changes and save is successful, then call updatePaymentOptions and exit`() =
        testBlocking {
            // Setup with successful update
            whenever(updatePaymentOptions.invoke(any(), any()))
                .thenReturn(Result.success(Unit))

            setup()

            // Get the content state, make changes, and call onSaveClicked
            val contentState =
                viewModel.viewState.captureValues().last() as WooShippingEditPaymentViewModel.ViewState.Content

            // Change payment method
            val newPaymentMethodId = 2L
            contentState.onPaymentMethodSelected(newPaymentMethodId)

            // Change email receipts
            contentState.onEmailReceiptsChanged(!defaultAccountSettings.paymentMethodOptions.emailReceipts)

            // Capture events and save changes
            val events = viewModel.event.runAndCaptureValues {
                contentState.onSaveClicked()
                advanceUntilIdle()
            }

            // Verify that updatePaymentOptions is called with the correct parameters
            verify(updatePaymentOptions).invoke(
                selectedPaymentMethodId = newPaymentMethodId,
                emailReceipts = !defaultAccountSettings.paymentMethodOptions.emailReceipts
            )

            // Verify that payment_method_selected event is tracked
            verify(tracker).track(AnalyticsEvent.WCS_PAYMENT_STEP, mapOf(KEY_STATE to "payment_method_selected"))

            // Verify that Exit event is triggered
            assertThat(events).anyMatch { it is Exit }
        }

    @Test
    fun `when save is clicked with changes and save fails, then call updatePaymentOptions and show error`() =
        testBlocking {
            // Setup with failed update
            whenever(updatePaymentOptions.invoke(any(), any()))
                .thenReturn(Result.failure(Exception("Failed to update payment options")))

            setup()

            // Get the content state, make changes, and call onSaveClicked
            val contentState =
                viewModel.viewState.captureValues().last() as WooShippingEditPaymentViewModel.ViewState.Content

            // Change payment method
            val newPaymentMethodId = 2L
            contentState.onPaymentMethodSelected(newPaymentMethodId)

            // Capture events and save changes
            val events = viewModel.event.runAndCaptureValues {
                contentState.onSaveClicked()
                advanceUntilIdle()
            }

            // Verify that updatePaymentOptions is called with the correct parameters
            verify(updatePaymentOptions).invoke(
                selectedPaymentMethodId = newPaymentMethodId,
                emailReceipts = defaultAccountSettings.paymentMethodOptions.emailReceipts
            )

            // Verify that ShowSnackbar event is triggered with error message
            assertThat(events).anyMatch { it is ShowSnackbar && it.message == R.string.error_generic }
        }
}
