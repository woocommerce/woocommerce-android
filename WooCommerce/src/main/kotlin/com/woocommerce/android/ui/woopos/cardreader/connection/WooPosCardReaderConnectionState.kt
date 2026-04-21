package com.woocommerce.android.ui.woopos.cardreader.connection

sealed interface WooPosCardReaderConnectionState {
    val showCloseButton: Boolean
        get() = true

    data class FoundReader(
        val id: String,
        val name: String,
        val onConnectClicked: () -> Unit,
    )

    data class Scanning(
        val isRemoteTapToPaySupported: Boolean,
    ) : WooPosCardReaderConnectionState

    data class ScanningFailed(
        val errorMessage: String,
        val onRetryClicked: () -> Unit,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class BluetoothDisabled(
        val onEnableBluetoothClicked: () -> Unit,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class LocationDisabled(
        val onEnableLocationClicked: () -> Unit,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class MissingLocationPermission(
        val onRequestPermissionClicked: () -> Unit,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class MissingBluetoothPermission(
        val onRequestPermissionClicked: () -> Unit,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class ReaderFound(
        val reader: FoundReader,
        val onKeepSearchingClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = true
    }

    data class MultipleReadersFound(
        val readers: List<FoundReader>,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = true
    }

    data object Connecting : WooPosCardReaderConnectionState

    data class ConnectingFailed(
        val errorMessage: String,
        val onRetryClicked: () -> Unit,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class ConnectingFailedBatteryLow(
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class InvalidMerchantAddress(
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class InvalidPostalCode(
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class Connected(
        val readerName: String,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class UpdateRequired(
        val progress: Float,
        val showCancelWarning: Boolean,
        val onCancelClicked: () -> Unit,
        val onBackClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class UpdateAvailable(
        val progress: Float,
        val showCancelWarning: Boolean,
        val onCancelClicked: () -> Unit,
        val onBackClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data object UpdateCompleted : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class UpdateFailed(
        val errorMessage: String,
        val onRetryClicked: () -> Unit,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class UpdateFailedBatteryLow(
        val currentBatteryLevel: Float?,
        val onCancelClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false
    }

    data class OnboardingError(
        val title: String,
        val message: String,
        val primaryButton: PrimaryButton?,
        val onDismissClicked: () -> Unit,
    ) : WooPosCardReaderConnectionState {
        override val showCloseButton: Boolean = false

        data class PrimaryButton(
            val label: String,
            val onClick: () -> Unit,
        )
    }
}
