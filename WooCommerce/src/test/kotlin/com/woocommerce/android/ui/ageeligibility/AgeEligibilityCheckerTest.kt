package com.woocommerce.android.ui.ageeligibility

import android.app.Activity
import android.os.RemoteException
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AgeEligibilityCheckerTest : BaseUnitTest() {
    private val activity: Activity = mock()
    private val client = FakeAgeSignalsClient()
    private val prefsWrapper: AppPrefsWrapper = mock()
    private val accountRepository: AccountRepository = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()
    private val evaluator = AgeEligibilityEvaluator()

    @Before
    fun setup() {
        whenever(prefsWrapper.userAgeRestrictionReason).thenReturn("")
        whenever(prefsWrapper.isUserAgeEligibleForAppUse).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AGE_ELIGIBILITY_CHECKS)).thenReturn(true)
    }

    @Test
    fun `given verified result, when age is checked, then access is allowed authoritatively`() = testBlocking {
        val checker = createChecker()
        client.result = AgeCheckResult(LegacyAgeVerificationStatus.VERIFIED, DEFAULT_USER_AGE_UPPER)

        checker.checkAge(activity)

        assertThat(checker.ageEligibilityState.value.decision).isEqualTo(AgeEligibilityDecision.Allowed)
        assertThat(client.receivedActivity).isSameAs(activity)
        verify(prefsWrapper).userAgeRestrictionReason = ""
        verify(prefsWrapper).isUserAgeEligibleForAppUse = true
        verify(accountRepository, never()).logout()
        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                "retrieved_age" to DEFAULT_USER_AGE_UPPER,
                "user_status" to "VERIFIED",
                "access_restricted" to false
            )
        )
    }

    @Test
    fun `given supervised user under 13, when age is checked, then restriction is persisted and user logs out`() =
        testBlocking {
            val checker = createChecker()
            client.result = AgeCheckResult(LegacyAgeVerificationStatus.SUPERVISED, 12)

            checker.checkAge(activity)

            assertThat(checker.ageEligibilityState.value.decision).isEqualTo(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE)
            )
            verify(prefsWrapper).userAgeRestrictionReason = AgeRestrictionReason.BELOW_MINIMUM_AGE.name
            verify(prefsWrapper).isUserAgeEligibleForAppUse = false
            verify(accountRepository).logout()
        }

    @Test
    fun `given legacy false preference, when checker is created, then it migrates to an authoritative restriction`() {
        whenever(prefsWrapper.isUserAgeEligibleForAppUse).thenReturn(false)

        val checker = createChecker()

        assertThat(checker.ageEligibilityState.value.decision).isEqualTo(
            AgeEligibilityDecision.Restricted(AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON)
        )
        verify(prefsWrapper).userAgeRestrictionReason = AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON.name
    }

    @Test
    fun `given prior restriction and unknown result, when age is checked, then restriction is retained`() =
        testBlocking {
            stubPriorRestriction(AgeRestrictionReason.BELOW_MINIMUM_AGE)
            val checker = createChecker()
            client.result = AgeCheckResult(LegacyAgeVerificationStatus.UNKNOWN, DEFAULT_USER_AGE_UPPER)

            checker.checkAge(activity)

            assertThat(checker.ageEligibilityState.value.decision).isEqualTo(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE)
            )
            verify(prefsWrapper, never()).isUserAgeEligibleForAppUse = true
            verify(accountRepository).logout()
        }

    @Test
    fun `given prior restriction and SDK failure, when age is checked, then restriction is retained`() = testBlocking {
        stubPriorRestriction(AgeRestrictionReason.BELOW_MINIMUM_AGE)
        val checker = createChecker()
        client.exception = mock<RemoteException>()

        checker.checkAge(activity)

        assertThat(checker.ageEligibilityState.value.decision).isEqualTo(
            AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE)
        )
        verify(prefsWrapper, never()).isUserAgeEligibleForAppUse = true
        verify(accountRepository).logout()
    }

    @Test
    fun `given prior restriction and conclusive eligible result, when checked, then restriction is cleared`() =
        testBlocking {
            stubPriorRestriction(AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON)
            val checker = createChecker()
            client.result = AgeCheckResult(LegacyAgeVerificationStatus.SUPERVISED, 13)

            checker.checkAge(activity)

            assertThat(checker.ageEligibilityState.value.decision).isEqualTo(AgeEligibilityDecision.Allowed)
            verify(prefsWrapper).userAgeRestrictionReason = ""
            verify(prefsWrapper).isUserAgeEligibleForAppUse = true
            verify(accountRepository, never()).logout()
        }

    @Test
    fun `given feature is disabled, when age is checked, then persisted restriction is bypassed without clearing it`() =
        testBlocking {
            stubPriorRestriction(AgeRestrictionReason.BELOW_MINIMUM_AGE)
            whenever(featureFlagRepository.isEnabled(FeatureFlag.AGE_ELIGIBILITY_CHECKS)).thenReturn(false)
            val checker = createChecker()

            checker.checkAge(activity)

            assertThat(checker.ageEligibilityState.value.decision).isEqualTo(AgeEligibilityDecision.Allowed)
            assertThat(client.callCount).isZero()
            verify(prefsWrapper, never()).userAgeRestrictionReason = ""
            verify(accountRepository, never()).logout()
        }

    @Test
    fun `given a check is running, when checks overlap, then concurrent request is skipped until first finishes`() =
        testBlocking {
            val checker = createChecker()
            client.gate = CompletableDeferred()

            val firstCheck = launch { checker.checkAge(activity) }
            yield()
            checker.checkAge(activity, AgeCheckTrigger.MANUAL_RETRY)

            assertThat(client.callCount).isEqualTo(1)
            client.gate?.complete(Unit)
            firstCheck.join()

            checker.checkAge(activity, AgeCheckTrigger.MANUAL_RETRY)

            assertThat(client.callCount).isEqualTo(2)
        }

    @Test
    fun `given startup check is cancelled, when another activity resumes, then startup check is retried`() =
        testBlocking {
            // GIVEN
            val checker = createChecker()
            val nextActivity: Activity = mock()
            client.gate = CompletableDeferred()
            val cancelledCheck = launch { checker.checkAgeOnStartup(activity) }
            yield()

            // WHEN
            cancelledCheck.cancelAndJoin()
            client.gate = null
            checker.checkAgeOnStartup(nextActivity)

            // THEN
            assertThat(client.callCount).isEqualTo(2)
            assertThat(client.receivedActivity).isSameAs(nextActivity)

            // WHEN
            checker.checkAgeOnStartup(activity)

            // THEN
            assertThat(client.callCount).isEqualTo(2)
        }

    @Test
    fun `given restricted result, when activity check is cancelled, then logout completes in app scope`() =
        testBlocking {
            // GIVEN
            val logoutGate = CompletableDeferred<Unit>()
            val logoutCompleted = CompletableDeferred<Unit>()
            whenever(accountRepository.logout()).doSuspendableAnswer {
                logoutGate.await()
                logoutCompleted.complete(Unit)
                true
            }
            val checker = createChecker()
            client.result = AgeCheckResult(LegacyAgeVerificationStatus.SUPERVISED, 12)
            val activityCheck = launch(start = CoroutineStart.LAZY) { checker.checkAge(activity) }
            val restrictionObserver = launch(start = CoroutineStart.UNDISPATCHED) {
                checker.ageEligibilityState.drop(1).first {
                    it.decision is AgeEligibilityDecision.Restricted
                }
                activityCheck.cancel()
            }

            // WHEN
            activityCheck.start()
            activityCheck.join()
            restrictionObserver.join()
            logoutGate.complete(Unit)
            yield()

            // THEN
            assertThat(logoutCompleted.isCompleted).isTrue()
            verify(accountRepository).logout()
        }

    private fun createChecker() = AgeEligibilityChecker(
        client = client,
        prefsWrapper = prefsWrapper,
        accountRepository = accountRepository,
        featureFlagRepository = featureFlagRepository,
        trackerWrapper = trackerWrapper,
        evaluator = evaluator,
        appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher)
    )

    private fun stubPriorRestriction(reason: AgeRestrictionReason) {
        whenever(prefsWrapper.userAgeRestrictionReason).thenReturn(reason.name)
    }

    private class FakeAgeSignalsClient : AgeSignalsClient {
        var result = AgeCheckResult(LegacyAgeVerificationStatus.VERIFIED, DEFAULT_USER_AGE_UPPER)
        var exception: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var callCount = 0
        var receivedActivity: Activity? = null

        override suspend fun checkAge(activity: Activity): AgeCheckResult {
            callCount++
            receivedActivity = activity
            gate?.await()
            exception?.let { throw it }
            return result
        }
    }

    companion object {
        private const val DEFAULT_USER_AGE_UPPER = 19
    }
}
