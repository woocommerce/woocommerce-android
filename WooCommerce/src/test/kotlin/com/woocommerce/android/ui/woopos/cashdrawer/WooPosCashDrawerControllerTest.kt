package com.woocommerce.android.ui.woopos.cashdrawer

import android.content.Context
import android.content.SharedPreferences
import com.woocommerce.android.ui.woopos.cashmanagement.WooPosCashSession
import com.woocommerce.android.ui.woopos.cashmanagement.WooPosCapturedCashSession
import com.woocommerce.android.ui.woopos.cashmanagement.WooPosCashSessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID

class WooPosCashDrawerControllerTest {
    private val context: Context = mock()
    private val preferences: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private val sessions: WooPosCashSessionRepository = mock()
    private val hardware = FakeHardware()

    private fun controller(autoOpen: Boolean = true): WooPosCashDrawerController {
        whenever(context.getSharedPreferences("woo_pos_cash_drawer", Context.MODE_PRIVATE)).thenReturn(preferences)
        whenever(preferences.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(preferences.getString("drawer_name", null)).thenReturn("Front counter")
        whenever(preferences.getBoolean("auto_open", true)).thenReturn(autoOpen)
        return WooPosCashDrawerController(context, hardware, sessions)
    }

    @Test
    fun `opening float before session sends hardware command without Core event`() = runTest {
        val drawer = controller()

        val result = drawer.openBeforeSession()

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.OPEN_REQUESTED)
        assertThat(hardware.opens).isEqualTo(1)
        verifyNoInteractions(sessions)
    }

