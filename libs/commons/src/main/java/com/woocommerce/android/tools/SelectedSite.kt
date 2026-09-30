package com.woocommerce.android.tools

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.woocommerce.commons.prefs.PreferenceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A wrapper for the currently active [SiteModel] for the app.
 * Persists and restores the selected site to/from the app preferences.
 */
@Singleton
class SelectedSite @Inject constructor(
    private val context: Context,
    private val siteStore: SiteStore
) {
    companion object {
        const val SELECTED_SITE_LOCAL_ID = "SELECTED_SITE_LOCAL_ID"

        fun getEventBus(): EventBus = EventBus.getDefault()

        private fun getSelectedSiteId(context: Context): Int = PreferenceUtils.getInt(
            PreferenceManager.getDefaultSharedPreferences(context),
            SELECTED_SITE_LOCAL_ID,
            -1
        )

        fun hasSelectedSiteId(context: Context): Boolean = getSelectedSiteId(context) != -1
    }

    private val state: MutableStateFlow<SiteModel?> = MutableStateFlow(getSelectedSiteFromPersistence())
    private var wasReset = false

    val connectionType: SiteConnectionType?
        get() = getIfExists()?.connectionType

    fun observe(): Flow<SiteModel?> = state

    @Suppress("SwallowedException")
    fun getOrNull(): SiteModel? =
        try {
            get()
        } catch (e: SelectedSiteException) {
            null
        }

    @Throws(SelectedSiteException::class)
    fun get(): SiteModel {
        state.value?.let { return it }

        synchronized(this) {
            getSelectedSiteFromPersistence()?.let {
                state.value = it
                return it
            }

            // if the selected site id is valid but the site isn't in the site store, reset the
            // preference. this can happen if the user has been removed from the active site.
            val localSiteId = getSelectedSiteId()
            if (localSiteId > -1) {
                getPreferences().edit().remove(SELECTED_SITE_LOCAL_ID).apply()
            }

            if (wasReset) {
                throw SelectedSiteResetException()
            } else {
                throw SelectedSiteUninitializedException(localSiteId)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Synchronized
    fun set(siteModel: SiteModel) {
        wasReset = false
        state.value = siteModel
        PreferenceUtils.setInt(getPreferences(), SELECTED_SITE_LOCAL_ID, siteModel.id)

        // Notify listeners
        getEventBus().post(SelectedSiteChangedEvent(siteModel))
    }

    /**
     * Clears the selected site.
     *
     * @param persistSynchronously when true the preference is written with a blocking [commit] instead
     * of [apply]. Recovery flows restart (and may kill) the process right after resetting, and an async
     * [apply] can be lost before it flushes, leaving the site still "selected" on relaunch. Normal
     * resets keep [apply] to avoid main-thread disk I/O.
     */
    @Synchronized
    fun reset(persistSynchronously: Boolean = false) {
        wasReset = true
        state.value = null
        getPreferences().edit(commit = persistSynchronously) { remove(SELECTED_SITE_LOCAL_ID) }
    }

    fun exists(): Boolean {
        val siteModel = siteStore.getSiteByLocalId(getSelectedSiteId())
        return siteModel != null
    }

    fun getIfExists(): SiteModel? = if (exists()) get() else null

    fun getSelectedSiteId() = getSelectedSiteId(context)

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getSelectedSiteFromPersistence(): SiteModel? {
        val localSiteId = getSelectedSiteId()
        return siteStore.getSiteByLocalId(localSiteId)
    }

    @Deprecated("Event bus is considered deprecated.", ReplaceWith("observe()"))
    class SelectedSiteChangedEvent(val site: SiteModel)

    open class SelectedSiteException(message: String? = null) : Exception(message)
    class SelectedSiteResetException : SelectedSiteException()
    class SelectedSiteUninitializedException(
        val siteId: Int
    ) : SelectedSiteException("Selected Site is missing, id: $siteId")
}
