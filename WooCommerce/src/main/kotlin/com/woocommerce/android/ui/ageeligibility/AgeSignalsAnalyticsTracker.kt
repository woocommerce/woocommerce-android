package com.woocommerce.android.ui.ageeligibility

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_ACCESS_RESTRICTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_ACCESS_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_FINAL_DECISION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_IS_RECOVERY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_RANGE_OUTCOME
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_REQUEST_STAGE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_RESTRICTION_REASON
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_RETRY_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_SDK_ERROR_CODE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_SIGNALS_SIGNIFICANT_CHANGE_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AGE_VERIFICATION_ACTION
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgeSignalsAnalyticsTracker @Inject constructor(
    private val trackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackCheck(
        result: AgeSignalsRequestResult?,
        failure: AgeSignalsRequestException?,
        evaluation: AgeEligibilityEvaluation,
        trigger: AgeCheckTrigger
    ) {
        val restriction = (evaluation.decision as? AgeEligibilityDecision.Restricted)?.reason
        trackerWrapper.track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            properties = mapOf(
                KEY_AGE_SIGNALS_REQUEST_STAGE to requestStage(result, failure),
                KEY_AGE_SIGNALS_ACCESS_STATUS to accessStatus(result, failure),
                KEY_AGE_SIGNALS_RANGE_OUTCOME to ageRangeOutcome(result),
                KEY_AGE_SIGNALS_SIGNIFICANT_CHANGE_STATUS to significantChangeStatus(result),
                KEY_AGE_SIGNALS_FINAL_DECISION to evaluation.decision.analyticsValue(),
                KEY_AGE_SIGNALS_RESTRICTION_REASON to restriction?.analyticsValue().orNone(),
                KEY_AGE_SIGNALS_SDK_ERROR_CODE to failure?.errorCode?.analyticsValue().orNone(),
                KEY_AGE_SIGNALS_RETRY_COUNT to (result?.retryCount ?: failure?.retryCount ?: 0),
                KEY_AGE_SIGNALS_IS_RECOVERY to (trigger != AgeCheckTrigger.STARTUP),
                KEY_AGE_SIGNALS_ACCESS_RESTRICTED to (restriction != null)
            )
        )
    }

    fun trackVerificationAction(trigger: AgeCheckTrigger) {
        val action = when (trigger) {
            AgeCheckTrigger.STARTUP -> return
            AgeCheckTrigger.MANUAL_RETRY -> ACTION_MANUAL_RETRY
            AgeCheckTrigger.RETURN_FROM_PLAY_STORE -> ACTION_RETURN_FROM_PLAY_RETRY
        }
        trackAction(action)
    }

    fun trackPlayStoreOpened() {
        trackAction(ACTION_OPEN_PLAY_STORE)
    }

    private fun trackAction(action: String) {
        trackerWrapper.track(
            AnalyticsEvent.ACCOUNT_AGE_VERIFICATION_ACTION,
            properties = mapOf(KEY_AGE_VERIFICATION_ACTION to action)
        )
    }

    private fun requestStage(result: AgeSignalsRequestResult?, failure: AgeSignalsRequestException?): String = when {
        failure != null -> failure.stage.analyticsValue()
        result?.accessStatus == AgeSignalsAccessStatus.SHARED -> REQUEST_STAGE_CHECK
        else -> REQUEST_STAGE_ACCESS
    }

    private fun accessStatus(result: AgeSignalsRequestResult?, failure: AgeSignalsRequestException?): String = when {
        result != null -> result.accessStatus.analyticsValue()
        failure?.stage == AgeSignalsRequestStage.CHECK -> ACCESS_STATUS_SHARED
        else -> VALUE_UNAVAILABLE
    }

    private fun ageRangeOutcome(result: AgeSignalsRequestResult?): String = when {
        result == null -> VALUE_UNAVAILABLE
        result.accessStatus != AgeSignalsAccessStatus.SHARED -> VALUE_NOT_APPLICABLE
        else -> result.ageSignals.analyticsAgeRange()
    }

    private fun SharedAgeSignals?.analyticsAgeRange(): String {
        val lower = this?.ageLower
        val upper = this?.ageUpper
        return when {
            lower == null -> AGE_RANGE_AMBIGUOUS
            upper != null && lower > upper -> AGE_RANGE_AMBIGUOUS
            upper != null && upper < MINIMUM_AGE -> AGE_RANGE_BELOW_13
            lower >= MINIMUM_ADULT_AGE -> AGE_RANGE_18_PLUS
            lower >= OLDER_TEEN_MINIMUM && upper != null && upper <= OLDER_TEEN_MAXIMUM -> AGE_RANGE_16_17
            lower >= MINIMUM_AGE && upper != null && upper <= YOUNGER_TEEN_MAXIMUM -> AGE_RANGE_13_15
            else -> AGE_RANGE_AMBIGUOUS
        }
    }

    private fun significantChangeStatus(result: AgeSignalsRequestResult?): String = when {
        result == null -> VALUE_UNAVAILABLE
        result.accessStatus != AgeSignalsAccessStatus.SHARED -> VALUE_NOT_APPLICABLE
        else -> result.ageSignals?.significantChangeStatus?.analyticsValue() ?: VALUE_UNSPECIFIED
    }

    private fun AgeSignalsRequestStage.analyticsValue(): String = when (this) {
        AgeSignalsRequestStage.ACCESS -> REQUEST_STAGE_ACCESS
        AgeSignalsRequestStage.CHECK -> REQUEST_STAGE_CHECK
    }

    private fun AgeSignalsAccessStatus.analyticsValue(): String = when (this) {
        AgeSignalsAccessStatus.UNSPECIFIED -> VALUE_UNSPECIFIED
        AgeSignalsAccessStatus.SHARED -> ACCESS_STATUS_SHARED
        AgeSignalsAccessStatus.NOT_SHARED -> ACCESS_STATUS_NOT_SHARED
        AgeSignalsAccessStatus.VERIFICATION_REQUIRED -> ACCESS_STATUS_VERIFICATION_REQUIRED
        AgeSignalsAccessStatus.UNEXPECTED -> VALUE_UNEXPECTED
    }

    private fun AppSignificantChangeStatus.analyticsValue(): String = when (this) {
        AppSignificantChangeStatus.UNSPECIFIED -> VALUE_UNSPECIFIED
        AppSignificantChangeStatus.APPROVED -> SIGNIFICANT_CHANGE_APPROVED
        AppSignificantChangeStatus.PENDING -> SIGNIFICANT_CHANGE_PENDING
        AppSignificantChangeStatus.DECLINED -> SIGNIFICANT_CHANGE_DECLINED
        AppSignificantChangeStatus.UNEXPECTED -> VALUE_UNEXPECTED
    }

    private fun AgeEligibilityDecision.analyticsValue(): String = when (this) {
        AgeEligibilityDecision.Allowed -> DECISION_ALLOWED
        AgeEligibilityDecision.VerificationRequired -> DECISION_VERIFICATION_REQUIRED
        is AgeEligibilityDecision.Restricted -> DECISION_RESTRICTED
    }

    private fun AgeRestrictionReason.analyticsValue(): String = when (this) {
        AgeRestrictionReason.BELOW_MINIMUM_AGE -> RESTRICTION_BELOW_MINIMUM_AGE
        AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON -> RESTRICTION_LEGACY_AUTHORITATIVE
    }

    private fun AppAgeSignalsErrorCode.analyticsValue(): String = when (this) {
        AppAgeSignalsErrorCode.API_NOT_AVAILABLE -> ERROR_API_NOT_AVAILABLE
        AppAgeSignalsErrorCode.PLAY_STORE_NOT_FOUND -> ERROR_PLAY_STORE_NOT_FOUND
        AppAgeSignalsErrorCode.NETWORK_ERROR -> ERROR_NETWORK
        AppAgeSignalsErrorCode.PLAY_SERVICES_NOT_FOUND -> ERROR_PLAY_SERVICES_NOT_FOUND
        AppAgeSignalsErrorCode.CANNOT_BIND_TO_SERVICE -> ERROR_CANNOT_BIND
        AppAgeSignalsErrorCode.PLAY_STORE_VERSION_OUTDATED -> ERROR_PLAY_STORE_OUTDATED
        AppAgeSignalsErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> ERROR_PLAY_SERVICES_OUTDATED
        AppAgeSignalsErrorCode.CLIENT_TRANSIENT_ERROR -> ERROR_CLIENT_TRANSIENT
        AppAgeSignalsErrorCode.APP_NOT_OWNED -> ERROR_APP_NOT_OWNED
        AppAgeSignalsErrorCode.SDK_VERSION_OUTDATED -> ERROR_SDK_OUTDATED
        AppAgeSignalsErrorCode.INTERNAL_ERROR -> ERROR_INTERNAL
        AppAgeSignalsErrorCode.BINDER_DIED -> ERROR_BINDER_DIED
        AppAgeSignalsErrorCode.UNEXPECTED -> VALUE_UNEXPECTED
    }

    private fun String?.orNone() = this ?: VALUE_NONE

    companion object {
        private const val MINIMUM_AGE = 13
        private const val YOUNGER_TEEN_MAXIMUM = 15
        private const val OLDER_TEEN_MINIMUM = 16
        private const val OLDER_TEEN_MAXIMUM = 17
        private const val MINIMUM_ADULT_AGE = 18

        private const val REQUEST_STAGE_ACCESS = "access"
        private const val REQUEST_STAGE_CHECK = "check"
        private const val ACCESS_STATUS_SHARED = "shared"
        private const val ACCESS_STATUS_NOT_SHARED = "not_shared"
        private const val ACCESS_STATUS_VERIFICATION_REQUIRED = "verification_required"
        private const val AGE_RANGE_BELOW_13 = "below_13"
        private const val AGE_RANGE_13_15 = "13_15"
        private const val AGE_RANGE_16_17 = "16_17"
        private const val AGE_RANGE_18_PLUS = "18_plus"
        private const val AGE_RANGE_AMBIGUOUS = "ambiguous"
        private const val SIGNIFICANT_CHANGE_APPROVED = "approved"
        private const val SIGNIFICANT_CHANGE_PENDING = "pending"
        private const val SIGNIFICANT_CHANGE_DECLINED = "declined"
        private const val DECISION_ALLOWED = "allowed"
        private const val DECISION_VERIFICATION_REQUIRED = "verification_required"
        private const val DECISION_RESTRICTED = "restricted"
        private const val RESTRICTION_BELOW_MINIMUM_AGE = "below_minimum_age"
        private const val RESTRICTION_LEGACY_AUTHORITATIVE = "legacy_authoritative_restriction"
        private const val ERROR_API_NOT_AVAILABLE = "api_not_available"
        private const val ERROR_PLAY_STORE_NOT_FOUND = "play_store_not_found"
        private const val ERROR_NETWORK = "network_error"
        private const val ERROR_PLAY_SERVICES_NOT_FOUND = "play_services_not_found"
        private const val ERROR_CANNOT_BIND = "cannot_bind_to_service"
        private const val ERROR_PLAY_STORE_OUTDATED = "play_store_version_outdated"
        private const val ERROR_PLAY_SERVICES_OUTDATED = "play_services_version_outdated"
        private const val ERROR_CLIENT_TRANSIENT = "client_transient_error"
        private const val ERROR_APP_NOT_OWNED = "app_not_owned"
        private const val ERROR_SDK_OUTDATED = "sdk_version_outdated"
        private const val ERROR_INTERNAL = "internal_error"
        private const val ERROR_BINDER_DIED = "binder_died"
        private const val VALUE_NONE = "none"
        private const val VALUE_NOT_APPLICABLE = "not_applicable"
        private const val VALUE_UNAVAILABLE = "unavailable"
        private const val VALUE_UNSPECIFIED = "unspecified"
        private const val VALUE_UNEXPECTED = "unexpected"
        private const val ACTION_OPEN_PLAY_STORE = "open_play_store"
        private const val ACTION_MANUAL_RETRY = "manual_retry"
        private const val ACTION_RETURN_FROM_PLAY_RETRY = "return_from_play_retry"
    }
}
