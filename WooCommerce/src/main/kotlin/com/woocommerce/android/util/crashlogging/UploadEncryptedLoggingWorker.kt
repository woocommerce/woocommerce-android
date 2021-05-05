package com.woocommerce.android.util.crashlogging

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ENCRYPTED_LOGGING_UPLOAD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogFailedToUpload
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogUploadedSuccessfully
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

@HiltWorker
class UploadEncryptedLoggingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val uploadEncryptedLogs: UploadEncryptedLogs
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        uploadEncryptedLogs()
        return Result.success()
    }

    class UploadEncryptedLogs @Inject constructor(
        eventBusDispatcher: Dispatcher,
        private val encryptedLogStore: EncryptedLogStore,
        private val analyticsTracker: AnalyticsTrackerWrapper,
        private val logger: AppLogWrapper
    ) {
        init {
            eventBusDispatcher.register(this)
            eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())
        }

        suspend operator fun invoke() {
            encryptedLogStore.uploadQueuedEncryptedLogs()
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
}
