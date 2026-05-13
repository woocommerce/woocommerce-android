package com.woocommerce.android.ui.woopos.home.items.customamount

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCustomAmountDialogViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val getCurrencyFormattingParameters: WooPosGetCurrencyFormattingParameters = mock {
        onBlocking { invoke() }.thenReturn(
            WooPosCurrencyFormattingParameters(
                currencySymbol = "$",
                currencyPosition = CurrencyPosition.LEFT,
                decimalSeparator = ".",
                numberOfDecimals = 2,
            )
        )
    }
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.woopos_custom_amount_dialog_name_placeholder) }
            .thenReturn(DEFAULT_NAME)
    }

    @Test
    fun `given add mode, when initialized, then state has empty fields and currency params`() = runTest {
        val sut = createSut()
        advanceUntilIdle()

        sut.initializeFor(editing = null)

        val state = sut.state.value
        assertThat(state.mode).isEqualTo(WooPosCustomAmountDialogState.Mode.Add)
        assertThat(state.amount).isNull()
        assertThat(state.name).isEmpty()
        assertThat(state.isTaxable).isFalse()
        assertThat(state.currencySymbol).isEqualTo("$")
        assertThat(state.decimalSeparator).isEqualTo(".")
    }

    @Test
    fun `given edit mode, when initialized, then state is pre-filled from editing item`() = runTest {
        val sut = createSut()
        advanceUntilIdle()

        sut.initializeFor(
            editing = WooPosCartItemViewState.CustomAmount(
                itemNumber = 42,
                name = "Service",
                amount = BigDecimal("25.00"),
                formattedAmount = "$25.00",
                isTaxable = true,
            )
        )

        val state = sut.state.value
        assertThat(state.mode).isEqualTo(WooPosCustomAmountDialogState.Mode.Edit(itemNumber = 42))
        assertThat(state.amount).isEqualByComparingTo(BigDecimal("25.00"))
        assertThat(state.name).isEqualTo("Service")
        assertThat(state.isTaxable).isTrue()
    }

    @Test
    fun `given amount is zero, when state inspected, then submit is disabled`() = runTest {
        val sut = createSut()
        advanceUntilIdle()

        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal.ZERO)

        assertThat(sut.state.value.isSubmitEnabled).isFalse()
    }

    @Test
    fun `given amount is positive, when state inspected, then submit is enabled`() = runTest {
        val sut = createSut()
        advanceUntilIdle()

        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("5.00"))

        assertThat(sut.state.value.isSubmitEnabled).isTrue()
    }

    @Test
    fun `given add mode with name and amount, when submitted, then event is sent to parent`() = runTest {
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("10.00"))
        sut.onNameChanged("Service fee")
        sut.onTaxableToggled(true)

        sut.onSubmit()
        advanceUntilIdle()

        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.CustomAmountSubmitted(
                name = "Service fee",
                amount = BigDecimal("10.00"),
                isTaxable = true,
                editingItemNumber = null,
            )
        )
    }

    @Test
    fun `given name is blank, when submitted, then default name is used`() = runTest {
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("10.00"))
        sut.onNameChanged("   ")

        sut.onSubmit()
        advanceUntilIdle()

        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.CustomAmountSubmitted(
                name = DEFAULT_NAME,
                amount = BigDecimal("10.00"),
                isTaxable = false,
                editingItemNumber = null,
            )
        )
    }

    @Test
    fun `given edit mode, when submitted, then event carries editingItemNumber`() = runTest {
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(
            editing = WooPosCartItemViewState.CustomAmount(
                itemNumber = 7,
                name = "Old",
                amount = BigDecimal("1.00"),
                formattedAmount = "$1.00",
                isTaxable = false,
            )
        )
        sut.onAmountChanged(BigDecimal("2.50"))
        sut.onNameChanged("Updated")

        sut.onSubmit()
        advanceUntilIdle()

        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.CustomAmountSubmitted(
                name = "Updated",
                amount = BigDecimal("2.50"),
                isTaxable = false,
                editingItemNumber = 7,
            )
        )
    }

    @Test
    fun `given submission in flight, when onSubmit called again, then second submission is ignored`() = runTest {
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("3.00"))

        sut.onSubmit()
        sut.onSubmit()
        advanceUntilIdle()

        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.CustomAmountSubmitted(
                name = DEFAULT_NAME,
                amount = BigDecimal("3.00"),
                isTaxable = false,
                editingItemNumber = null,
            )
        )
    }

    @Test
    fun `given amount is null, when onSubmit called, then nothing is sent`() = runTest {
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)

        sut.onSubmit()
        advanceUntilIdle()

        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given same editing item, when initializeFor called twice, then state is set once`() = runTest {
        val sut = createSut()
        advanceUntilIdle()
        val item = WooPosCartItemViewState.CustomAmount(
            itemNumber = 1,
            name = "Original",
            amount = BigDecimal("5.00"),
            formattedAmount = "$5.00",
            isTaxable = false,
        )

        sut.initializeFor(item)
        sut.onNameChanged("User typed")
        sut.initializeFor(item)

        assertThat(sut.state.value.name).isEqualTo("User typed")
    }

    private fun createSut() = WooPosCustomAmountDialogViewModel(
        getCurrencyFormattingParameters = getCurrencyFormattingParameters,
        childrenToParentEventSender = childrenToParentEventSender,
        resourceProvider = resourceProvider,
    )

    private companion object {
        const val DEFAULT_NAME = "Custom amount"
    }
}
