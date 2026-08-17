package com.woocommerce.android.ui.ageeligibility

import javax.inject.Inject

class AgeEligibilityEvaluator @Inject constructor() {
    fun evaluateLegacyResult(
        result: AgeCheckResult,
        priorRestriction: AgeRestrictionReason?
    ): AgeEligibilityEvaluation = when (result.verificationStatus) {
        LegacyAgeVerificationStatus.VERIFIED -> authoritativeAllowed()
        LegacyAgeVerificationStatus.SUPERVISED,
        LegacyAgeVerificationStatus.SUPERVISED_APPROVAL_PENDING -> evaluateAgeUpper(
            ageUpper = result.ageUpper,
            priorRestriction = priorRestriction
        )

        LegacyAgeVerificationStatus.SUPERVISED_APPROVAL_DENIED -> authoritativeRestriction(
            AgeRestrictionReason.SUPERVISED_APPROVAL_DENIED
        )

        LegacyAgeVerificationStatus.UNKNOWN,
        LegacyAgeVerificationStatus.UNEXPECTED -> nonAuthoritative(priorRestriction)
    }

    fun preservePriorRestriction(priorRestriction: AgeRestrictionReason?): AgeEligibilityEvaluation =
        nonAuthoritative(priorRestriction)

    private fun evaluateAgeUpper(
        ageUpper: Int?,
        priorRestriction: AgeRestrictionReason?
    ): AgeEligibilityEvaluation = when {
        ageUpper == null -> nonAuthoritative(priorRestriction)
        ageUpper < WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE -> authoritativeRestriction(
            AgeRestrictionReason.BELOW_MINIMUM_AGE
        )

        else -> authoritativeAllowed()
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
    LEGACY_RESTRICTION_UNKNOWN_REASON,
    SUPERVISED_APPROVAL_DENIED
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
