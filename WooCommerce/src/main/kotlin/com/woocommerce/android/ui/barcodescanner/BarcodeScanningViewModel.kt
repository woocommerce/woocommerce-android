package com.woocommerce.android.ui.barcodescanner

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.StringRes
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.GoogleMLKitCodeScanner
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeScanningViewModel @Inject constructor(
    private val codeScanner: GoogleMLKitCodeScanner,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    private var frameChannel = createChannel()

    private var processingJob: Job? = null

    init {
        _permissionState.value = PermissionState.Unknown
    }

    fun startCodesRecognition() {
        frameChannel = createChannel()
        processingJob = launch {
            for (frame in frameChannel) {
                codeScanner.recogniseCode(frame).let { status ->
                    when (status) {
                        is CodeScannerStatus.Success -> {
                            triggerEvent(ScanningEvents.OnScanningResult(status))
                        }

                        is CodeScannerStatus.Failure -> {
                            triggerEvent(ScanningEvents.OnScanningResult(status))
                        }

                        CodeScannerStatus.NotFound -> {
                            // do nothing
                        }
                    }
                }
            }
        }
    }

    fun stopCodesRecognition() {
        processingJob?.cancel()
        frameChannel.close()
    }

    fun updatePermissionState(
        isPermissionGranted: Boolean,
        shouldShowRequestPermissionRationale: Boolean,
    ) {
        when {
            isPermissionGranted -> {
                // display scanning screen
                _permissionState.value = PermissionState.Granted
            }

            shouldShowRequestPermissionRationale -> {
                // Denied once, ask to grant camera permission
                _permissionState.value = PermissionState.ShouldShowRationale(
                    title = R.string.barcode_scanning_alert_dialog_title,
                    message = R.string.barcode_scanning_alert_dialog_rationale_message,
                    ctaLabel = R.string.barcode_scanning_alert_dialog_rationale_cta_label,
                    dismissCtaLabel = R.string.barcode_scanning_alert_dialog_dismiss_label,
                    ctaAction = { triggerEvent(ScanningEvents.LaunchCameraPermission(it)) },
                    dismissCtaAction = {
                        triggerEvent(Event.Exit)
                    }
                )
            }

            else -> {
                // Permanently denied, ask to enable permission from the app settings
                _permissionState.value = PermissionState.PermanentlyDenied(
                    title = R.string.barcode_scanning_alert_dialog_title,
                    message = R.string.barcode_scanning_alert_dialog_permanently_denied_message,
                    ctaLabel = R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label,
                    dismissCtaLabel = R.string.barcode_scanning_alert_dialog_dismiss_label,
                    ctaAction = {
                        triggerEvent(ScanningEvents.OpenAppSettings(it))
                    },
                    dismissCtaAction = {
                        triggerEvent(Event.Exit)
                    }
                )
            }
        }
    }

    fun onNewFrame(imageProxy: ImageProxy) {
        @OptIn(DelicateCoroutinesApi::class)
        if (!frameChannel.isClosedForSend) {
            frameChannel.trySend(imageProxy)
        }
    }

    fun onBindingException(exception: Exception) {
        triggerEvent(
            ScanningEvents.OnScanningResult(
                CodeScannerStatus.Failure(
                    error = exception.message,
                    type = CodeScanningErrorType.Other(exception)
                )
            )
        )
        stopCodesRecognition()
    }

    private fun createChannel() = Channel<ImageProxy>(
        capacity = BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun onCleared() {
        stopCodesRecognition()
    }

    sealed class ScanningEvents : Event() {
        data class LaunchCameraPermission(
            val cameraLauncher: ManagedActivityResultLauncher<String, Boolean>
        ) : ScanningEvents()

        data class OpenAppSettings(
            val cameraLauncher: ManagedActivityResultLauncher<String, Boolean>
        ) : ScanningEvents()

        data class OnScanningResult(
            val status: CodeScannerStatus
        ) : ScanningEvents()
    }

    sealed class PermissionState {
        object Granted : PermissionState()

        data class ShouldShowRationale(
            @StringRes val title: Int,
            @StringRes val message: Int,
            @StringRes val ctaLabel: Int,
            @StringRes val dismissCtaLabel: Int,
            val ctaAction: (ManagedActivityResultLauncher<String, Boolean>) -> Unit,
            val dismissCtaAction: () -> Unit,
        ) : PermissionState()

        data class PermanentlyDenied(
            @StringRes val title: Int,
            @StringRes val message: Int,
            @StringRes val ctaLabel: Int,
            @StringRes val dismissCtaLabel: Int,
            val ctaAction: (ManagedActivityResultLauncher<String, Boolean>) -> Unit,
            val dismissCtaAction: () -> Unit,
        ) : PermissionState()

        object Unknown : PermissionState()
    }

    private companion object {
        private const val BUFFER_SIZE = 30
    }
}
