package com.woocommerce.android.ui.woopos.cashmanagement

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerController
import com.woocommerce.android.ui.woopos.home.items.customamount.WooPosCurrencyFormattingParameters
import com.woocommerce.android.ui.woopos.home.items.customamount.WooPosGetCurrencyFormattingParameters
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.UUID
import org.wordpress.android.fluxc.model.settings.CurrencyPosition

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCashManagementViewModelTest {
    @get:Rule val coroutines = WooPosCoroutineTestRule()
    @get:Rule val instant = InstantTaskExecutorRule()

    private val repository: WooPosCashSessionRepository = mock()
    private val drawer: WooPosCashDrawerController = mock()
    private val recorder: WooPosCashMovementRecorder = mock()
    private val resources: ResourceProvider = mock()
    private val currencyFormatting: WooPosGetCurrencyFormattingParameters = mock()

    @Before fun setUp() = runTest {
        whenever(currencyFormatting()).thenReturn(
            WooPosCurrencyFormattingParameters("$", CurrencyPosition.LEFT, ".", 2)
        )
    }

    private fun model() = WooPosCashManagementViewModel(repository, drawer, recorder, resources, currencyFormatting)

    @Test fun `current failure leaves retryable error and retry loads session`() = runTest {
        whenever(repository.current()).thenAnswer { throw CashSessionException("offline") }.thenReturn(session())
        whenever(resources.getString(R.string.woopos_cash_error_current)).thenReturn("Could not load current session")
        val model = model()
        advanceUntilIdle()
        assertThat(model.state.value.currentError).isEqualTo("Could not load current session")
        model.refresh()
        advanceUntilIdle()
        assertThat(model.state.value.current?.id).isEqualTo(7L)
        assertThat(model.state.value.currentError).isNull()
    }

    @Test fun `history and activity paginate without duplicate rows`() = runTest {
        whenever(repository.current()).thenReturn(null)
        whenever(repository.past(1)).thenReturn(WooPosCashPage(listOf(session(7)), true))
        whenever(repository.past(2)).thenReturn(WooPosCashPage(listOf(session(7), session(8)), false))
        whenever(repository.detail(7)).thenReturn(session(7))
        whenever(repository.movements(7, 1)).thenReturn(WooPosCashPage(listOf(movement(1)), true))
        whenever(repository.movements(7, 2)).thenReturn(WooPosCashPage(listOf(movement(1), movement(2)), false))
        val model = model()
        advanceUntilIdle()
        model.loadHistory()
        advanceUntilIdle()
        model.loadMoreHistory()
        advanceUntilIdle()
        assertThat(model.state.value.history.map { it.id }).containsExactly(7L, 8L)
        model.selectSession(7)
        advanceUntilIdle()
        model.loadMoreMovements()
        advanceUntilIdle()
        assertThat(model.state.value.movements.map { it.id }).containsExactly(1L, 2L)
    }

    @Test fun `detail refresh keeps loaded content after server error`() = runTest {
        whenever(repository.current()).thenReturn(null)
        whenever(repository.detail(7)).thenReturn(session()).thenAnswer { throw CashSessionException("offline") }
        whenever(repository.movements(7, 1)).thenReturn(WooPosCashPage(listOf(movement(1)), false))
        whenever(resources.getString(R.string.woopos_cash_error_activity)).thenReturn("Could not load activity")
        val model = model()
        advanceUntilIdle()
        model.selectSession(7)
        advanceUntilIdle()
        model.refreshDetail()
        advanceUntilIdle()
        assertThat(model.state.value.detail?.id).isEqualTo(7L)
        assertThat(model.state.value.movements.map { it.id }).containsExactly(1L)
        assertThat(model.state.value.detailError).isEqualTo("Could not load activity")
    }

    @Test fun `activity failure still shows loaded session summary`() = runTest {
        whenever(repository.current()).thenReturn(null)
        whenever(repository.detail(7)).thenReturn(session())
        whenever(repository.movements(7, 1)).thenAnswer { throw CashSessionException("REST failure") }
        whenever(resources.getString(R.string.woopos_cash_error_activity)).thenReturn("Could not load activity")
        val model = model()
        advanceUntilIdle()

        model.selectSession(7)
        advanceUntilIdle()

        assertThat(model.state.value.detail?.id).isEqualTo(7L)
        assertThat(model.state.value.detailError).isEqualTo("Could not load activity")
        assertThat(model.state.value.loadingDetail).isFalse()
    }

    @Test fun `pay out above expected cash is rejected and blank reason gets directional default`() = runTest {
        whenever(repository.current()).thenReturn(session())
        whenever(repository.adjust(eq(7), eq("paid_in"), any(), any(), any(), any())).thenReturn(movement(1))
        val model = model()
        advanceUntilIdle()
        model.adjust("paid_out", BigDecimal("101"), "") { _, _, _ -> }
        advanceUntilIdle()
        verify(repository, never()).adjust(eq(7), eq("paid_out"), any(), any(), any(), any())
        model.adjust("paid_in", BigDecimal("5"), "") { _, _, _ -> }
        advanceUntilIdle()
        verify(repository).adjust(eq(7), eq("paid_in"), eq(BigDecimal("5")), eq("Paid in"), any(), any())
        whenever(repository.adjust(eq(7), eq("paid_out"), any(), any(), any(), any())).thenReturn(movement(2))
        model.adjust("paid_out", BigDecimal("5"), "") { _, _, _ -> }
        advanceUntilIdle()
        verify(repository).adjust(eq(7), eq("paid_out"), eq(BigDecimal("5")), eq("Paid out"), any(), any())
    }

    @Test fun `start retries uncertain response with same request id`() = runTest {
        whenever(repository.current()).thenReturn(null)
        val ids = mutableListOf<UUID>()
        whenever(repository.start(eq(BigDecimal("25")), anyOrNull(), any())).thenAnswer {
            ids += it.getArgument<UUID>(2)
            if (ids.size == 1) throw CashSessionException("response lost")
            session()
        }
        val model = model()
        advanceUntilIdle()
        model.start(BigDecimal("25"), null) { }
        advanceUntilIdle()
        model.start(BigDecimal("25"), null) { }
        advanceUntilIdle()
        assertThat(ids).hasSize(2)
        assertThat(ids[0]).isEqualTo(ids[1])
        assertThat(model.state.value.current?.id).isEqualTo(7L)
    }

    @Test fun `start uses zero-decimal store precision`() = runTest {
        whenever(repository.current()).thenReturn(null)
        whenever(currencyFormatting()).thenReturn(
            WooPosCurrencyFormattingParameters("¥", CurrencyPosition.LEFT, ".", 0)
        )
        whenever(repository.start(eq(BigDecimal("5")), anyOrNull(), any())).thenReturn(session())
        val model = model()
        advanceUntilIdle()

        model.start(BigDecimal("5.5"), null) { }
        advanceUntilIdle()
        verify(repository, never()).start(eq(BigDecimal("5.5")), anyOrNull(), any())
        model.start(BigDecimal("5"), null) { }
        advanceUntilIdle()
        verify(repository).start(eq(BigDecimal("5")), anyOrNull(), any())
    }

    @Test fun `start accepts three-decimal store float`() = runTest {
        whenever(repository.current()).thenReturn(null)
        whenever(currencyFormatting()).thenReturn(
            WooPosCurrencyFormattingParameters("KD", CurrencyPosition.LEFT, ".", 3)
        )
        whenever(repository.start(eq(BigDecimal("5.123")), anyOrNull(), any())).thenReturn(session())
        val model = model()
        advanceUntilIdle()

        model.start(BigDecimal("5.123"), null) { }
        advanceUntilIdle()
        verify(repository).start(eq(BigDecimal("5.123")), anyOrNull(), any())
    }

    @Test fun `close waits for pending sale and requires recount after revision conflict`() = runTest {
        whenever(repository.current()).thenReturn(session())
        whenever(recorder.hasPending(7)).thenReturn(false, true, false)
        whenever(repository.close(eq(7), eq(1), any(), anyOrNull(), any())).thenAnswer {
            throw CashSessionException("revision conflict")
        }
        val model = model()
        advanceUntilIdle()
        model.close(BigDecimal("100"), null) { }
        advanceUntilIdle()
        verify(repository, never()).close(any(), any(), any(), anyOrNull(), any())
        whenever(repository.current()).thenReturn(session(revision = 2))
        model.close(BigDecimal("100"), null) { }
        advanceUntilIdle()
        assertThat(model.state.value.recountRequired).isTrue()
        assertThat(model.state.value.current?.revision).isEqualTo(2)
    }

    @Test fun `a close completed elsewhere never reports this count as saved`() = runTest {
        whenever(repository.current()).thenReturn(session(), null)
        whenever(repository.close(eq(7), eq(1), any(), anyOrNull(), any())).thenAnswer {
            throw CashSessionException("session closed", "woocommerce_rest_cash_session_closed")
        }
        whenever(repository.detail(7)).thenReturn(
            session().copy(status = "closed", countedAmount = "90.00", variance = "-10.00")
        )
        whenever(repository.past(1)).thenReturn(WooPosCashPage(emptyList(), false))
        whenever(resources.getString(R.string.woopos_cash_error_session_closed)).thenReturn("Session already closed")
        val model = model()
        advanceUntilIdle()
        var success = false

        model.close(BigDecimal("100"), null) { success = true }
        advanceUntilIdle()

        assertThat(success).isFalse()
        assertThat(model.state.value.current).isNull()
        assertThat(model.state.value.detail?.countedAmount).isEqualTo("90.00")
        assertThat(model.state.value.operationError).isEqualTo("Session already closed")
    }

    private fun session(id: Long = 7, revision: Int = 1) = WooPosCashSession(
        id = id, deviceId = "device", drawerId = "drawer", status = "open", revision = revision,
        currency = "USD", currencyPrecision = 2, openingAmount = "100.00", cashSalesTotal = "0.00",
        cashRefundsTotal = "0.00", paidInTotal = "0.00", paidOutTotal = "0.00",
        expectedAmount = "100.00", countedAmount = null, variance = null, note = null,
        dateCreatedGmt = "2026-09-28T12:00:00Z", dateClosedGmt = null,
        openedByName = "Cashier", closedByName = null,
    )

    private fun movement(id: Long) = WooPosCashMovement(
        id = id, type = "paid_in", amount = "1.00", reason = null,
        orderId = null, refundId = null, occurredAt = "2026-09-28T12:00:00Z", createdByName = "Cashier"
    )
}
