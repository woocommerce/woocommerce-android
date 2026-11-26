package com.woocommerce.android.ui.woopos.roles.lock

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.util.analytics.WooPosElapsedTime
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the last unlocked staff member in memory, so POS can reopen without a PIN
 * when "Require PIN to launch POS" is off. A relaunched app never resumes anyone.
 */
@MainThread
@Singleton
class WooPosKeptStaff @Inject constructor(
    private val lockSettings: WooPosLockSettings,
    private val clock: WooPosElapsedTime,
) {
    private var kept: Kept? = null
    private var isAppInBackground = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    isAppInBackground = false
                }

                override fun onStop(owner: LifecycleOwner) {
                    isAppInBackground = true
                    kept = kept?.copy(wasBackgrounded = true)
                }
            }
        )
    }

    fun keep(localSiteId: Int, role: WooPosLocalRole, logoutVersion: Long) {
        kept = Kept(localSiteId, role, logoutVersion, clock.milliseconds(), wasBackgrounded = isAppInBackground)
    }

    suspend fun resume(localSiteId: Int, logoutVersion: Long): WooPosLocalRole? {
        val staff = kept?.takeIf { it.localSiteId == localSiteId && it.logoutVersion == logoutVersion }
        kept = null
        if (staff == null) return null
        val preferences = lockSettings.preferences(localSiteId).first()
        val timeout = preferences.inactivityTimeout.duration?.inWholeMilliseconds
        val isWithinTimeout = timeout == null || clock.milliseconds() - staff.endedAt < timeout
        val wasLockedInBackground = staff.wasBackgrounded && preferences.locksInBackground
        val resumes = !preferences.locksWhenPOSOpens && !wasLockedInBackground && isWithinTimeout
        return staff.role.takeIf { resumes }
    }

    private data class Kept(
        val localSiteId: Int,
        val role: WooPosLocalRole,
        val logoutVersion: Long,
        val endedAt: Long,
        val wasBackgrounded: Boolean,
    )
}
