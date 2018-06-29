package com.woocommerce.android.tools

import android.content.Context
import android.preference.PreferenceManager
import com.woocommerce.android.util.PreferenceUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Singleton

/**
 * A wrapper for the currently active [SiteModel] for the app.
 * Persists and restores the selected site to/from the app preferences.
 */
@Singleton
class SelectedSite(private var context: Context, private var siteStore: SiteStore) {
    companion object {
        const val SELECTED_SITE_LOCAL_ID = "SELECTED_SITE_LOCAL_ID"
    }

    private var selectedSite: SiteModel? = null

    fun get(): SiteModel {
        selectedSite?.let { return it }

        val localSiteId = PreferenceUtils.getInt(getPreferences(), SELECTED_SITE_LOCAL_ID, -1)
        val siteModel = siteStore.getSiteByLocalId(localSiteId)
        siteModel?.let {
            selectedSite = it
            return it
        }

        throw IllegalStateException("SelectedSite was accessed before being initialized")
    }

    fun set(siteModel: SiteModel) {
        selectedSite = siteModel
        PreferenceUtils.setInt(getPreferences(), SELECTED_SITE_LOCAL_ID, siteModel.id)
    }

    fun isSet() = PreferenceUtils.getInt(getPreferences(), SELECTED_SITE_LOCAL_ID, -1) != -1

    fun reset() {
        selectedSite = null
        getPreferences().edit().remove(SELECTED_SITE_LOCAL_ID).apply()
    }

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)
}
