package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ENCRYPTED_LOGGING_UPLOAD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogFailedToUpload
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogUploadedSuccessfully
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.helpers.logfile.LogFileProviderInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedLogging @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val eventBusDispatcher: Dispatcher,
    private val encryptedLogStore: EncryptedLogStore,
    private val logFileProvider: LogFileProviderInterface,
    private val networkStatus: NetworkStatus,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val logger: AppLogWrapper
) {
    private val coroutineScope = CoroutineScope(dispatchers.io)

    init {
        eventBusDispatcher.register(this)
        eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())

        coroutineScope.launch {
            encryptedLogStore.uploadQueuedEncryptedLogs()
        }
    }

    fun enqueue(
        uuid: String,
        shouldStartUploadImmediately: Boolean
    ) {
        logFileProvider.getLogFiles().lastOrNull()?.let { logFile ->
            val payload = UploadEncryptedLogPayload(
                uuid = uuid,
                file = logFile,
                shouldStartUploadImmediately = shouldStartUploadImmediately && networkStatus.isConnected()
            )
            eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newUploadLogAction(payload))
        }
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
                logger.e(T.MAIN, "Encrypted log with uuid: ${event.uuid} failed to upload with error: ${event.error}")
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