    @Test
    fun `manual no-sale without active session does not open drawer`() = runTest {
        whenever(sessions.currentWithSite()).thenReturn(null)
        val drawer = controller()

        val result = drawer.open(WooPosCashDrawerReason.NO_SALE)

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.NO_SESSION)
        assertThat(hardware.opens).isZero()
    }

    @Test
    fun `manual no-sale in unbound session opens without Core event`() = runTest {
        whenever(sessions.currentWithSite()).thenReturn(capture(drawerId = null))
        val drawer = controller()

        val result = drawer.open(WooPosCashDrawerReason.NO_SALE)

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.OPEN_REQUESTED)
        assertThat(hardware.opens).isEqualTo(1)
        verify(sessions, never()).drawerEvent(any(), any(), any(), anyOrNull(), anyOrNull(), any())
    }

    @Test
    fun `automatic open uses captured session even when drawer name changed`() = runTest {
        val drawer = controller()

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_SALE, 12L, "Old drawer", "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.OPEN_REQUESTED)
        assertThat(hardware.opens).isEqualTo(1)
        verify(sessions, never()).currentWithSite()
        verify(sessions).drawerEvent(eq(12L), eq("open_requested"), eq("cash_sale"), eq(99L), any(), eq(1))
    }

    @Test
    fun `automatic open in unbound session does not post Core event`() = runTest {
        val drawer = controller()

        val result = drawer.openAutomatically(WooPosCashDrawerReason.CASH_SALE, 12L, null, "Front counter", 99L, 1)

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.OPEN_REQUESTED)
        assertThat(hardware.opens).isEqualTo(1)
        verify(sessions, never()).drawerEvent(any(), any(), any(), anyOrNull(), anyOrNull(), any())
    }

    @Test
    fun `automatic open records request in captured bound session`() = runTest {
        val drawer = controller()

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_REFUND, 12L, "front COUNTER", "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.OPEN_REQUESTED)
        verify(sessions).drawerEvent(eq(12L), eq("open_requested"), eq("cash_refund"), eq(99L), any(), eq(1))
        verify(sessions, never()).currentWithSite()
    }

    @Test
    fun `automatic open disabled in settings does not command hardware`() = runTest {
        val drawer = controller(autoOpen = false)

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_SALE, 12L, "Front counter", "Front counter"
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.NO_SESSION)
        assertThat(hardware.opens).isZero()
        assertThat(drawer.automaticOpenFailure.value).isNull()
        verifyNoInteractions(sessions)
    }

    @Test
    fun `failed automatic open reports order for payment success`() = runTest {
        val drawer = controller()
        hardware.openError = WooPosPrinterNotConnectedException()

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_SALE, 12L, "Front counter", "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.NOT_CONNECTED)
        assertThat(drawer.automaticOpenFailure.value).isEqualTo(99L)
    }

    @Test
    fun `automatic open without session does not report failure`() = runTest {
        val drawer = controller()

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_SALE, null, null, "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.NO_SESSION)
        assertThat(drawer.automaticOpenFailure.value).isNull()
    }

    @Test
    fun `printer failure reports order for payment success`() = runTest {
        val drawer = controller()
        hardware.openError = IllegalStateException("Printer failed")

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_SALE, 12L, "Front counter", "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.FAILED)
        assertThat(drawer.automaticOpenFailure.value).isEqualTo(99L)
    }

    @Test
    fun `disconnected printer reports cash refund drawer failure`() = runTest {
        val drawer = controller()
        hardware.openError = WooPosPrinterNotConnectedException()

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_REFUND, 12L, "Front counter", "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.NOT_CONNECTED)
        assertThat(drawer.automaticRefundOpenFailure.value).isEqualTo(99L)
        assertThat(drawer.automaticOpenFailure.value).isNull()
    }

    @Test
    fun `printer command failure reports cash refund drawer failure`() = runTest {
        val drawer = controller()
        hardware.openError = IllegalStateException("Printer failed")

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_REFUND, 12L, "Front counter", "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.FAILED)
        assertThat(drawer.automaticRefundOpenFailure.value).isEqualTo(99L)
    }

    @Test
    fun `cash refund without session does not report drawer failure`() = runTest {
        val drawer = controller()

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_REFUND, null, null, "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.NO_SESSION)
        assertThat(drawer.automaticRefundOpenFailure.value).isNull()
    }

    @Test
    fun `disabled automatic cash refund open does not report drawer failure`() = runTest {
        val drawer = controller(autoOpen = false)

        val result = drawer.openAutomatically(
            WooPosCashDrawerReason.CASH_REFUND, 12L, "Front counter", "Front counter", 99L, 1
        )

        assertThat(result).isEqualTo(WooPosCashDrawerOpenResult.NO_SESSION)
        assertThat(drawer.automaticRefundOpenFailure.value).isNull()
        assertThat(hardware.opens).isZero()
    }

    @Test
    fun `sensor confirmation reuses request correlation in captured session`() = runTest {
        val drawer = controller()

        drawer.openAutomatically(WooPosCashDrawerReason.CASH_SALE, 12L, "Front counter", "Front counter", 99L, 1)
        drawer.handleDrawerSignal(true)

        val correlations = argumentCaptor<UUID>()
        verify(sessions).drawerEvent(eq(12L), eq("open_requested"), eq("cash_sale"), eq(99L), correlations.capture(), eq(1))
        verify(sessions).drawerEvent(eq(12L), eq("opened"), eq("cash_sale"), eq(99L), correlations.capture(), eq(1))
        assertThat(correlations.allValues).hasSize(2)
        assertThat(correlations.allValues[1]).isEqualTo(correlations.allValues[0])
    }

    @Test
    fun `manual sensor opening records unknown only in current bound session`() = runTest {
        whenever(sessions.currentWithSite()).thenReturn(capture(drawerId = "Old drawer"))
        val drawer = controller()

        drawer.openBeforeSession()
        drawer.handleDrawerSignal(false)
        drawer.handleDrawerSignal(true)
        drawer.handleDrawerSignal(false)

        verify(sessions).drawerEvent(eq(12L), eq("opened"), eq("unknown"), anyOrNull(), anyOrNull(), eq(1))
    }

    @Test
    fun `late sensor opening is not attributed to a newer session`() = runTest {
        whenever(sessions.currentWithSite()).thenReturn(capture(drawerId = "Front counter", createdAt = "2099-01-01T00:00:00"))
        val drawer = controller()

        drawer.openBeforeSession()
        drawer.handleDrawerSignal(false)
        drawer.handleDrawerSignal(true)
        drawer.handleDrawerSignal(false)

        verify(sessions, never()).drawerEvent(any(), any(), any(), anyOrNull(), anyOrNull(), any())
    }

    private fun session(drawerId: String?, createdAt: String = "2020-01-01T00:00:00"): WooPosCashSession = WooPosCashSession(
        id = 12L,
        deviceId = "device",
        drawerId = drawerId,
        status = "open",
        revision = 1,
        currency = "USD",
        currencyPrecision = 2,
        openingAmount = "0.00",
        cashSalesTotal = "0.00",
        cashRefundsTotal = "0.00",
        paidInTotal = "0.00",
        paidOutTotal = "0.00",
        expectedAmount = "0.00",
        countedAmount = null,
        variance = null,
        note = null,
        dateCreatedGmt = createdAt,
        dateClosedGmt = null,
        openedByName = "Cashier",
        closedByName = null,
    )

    private fun capture(drawerId: String?, createdAt: String = "2020-01-01T00:00:00") =
        WooPosCapturedCashSession(session(drawerId, createdAt), 1)

    private class FakeHardware : WooPosCashDrawerHardware {
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
        override val selectedPrinter: StateFlow<WooPosReceiptPrinter?> = MutableStateFlow(null)
        override val drawerSignals: SharedFlow<Boolean> = MutableSharedFlow()
        var opens = 0
        var openError: Exception? = null

        override suspend fun discover(): List<WooPosReceiptPrinter> = emptyList()
        override suspend fun connect(printer: WooPosReceiptPrinter) = Unit
        override suspend fun disconnect() = Unit
        override suspend fun open() {
            opens++
            openError?.let { throw it }
        }
        override suspend fun printCloseOut(text: String) = Unit
    }
}
