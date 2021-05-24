package com.woocommerce.android.cardreader

sealed class SoftwareUpdateAvailability {
    object Initializing : SoftwareUpdateAvailability()
    object UpToDate : SoftwareUpdateAvailability()
    data class UpdateAvailable(
        val hasConfigUpdate: Boolean,
        val hasFirmwareUpdate: Boolean,
        val hasKeyUpdate: Boolean,
        val timeEstimate: TimeEstimate,
        val version: String
    ) : SoftwareUpdateAvailability() {
        enum class TimeEstimate {
            LESS_THAN_ONE_MINUTE,
            ONE_TO_TWO_MINUTES,
            TWO_TO_FIVE_MINUTES,
            FIVE_TO_FIFTEEN_MINUTES
        }
    }
    object CheckForUpdatesFailed : SoftwareUpdateAvailability()
}
