package com.woocommerce.android.support

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.logs.LogEntry
import com.woocommerce.android.util.logs.LogFileWriter
import com.woocommerce.android.util.logs.WooFileLogger
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WooLogViewerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooFileLogger: WooFileLogger,
    private val deviceInfo: DeviceInfoWrapper
) : ScopedViewModel(savedState) {

    private val selectedLogFile = savedState.getNullableStateFlow(
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
        fun prepareFileDisplayName(file: File): String {
            val dateString = file.name
                .removePrefix(LogFileWriter.LOG_FILE_NAME_PREFIX)
                .substringBeforeLast(".")

            val date = runCatching {
                SimpleDateFormat(LogFileWriter.DATE_FORMAT_PATTERN, Locale.ROOT).parse(dateString)
            }.getOrNull()

            return date?.let {
                val dateDisplayFormatter = SimpleDateFormat.getDateInstance()
                dateDisplayFormatter.format(date)
            } ?: dateString
        }

        return UiState.LogFilesList(
            logFiles = wooFileLogger.getLogFiles().mapIndexed { index, file ->
                LogFile(
                    name = file.name,
                    displayName = if (index == 0) {
                        UiString.UiStringRes(R.string.logviewer_current_log_file)
                    } else {
                        UiString.UiStringText(prepareFileDisplayName(file))
                    },
                    isCurrent = index == 0
                )
            },
            onLogFileSelected = { selectedFile ->
                selectedLogFile.value = selectedFile
            }
        )
    }

    private suspend fun prepareLogFileContentState(logFile: LogFile): UiState.LogFileContent {
        fun getShareLogs(entries: List<LogEntry>): String {
            return (entries.takeLast(MAX_LOG_ENTRIES_TO_SHARE) + getDeviceInfo())
                .joinToString("\n")
        }

        if (logFile.isCurrent) {
            // When the current log file is selected, ensure the buffer is flushed to disk
            wooFileLogger.forceFlush()
        }
        val logEntries = wooFileLogger.getLogFileContent(logFile.name).orEmpty()
        return UiState.LogFileContent(
            logFile = logFile,
            logEntries = logEntries,
            onBackPressed = { selectedLogFile.value = null },
            onShareClicked = { triggerEvent(ShareLogs(logs = getShareLogs(logEntries))) },
            onCopyClicked = { triggerEvent(CopyLogs(logs = getShareLogs(logEntries))) }
        )
    }

    private fun getDeviceInfo(): LogEntry {
        return with(deviceInfo) {
            LogEntry(
                WooLog.T.DEVICE,
                WooLog.LogLevel.w,
                "OS: $osName\nDeviceName: $name\nLanguage: $locale"
            )
        }
    }

    sealed interface UiState {
        data class LogFilesList(
            val logFiles: List<LogFile>,
            val onLogFileSelected: (LogFile) -> Unit
        ) : UiState

        data class LogFileContent(
            val logFile: LogFile,
            val logEntries: List<LogEntry>,
            val onBackPressed: () -> Unit,
            val onShareClicked: () -> Unit,
            val onCopyClicked: () -> Unit
        ) : UiState
    }

    @Parcelize
    data class LogFile(
        val name: String,
        val displayName: UiString,
        val isCurrent: Boolean = false
    ) : Parcelable

    data class ShareLogs(val logs: String) : MultiLiveEvent.Event()
    data class CopyLogs(val logs: String) : MultiLiveEvent.Event()

    companion object {
        // If we try to share very large number of entries, Android might throw TransactionTooLargeException
        // when trying to send the intent.
        private const val MAX_LOG_ENTRIES_TO_SHARE = 1000
    }
}
