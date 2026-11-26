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

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    kept = kept?.copy(wasBackgrounded = true)
                }
            }
        )
    }

    fun keep(localSiteId: Int, role: WooPosLocalRole, logoutVersion: Long) {
        kept = Kept(localSiteId, role, logoutVersion, endedAt = clock.milliseconds(), wasBackgrounded = false)
    }

    suspend fun resume(localSiteId: Int, logoutVersion: Long): WooPosLocalRole? {
        val staff = kept?.takeIf { it.localSiteId == localSiteId && it.logoutVersion == logoutVersion }
        kept = null
        if (staff == null) return null
        val preferences = lockSettings.preferences(localSiteId).first()
        val timeout = preferences.inactivityTimeout.duration?.inWholeMilliseconds
        val elapsed = clock.milliseconds() - staff.endedAt
        return staff.role.takeUnless {
            preferences.locksWhenPOSOpens ||
                (staff.wasBackgrounded && preferences.locksInBackground) ||
                (timeout != null && elapsed >= timeout)
        }
    }

    private data class Kept(
        val localSiteId: Int,
        val role: WooPosLocalRole,
        val logoutVersion: Long,
        val endedAt: Long,
        val wasBackgrounded: Boolean,
    )
}
