package com.woocommerce.android.util.encryptedlogging

import com.woocommerce.android.analytics.AnalyticsEvent.ENCRYPTED_LOGGING_UPLOAD_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogFailedToUpload
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogUploadedSuccessfully
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveEncryptedLogsUploadResult @Inject constructor(
    private val eventBusDispatcher: Dispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val logger: AppLogWrapper
) {
    operator fun invoke() {
        eventBusDispatcher.register(this)
        eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ASYNC)
    fun onEncryptedLogUploaded(event: OnEncryptedLogUploaded) {
        when (event) {
            is EncryptedLogUploadedSuccessfully -> {
                logger.d(T.MAIN, "Encrypted log with uuid: ${event.uuid} uploaded successfully!")
                analyticsTracker.track(ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL)
            }
            is EncryptedLogFailedToUpload -> {
                logger.e(
                    T.MAIN,
                    "Encrypted log with uuid: ${event.uuid} failed to upload with error: ${event.error}"
                )
                if (!event.willRetry) {
                    analyticsTracker.track(
                        ENCRYPTED_LOGGING_UPLOAD_FAILED,
                        mapOf("error_type" to event.error.javaClass.simpleName)
                    )
                }
            }
        }
    }
}
