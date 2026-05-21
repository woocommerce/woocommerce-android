package com.woocommerce.android.ui.aisupportchat

import android.os.Parcelable
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.zendesk.ZendeskException
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatSupportArea
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class AiSupportChatAnalyticsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackEntryPointTapped(entryPoint: AiSupportChatEntryPoint, isAuthenticated: Boolean, isResumedChat: Boolean) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_ENTRY_POINT_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to entryPoint.value,
                AnalyticsTracker.KEY_SUPPORT_CHAT_IS_AUTHENTICATED to isAuthenticated,
                AnalyticsTracker.KEY_SUPPORT_CHAT_IS_RESUMED_CHAT to isResumedChat
            )
        )
    }

    fun trackIssueSelected(issueType: SupportIssueType, entryPoint: AiSupportChatEntryPoint) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_ISSUE_SELECTED,
            properties = mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_ISSUE_TYPE to issueType.analyticsValue,
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to entryPoint.value
            )
        )
    }

    fun trackTroubleshootingCompleted(
        issueType: SupportIssueType,
        result: TroubleshootingResult,
        failedTest: DiagnosticTest?
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_TROUBLESHOOTING_COMPLETED,
            properties = buildMap {
                put(AnalyticsTracker.KEY_SUPPORT_CHAT_ISSUE_TYPE, issueType.analyticsValue)
                put(AnalyticsTracker.KEY_RESULT, result.value)
                failedTest?.let { put(AnalyticsTracker.KEY_SUPPORT_CHAT_FAILED_TEST, it.analyticsValue) }
            }
        )
    }

    fun trackMessageSent(
        entryPoint: AiSupportChatEntryPoint,
        isFirstMessage: Boolean,
        hasDiagnosticsContext: Boolean
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_MESSAGE_SENT,
            properties = mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to entryPoint.value,
                AnalyticsTracker.KEY_SUPPORT_CHAT_IS_FIRST_MESSAGE to isFirstMessage,
                AnalyticsTracker.KEY_SUPPORT_CHAT_HAS_DIAGNOSTICS_CONTEXT to hasDiagnosticsContext
            )
        )
    }

    fun trackResponseReceived(
        entryPoint: AiSupportChatEntryPoint,
        supportArea: SupportChatSupportArea?,
        forwardToHumanSupport: Boolean
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_RESPONSE_RECEIVED,
            properties = supportAreaProperties(supportArea) + mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to entryPoint.value,
                AnalyticsTracker.KEY_SUPPORT_CHAT_HAS_CHAT_TOPIC to (supportArea?.topic?.isNotBlank() == true),
                AnalyticsTracker.KEY_SUPPORT_CHAT_FORWARD_TO_HUMAN_SUPPORT to forwardToHumanSupport
            )
        )
    }

    fun trackFeedbackSubmitted(
        rating: AiSupportChatFeedbackRating,
        entryPoint: AiSupportChatEntryPoint,
        supportArea: SupportChatSupportArea?,
        userMessageCount: Int
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_FEEDBACK_SUBMITTED,
            properties = supportAreaProperties(supportArea) + mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_RATING to rating.analyticsValue,
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to entryPoint.value,
                AnalyticsTracker.KEY_SUPPORT_CHAT_USER_MESSAGE_COUNT to userMessageCount
            )
        )
    }

    fun trackEscalationButtonShown(
        trigger: AiSupportChatEscalationTrigger,
        entryPoint: AiSupportChatEntryPoint,
        supportArea: SupportChatSupportArea?,
        userMessageCount: Int?
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_ESCALATION_BUTTON_SHOWN,
            properties = buildMap {
                putAll(supportAreaProperties(supportArea))
                put(AnalyticsTracker.KEY_SUPPORT_CHAT_TRIGGER, trigger.value)
                put(AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT, entryPoint.value)
                userMessageCount?.let { put(AnalyticsTracker.KEY_SUPPORT_CHAT_USER_MESSAGE_COUNT, it) }
            }
        )
    }

    fun trackEscalationTapped(
        source: HumanSupportContactSource,
        entryPoint: AiSupportChatEntryPoint,
        supportArea: SupportChatSupportArea?,
        userMessageCount: Int
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_ESCALATION_TAPPED,
            properties = supportAreaProperties(supportArea) + mapOf(
                AnalyticsTracker.KEY_SOURCE to source.analyticsValue,
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to entryPoint.value,
                AnalyticsTracker.KEY_SUPPORT_CHAT_USER_MESSAGE_COUNT to userMessageCount
            )
        )
    }

    fun trackTicketCreated(route: AiSupportChatTicketRoute, context: AiSupportChatTicketAnalyticsContext) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_TICKET_CREATED,
            properties = ticketProperties(route, context)
        )
    }

    fun trackTicketCreationFailed(
        route: AiSupportChatTicketRoute,
        context: AiSupportChatTicketAnalyticsContext,
        error: Throwable
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_TICKET_CREATION_FAILED,
            properties = ticketProperties(route, context) + mapOf(
                AnalyticsTracker.KEY_ERROR_TYPE to error.ticketCreationErrorType
            )
        )
    }

    fun trackResolutionButtonShown(
        entryPoint: AiSupportChatEntryPoint,
        supportArea: SupportChatSupportArea?,
        userMessageCount: Int
    ) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SUPPORT_CHAT_RESOLUTION_BUTTON_SHOWN,
            properties = supportAreaProperties(supportArea) + mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT to entryPoint.value,
                AnalyticsTracker.KEY_SUPPORT_CHAT_USER_MESSAGE_COUNT to userMessageCount
            )
        )
    }

    fun trackMarkResolvedTapped() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SUPPORT_CHAT_MARK_RESOLVED_TAPPED)
    }

    private fun ticketProperties(
        route: AiSupportChatTicketRoute,
        context: AiSupportChatTicketAnalyticsContext
    ): Map<String, Any> = buildMap {
        put(AnalyticsTracker.KEY_SUPPORT_CHAT_ROUTE, route.value)
        put(AnalyticsTracker.KEY_SUPPORT_CHAT_ENTRY_POINT, context.entryPoint)
        put(AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA, context.supportArea)
        put(AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA_CONFIDENCE, context.supportAreaConfidence)
        context.chatTopic?.takeIf { it.isNotBlank() }?.let {
            put(AnalyticsTracker.KEY_SUPPORT_CHAT_CHAT_TOPIC, it)
        }
    }

    companion object {
        fun supportAreaProperties(supportArea: SupportChatSupportArea?): Map<String, String> =
            mapOf(
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA to supportArea?.analyticsArea.orUnknown(),
                AnalyticsTracker.KEY_SUPPORT_CHAT_SUPPORT_AREA_CONFIDENCE to
                    supportArea?.analyticsConfidence.orUnknown()
            )
    }
}

