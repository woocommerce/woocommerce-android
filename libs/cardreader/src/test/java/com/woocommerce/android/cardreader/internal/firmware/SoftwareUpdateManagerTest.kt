package com.woocommerce.android.cardreader.internal.firmware

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.model.external.ReaderSoftwareUpdate
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallSoftwareUpdateAction.InstallSoftwareUpdateStatus.Success
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SoftwareUpdateManagerTest {
    private lateinit var updateManager: SoftwareUpdateManager
    private val checkUpdatesAction: CheckSoftwareUpdatesAction = mock()
    private val installSoftwareUpdatesAction: InstallSoftwareUpdateAction = mock()

    @Before
    fun setUp() = runBlockingTest {
        updateManager = SoftwareUpdateManager(checkUpdatesAction, installSoftwareUpdatesAction)

        whenever(checkUpdatesAction.checkUpdates())
            .thenReturn(CheckSoftwareUpdates.UpdateAvailable(mock()))
        whenever(installSoftwareUpdatesAction.installUpdate(any()))
            .thenAnswer {
                flow<InstallSoftwareUpdateStatus> {}
            }
    }

    @Test
    fun `when check for udpate started, then Initializing emitted`() = runBlockingTest {
        val result = updateManager.updateSoftware().single()

        assertThat(result).isEqualTo(SoftwareUpdateStatus.Initializing)
    }

    @Test
    fun `when update not available, then UpToDate emitted`() = runBlockingTest {
        whenever(checkUpdatesAction.checkUpdates()).thenReturn(CheckSoftwareUpdates.UpToDate)

        val result = updateManager.updateSoftware().toList().last()

        assertThat(result).isEqualTo(SoftwareUpdateStatus.UpToDate)
    }

    @Test
    fun `when check for updates fails, then CheckForUpdatesFailed emitted`() = runBlockingTest {
        val message = "error"
        val terminalException: TerminalException = mock {
            on { errorMessage }.thenReturn(message)
        }
        whenever(checkUpdatesAction.checkUpdates()).thenReturn(CheckSoftwareUpdates.Failed(terminalException))

        val result = updateManager.updateSoftware().toList().last()

        assertThat(result).isEqualTo(SoftwareUpdateStatus.Failed(message))
    }

    @Test
    fun `when udpate available, then installation is started`() = runBlockingTest {
        whenever(checkUpdatesAction.checkUpdates())
            .thenReturn(CheckSoftwareUpdates.UpdateAvailable(mock()))

        updateManager.updateSoftware().toList().last()

        verify(installSoftwareUpdatesAction).installUpdate(any())
    }

    @Test
    fun `when installation progresses, then Installing state with progress emitted`() = runBlockingTest {
        whenever(installSoftwareUpdatesAction.installUpdate(any())).thenAnswer {
            flow<InstallSoftwareUpdateStatus> {
                emit(Installing(0.1f))
            }
        }

        val result = updateManager.updateSoftware().toList().last()

        assertThat(result).isEqualTo(SoftwareUpdateStatus.Installing(0.1f))
    }

    @Test
    fun `when installation succeeds, then Success state emitted`() = runBlockingTest {
        whenever(installSoftwareUpdatesAction.installUpdate(any())).thenAnswer {
            flow<InstallSoftwareUpdateStatus> {
                emit(Success)
            }
        }

        val result = updateManager.updateSoftware().toList().last()

        assertThat(result).isEqualTo(SoftwareUpdateStatus.Success)
    }

    @Test
    fun `when installation fails, then Failed state emitted`() = runBlockingTest {
        val terminalException = mock<TerminalException>().also {
            whenever(it.errorMessage).thenReturn("dummy message")
        }
        whenever(installSoftwareUpdatesAction.installUpdate(any())).thenAnswer {
            flow<InstallSoftwareUpdateStatus> {
                emit(Failed(terminalException))
            }
        }

        val result = updateManager.updateSoftware().toList().last()

        assertThat(result).isEqualTo(SoftwareUpdateStatus.Failed("dummy message"))
    }

    @Test
    fun `when software update check starts then initializing emitted`() = runBlockingTest {
        // GIVEN
        whenever(checkUpdatesAction.checkUpdates()).thenReturn(CheckSoftwareUpdates.UpToDate)

        // WHEN
        val status = updateManager.softwareUpdateStatus().toList().first()

        // THEN
        assertThat(status).isEqualTo(SoftwareUpdateAvailability.Initializing)
    }

    @Test
    fun `when software update check returns up to date then uptodate emitted`() = runBlockingTest {
        // GIVEN
        whenever(checkUpdatesAction.checkUpdates()).thenReturn(CheckSoftwareUpdates.UpToDate)

        // WHEN
        val status = updateManager.softwareUpdateStatus().toList().last()

        // THEN
        assertThat(status).isEqualTo(SoftwareUpdateAvailability.UpToDate)
    }

    @Test
    fun `when software update check returns update available then updateavailable emitted`() = runBlockingTest {
        // GIVEN
        val updateData: ReaderSoftwareUpdate = mock()
        val updateStatus = CheckSoftwareUpdates.UpdateAvailable(updateData)
        whenever(checkUpdatesAction.checkUpdates()).thenReturn(updateStatus)

        // WHEN
        val status = updateManager.softwareUpdateStatus().toList().last()

        // THEN
        assertThat(status).isInstanceOf(SoftwareUpdateAvailability.UpdateAvailable::class.java)
    }

    @Test
    fun `when software update check returns failed then check failed emitted`() = runBlockingTest {
        // GIVEN
        whenever(checkUpdatesAction.checkUpdates()).thenReturn(CheckSoftwareUpdates.Failed(mock()))

        // WHEN
        val status = updateManager.softwareUpdateStatus().toList().last()

        // THEN
        assertThat(status).isEqualTo(SoftwareUpdateAvailability.CheckForUpdatesFailed)
    }
}
