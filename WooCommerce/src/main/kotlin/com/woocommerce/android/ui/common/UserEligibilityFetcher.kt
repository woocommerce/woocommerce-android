package com.woocommerce.android.ui.common

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.store.WCUserStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserEligibilityFetcher @Inject constructor(
    private val appPrefs: AppPrefs,
    private val userStore: WCUserStore,
    private val selectedSite: SelectedSite
) {
    suspend fun fetchUserInfo(): WCUserModel? {
        return userStore.fetchUserRole(selectedSite.get()).model?.also {
            updateUserInfo(it)
        }
    }

    fun getUserByEmail(email: String) = userStore.getUserByEmail(selectedSite.get(), email)

    private fun updateUserInfo(user: WCUserModel) {
        appPrefs.setIsUserEligible(user.isUserEligible())
        appPrefs.setUserEmail(user.email)
    }
}
