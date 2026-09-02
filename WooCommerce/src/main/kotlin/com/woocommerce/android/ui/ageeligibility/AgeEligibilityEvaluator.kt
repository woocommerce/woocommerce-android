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
        val ageRangeOutcome = signals.toAgeRangeOutcome()
        return when (ageRangeOutcome) {
            AgeRangeOutcome.BELOW_13 -> authoritativeRestriction(
                reason = AgeRestrictionReason.BELOW_MINIMUM_AGE,
                ageRangeOutcome = ageRangeOutcome
            )

            AgeRangeOutcome.AMBIGUOUS -> nonAuthoritative(priorRestriction, ageRangeOutcome)
            AgeRangeOutcome.AGE_13_15,
            AgeRangeOutcome.AGE_16_17,
            AgeRangeOutcome.AGE_18_PLUS,
            AgeRangeOutcome.ELIGIBLE -> authoritativeAllowed(ageRangeOutcome)
        }
    }

    private fun SharedAgeSignals?.toAgeRangeOutcome(): AgeRangeOutcome {
        val ageUpper = this?.ageUpper
        if (ageUpper != null && ageUpper < WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE) {
            return AgeRangeOutcome.BELOW_13
        }

        val ageLower = this?.ageLower ?: return AgeRangeOutcome.AMBIGUOUS
        if (ageUpper != null && ageLower > ageUpper) return AgeRangeOutcome.AMBIGUOUS

        return when {
            ageLower >= MINIMUM_ADULT_AGE -> AgeRangeOutcome.AGE_18_PLUS
            ageLower.isWithin(ageUpper, OLDER_TEEN_MINIMUM, OLDER_TEEN_MAXIMUM) -> AgeRangeOutcome.AGE_16_17
            ageLower.isWithin(
                ageUpper,
                WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE,
                YOUNGER_TEEN_MAXIMUM
            ) -> AgeRangeOutcome.AGE_13_15

            ageLower >= WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE -> AgeRangeOutcome.ELIGIBLE
            else -> AgeRangeOutcome.AMBIGUOUS
        }
    }

    private fun Int.isWithin(upper: Int?, minimum: Int, maximum: Int) =
        this >= minimum && upper != null && upper <= maximum

    private fun authoritativeAllowed(ageRangeOutcome: AgeRangeOutcome) = AgeEligibilityEvaluation(
        decision = AgeEligibilityDecision.Allowed,
        isAuthoritative = true,
        ageRangeOutcome = ageRangeOutcome
    )

    private fun authoritativeRestriction(
        reason: AgeRestrictionReason,
        ageRangeOutcome: AgeRangeOutcome
    ) = AgeEligibilityEvaluation(
        decision = AgeEligibilityDecision.Restricted(reason),
        isAuthoritative = true,
        ageRangeOutcome = ageRangeOutcome
    )

    private fun nonAuthoritative(
        priorRestriction: AgeRestrictionReason?,
        ageRangeOutcome: AgeRangeOutcome? = null
    ) = AgeEligibilityEvaluation(
        decision = priorRestriction?.let(AgeEligibilityDecision::Restricted) ?: AgeEligibilityDecision.Allowed,
        isAuthoritative = false,
        ageRangeOutcome = ageRangeOutcome
    )

    companion object {
        private const val WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE = 13
        private const val YOUNGER_TEEN_MAXIMUM = 15
        private const val OLDER_TEEN_MINIMUM = 16
        private const val OLDER_TEEN_MAXIMUM = 17
        private const val MINIMUM_ADULT_AGE = 18
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
    val isAuthoritative: Boolean,
    val ageRangeOutcome: AgeRangeOutcome? = null
)

enum class AgeRangeOutcome {
    BELOW_13,
    AGE_13_15,
    AGE_16_17,
    AGE_18_PLUS,
    ELIGIBLE,
    AMBIGUOUS
}
