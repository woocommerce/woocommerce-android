package com.woocommerce.android.cardreader.internal.firmware

import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.firmware.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.firmware.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Success
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SoftwareUpdateManagerTest {
    private lateinit var updateManager: SoftwareUpdateManager
    private val checkUpdatesAction: CheckSoftwareUpdatesAction = mock()
    private val installAvailableSoftwareUpdatesAction: InstallAvailableSoftwareUpdateAction = mock()

    @Before
    fun setUp() = runBlockingTest {
        updateManager = SoftwareUpdateManager(checkUpdatesAction, installAvailableSoftwareUpdatesAction)

        whenever(checkUpdatesAction.checkUpdates())
            .thenReturn(CheckSoftwareUpdates.UpdateAvailable(mock()))
        whenever(installAvailableSoftwareUpdatesAction.installUpdate())
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

        verify(installAvailableSoftwareUpdatesAction).installUpdate()
    }

    @Test
    fun `when installation progresses, then Installing state with progress emitted`() = runBlockingTest {
        whenever(installAvailableSoftwareUpdatesAction.installUpdate()).thenAnswer {
            flow<InstallSoftwareUpdateStatus> {
                emit(Installing(0.1f))
            }
        }

        val result = updateManager.updateSoftware().toList().last()

        assertThat(result).isEqualTo(SoftwareUpdateStatus.Installing(0.1f))
    }

    @Test
    fun `when installation succeeds, then Success state emitted`() = runBlockingTest {
        whenever(installAvailableSoftwareUpdatesAction.installUpdate()).thenAnswer {
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
        whenever(installAvailableSoftwareUpdatesAction.installUpdate()).thenAnswer {
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
