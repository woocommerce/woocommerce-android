package com.woocommerce.android.util.encryptedlogging

import com.woocommerce.android.analytics.AnalyticsEvent.ENCRYPTED_LOGGING_UPLOAD_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argForWhich
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.EncryptedLogAction.RESET_UPLOAD_STATES
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogError
import org.wordpress.android.fluxc.utils.AppLogWrapper
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class ObserveEncryptedLogsUploadResultTest {
    private lateinit var sut: ObserveEncryptedLogsUploadResult

    private val eventBusDispatcher: Dispatcher = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val logger: AppLogWrapper = mock()

    @Before
    fun setUp() {
        sut = ObserveEncryptedLogsUploadResult(
            analyticsTracker = analyticsTracker,
            logger = logger,
            eventBusDispatcher = eventBusDispatcher
        )
    }

    @Test
    fun `should reset upload states on initialization`() {
        sut.invoke()

        verify(eventBusDispatcher, times(1)).dispatch(
            argForWhich {
                payload == null && type == RESET_UPLOAD_STATES
            }
        )
    }

    @Test
    fun `should track successful upload`() {
        sut.onEncryptedLogUploaded(event = OnEncryptedLogUploaded.EncryptedLogUploadedSuccessfully("", File("temp")))

        verify(analyticsTracker, times(1)).track(ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL)
    }

    @Test
    fun `should track failed upload when sending won't be retried`() {
        val event = OnEncryptedLogUploaded.EncryptedLogFailedToUpload(
            "",
            File("temp"),
            UploadEncryptedLogError.InvalidRequest,
            willRetry = false
        )

        sut.onEncryptedLogUploaded(event = event)

        verify(analyticsTracker, times(1)).track(
            ENCRYPTED_LOGGING_UPLOAD_FAILED,
            mapOf("error_type" to event.error.javaClass.simpleName)
        )
    }

    @Test
    fun `should not track failed upload when sending will be retried`() {
        val event = OnEncryptedLogUploaded.EncryptedLogFailedToUpload(
            "",
            File("temp"),
            UploadEncryptedLogError.InvalidRequest,
            willRetry = true
        )

        sut.onEncryptedLogUploaded(event = event)

        verifyNoInteractions(analyticsTracker)
    }
}
