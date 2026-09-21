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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class AgeSignalsAnalyticsTrackerTest {
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()
    private val tracker = AgeSignalsAnalyticsTracker(trackerWrapper)
    private val evaluator = AgeEligibilityEvaluator()

    @Test
    fun `given a restricted recovery result, when tracked, then the exact bounded payload is sent`() {
        val result = sharedResult(
            ageLower = 0,
            ageUpper = 12,
            significantChangeStatus = AppSignificantChangeStatus.PENDING,
            retryCount = 2
        )
        tracker.trackCheck(
            result = result,
            failure = null,
            evaluation = evaluator.evaluate(result, priorRestriction = null),
            trigger = AgeCheckTrigger.RETURN_FROM_PLAY_STORE
        )

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                KEY_AGE_SIGNALS_REQUEST_STAGE to "check",
                KEY_AGE_SIGNALS_ACCESS_STATUS to "shared",
                KEY_AGE_SIGNALS_RANGE_OUTCOME to "below_13",
                KEY_AGE_SIGNALS_SIGNIFICANT_CHANGE_STATUS to "pending",
                KEY_AGE_SIGNALS_FINAL_DECISION to "restricted",
                KEY_AGE_SIGNALS_RESTRICTION_REASON to "below_minimum_age",
                KEY_AGE_SIGNALS_SDK_ERROR_CODE to "none",
                KEY_AGE_SIGNALS_RETRY_COUNT to 2,
                KEY_AGE_SIGNALS_IS_RECOVERY to true,
                KEY_AGE_SIGNALS_ACCESS_RESTRICTED to true
            )
        )
    }

    @Test
    fun `given a check-stage failure, when tracked, then failure categories preserve shared access context`() {
        tracker.trackCheck(
            result = null,
            failure = AgeSignalsRequestException(
                stage = AgeSignalsRequestStage.CHECK,
                errorCode = AppAgeSignalsErrorCode.INTERNAL_ERROR,
                retryCount = 2,
                cause = IllegalStateException("must-not-be-tracked")
            ),
            evaluation = allowedEvaluation(isAuthoritative = false),
            trigger = AgeCheckTrigger.MANUAL_RETRY
        )

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                KEY_AGE_SIGNALS_REQUEST_STAGE to "check",
                KEY_AGE_SIGNALS_ACCESS_STATUS to "shared",
                KEY_AGE_SIGNALS_RANGE_OUTCOME to "unavailable",
                KEY_AGE_SIGNALS_SIGNIFICANT_CHANGE_STATUS to "unavailable",
                KEY_AGE_SIGNALS_FINAL_DECISION to "allowed",
                KEY_AGE_SIGNALS_RESTRICTION_REASON to "none",
                KEY_AGE_SIGNALS_SDK_ERROR_CODE to "internal_error",
                KEY_AGE_SIGNALS_RETRY_COUNT to 2,
                KEY_AGE_SIGNALS_IS_RECOVERY to true,
                KEY_AGE_SIGNALS_ACCESS_RESTRICTED to false
            )
        )
    }

    @Test
    fun `given each approved age band or malformed bounds, when tracked, then only band categories are sent`() {
        // GIVEN
        val cases = listOf(
            AgeRangeCase(ageLower = 0, ageUpper = 12, expected = "below_13"),
            AgeRangeCase(ageLower = 13, ageUpper = 15, expected = "13_15"),
            AgeRangeCase(ageLower = 16, ageUpper = 17, expected = "16_17"),
            AgeRangeCase(ageLower = 18, ageUpper = null, expected = "18_plus"),
            AgeRangeCase(ageLower = 13, ageUpper = null, expected = "eligible"),
            AgeRangeCase(ageLower = null, ageUpper = null, expected = "ambiguous"),
            AgeRangeCase(ageLower = 16, ageUpper = 15, expected = "ambiguous")
        )

        // WHEN
        val outcomes = cases.map { captureAgeRangeOutcome(it.ageLower, it.ageUpper) }

        // THEN
        assertThat(outcomes).containsExactlyElementsOf(cases.map { it.expected })
    }

    @Test
    fun `given every significant change status, when tracked, then it remains categorical and does not change access`() {
        val expectedValues = mapOf(
            AppSignificantChangeStatus.UNSPECIFIED to "unspecified",
            AppSignificantChangeStatus.APPROVED to "approved",
            AppSignificantChangeStatus.PENDING to "pending",
            AppSignificantChangeStatus.DECLINED to "declined",
            AppSignificantChangeStatus.UNEXPECTED to "unexpected"
        )

        expectedValues.forEach { (status, expected) ->
            val wrapper: AnalyticsTrackerWrapper = mock()
            val result = sharedResult(18, null, status)
            AgeSignalsAnalyticsTracker(wrapper).trackCheck(
                result = result,
                failure = null,
                evaluation = evaluator.evaluate(result, priorRestriction = null),
                trigger = AgeCheckTrigger.STARTUP
            )

            val properties = captureCheckProperties(wrapper)
            assertThat(properties[KEY_AGE_SIGNALS_SIGNIFICANT_CHANGE_STATUS]).isEqualTo(expected)
            assertThat(properties[KEY_AGE_SIGNALS_FINAL_DECISION]).isEqualTo("allowed")
        }
    }

    @Test
    fun `when verification recovery actions are tracked, then exact action categories are sent`() {
        tracker.trackPlayStoreOpened()
        tracker.trackVerificationAction(AgeCheckTrigger.MANUAL_RETRY)
        tracker.trackVerificationAction(AgeCheckTrigger.RETURN_FROM_PLAY_STORE)
        tracker.trackVerificationAction(AgeCheckTrigger.STARTUP)

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_VERIFICATION_ACTION,
            mapOf(KEY_AGE_VERIFICATION_ACTION to "open_play_store")
        )
        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_VERIFICATION_ACTION,
            mapOf(KEY_AGE_VERIFICATION_ACTION to "manual_retry")
        )
        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_VERIFICATION_ACTION,
            mapOf(KEY_AGE_VERIFICATION_ACTION to "return_from_play_retry")
        )
        verifyNoMoreInteractions(trackerWrapper)
    }

    @Test
    fun `when a check is tracked, then prohibited raw fields are absent`() {
        val result = sharedResult(13, 15, AppSignificantChangeStatus.APPROVED)
        tracker.trackCheck(
            result = result,
            failure = null,
            evaluation = evaluator.evaluate(result, priorRestriction = null),
            trigger = AgeCheckTrigger.STARTUP
        )

        val properties = captureCheckProperties(trackerWrapper)

        assertThat(properties.keys).doesNotContain(
            "age_lower",
            "age_upper",
            "install_id",
            "approval_date",
            "exception",
            "exception_message",
            "user_id"
        )
        assertThat(properties.values).doesNotContain(13, 15)
    }

    private fun captureAgeRangeOutcome(ageLower: Int?, ageUpper: Int?): String {
        val wrapper: AnalyticsTrackerWrapper = mock()
        val result = sharedResult(ageLower, ageUpper)
        AgeSignalsAnalyticsTracker(wrapper).trackCheck(
            result = result,
            failure = null,
            evaluation = evaluator.evaluate(result, priorRestriction = null),
            trigger = AgeCheckTrigger.STARTUP
        )

        return captureCheckProperties(wrapper)[KEY_AGE_SIGNALS_RANGE_OUTCOME] as String
    }

    private fun captureCheckProperties(wrapper: AnalyticsTrackerWrapper): Map<String, *> {
        val propertiesCaptor = argumentCaptor<Map<String, *>>()
        verify(wrapper).track(
            org.mockito.kotlin.eq(AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED),
            propertiesCaptor.capture()
        )
        return propertiesCaptor.firstValue
    }

    private fun allowedEvaluation(isAuthoritative: Boolean = true) = AgeEligibilityEvaluation(
        decision = AgeEligibilityDecision.Allowed,
        isAuthoritative = isAuthoritative
    )

    private fun sharedResult(
        ageLower: Int?,
        ageUpper: Int?,
        significantChangeStatus: AppSignificantChangeStatus = AppSignificantChangeStatus.UNSPECIFIED,
        retryCount: Int = 0
    ) = AgeSignalsRequestResult(
        accessStatus = AgeSignalsAccessStatus.SHARED,
        ageSignals = SharedAgeSignals(
            ageLower = ageLower,
            ageUpper = ageUpper,
            ageRangeSource = AppAgeRangeSource.UNSPECIFIED,
            significantChangeStatus = significantChangeStatus
        ),
        retryCount = retryCount
    )

    private data class AgeRangeCase(
        val ageLower: Int?,
        val ageUpper: Int?,
        val expected: String
    )
}
