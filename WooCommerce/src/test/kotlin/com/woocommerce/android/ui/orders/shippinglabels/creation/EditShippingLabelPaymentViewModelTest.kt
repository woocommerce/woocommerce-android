package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingAccountSettings
import com.woocommerce.android.model.StoreOwnerDetails
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.PaymentMethodUiModel
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.ViewState
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.util.Date

@ExperimentalCoroutinesApi
class EditShippingLabelPaymentViewModelTest : BaseUnitTest() {
    private val shippingLabelRepository: ShippingLabelRepository = mock()

    @Suppress("DEPRECATION")
    private val paymentMethods = listOf<PaymentMethod>(
        PaymentMethod(1, "Jhon Doe", "visa", "1234", Date(2030, 11, 31)),
        PaymentMethod(2, "Jhon Doe", "mastercard", "1234", Date(2030, 11, 31))
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

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private lateinit var viewModel: EditShippingLabelPaymentViewModel

    fun setup(accountSettings: ShippingAccountSettings = shippingAccountSettings) {
        val savedState: SavedStateWithArgs = spy(
            SavedStateWithArgs(
                SavedStateHandle(),
                null
            )
        )
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(shippingLabelRepository.getAccountSettings()).thenReturn(WooResult(accountSettings))
        }
        viewModel = EditShippingLabelPaymentViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            shippingLabelRepository = shippingLabelRepository
        )
    }

    @Test
    fun `test display settings`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        setup()
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val paymentMethodModels = listOf(
            PaymentMethodUiModel(paymentMethods[0], isSelected = true),
            PaymentMethodUiModel(paymentMethods[1], isSelected = false)
        )
        verify(shippingLabelRepository).getAccountSettings()
        assertThat(viewState!!.paymentMethods).isEqualTo(paymentMethodModels)
        assertThat(viewState!!.emailReceipts).isEqualTo(shippingAccountSettings.isEmailReceiptEnabled)
        assertThat(viewState!!.canManagePayments).isEqualTo(shippingAccountSettings.canManagePayments)
    }

    @Test
    fun `display done button when selecting payment method`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        setup()
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onPaymentMethodSelected(paymentMethods[1])

        assertThat(viewState!!.hasChanges).isEqualTo(true)
    }

    @Test
    fun `can't edit payments if not store owner`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val accountSettings = shippingAccountSettings.copy(canManagePayments = false)
        setup(accountSettings)
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState!!.canManagePayments).isEqualTo(false)
    }

    @Test
    fun `can't edit settings`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val accountSettings = shippingAccountSettings.copy(canManagePayments = false, canEditSettings = false)
        setup(accountSettings)
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState!!.canEditSettings).isEqualTo(false)
    }

    @Test
    fun `display done button when changing email receipts`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        setup()
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        viewModel.onEmailReceiptsCheckboxChanged(!shippingAccountSettings.isEmailReceiptEnabled)

        assertThat(viewState!!.hasChanges).isEqualTo(true)
    }

    @Test
    fun `save settings success`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `save settings failure`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
}
