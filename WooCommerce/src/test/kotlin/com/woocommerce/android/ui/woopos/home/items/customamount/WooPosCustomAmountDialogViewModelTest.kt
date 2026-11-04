package com.woocommerce.android.ui.woopos.home.items.customamount

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCustomAmountDialogViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val getCurrencyFormattingParameters: WooPosGetCurrencyFormattingParameters = mock {
        on { invoke() }.thenReturn(
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
        val sendGate = CompletableDeferred<Unit>()
        whenever(childrenToParentEventSender.sendToParent(any())).doSuspendableAnswer {
            sendGate.await()
        }
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("3.00"))

        sut.onSubmit()
        sut.onSubmit()
        sendGate.complete(Unit)
        advanceUntilIdle()

        verify(childrenToParentEventSender, times(1)).sendToParent(
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
    fun `given valid amount, when submitted, then onSubmitted is invoked after event is sent`() = runTest {
        val order = mutableListOf<String>()
        whenever(childrenToParentEventSender.sendToParent(any())).doSuspendableAnswer {
            order.add("sent")
            Unit
        }
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("5.00"))

        sut.onSubmit(onSubmitted = { order.add("callback") })
        advanceUntilIdle()

        assertThat(order).containsExactly("sent", "callback")
    }

    @Test
    fun `given amount is null, when onSubmit called, then onSubmitted is not invoked`() = runTest {
        var invocations = 0
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)

        sut.onSubmit(onSubmitted = { invocations++ })
        advanceUntilIdle()

        assertThat(invocations).isZero()
    }

    @Test
    fun `given submission in flight, when onSubmit called again, then onSubmitted is invoked once`() = runTest {
        var invocations = 0
        val sendGate = CompletableDeferred<Unit>()
        whenever(childrenToParentEventSender.sendToParent(any())).doSuspendableAnswer {
            sendGate.await()
        }
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("3.00"))

        sut.onSubmit(onSubmitted = { invocations++ })
        sut.onSubmit(onSubmitted = { invocations++ })
        sendGate.complete(Unit)
        advanceUntilIdle()

        assertThat(invocations).isEqualTo(1)
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

    @Test
    fun `given invalid amount, when continued, then amount screen remains open`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)

        listOf(null, BigDecimal.ZERO, BigDecimal("-1.00")).forEach { amount ->
            // WHEN
            sut.onAmountChanged(amount)
            sut.onAmountConfirmed()

            // THEN
            assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.AMOUNT)
        }
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given valid amount, when continued, then form opens without submitting to cart`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("12.50"))

        // WHEN
        sut.onAmountConfirmed()
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.FORM)
        assertThat(sut.state.value.amount).isEqualByComparingTo("12.50")
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given edited form, when returning to amount and continuing, then name taxes and item identity are preserved`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(
            WooPosCartItemViewState.CustomAmount(
                itemNumber = 7,
                name = "Original",
                amount = BigDecimal("10.00"),
                formattedAmount = "$10.00",
                isTaxable = false,
            )
        )
        sut.onAmountConfirmed()
        sut.onNameChanged("Gift wrap")
        sut.onTaxableToggled(true)

        // WHEN
        sut.onEditAmount()
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.AMOUNT)
        sut.onAmountChanged(BigDecimal("15.00"))
        sut.onAmountConfirmed()
        sut.onSubmit()
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.CustomAmountSubmitted(
                name = "Gift wrap",
                amount = BigDecimal("15.00"),
                isTaxable = true,
                editingItemNumber = 7,
            )
        )
    }

    @Test
    fun `given dismissed form, when adding again, then amount screen opens with empty fields`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(editing = null)
        sut.onAmountChanged(BigDecimal("5.00"))
        sut.onAmountConfirmed()
        sut.onNameChanged("Discarded")
        sut.onTaxableToggled(true)
        sut.onDismissed()

        // WHEN
        sut.initializeFor(editing = null)

        // THEN
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.AMOUNT)
        assertThat(sut.state.value.amount).isNull()
        assertThat(sut.state.value.name).isEmpty()
        assertThat(sut.state.value.isTaxable).isFalse()
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
