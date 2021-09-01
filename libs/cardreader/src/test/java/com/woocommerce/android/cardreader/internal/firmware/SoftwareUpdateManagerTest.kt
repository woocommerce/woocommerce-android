package com.woocommerce.android.cardreader.internal.firmware

import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction
import com.woocommerce.android.cardreader.internal.firmware.actions.CheckSoftwareUpdatesAction.CheckSoftwareUpdates
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.internal.firmware.actions.InstallAvailableSoftwareUpdateAction.InstallSoftwareUpdateStatus.Success
import kotlinx.coroutines.flow.flow
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
            .thenAnswer { flow<InstallSoftwareUpdateStatus> { emit(Success) } }
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
    fun `when update available, then installation is started`() = runBlockingTest {
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
}
