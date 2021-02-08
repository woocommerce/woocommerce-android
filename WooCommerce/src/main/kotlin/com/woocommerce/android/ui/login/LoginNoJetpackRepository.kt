package com.woocommerce.android.ui.login

import com.woocommerce.android.AppConstants
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class LoginNoJetpackRepository @Inject constructor(
    private val dispatcher: Dispatcher
) {
    private var continuationFetchSiteInfo: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun verifyJetpackAvailable(siteAddress: String): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationFetchSiteInfo = it

                dispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(siteAddress))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(LOGIN, "Exception encountered while fetching connected site info for $siteAddress", e)
            false
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onFetchedConnectSiteInfo(event: OnConnectSiteInfoChecked) {
        if (event.isError) {
            continuationFetchSiteInfo?.resume(false)
        } else {
            val hasJetpack =
                    event.info.hasJetpack &&
                    event.info.isJetpackActive &&
                    event.info.isJetpackConnected
            continuationFetchSiteInfo?.resume(hasJetpack)
        }
        continuationFetchSiteInfo = null
    }
}
