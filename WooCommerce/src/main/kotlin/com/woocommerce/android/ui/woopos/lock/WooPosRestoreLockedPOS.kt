package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosPINCredentialSource
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosRestoreLockedPOS @Inject constructor(
    private val selectedSite: SelectedSite,
    private val flags: FeatureFlagRepository,
    private val credentials: WooPosPINCredentialSource,
    private val lockedPOS: WooPosLockedPOSStore,
) {
    fun needsCheck(): Boolean {
        if (!flags.isEnabled(FeatureFlag.WOO_POS_ROLES)) return false
        val id = selectedSite.getOrNull()?.id ?: return false
        val marked = lockedPOS.isMarked(id).getOrDefault(true)
        return marked || id != selectedSite.getOrNull()?.id
    }

    suspend operator fun invoke(): Boolean {
        var decision = Decision.RETRY
        while (decision == Decision.RETRY) {
            val id = selectedSite.getOrNull()?.id
            decision = if (!flags.isEnabled(FeatureFlag.WOO_POS_ROLES) || id == null) {
                Decision.MAIN
            } else {
                resolve(id)
            }
        }
        return decision == Decision.POS
    }

    private suspend fun resolve(id: Int): Decision {
        if (!lockedPOS.isMarked(id).getOrDefault(true)) return unprotectedOrRetry(id)
        val configured = credentials.credentials(id)
        if (id != selectedSite.getOrNull()?.id) return Decision.RETRY
        if (configured.getOrNull()?.isEmpty() == true) {
            lockedPOS.clear(id)
            return unprotectedOrRetry(id)
        }
        return Decision.POS
    }

    private fun unprotectedOrRetry(id: Int): Decision =
        if (id == selectedSite.getOrNull()?.id) Decision.MAIN else Decision.RETRY

    private enum class Decision { MAIN, POS, RETRY }
}
