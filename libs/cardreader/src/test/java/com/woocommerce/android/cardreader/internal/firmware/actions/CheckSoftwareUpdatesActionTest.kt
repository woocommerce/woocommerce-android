package com.woocommerce.android.cardreader.internal.firmware.actions

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.ReaderSoftwareUpdateCallback
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates.Failed
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates.UpToDate
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates.UpdateAvailable
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class CheckSoftwareUpdatesActionTest {
    private lateinit var action: CheckSoftwareUpdatesAction
    private val terminal: TerminalWrapper = mock()
    private val updateData: ReaderSoftwareUpdate = mock()

    @Before
    fun setUp() {
        action = CheckSoftwareUpdatesAction(terminal, mock())
        whenever(updateData.hasConfigUpdate).thenReturn(false)
        whenever(updateData.hasFirmwareUpdate).thenReturn(false)
        whenever(updateData.hasKeyUpdate).thenReturn(false)
    }

    @Test
    fun `when config update available, then UpdateAvailable returned`() = runBlockingTest {
        whenever(updateData.hasConfigUpdate).thenReturn(true)
        whenever(terminal.checkForUpdate(any())).thenAnswer {
            (it.arguments[0] as ReaderSoftwareUpdateCallback).onSuccess(updateData)
            mock<Cancelable>()
        }

        val result = action.checkUpdates()

        assertThat(result).isEqualTo(UpdateAvailable(updateData))
    }

    @Test
    fun `when firmware update available, then UpdateAvailable returned`() = runBlockingTest {
        whenever(updateData.hasFirmwareUpdate).thenReturn(true)
        whenever(terminal.checkForUpdate(any())).thenAnswer {
            (it.arguments[0] as ReaderSoftwareUpdateCallback).onSuccess(updateData)
            mock<Cancelable>()
        }

        val result = action.checkUpdates()

        assertThat(result).isEqualTo(UpdateAvailable(updateData))
    }

    @Test
    fun `when key update available, then UpdateAvailable returned`() = runBlockingTest {
        whenever(updateData.hasKeyUpdate).thenReturn(true)
        whenever(terminal.checkForUpdate(any())).thenAnswer {
            (it.arguments[0] as ReaderSoftwareUpdateCallback).onSuccess(updateData)
            mock<Cancelable>()
        }

        val result = action.checkUpdates()

        assertThat(result).isEqualTo(UpdateAvailable(updateData))
    }

    @Test
    fun `when update not available, then UpToDate returned`() = runBlockingTest {
        whenever(updateData.hasConfigUpdate).thenReturn(false)
        whenever(updateData.hasFirmwareUpdate).thenReturn(false)
        whenever(updateData.hasKeyUpdate).thenReturn(false)
        whenever(terminal.checkForUpdate(any())).thenAnswer {
            (it.arguments[0] as ReaderSoftwareUpdateCallback).onSuccess(updateData)
            mock<Cancelable>()
        }

        val result = action.checkUpdates()

        assertThat(result).isEqualTo(UpToDate)
    }

    @Test
    fun `when checking for updates fails, then Failed returned`() = runBlockingTest {
        whenever(terminal.checkForUpdate(any())).thenAnswer {
            (it.arguments[0] as ReaderSoftwareUpdateCallback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.checkUpdates()

        assertThat(result).isInstanceOf(Failed::class.java)
    }
}
