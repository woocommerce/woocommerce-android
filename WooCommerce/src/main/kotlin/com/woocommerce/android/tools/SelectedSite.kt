package com.woocommerce.android.tools

import android.content.Context
import androidx.preference.PreferenceManager
import com.woocommerce.android.di.SiteComponent
import com.woocommerce.android.di.SiteComponent.Builder
import com.woocommerce.android.util.PreferenceUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * A wrapper for the currently active [SiteModel] for the app.
 * Persists and restores the selected site to/from the app preferences.
 */
@Singleton
class SelectedSite @Inject constructor(
    private val context: Context,
    private val siteStore: SiteStore,
    private val siteComponentProvider: Provider<Builder>,
    private val dispatcher: CoroutineDispatcher
) {
    companion object {
        const val SELECTED_SITE_LOCAL_ID = "SELECTED_SITE_LOCAL_ID"

        fun getEventBus(): EventBus = EventBus.getDefault()
    }

    private val state: MutableStateFlow<SiteModel?> = MutableStateFlow(getSelectedSiteFromPersistence())
    private var wasReset = false

    val connectionType: SiteConnectionType?
        get() = getIfExists()?.connectionType

    var siteComponent: SiteComponent? = getOrNull()?.let {
        siteComponentProvider.get().setSite(it).setCoroutineScope(createSiteCoroutineScope()).build()
    }
        private set

    // Coroutine scope that follows the lifecycle of the current site
    private var siteCoroutineScope: CoroutineScope? = null

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

        // Create a new site component tied to the lifecycle of the selected site
        siteComponent = siteComponentProvider.get()
            .setSite(get())
            .setCoroutineScope(createSiteCoroutineScope())
            .build()
    }

    @Synchronized
    fun reset() {
        wasReset = true
        state.value = null
        getPreferences().edit().remove(SELECTED_SITE_LOCAL_ID).apply()
        siteComponent = null
        siteCoroutineScope?.cancel()
    }

    fun exists(): Boolean {
        val siteModel = siteStore.getSiteByLocalId(getSelectedSiteId())
        return siteModel != null
    }

    fun getIfExists(): SiteModel? = if (exists()) get() else null

    fun getSelectedSiteId() = PreferenceUtils.getInt(getPreferences(), SELECTED_SITE_LOCAL_ID, -1)

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getSelectedSiteFromPersistence(): SiteModel? {
        val localSiteId = getSelectedSiteId()
        return siteStore.getSiteByLocalId(localSiteId)
    }

    private fun createSiteCoroutineScope(): CoroutineScope {
        siteCoroutineScope?.cancel()
        siteCoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
        return siteCoroutineScope!!
    }

    @Deprecated("Event bus is considered deprecated.", ReplaceWith("observe()"))
    class SelectedSiteChangedEvent(val site: SiteModel)

    open class SelectedSiteException(message: String? = null) : Exception(message)
    class SelectedSiteResetException : SelectedSiteException()
    class SelectedSiteUninitializedException(
        val siteId: Int
    ) : SelectedSiteException("Selected Site is missing, id: $siteId")
}
