package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.zendesk.ZendeskException
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatSupportArea
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AiSupportChatAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val tracker = AiSupportChatAnalyticsTracker(analyticsTrackerWrapper)

    @Test
    fun `when entry point is tracked, then expected properties are emitted`() {
        tracker.trackEntryPointTapped(
            entryPoint = AiSupportChatEntryPoint.PRE_LOGIN,
            isAuthenticated = false,
            isResumedChat = false
        )

        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.SUPPORT_CHAT_ENTRY_POINT_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to "pre_login",
                AnalyticsTracker.KEY_SUPPORT_CHAT_IS_AUTHENTICATED to false,
                AnalyticsTracker.KEY_SUPPORT_CHAT_IS_RESUMED_CHAT to false
            )
        )
    }

    @Test
    fun `given missing support area, when response is tracked, then unknown classification is emitted`() {
        tracker.trackResponseReceived(
            entryPoint = AiSupportChatEntryPoint.HELP_AND_SUPPORT,
            supportArea = null,
            forwardToHumanSupport = false
        )

        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.SUPPORT_CHAT_RESPONSE_RECEIVED,
            properties = mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA to "unknown",
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA_CONFIDENCE to "unknown",
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to "help_and_support",
                AnalyticsTracker.KEY_SUPPORT_CHAT_HAS_CHAT_TOPIC to false,
                AnalyticsTracker.KEY_SUPPORT_CHAT_FORWARD_TO_HUMAN_SUPPORT to false
            )
        )
    }

    @Test
    fun `given unrecognized support area, when response is tracked, then unknown classification is emitted`() {
        tracker.trackResponseReceived(
            entryPoint = AiSupportChatEntryPoint.HELP_AND_SUPPORT,
            supportArea = SupportChatSupportArea(area = "custom-area", confidence = "certain"),
            forwardToHumanSupport = true
        )

        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.SUPPORT_CHAT_RESPONSE_RECEIVED,
            properties = mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA to "unknown",
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA_CONFIDENCE to "unknown",
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to "help_and_support",
                AnalyticsTracker.KEY_SUPPORT_CHAT_HAS_CHAT_TOPIC to false,
                AnalyticsTracker.KEY_SUPPORT_CHAT_FORWARD_TO_HUMAN_SUPPORT to true
            )
        )
    }

    @Test
    fun `given timeout error, when ticket failure is tracked, then shared error type is emitted`() {
        val context = AiSupportChatTicketAnalyticsContext(
            entryPoint = "help_and_support",
            supportArea = "mobile-app",
            supportAreaConfidence = "high",
            chatTopic = "woo_mobile_issue_mobile_app"
        )

        tracker.trackTicketCreationFailed(
            route = AiSupportChatTicketRoute.SUPPORT_FORM,
            context = context,
            error = ZendeskException.RequestCreationTimeoutException
        )

        verify(analyticsTrackerWrapper).track(
            stat = AnalyticsEvent.SUPPORT_CHAT_TICKET_CREATION_FAILED,
            properties = mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_ROUTE to "support_form",
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to "help_and_support",
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA to "mobile-app",
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA_CONFIDENCE to "high",
                AnalyticsTracker.KEY_SUPPORT_CHAT_CHAT_TOPIC to "woo_mobile_issue_mobile_app",
                AnalyticsTracker.KEY_ERROR_TYPE to "request_timeout"
            )
        )
    }
}
