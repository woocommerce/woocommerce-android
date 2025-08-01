package com.woocommerce.android.support

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.DeviceInfo
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.logs.LogEntry
import com.woocommerce.android.util.logs.WooFileLogger
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooLogViewerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooFileLogger: WooFileLogger
) : ScopedViewModel(savedState) {

    private val selectedLogFile = savedState.getNullableStateFlow<LogFile?>(
        viewModelScope,
        null,
        LogFile::class.java,
        "selected_log_file"
    )

    val uiState = selectedLogFile.map { logFile ->
        if (logFile == null) {
            prepareFilesListState()
        } else {
            prepareLogFileContentState(logFile)
        }
    }.asLiveData()

    private suspend fun prepareFilesListState(): UiState.LogFilesList {
        return UiState.LogFilesList(
            logFiles = wooFileLogger.getLogFiles().mapIndexed { index, file ->
                LogFile(
                    name = file.name,
                    displayName = if (index == 0) {
                        UiString.UiStringRes(R.string.logviewer_current_log_file)
                    } else {
                        UiString.UiStringText(file.name)
                    }
                )
            },
            onLogFileSelected = { selectedFile ->
                selectedLogFile.value = selectedFile
            }
        )
    }

    private suspend fun prepareLogFileContentState(logFile: LogFile): UiState.LogFileContent {
        val logContent = wooFileLogger.getLogFileContent(logFile.name).orEmpty()
        return UiState.LogFileContent(
            logFile = logFile,
            logContent = logContent,
            onBackPressed = {
                selectedLogFile.value = null
            },
            onShareClicked = {
                triggerEvent(
                    ShareLogs(
                        logs = (logContent + getDeviceInfo()).joinToString("\n")
                    )
                )
            },
            onCopyClicked = {
                triggerEvent(
                    CopyLogs(
                        logs = (logContent + getDeviceInfo()).joinToString("\n")
                    )
                )
            }
        )
    }

    private fun getDeviceInfo(): LogEntry {
        return with(DeviceInfo) {
            LogEntry(WooLog.T.DEVICE, WooLog.LogLevel.w, "OS: ${OS}\nDeviceName: ${name}\nLanguage: $locale")
        }
    }

    sealed interface UiState {
        data class LogFilesList(
            val logFiles: List<LogFile>,
            val onLogFileSelected: (LogFile) -> Unit
        ) : UiState

        data class LogFileContent(
            val logFile: LogFile,
            val logContent: List<LogEntry>,
            val onBackPressed: () -> Unit,
            val onShareClicked: () -> Unit,
            val onCopyClicked: () -> Unit
        ) : UiState
    }

    @Parcelize
    data class LogFile(
        val name: String,
        val displayName: UiString
    ) : Parcelable

    data class ShareLogs(val logs: String) : MultiLiveEvent.Event()
    data class CopyLogs(val logs: String) : MultiLiveEvent.Event()
}
