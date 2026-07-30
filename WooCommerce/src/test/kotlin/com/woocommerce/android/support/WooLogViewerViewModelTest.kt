package com.woocommerce.android.support

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.logs.WooFileLogger
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class WooLogViewerViewModelTest : BaseUnitTest() {
    private val wooFileLogger: WooFileLogger = mock()
    private val deviceInfo: DeviceInfoWrapper = mock {
        on { osName } doReturn "15"
        on { name } doReturn "Google Pixel 9"
        on { locale } doReturn "English"
    }

    private lateinit var viewModel: WooLogViewerViewModel

    private suspend fun setup(logFiles: List<File> = listOf(File("log_2026-07-29.txt"))) {
        whenever(wooFileLogger.getLogFiles()).thenReturn(logFiles)
        viewModel = WooLogViewerViewModel(SavedStateHandle(), wooFileLogger, deviceInfo)
    }

    private fun filesListState() =
        viewModel.uiState.captureValues().last() as WooLogViewerViewModel.UiState.LogFilesList

    @Test
    fun `when share all is clicked, then the archive is shared`() = testBlocking {
        setup()
        val archive = File("woocommerce-logs.zip")
        whenever(wooFileLogger.archiveLogFiles(any())).thenReturn(archive)
        val state = filesListState()

        state.onShareAllClicked()
        advanceUntilIdle()

        assertThat(viewModel.event.captureValues().last()).isEqualTo(WooLogViewerViewModel.ShareLogsArchive(archive))
    }

    @Test
    fun `given archiving fails, when share all is clicked, then an error is shown`() = testBlocking {
        setup()
        whenever(wooFileLogger.archiveLogFiles(any())).thenReturn(null)
        val state = filesListState()

        state.onShareAllClicked()
        advanceUntilIdle()

        assertThat(viewModel.event.captureValues().last())
            .isEqualTo(WooLogViewerViewModel.ShareLogsArchiveFailed)
    }

    @Test
    fun `when share all is clicked, then the device info is included in the archive`() = testBlocking {
        setup()
        whenever(wooFileLogger.archiveLogFiles(any())).thenReturn(File("woocommerce-logs.zip"))

        filesListState().onShareAllClicked()
        advanceUntilIdle()

        val deviceInfo = argumentCaptor<String>()
        verify(wooFileLogger).archiveLogFiles(deviceInfo.capture())
        assertThat(deviceInfo.firstValue).contains("OS:")
    }
}
