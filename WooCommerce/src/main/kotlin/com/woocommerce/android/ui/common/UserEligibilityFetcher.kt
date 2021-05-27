package com.woocommerce.android.ui.common

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.store.WCUserStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class UserEligibilityFetcher @Inject constructor(
    private val appPrefs: AppPrefs,
    private val userStore: WCUserStore,
    private val selectedSite: SelectedSite
) : CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    fun fetchUserEligibility() {
        launch(Dispatchers.Default) {
            fetchUserInfo()?.let { updateUserInfo(it) }
        }
    }

    suspend fun fetchUserInfo(): WCUserModel? = userStore.fetchUserRole(selectedSite.get()).model

    fun getUserByEmail(email: String) = userStore.getUserByEmail(selectedSite.get(), email)

    fun updateUserInfo(user: WCUserModel) {
        appPrefs.setIsUserEligible(user.isUserEligible())
        appPrefs.setUserEmail(user.email)
    }
}
