package com.woocommerce.android.ui.ageeligibility

import javax.inject.Inject

class AgeEligibilityEvaluator @Inject constructor() {
    fun evaluate(
        result: AgeSignalsRequestResult,
        priorRestriction: AgeRestrictionReason?
    ): AgeEligibilityEvaluation = when (result.accessStatus) {
        AgeSignalsAccessStatus.SHARED -> evaluateSharedSignals(result.ageSignals, priorRestriction)
        AgeSignalsAccessStatus.VERIFICATION_REQUIRED -> AgeEligibilityEvaluation(
            decision = AgeEligibilityDecision.VerificationRequired,
            isAuthoritative = false
        )

        AgeSignalsAccessStatus.NOT_SHARED,
        AgeSignalsAccessStatus.UNSPECIFIED,
        AgeSignalsAccessStatus.UNEXPECTED -> nonAuthoritative(priorRestriction)
    }

    fun preservePriorRestriction(priorRestriction: AgeRestrictionReason?): AgeEligibilityEvaluation =
        nonAuthoritative(priorRestriction)

    private fun evaluateSharedSignals(
        signals: SharedAgeSignals?,
        priorRestriction: AgeRestrictionReason?
    ): AgeEligibilityEvaluation {
        val ageUpper = signals?.ageUpper
        if (ageUpper != null && ageUpper < WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE) {
            return authoritativeRestriction(AgeRestrictionReason.BELOW_MINIMUM_AGE)
        }

        val ageLower = signals?.ageLower ?: return nonAuthoritative(priorRestriction)
        if (ageUpper != null && ageLower > ageUpper) return nonAuthoritative(priorRestriction)

        return when {
            ageLower >= WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE -> authoritativeAllowed()
            else -> nonAuthoritative(priorRestriction)
        }
    }

    private fun authoritativeAllowed() = AgeEligibilityEvaluation(
        decision = AgeEligibilityDecision.Allowed,
        isAuthoritative = true
    )

    private fun authoritativeRestriction(reason: AgeRestrictionReason) = AgeEligibilityEvaluation(
        decision = AgeEligibilityDecision.Restricted(reason),
        isAuthoritative = true
    )

    private fun nonAuthoritative(priorRestriction: AgeRestrictionReason?) = AgeEligibilityEvaluation(
        decision = priorRestriction?.let(AgeEligibilityDecision::Restricted) ?: AgeEligibilityDecision.Allowed,
        isAuthoritative = false
    )

    companion object {
        private const val WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE = 13
    }
}

sealed interface AgeEligibilityDecision {
    data object Allowed : AgeEligibilityDecision

    data object VerificationRequired : AgeEligibilityDecision

    data class Restricted(val reason: AgeRestrictionReason) : AgeEligibilityDecision
}

enum class AgeRestrictionReason {
    BELOW_MINIMUM_AGE,
    LEGACY_RESTRICTION_UNKNOWN_REASON
}

enum class AgeCheckTrigger {
    STARTUP,
    MANUAL_RETRY,
    RETURN_FROM_PLAY_STORE
}

data class AgeEligibilityEvaluation(
    val decision: AgeEligibilityDecision,
    val isAuthoritative: Boolean
)
