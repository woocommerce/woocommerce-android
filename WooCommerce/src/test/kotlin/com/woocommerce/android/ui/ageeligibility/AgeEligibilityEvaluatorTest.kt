package com.woocommerce.android.ui.ageeligibility

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AgeEligibilityEvaluatorTest {
    private val evaluator = AgeEligibilityEvaluator()

    @Test
    fun `when status is verified, then result is authoritatively allowed`() {
        val evaluation = evaluate(LegacyAgeVerificationStatus.VERIFIED, ageUpper = null)

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(AgeEligibilityDecision.Allowed, isAuthoritative = true)
        )
    }

    @Test
    fun `given supervised user under 13, when evaluated, then result is authoritatively restricted`() {
        val evaluation = evaluate(LegacyAgeVerificationStatus.SUPERVISED, ageUpper = 12)

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE),
                isAuthoritative = true
            )
        )
    }

    @Test
    fun `given supervised user age 13, when evaluated, then result is authoritatively allowed`() {
        val evaluation = evaluate(LegacyAgeVerificationStatus.SUPERVISED, ageUpper = 13)

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(AgeEligibilityDecision.Allowed, isAuthoritative = true)
        )
    }

    @Test
    fun `given approval is pending, when evaluated, then legacy age behavior is preserved`() {
        val evaluation = evaluate(LegacyAgeVerificationStatus.SUPERVISED_APPROVAL_PENDING, ageUpper = 12)

        assertThat(evaluation.decision).isEqualTo(
            AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE)
        )
    }

    @Test
    fun `given approval is denied, when evaluated, then result is authoritatively restricted`() {
        val evaluation = evaluate(LegacyAgeVerificationStatus.SUPERVISED_APPROVAL_DENIED, ageUpper = 18)

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.SUPERVISED_APPROVAL_DENIED),
                isAuthoritative = true
            )
        )
    }

    @Test
    fun `given missing age, when evaluated, then prior restriction is preserved non-authoritatively`() {
        val priorRestriction = AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON

        val evaluation = evaluate(
            verificationStatus = LegacyAgeVerificationStatus.SUPERVISED,
            ageUpper = null,
            priorRestriction = priorRestriction
        )

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(
                AgeEligibilityDecision.Restricted(priorRestriction),
                isAuthoritative = false
            )
        )
    }

    @Test
    fun `given unknown status, when evaluated without prior restriction, then result allows non-authoritatively`() {
        val evaluation = evaluate(LegacyAgeVerificationStatus.UNKNOWN, ageUpper = 18)

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(AgeEligibilityDecision.Allowed, isAuthoritative = false)
        )
    }

    @Test
    fun `given unexpected status, when evaluated, then result is non-authoritative`() {
        val evaluation = evaluate(LegacyAgeVerificationStatus.UNEXPECTED, ageUpper = 18)

        assertThat(evaluation.isAuthoritative).isFalse()
    }

    private fun evaluate(
        verificationStatus: LegacyAgeVerificationStatus,
        ageUpper: Int?,
        priorRestriction: AgeRestrictionReason? = null
    ) = evaluator.evaluateLegacyResult(
        result = AgeCheckResult(verificationStatus, ageUpper),
        priorRestriction = priorRestriction
    )
}
