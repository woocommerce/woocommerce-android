package com.woocommerce.android.cardreader.internal.firmware.actions

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.ReaderSoftwareUpdateListener
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
internal class InstallSoftwareUpdateActionTest {
    private lateinit var action: InstallSoftwareUpdateAction
    private val terminal: TerminalWrapper = mock()
    private val updateData: ReaderSoftwareUpdate = mock()

    @Before
    fun setUp() {
        action = InstallSoftwareUpdateAction(terminal, mock())
    }

    @Test
    fun `when installation in progress, then progress values emitted`() = runBlockingTest {
        val progressValues = listOf(0.1f, 0.2f, 1f)
        whenever(terminal.installSoftwareUpdate(any(), any(), any())).thenAnswer {
            progressValues.forEach { progressValue ->
                (it.arguments[1] as ReaderSoftwareUpdateListener).onReportReaderSoftwareUpdateProgress(progressValue)
            }
            mock<Cancelable>()
        }

        val result = action.installUpdate(mock()).take(progressValues.size).toList()

        assertThat(result).isEqualTo(progressValues.map { Installing(it) })
    }

    @Test
    fun `when installation succeeds, then Success state emitted`() = runBlockingTest {
        whenever(terminal.installSoftwareUpdate(any(), any(), any())).thenAnswer {
            (it.arguments[2] as Callback).onSuccess()
            mock<Cancelable>()
        }

        val result = action.installUpdate(mock()).single()

        assertThat(result).isEqualTo(Success)
    }

    @Test
    fun `when installation fails, then Failed state emitted`() = runBlockingTest {
        whenever(terminal.installSoftwareUpdate(any(), any(), any())).thenAnswer {
            (it.arguments[2] as Callback).onFailure(mock())
            mock<Cancelable>()
        }

        val result = action.installUpdate(mock()).single()

        assertThat(result).isInstanceOf(Failed::class.java)
    }
}
