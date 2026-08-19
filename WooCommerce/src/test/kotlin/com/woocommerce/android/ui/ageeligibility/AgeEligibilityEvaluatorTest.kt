package com.woocommerce.android.ui.ageeligibility

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AgeEligibilityEvaluatorTest {
    private val evaluator = AgeEligibilityEvaluator()

    @Test
    fun `given access was not shared, when evaluated, then prior restriction is preserved non-authoritatively`() {
        val evaluation = evaluate(
            result = AgeSignalsRequestResult(AgeSignalsAccessStatus.NOT_SHARED),
            priorRestriction = AgeRestrictionReason.BELOW_MINIMUM_AGE
        )

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE),
                isAuthoritative = false
            )
        )
    }

    @Test
    fun `given access was not shared without a prior restriction, when evaluated, then access is allowed`() {
        val evaluation = evaluate(AgeSignalsRequestResult(AgeSignalsAccessStatus.NOT_SHARED))

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(AgeEligibilityDecision.Allowed, isAuthoritative = false)
        )
    }

    @Test
    fun `given verification is required, when evaluated, then verification decision is returned`() {
        val evaluation = evaluate(AgeSignalsRequestResult(AgeSignalsAccessStatus.VERIFICATION_REQUIRED))

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(AgeEligibilityDecision.VerificationRequired, isAuthoritative = false)
        )
    }

    @Test
    fun `given unspecified or unexpected access, when evaluated, then result is non-authoritative`() {
        listOf(AgeSignalsAccessStatus.UNSPECIFIED, AgeSignalsAccessStatus.UNEXPECTED).forEach { status ->
            val evaluation = evaluate(AgeSignalsRequestResult(status))

            assertThat(evaluation).isEqualTo(
                AgeEligibilityEvaluation(AgeEligibilityDecision.Allowed, isAuthoritative = false)
            )
        }
    }

    @Test
    fun `given shared age below 13, when evaluated, then result is authoritatively restricted`() {
        val evaluation = evaluate(sharedResult(ageLower = 0, ageUpper = 12))

        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE),
                isAuthoritative = true
            )
        )
    }

    @Test
    fun `given shared upper age below 13 without lower bound, when evaluated, then result is restricted`() {
        // GIVEN
        val result = sharedResult(ageLower = null, ageUpper = 12)

        // WHEN
        val evaluation = evaluate(result)

        // THEN
        assertThat(evaluation).isEqualTo(
            AgeEligibilityEvaluation(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE),
                isAuthoritative = true
            )
        )
    }

    @Test
    fun `given a conclusive eligible shared age, when evaluated, then result is authoritatively allowed`() {
        listOf(
            sharedResult(ageLower = 13, ageUpper = 15),
            sharedResult(ageLower = 16, ageUpper = 17),
            sharedResult(ageLower = 18, ageUpper = null)
        ).forEach { result ->
            val evaluation = evaluate(
                result = result,
                priorRestriction = AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON
            )

            assertThat(evaluation).isEqualTo(
                AgeEligibilityEvaluation(AgeEligibilityDecision.Allowed, isAuthoritative = true)
            )
        }
    }

    @Test
    fun `given missing or crossing bounds, when evaluated, then prior restriction is preserved`() {
        listOf(
            sharedResult(ageLower = null, ageUpper = null),
            sharedResult(ageLower = 16, ageUpper = 15),
            sharedResult(ageLower = 12, ageUpper = 13)
        ).forEach { result ->
            val evaluation = evaluate(result, AgeRestrictionReason.BELOW_MINIMUM_AGE)

            assertThat(evaluation).isEqualTo(
                AgeEligibilityEvaluation(
                    AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE),
                    isAuthoritative = false
                )
            )
        }
    }

    @Test
    fun `given any significant change status with an eligible age, when evaluated, then app access is allowed`() {
        AppSignificantChangeStatus.entries.forEach { status ->
            val evaluation = evaluate(
                sharedResult(
                    ageLower = 18,
                    ageUpper = null,
                    significantChangeStatus = status
                )
            )

            assertThat(evaluation).isEqualTo(
                AgeEligibilityEvaluation(AgeEligibilityDecision.Allowed, isAuthoritative = true)
            )
        }
    }

    private fun evaluate(
        result: AgeSignalsRequestResult,
        priorRestriction: AgeRestrictionReason? = null
    ) = evaluator.evaluate(result, priorRestriction)

    private fun sharedResult(
        ageLower: Int?,
        ageUpper: Int?,
        significantChangeStatus: AppSignificantChangeStatus = AppSignificantChangeStatus.UNSPECIFIED
    ) = AgeSignalsRequestResult(
        accessStatus = AgeSignalsAccessStatus.SHARED,
        ageSignals = SharedAgeSignals(
            ageLower = ageLower,
            ageUpper = ageUpper,
            ageRangeSource = AppAgeRangeSource.UNSPECIFIED,
            significantChangeStatus = significantChangeStatus
        )
    )
}
