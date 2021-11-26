package com.woocommerce.android.tools

import android.content.Context
import androidx.preference.PreferenceManager
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.PreferenceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Singleton

/**
 * A wrapper for the currently active [SiteModel] for the app.
 * Persists and restores the selected site to/from the app preferences.
 */
@Singleton
class SelectedSite(
    private val context: Context,
    private val siteStore: SiteStore,
) {
    companion object {
        const val SELECTED_SITE_LOCAL_ID = "SELECTED_SITE_LOCAL_ID"

        fun getEventBus(): EventBus = EventBus.getDefault()
    }

    private val state: MutableStateFlow<SiteModel?> = MutableStateFlow(getSelectedSiteFromPersistance())

    fun observe(): Flow<SiteModel?> = state

    fun get(): SiteModel {
        state.value?.let { return it }

        getSelectedSiteFromPersistance()?.let {
            state.value = it
            return it
        }

        // if the selected site id is valid but the site isn't in the site store, reset the
        // preference. this can happen if the user has been removed from the active site.
        val localSiteId = getSelectedSiteId()
        if (localSiteId > -1) {
            getPreferences().edit().remove(SELECTED_SITE_LOCAL_ID).apply()
        }

        throw IllegalStateException(
            "SelectedSite.get() was accessed before being initialized - siteId $localSiteId." +
                "\nConsider calling selectedSite.exists() to ensure site exists prior to calling selectedSite.get()."
        )
    }

    fun set(siteModel: SiteModel) {
        state.value = siteModel
        PreferenceUtils.setInt(getPreferences(), SELECTED_SITE_LOCAL_ID, siteModel.id)

        AnalyticsTracker.refreshSiteMetadata(siteModel)

        // Notify listeners
        getEventBus().post(SelectedSiteChangedEvent(siteModel))
    }

    fun exists(): Boolean {
        val siteModel = siteStore.getSiteByLocalId(getSelectedSiteId())
        return siteModel != null
    }

    fun getIfExists(): SiteModel? = if (exists()) get() else null

    fun getSelectedSiteId() = PreferenceUtils.getInt(getPreferences(), SELECTED_SITE_LOCAL_ID, -1)

    fun reset() {
        state.value = null
        getPreferences().edit().remove(SELECTED_SITE_LOCAL_ID).apply()
    }

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getSelectedSiteFromPersistance(): SiteModel? {
        val localSiteId = getSelectedSiteId()
        return siteStore.getSiteByLocalId(localSiteId)
    }

    @Deprecated("Event bus is considered deprecated.", ReplaceWith("observe()"))
    class SelectedSiteChangedEvent(val site: SiteModel)
}
