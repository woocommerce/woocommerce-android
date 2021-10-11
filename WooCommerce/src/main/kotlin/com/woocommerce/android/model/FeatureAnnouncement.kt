package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.utils.WhatsNewAppVersionUtils

@Parcelize
data class FeatureAnnouncement(
    val appVersionName: String,
    val announcementVersion: Int,
    val minimumAppVersion: String,
    val maximumAppVersion: String,
    val appVersionTargets: List<String>,
    val detailsUrl: String?,
    val isLocalized: Boolean = false,
    val features: List<FeatureAnnouncementItem>
) : Parcelable {
    companion object {
        const val openEndedVersionBracketIndicator = "-1.0"
    }

    // An announcement is valid and can be shown if:
    // - It is already localized, and
    // - Its features are not empty, and
    // - Current app version is included in the `appVersionTargets` list, or current app version is within the range
    //   of `minimumAppVersion` and `maximumAppVersion`
    fun canBeDisplayedOnAppUpgrade(appVersionName: String): Boolean {
        if (!isLocalized || features.isEmpty()) return false

        return if (appVersionTargets.contains(appVersionName)) {
            true
        } else {
            val integerRepresentationOfVersionName = WhatsNewAppVersionUtils.versionNameToInt(appVersionName)
            if (integerRepresentationOfVersionName == -1) {
                false
            } else {
                val minAppVersion = WhatsNewAppVersionUtils.versionNameToInt(minimumAppVersion)
                val maxAppVersion = WhatsNewAppVersionUtils.versionNameToInt(maximumAppVersion)

                when {
                    minimumAppVersion == openEndedVersionBracketIndicator -> {
                        integerRepresentationOfVersionName <= maxAppVersion
                    }
                    maximumAppVersion == openEndedVersionBracketIndicator -> {
                        integerRepresentationOfVersionName >= minAppVersion
                    }
                    else -> {
                        IntRange(
                            minAppVersion,
                            maxAppVersion
                        ).contains(integerRepresentationOfVersionName)
                    }
                }
            }
        }
    }
}

@Parcelize
data class FeatureAnnouncementItem(
    val title: String,
    val subtitle: String,
    val iconBase64: String,
    val iconUrl: String
) : Parcelable
