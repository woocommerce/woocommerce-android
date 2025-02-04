package org.wordpress.android.fluxc.model.google

import org.wordpress.android.util.AppLog

data class WCGoogleAdsCampaign(
    val id: Long?,
    val name: String?,
    val status: Status?,
    val type: String?,
    val amount: Double?,
    val countryISOCode: String?,
    val targetedCountryISOCodes: List<String>?
) {
    enum class Status {
        ENABLED,
        DISABLED,
        REMOVED;

        companion object {
            fun fromString(value: String): Status {
                return try {
                    Status.valueOf(value.uppercase())
                } catch (e: IllegalArgumentException) {
                    AppLog.w(
                        AppLog.T.API,
                        "Unknown campaign status returned: `$value`, defaulting to DISABLED " +
                            e.message
                    )
                    DISABLED
                }
            }
        }
    }
}
