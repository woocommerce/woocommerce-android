package com.woocommerce.android.ui.sitepicker.sitediscovery

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import kotlin.coroutines.resume

class FetchSiteInfo @Inject constructor(private val dispatcher: Dispatcher) {
    suspend operator fun invoke(siteAddress: String) =
        suspendCancellableCoroutine<Result<ConnectSiteInfoPayload>> { continuation ->
            val listener = ConnectSiteInfoListener(continuation)
            dispatcher.register(listener)
            dispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(siteAddress))

            continuation.invokeOnCancellation {
                dispatcher.unregister(listener)
            }
        }

    private inner class ConnectSiteInfoListener(
        private val continuation: CancellableContinuation<Result<ConnectSiteInfoPayload>>
    ) {
        @Subscribe(threadMode = MAIN)
        fun onFetchedConnectSiteInfo(event: OnConnectSiteInfoChecked) {
            dispatcher.unregister(this@ConnectSiteInfoListener)

            if (!continuation.isActive) return
            if (event.isError) {
                AppLog.e(API, "onFetchedConnectSiteInfo has error: " + event.error.message)
                continuation.resume(Result.failure(FetchSiteInfoException(event.error.type, event.error.message)))
            } else {
                continuation.resume(Result.success(event.info))
            }
        }
    }

    class FetchSiteInfoException(val type: SiteErrorType, message: String?) : Exception(message)
}
