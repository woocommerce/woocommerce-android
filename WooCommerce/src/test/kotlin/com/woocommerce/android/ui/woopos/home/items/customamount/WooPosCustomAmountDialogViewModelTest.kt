package com.woocommerce.android.ui.woopos.home.items.customamount

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
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
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()

        // WHEN
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)

        // THEN
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
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()

        // WHEN
        sut.initializeFor(
            mode = WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 42,
                name = "Service",
                amount = BigDecimal("25.00"),
                isTaxable = true,
            )
        )

        // THEN
        val state = sut.state.value
        assertThat(state.mode).isEqualTo(
            WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 42,
                amount = BigDecimal("25.00"),
                name = "Service",
                isTaxable = true,
            )
        )
        assertThat(state.amount).isEqualByComparingTo(BigDecimal("25.00"))
        assertThat(state.name).isEqualTo("Service")
        assertThat(state.isTaxable).isTrue()
    }

    @Test
    fun `given amount is zero, when state inspected, then submit is disabled`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()

        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal.ZERO))

        // THEN
        assertThat(sut.state.value.isSubmitEnabled).isFalse()
    }

    @Test
    fun `given amount is positive, when state inspected, then submit is enabled`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()

        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("5.00")))

        // THEN
        assertThat(sut.state.value.isSubmitEnabled).isTrue()
    }

    @Test
    fun `given add mode with name and amount, when submitted, then event is sent to parent`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("10.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("Service fee"))
        sut.onUIEvent(WooPosCustomAmountUIEvent.TaxableToggled(true))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
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
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("10.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("   "))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
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
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(
            mode = WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 7,
                name = "Old",
                amount = BigDecimal("1.00"),
                isTaxable = false,
            )
        )
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("2.50")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("Updated"))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
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
        // GIVEN
        val sendGate = CompletableDeferred<Unit>()
        whenever(childrenToParentEventSender.sendToParent(any())).doSuspendableAnswer {
            sendGate.await()
        }
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("3.00")))

        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        sendGate.complete(Unit)
        advanceUntilIdle()

        // THEN
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
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given valid amount, when submitted, then state is marked as submitted after event is sent`() = runTest {
        // GIVEN
        val submittedWhenSent = mutableListOf<Boolean>()
        val sut = createSut()
        whenever(childrenToParentEventSender.sendToParent(any())).doSuspendableAnswer {
            submittedWhenSent.add(sut.state.value.isSubmitted)
            Unit
        }
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("5.00")))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
        assertThat(submittedWhenSent).containsExactly(false)
        assertThat(sut.state.value.isSubmitted).isTrue()
        assertThat(sut.state.value.isSubmitEnabled).isFalse()
    }

    @Test
    fun `given amount is null, when onSubmit called, then state is not marked as submitted`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.isSubmitted).isFalse()
    }

    @Test
    fun `given submission completed, when onSubmit called again, then event is sent once`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("3.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender, times(1)).sendToParent(any())
    }

    @Test
    fun `given submitted, when dismissed and reopened, then form can be submitted again`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("3.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.Closed)
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("4.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.isSubmitted).isTrue()
        verify(childrenToParentEventSender, times(2)).sendToParent(any())
    }

    @Test
    fun `given same editing item, when initializeFor called twice, then state is set once`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        val item = WooPosCustomAmountDialogState.Mode.Edit(
            itemNumber = 1,
            name = "Original",
            amount = BigDecimal("5.00"),
            isTaxable = false,
        )

        sut.initializeFor(item)
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("User typed"))

        // WHEN
        sut.initializeFor(item)

        // THEN
        assertThat(sut.state.value.name).isEqualTo("User typed")
    }

    @Test
    fun `given invalid amount, when continued, then amount screen remains open`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)

        listOf(null, BigDecimal.ZERO, BigDecimal("-1.00")).forEach { amount ->
            // WHEN
            sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(amount))
            sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)

            // THEN
            assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.AMOUNT)
        }
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given valid amount, when continued, then details open without submitting to cart`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("12.50")))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.DETAILS)
        assertThat(sut.state.value.amount).isEqualByComparingTo("12.50")
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given edited form, when returning to amount and continuing, then name taxes and item identity are preserved`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(
            WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 7,
                amount = BigDecimal("10.00"),
                name = "Original",
                isTaxable = false,
            )
        )
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("Gift wrap"))
        sut.onUIEvent(WooPosCustomAmountUIEvent.TaxableToggled(true))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditAmountClicked)
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.AMOUNT)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("15.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
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
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("5.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("Discarded"))
        sut.onUIEvent(WooPosCustomAmountUIEvent.TaxableToggled(true))
        sut.onUIEvent(WooPosCustomAmountUIEvent.Closed)

        // WHEN
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)

        // THEN
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.AMOUNT)
        assertThat(sut.state.value.amount).isNull()
        assertThat(sut.state.value.name).isEmpty()
        assertThat(sut.state.value.isTaxable).isFalse()
    }

    @Test
    fun `given an existing item, when opened, then details are shown before amount editing`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()

        // WHEN
        sut.initializeFor(
            WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 7,
                amount = BigDecimal.TEN,
                name = "Service",
                isTaxable = true,
            )
        )

        // THEN
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.DETAILS)
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given amount edited from details, when cancelled, then original amount and other fields are retained`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal.TEN))
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("Gift wrap"))
        sut.onUIEvent(WooPosCustomAmountUIEvent.TaxableToggled(true))
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditAmountClicked)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal.ZERO))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountBackClicked)

        // THEN
        assertThat(sut.state.value.isClosed).isFalse()
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.DETAILS)
        assertThat(sut.state.value.amount).isEqualByComparingTo(BigDecimal.TEN)
        assertThat(sut.state.value.name).isEqualTo("Gift wrap")
        assertThat(sut.state.value.isTaxable).isTrue()
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given initial amount entry, when back is clicked, then the flow is dismissed without submitting`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal.TEN))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountBackClicked)

        // THEN
        assertThat(sut.state.value.isClosed).isTrue()
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given a confirmed amount change, when another edit is cancelled, then the last confirmed amount is restored`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal.TEN))
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditAmountClicked)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal("15.00")))
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditAmountClicked)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(null))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountBackClicked)

        // THEN
        assertThat(sut.state.value.isClosed).isFalse()
        assertThat(sut.state.value.amount).isEqualByComparingTo("15.00")
        assertThat(sut.state.value.isSubmitEnabled).isTrue()
    }

    @Test
    fun `given a name draft, when saved, then details receive it without submitting the cart`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(BigDecimal.TEN))
        sut.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.TaxableToggled(true))
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditNameClicked)
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameDraftChanged("Gift wrap"))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameConfirmed)

        // THEN
        assertThat(sut.state.value.screen).isEqualTo(WooPosCustomAmountDialogState.Screen.DETAILS)
        assertThat(sut.state.value.name).isEqualTo("Gift wrap")
        assertThat(sut.state.value.amount).isEqualByComparingTo(BigDecimal.TEN)
        assertThat(sut.state.value.isTaxable).isTrue()
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given an existing name, when a name draft is cancelled, then the original name is kept`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(WooPosCustomAmountDialogState.Mode.Add)
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameChanged("Original"))
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditNameClicked)
        assertThat(sut.state.value.nameDraft).isEqualTo("Original")
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameDraftChanged("Discarded"))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameBackClicked)
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditNameClicked)

        // THEN
        assertThat(sut.state.value.name).isEqualTo("Original")
        assertThat(sut.state.value.nameDraft).isEqualTo("Original")
        verify(childrenToParentEventSender, never()).sendToParent(any())
    }

    @Test
    fun `given an existing item, when a blank name is saved and submitted, then default name and identity are preserved`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(
            WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 7,
                amount = BigDecimal.TEN,
                name = "Original",
                isTaxable = true,
            )
        )
        sut.onUIEvent(WooPosCustomAmountUIEvent.EditNameClicked)
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameDraftChanged("   "))

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.NameConfirmed)
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.CustomAmountSubmitted(
                name = DEFAULT_NAME,
                amount = BigDecimal.TEN,
                isTaxable = true,
                editingItemNumber = 7,
            )
        )
    }

    @Test
    fun `given an existing item with the default name, when opened, then the name is shown as unset`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()

        // WHEN
        sut.initializeFor(
            WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 7,
                amount = BigDecimal.TEN,
                name = DEFAULT_NAME,
                isTaxable = false,
            )
        )

        // THEN
        assertThat(sut.state.value.name).isEmpty()
    }

    @Test
    fun `given an existing item with the default name, when submitted unchanged, then the default name is kept`() = runTest {
        // GIVEN
        val sut = createSut()
        advanceUntilIdle()
        sut.initializeFor(
            WooPosCustomAmountDialogState.Mode.Edit(
                itemNumber = 7,
                amount = BigDecimal.TEN,
                name = DEFAULT_NAME,
                isTaxable = false,
            )
        )

        // WHEN
        sut.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(
            ChildToParentEvent.CustomAmountSubmitted(
                name = DEFAULT_NAME,
                amount = BigDecimal.TEN,
                isTaxable = false,
                editingItemNumber = 7,
            )
        )
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
