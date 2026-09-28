package com.woocommerce.android.ui.woopos.cashmanagement

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class WooPosCashMovementRecorderTest {
    private val sessions: WooPosCashSessionRepository = mock()
    private lateinit var recorder: WooPosCashMovementRecorder

    @Before fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("woo_pos_cash_movements", Context.MODE_PRIVATE).edit().clear().commit()
        whenever(sessions.selectedSiteLocalId).thenReturn(1)
        recorder = WooPosCashMovementRecorder(context, sessions)
    }

    @Test fun `confirmed sale stays queued until Core accepts movement`() = runTest {
        val online = AtomicBoolean(false)
        whenever(sessions.sale(eq(7), eq(99), any(), eq(1))).thenAnswer {
            if (!online.get()) throw CashSessionException("offline")
            movement(1)
        }

        recorder.recordSale(capture(), 99)
        assertThat(recorder.hasPending(7)).isTrue()
        online.set(true)
        recorder.retryPending()

        assertThat(recorder.hasPending(7)).isFalse()
        verify(sessions, atLeastOnce()).sale(eq(7), eq(99), any(), eq(1))
    }

    @Test fun `confirmed refund uses the server refund id and clears after Core accepts`() = runTest {
        whenever(sessions.refund(eq(7), eq(99), eq(55), any(), eq(1))).thenReturn(movement(2))

        recorder.recordRefund(capture(), 99, 55)
        recorder.retryPending()

        assertThat(recorder.hasPending(7)).isFalse()
        verify(sessions, atLeastOnce()).refund(eq(7), eq(99), eq(55), any(), eq(1))
    }

    @Test fun `pending movement remains queued when another store is selected`() = runTest {
        whenever(sessions.selectedSiteLocalId).thenReturn(2)
        whenever(sessions.sale(eq(7), eq(99), any(), eq(1))).thenReturn(movement(1))

        recorder.recordSale(capture(), 99)
        recorder.retryPending()
        assertThat(recorder.hasPending(7)).isFalse()
        verify(sessions, never()).sale(eq(7), eq(99), any(), any())

        whenever(sessions.selectedSiteLocalId).thenReturn(1)
        assertThat(recorder.hasPending(7)).isTrue()
        recorder.retryPending()
        assertThat(recorder.hasPending(7)).isFalse()
        verify(sessions, atLeastOnce()).sale(eq(7), eq(99), any(), eq(1))
    }

    @Test fun `unsupported cash session endpoint means no active session`() = runTest {
        whenever(sessions.currentWithSite()).thenAnswer { throw CashSessionUnsupportedException() }

        assertThat(recorder.captureSession()).isNull()
    }

    private fun movement(id: Long) = WooPosCashMovement(
        id = id, type = "cash_sale", amount = "10.00", reason = null, orderId = 99,
        refundId = null, occurredAt = "2026-09-28T12:00:00Z", createdByName = "Cashier"
    )

    private fun capture() = WooPosCapturedCashSession(
        WooPosCashSession(7, "device", null, "open", 1, "USD", 2, "0.00", "0.00", "0.00",
            "0.00", "0.00", "0.00", null, null, null, "2026-09-28T12:00:00Z", null,
            "Cashier", null),
        1
    )
}
