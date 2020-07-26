package com.woocommerce.android.tools

import android.content.Context
import androidx.preference.PreferenceManager
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.PreferenceUtils
import org.greenrobot.eventbus.EventBus
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

        fun getEventBus() = EventBus.getDefault()
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

        throw IllegalStateException("SelectedSite.get() was accessed before being initialized - siteId $localSiteId." +
                "\nConsider calling selectedSite.exists() to ensure site exists prior to calling selectedSite.get().")
    }

    fun set(siteModel: SiteModel) {
        selectedSite = siteModel
        PreferenceUtils.setInt(getPreferences(), SELECTED_SITE_LOCAL_ID, siteModel.id)

        AnalyticsTracker.refreshSiteMetadata(siteModel)

        // Notify listeners
        getEventBus().post(SelectedSiteChangedEvent(siteModel))
    }

    fun exists(): Boolean {
        val localSiteId = PreferenceUtils.getInt(getPreferences(), SELECTED_SITE_LOCAL_ID, -1)
        val siteModel = siteStore.getSiteByLocalId(localSiteId)
        return siteModel != null
    }

    fun getIfExists(): SiteModel? = if (exists()) get() else null

    fun reset() {
        selectedSite = null
        getPreferences().edit().remove(SELECTED_SITE_LOCAL_ID).apply()
    }

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    class SelectedSiteChangedEvent(val site: SiteModel)
}