enum class AiSupportChatEntryPoint(val value: String) {
    HELP_AND_SUPPORT("help_and_support"),
    CONNECTIVITY_TOOL("connectivity_tool"),
    CHAT_HISTORY("chat_history"),
    PRE_LOGIN("pre_login")
}

enum class TroubleshootingResult(val value: String) {
    SKIPPED("skipped"),
    FAILED("failed"),
    PASSED("passed")
}

enum class AiSupportChatEscalationTrigger(val value: String) {
    ERROR_DIALOG("error_dialog"),
    MANUAL_TOOLBAR("manual_toolbar"),
    BOT_FORWARDED_TO_HUMAN_SUPPORT("bot_forwarded_to_human_support")
}

enum class AiSupportChatTicketRoute(val value: String) {
    SUPPORT_FORM("support_form"),
    DIRECT_TICKET_CREATION("direct_ticket_creation")
}

@Parcelize
data class AiSupportChatTicketAnalyticsContext(
    val entryPoint: String,
    val supportArea: String,
    val supportAreaConfidence: String,
    val chatTopic: String?
) : Parcelable

fun SupportChatSupportArea?.toTicketAnalyticsContext(
    entryPoint: AiSupportChatEntryPoint
): AiSupportChatTicketAnalyticsContext =
    AiSupportChatTicketAnalyticsContext(
        entryPoint = entryPoint.value,
        supportArea = this?.analyticsArea.orUnknown(),
        supportAreaConfidence = this?.analyticsConfidence.orUnknown(),
        chatTopic = this?.topic?.takeIf { it.isNotBlank() }
    )

private val SupportIssueType.analyticsValue: String
    get() = when (this) {
        SupportIssueType.LOADING_ORDERS -> "loading_orders"
        SupportIssueType.LOADING_PRODUCTS -> "loading_products"
        SupportIssueType.LOADING_ANALYTICS -> "loading_analytics"
        SupportIssueType.RECEIVING_NOTIFICATIONS -> "receiving_notifications"
        SupportIssueType.OTHER -> "other"
    }

private val DiagnosticTest.analyticsValue: String
    get() = when (this) {
        DiagnosticTest.INTERNET_CONNECTION -> "internet_connection"
        DiagnosticTest.WPCOM_SERVERS -> "wp_com_servers"
        DiagnosticTest.STORE_CONNECTION -> "site"
        DiagnosticTest.STORE_ORDERS -> "site_orders"
        DiagnosticTest.STORE_PRODUCTS -> "loading_products"
        DiagnosticTest.ANALYTICS_SETTING -> "analytics_setting"
    }

private val AiSupportChatFeedbackRating.analyticsValue: String
    get() = when (this) {
        AiSupportChatFeedbackRating.UP -> "up"
        AiSupportChatFeedbackRating.DOWN -> "down"
    }

private val HumanSupportContactSource.analyticsValue: String
    get() = when (this) {
        HumanSupportContactSource.TOOLBAR -> "toolbar"
        HumanSupportContactSource.BANNER -> "banner"
        HumanSupportContactSource.ERROR_DIALOG -> "error_dialog"
    }

private val SupportChatSupportArea.analyticsArea: String
    get() = area?.lowercase().orUnknown()

private val SupportChatSupportArea.analyticsConfidence: String
    get() = confidence?.lowercase().orUnknown()

private fun String?.orUnknown(): String = this ?: "unknown"

private val Throwable.ticketCreationErrorType: String
    get() = when (this) {
        is ZendeskException.IdentityNotSetException -> "identity_creation_failed"
        is ZendeskException.RequestCreationTimeoutException -> "request_timeout"
        else -> "zendesk_request_failed"
    }
