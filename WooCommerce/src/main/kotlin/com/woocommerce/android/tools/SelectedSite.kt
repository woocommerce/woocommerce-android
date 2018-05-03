package com.woocommerce.android.tools

import android.content.Context
import android.preference.PreferenceManager
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

        val localSiteId = getInt(SELECTED_SITE_LOCAL_ID, -1)
        val siteModel = siteStore.getSiteByLocalId(localSiteId)
        siteModel?.let {
            selectedSite = it
            return it
        }

        throw IllegalStateException("SelectedSite was accessed before being initialized")
    }

    fun set(siteModel: SiteModel) {
        selectedSite = siteModel
        setInt(SELECTED_SITE_LOCAL_ID, siteModel.id)
    }

    private fun getInt(key: String, def: Int): Int {
        return try {
            val value = getPreferences().getString(key, "")
            if (value.isEmpty()) {
                def
            } else Integer.parseInt(value)
        } catch (e: NumberFormatException) {
            def
        }
    }

    private fun setInt(key: String, value: Int) {
        val editor = getPreferences().edit()
        if (value == 0) {
            editor.remove(key)
        } else {
            editor.putString(key, value.toString())
        }
        editor.apply()
    }

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)
}
