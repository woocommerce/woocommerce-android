package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import androidx.core.content.edit
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class JetpackApplicationPasswordsSupport @Inject constructor(context: Context) {
    private val fluxCPreferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    fun supportsAppPasswords(siteModel: SiteModel): Boolean {
        return siteModel.isApplicationPasswordsSupported &&
            fluxCPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null)
                ?.contains(siteModel.siteId.toString()) != true
    }

    fun flagAsUnsupported(siteModel: SiteModel) {
        val unsupportedSites = fluxCPreferences.getStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, null) ?: setOf()
        fluxCPreferences.edit {
            putStringSet(UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES, unsupportedSites + siteModel.siteId.toString())
        }
    }

    companion object {
        private const val UNSUPPORTED_JETPACK_APP_PASSWORDS_SITES = "unsupported_jetpack_app_passwords_sites"
    }
}
