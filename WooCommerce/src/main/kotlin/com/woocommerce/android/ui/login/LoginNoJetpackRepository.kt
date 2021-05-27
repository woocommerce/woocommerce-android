package com.woocommerce.android.ui.login

import com.woocommerce.android.AppConstants
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.WooLog.T.LOGIN
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked
import javax.inject.Inject

class LoginNoJetpackRepository @Inject constructor(
    private val dispatcher: Dispatcher
) {
    private var continuationFetchSiteInfo = ContinuationWrapper<Boolean>(LOGIN)

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun verifyJetpackAvailable(siteAddress: String): Boolean {
        val result = continuationFetchSiteInfo.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            dispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(siteAddress))
        }
        return when (result) {
            is Cancellation -> false
            is Success -> result.value
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onFetchedConnectSiteInfo(event: OnConnectSiteInfoChecked) {
        if (event.isError) {
            continuationFetchSiteInfo.continueWith(false)
        } else {
            val hasJetpack =
                event.info.hasJetpack &&
                    event.info.isJetpackActive &&
                    event.info.isJetpackConnected
            continuationFetchSiteInfo.continueWith(hasJetpack)
        }
    }
}
