package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class JetpackApplicationPasswordsSupport @Inject constructor(
    context: Context,
    private val appLog: AppLogWrapper
) {
    private val fluxCPreferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    fun supportsAppPasswords(siteModel: SiteModel): Boolean {
        val siteFlag = fluxCPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
            ?.firstOrNull { it.startsWith("${siteModel.siteId}:") }

        if (siteFlag != null) {
            val flagTimestamp = siteFlag.substringAfter(":").toLongOrNull() ?: 0L
            if (System.currentTimeMillis() - flagTimestamp < FLAG_REFRESH_DURATION_DAYS.days.inWholeMilliseconds) {
                return false
            } else {
                appLog.d(
                    AppLog.T.API,
                    "Clearing Jetpack Application Passwords unsupported flag for site ${siteModel.siteId} after refresh period"
                )
                clearFlag(siteModel)
            }
        }

        return siteModel.isApplicationPasswordsSupported
    }

    fun flagAsUnsupported(siteModel: SiteModel) {
        val unsupportedSites = fluxCPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null) ?: setOf()
        if (unsupportedSites.any { it.startsWith("${siteModel.siteId}:") }) {
            // Already flagged
            return
        }
        fluxCPreferences.edit {
            putStringSet(
                UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES,
                unsupportedSites + "${siteModel.siteId}:${System.currentTimeMillis()}"
            )
        }
    }

    private fun clearFlag(siteModel: SiteModel) {
        val unsupportedSites = fluxCPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null) ?: setOf()
        val updatedSites = unsupportedSites.filterNot { it.startsWith("${siteModel.siteId}:") }.toSet()
        fluxCPreferences.edit {
            putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, updatedSites)
        }
    }

    companion object {
        @VisibleForTesting
        internal const val FLAG_REFRESH_DURATION_DAYS = 7

        @VisibleForTesting
        internal const val UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES = "unsupported_jetpack_app_passwords_sites"
    }
}
