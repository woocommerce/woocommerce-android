package com.woocommerce.android.ui.woopos.cashmanagement

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerController
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
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

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCashManagementViewModelTest {
    @get:Rule val coroutines = WooPosCoroutineTestRule()
    @get:Rule val instant = InstantTaskExecutorRule()

    private val repository: WooPosCashSessionRepository = mock()
    private val drawer: WooPosCashDrawerController = mock()
    private val recorder: WooPosCashMovementRecorder = mock()

    @Test fun `current failure leaves retryable error and retry loads session`() = runTest {
        whenever(repository.current()).thenAnswer { throw CashSessionException("offline") }.thenReturn(session())
        val model = WooPosCashManagementViewModel(repository, drawer, recorder)
        advanceUntilIdle()
        assertThat(model.state.value.currentError).isEqualTo("offline")
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
        val model = WooPosCashManagementViewModel(repository, drawer, recorder)
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
        val model = WooPosCashManagementViewModel(repository, drawer, recorder)
        advanceUntilIdle()
        model.selectSession(7)
        advanceUntilIdle()
        model.refreshDetail()
        advanceUntilIdle()
        assertThat(model.state.value.detail?.id).isEqualTo(7L)
        assertThat(model.state.value.movements.map { it.id }).containsExactly(1L)
        assertThat(model.state.value.detailError).isEqualTo("offline")
    }

    @Test fun `pay out above expected cash is rejected and blank reason gets directional default`() = runTest {
        whenever(repository.current()).thenReturn(session())
        whenever(repository.adjust(eq(7), eq("pay_in"), any(), any(), any())).thenReturn(movement(1))
        val model = WooPosCashManagementViewModel(repository, drawer, recorder)
        advanceUntilIdle()
        model.adjust("pay_out", BigDecimal("101"), "") { _, _ -> }
        advanceUntilIdle()
        verify(repository, never()).adjust(eq(7), eq("pay_out"), any(), any(), any())
        model.adjust("pay_in", BigDecimal("5"), "") { _, _ -> }
        advanceUntilIdle()
        verify(repository).adjust(eq(7), eq("pay_in"), eq(BigDecimal("5")), eq("Paid in"), any())
    }

    @Test fun `start retries uncertain response with same request id`() = runTest {
        whenever(repository.current()).thenReturn(null)
        val ids = mutableListOf<UUID>()
        whenever(repository.start(eq(BigDecimal("25")), anyOrNull(), any())).thenAnswer {
            ids += it.getArgument<UUID>(2)
            if (ids.size == 1) throw CashSessionException("response lost")
            session()
        }
        val model = WooPosCashManagementViewModel(repository, drawer, recorder)
        advanceUntilIdle()
        model.start(BigDecimal("25"), null) { }
        advanceUntilIdle()
        model.start(BigDecimal("25"), null) { }
        advanceUntilIdle()
        assertThat(ids).hasSize(2)
        assertThat(ids[0]).isEqualTo(ids[1])
        assertThat(model.state.value.current?.id).isEqualTo(7L)
    }

    @Test fun `close waits for pending sale and requires recount after revision conflict`() = runTest {
        whenever(repository.current()).thenReturn(session())
        whenever(recorder.hasPending(7)).thenReturn(false, true, false)
        whenever(repository.close(eq(7), eq(1), any(), anyOrNull(), any())).thenAnswer {
            throw CashSessionException("revision conflict")
        }
        val model = WooPosCashManagementViewModel(repository, drawer, recorder)
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

    private fun session(id: Long = 7, revision: Int = 1) = WooPosCashSession(
        id = id, deviceId = "device", drawerId = "drawer", status = "open", revision = revision,
        currency = "USD", currencyPrecision = 2, openingAmount = "100.00", cashSalesTotal = "0.00",
        cashRefundsTotal = "0.00", paidInTotal = "0.00", paidOutTotal = "0.00",
        expectedAmount = "100.00", countedAmount = null, variance = null, note = null,
        dateCreatedGmt = "2026-09-28T12:00:00Z", dateClosedGmt = null,
        openedByName = "Cashier", closedByName = null,
    )

    private fun movement(id: Long) = WooPosCashMovement(
        id = id, type = "pay_in", amount = "1.00", reason = null,
        orderId = null, refundId = null, occurredAt = "2026-09-28T12:00:00Z", createdByName = "Cashier"
    )
}
