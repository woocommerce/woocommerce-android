package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingAccountSettings
import com.woocommerce.android.model.StoreOwnerDetails
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class EditShippingLabelPaymentViewModelTest : BaseUnitTest() {
    private val shippingLabelRepository: ShippingLabelRepository = mock()

    @Suppress("DEPRECATION")
    private val paymentMethods = listOf(
        CreateShippingLabelTestUtils.generatePaymentMethod(id = 1, cardType = "visa"),
        CreateShippingLabelTestUtils.generatePaymentMethod(id = 2, cardType = "mastercard")
    )

    private val shippingAccountSettings = ShippingAccountSettings(
        canManagePayments = true,
        canEditSettings = true,
        paymentMethods = paymentMethods,
        selectedPaymentId = 1,
        lastUsedBoxId = null,
        storeOwnerDetails = StoreOwnerDetails(
            "email", "username", "username", "name"
        ),
        isEmailReceiptEnabled = true
    )

    private lateinit var viewModel: EditShippingLabelPaymentViewModel

    fun setup(accountSettings: WooResult<ShippingAccountSettings> = WooResult(shippingAccountSettings)) {
        testBlocking {
            whenever(shippingLabelRepository.getAccountSettings()).thenReturn(accountSettings)
        }
        viewModel = EditShippingLabelPaymentViewModel(
            SavedStateHandle(),
            shippingLabelRepository = shippingLabelRepository
        )
    }

    @Test
    fun `display settings when screen is opened`() = testBlocking {
        setup()
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val paymentMethodModels = listOf(
            PaymentMethodUiModel(paymentMethods[0], isSelected = true),
            PaymentMethodUiModel(paymentMethods[1], isSelected = false)
        )
        verify(shippingLabelRepository).getAccountSettings()
        assertThat(viewState!!.dataLoadState).isEqualTo(DataLoadState.Success)
        assertThat(viewState!!.paymentMethods).isEqualTo(paymentMethodModels)
        assertThat(viewState!!.emailReceipts).isEqualTo(shippingAccountSettings.isEmailReceiptEnabled)
        assertThat(viewState!!.canManagePayments).isEqualTo(shippingAccountSettings.canManagePayments)
    }

    @Test
    fun `display error when data loading fails`() = testBlocking {
        setup(WooResult(WooError(GENERIC_ERROR, UNKNOWN)))

        val viewState = viewModel.viewStateData.liveData.value
        assertThat(viewState!!.dataLoadState).isEqualTo(DataLoadState.Error)
    }

    @Test
    fun `display done button when data is valid`() = testBlocking {
        setup()

        viewModel.onPaymentMethodSelected(paymentMethods[1])

        val viewState = viewModel.viewStateData.liveData.value
        assertThat(viewState!!.canSave).isEqualTo(true)
    }

    @Test
    fun `hide done button when no method is selected`() = testBlocking {
        val accountSettings = shippingAccountSettings.copy(selectedPaymentId = null)
        setup(WooResult(accountSettings))

        val viewState = viewModel.viewStateData.liveData.value
        assertThat(viewState!!.canSave).isEqualTo(false)
    }

    @Test
    fun `can't edit payments if not store owner`() = testBlocking {
        val accountSettings = shippingAccountSettings.copy(canManagePayments = false)
        setup(WooResult(accountSettings))
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState!!.canManagePayments).isEqualTo(false)
    }

    @Test
    fun `can't edit settings`() = testBlocking {
        val accountSettings = shippingAccountSettings.copy(canManagePayments = false, canEditSettings = false)
        setup(WooResult(accountSettings))
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState!!.canEditSettings).isEqualTo(false)
    }

    @Test
    fun `save settings success`() = testBlocking {
        whenever(shippingLabelRepository.updatePaymentSettings(any(), any())).thenReturn(WooResult(Unit))
        setup()

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }

        viewModel.onPaymentMethodSelected(paymentMethods[1])
        viewModel.onEmailReceiptsCheckboxChanged(false)
        viewModel.onDoneButtonClicked()

        verify(shippingLabelRepository).updatePaymentSettings(paymentMethods[1].id, false)
        assertThat(event).isEqualTo(ExitWithResult(paymentMethods[1]))
    }

    @Test
    fun `save settings failure`() = testBlocking {
        val error = WooError(API_ERROR, NETWORK_ERROR, "")
        whenever(shippingLabelRepository.updatePaymentSettings(any(), any())).thenReturn(WooResult(error = error))
        setup()

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }

        viewModel.onPaymentMethodSelected(paymentMethods[1])
        viewModel.onEmailReceiptsCheckboxChanged(false)
        viewModel.onDoneButtonClicked()

        verify(shippingLabelRepository).updatePaymentSettings(paymentMethods[1].id, false)
        assertThat(event).isEqualTo(ShowSnackbar(R.string.shipping_label_payments_saving_error))
    }

    @Test
    fun `open add payment screen when button is clicked`() = testBlocking {
        setup()

        viewModel.onAddPaymentMethodClicked()

        assertThat(viewModel.event.value).isEqualTo(AddPaymentMethod)
    }

    @Test
    fun `refresh data when payment method is added`() = testBlocking {
        setup()

        viewModel.onPaymentMethodAdded()

        verify(shippingLabelRepository).getAccountSettings(true)
    }

    @Test
    fun `show snackbar when new payment method is added`() = testBlocking {
        val singleCardResult = shippingAccountSettings
            .copy(paymentMethods = shippingAccountSettings.paymentMethods.subList(0, 1))
        whenever(shippingLabelRepository.getAccountSettings(any()))
            .thenReturn(WooResult(singleCardResult))
            .thenReturn(WooResult(shippingAccountSettings))
        viewModel = EditShippingLabelPaymentViewModel(
            SavedStateHandle(),
            shippingLabelRepository = shippingLabelRepository
        )

        viewModel.onPaymentMethodAdded()

        assertThat(viewModel.event.value)
            .isEqualTo(ShowSnackbar(R.string.shipping_label_payment_method_added))
    }

    @Test
    fun `given no existing payment methods, when user is store owner, then show Add First Payment button`() {
        val accountSettings = shippingAccountSettings.copy(paymentMethods = emptyList())
        setup(WooResult(accountSettings))

        val viewState = viewModel.viewStateData.liveData.value
        assertThat(viewState!!.showAddFirstPaymentButton).isEqualTo(true)
    }

    @Test
    fun `given existing payment methods, when user is store owner, then show Add Payment button`() {
        setup(WooResult(shippingAccountSettings))

        val viewState = viewModel.viewStateData.liveData.value
        assertThat(viewState!!.showAddPaymentButton).isEqualTo(true)
    }

    @Test
    fun `when user is not store owner, then hide both Add First Payment button and Add Payment Button`() {
        val accountSettings = shippingAccountSettings.copy(canManagePayments = false)
        setup(WooResult(accountSettings))

        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState!!.showAddFirstPaymentButton).isEqualTo(false)
        assertThat(viewState!!.showAddPaymentButton).isEqualTo(false)
    }

    @Test
    fun `given no existing payments, when user is store owner, redirect to add payment method screen`() {
        val accountSettings = shippingAccountSettings.copy(paymentMethods = emptyList())
        setup(WooResult(accountSettings))
        assertThat(viewModel.event.value).isEqualTo(AddPaymentMethod)
    }
}
