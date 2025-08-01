package com.woocommerce.android.support

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.util.logs.LogEntry
import com.woocommerce.android.util.logs.WooFileLogger
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
                    displayName = if (index == 0) "Current" else file.name
                )
            },
            onLogFileSelected = { selectedFile ->
                selectedLogFile.value = selectedFile
            }
        )
    }

    private suspend fun prepareLogFileContentState(logFile: LogFile) =
        UiState.LogFileContent(
            logFile = logFile,
            logContent = wooFileLogger.getLogFileContent(logFile.name).orEmpty(),
            onBackPressed = {
                selectedLogFile.value = null
            }
        )

    sealed interface UiState {
        data class LogFilesList(
            val logFiles: List<LogFile>,
            val onLogFileSelected: (LogFile) -> Unit
        ) : UiState

        data class LogFileContent(
            val logFile: LogFile,
            val logContent: List<LogEntry>,
            val onBackPressed: () -> Unit
        ) : UiState
    }

    @Parcelize
    data class LogFile(
        val name: String,
        val displayName: String
    ) : Parcelable
}
