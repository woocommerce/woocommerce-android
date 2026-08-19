package com.woocommerce.android.ui.ageeligibility

import android.app.Activity
import com.woocommerce.android.AppPrefsWrapper
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
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
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
    private val analyticsTracker: AgeSignalsAnalyticsTracker = mock()
    private val evaluator = AgeEligibilityEvaluator()

    @Before
    fun setup() {
        whenever(prefsWrapper.userAgeRestrictionReason).thenReturn("")
        whenever(prefsWrapper.isUserAgeEligibleForAppUse).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AGE_ELIGIBILITY_CHECKS)).thenReturn(true)
    }

    @Test
    fun `given eligible shared result, when age is checked, then access is allowed authoritatively`() = testBlocking {
        val checker = createChecker()
        client.result = sharedResult(ageLower = 18, ageUpper = null)

        checker.checkAge(activity)

        assertThat(checker.ageEligibilityState.value.decision).isEqualTo(AgeEligibilityDecision.Allowed)
        assertThat(client.receivedActivity).isSameAs(activity)
        verify(prefsWrapper).userAgeRestrictionReason = ""
        verify(prefsWrapper).clearLegacyAgeRestriction()
        verify(accountRepository, never()).logout()
    }

    @Test
    fun `given shared age under 13, when checked, then restriction is persisted and user logs out`() = testBlocking {
        val checker = createChecker()
        client.result = sharedResult(ageLower = 0, ageUpper = 12)

        checker.checkAge(activity)

        assertThat(checker.ageEligibilityState.value.decision).isEqualTo(
            AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE)
        )
        verify(prefsWrapper).userAgeRestrictionReason = AgeRestrictionReason.BELOW_MINIMUM_AGE.name
        verify(prefsWrapper, never()).clearLegacyAgeRestriction()
        verify(accountRepository).logout()
    }

    @Test
    fun `given verification is required, when checked, then no logout or persistence change occurs`() = testBlocking {
        val checker = createChecker()
        client.result = AgeSignalsRequestResult(AgeSignalsAccessStatus.VERIFICATION_REQUIRED)

        checker.checkAge(activity)

        assertThat(checker.ageEligibilityState.value.decision)
            .isEqualTo(AgeEligibilityDecision.VerificationRequired)
        verify(accountRepository, never()).logout()
        verify(prefsWrapper, never()).userAgeRestrictionReason = ""
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
    fun `given prior restriction and non-authoritative result, when checked, then restriction is retained`() =
        testBlocking {
            stubPriorRestriction(AgeRestrictionReason.BELOW_MINIMUM_AGE)
            val checker = createChecker()
            client.result = AgeSignalsRequestResult(AgeSignalsAccessStatus.NOT_SHARED)

            checker.checkAge(activity)

            assertThat(checker.ageEligibilityState.value.decision).isEqualTo(
                AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE)
            )
            verify(accountRepository).logout()
        }

    @Test
    fun `given prior restriction and SDK failure, when checked, then restriction is retained`() = testBlocking {
        stubPriorRestriction(AgeRestrictionReason.BELOW_MINIMUM_AGE)
        val checker = createChecker()
        client.exception = requestFailure()

        checker.checkAge(activity)

        assertThat(checker.ageEligibilityState.value.decision).isEqualTo(
            AgeEligibilityDecision.Restricted(AgeRestrictionReason.BELOW_MINIMUM_AGE)
        )
        verify(accountRepository).logout()
    }

    @Test
    fun `given verification state and SDK failure, when retried, then verification state is retained`() = testBlocking {
        val checker = createChecker()
        client.result = AgeSignalsRequestResult(AgeSignalsAccessStatus.VERIFICATION_REQUIRED)
        checker.checkAge(activity)
        client.exception = requestFailure()

        checker.checkAge(activity, AgeCheckTrigger.MANUAL_RETRY)

        assertThat(checker.ageEligibilityState.value.decision)
            .isEqualTo(AgeEligibilityDecision.VerificationRequired)
        verify(accountRepository, never()).logout()
    }

    @Test
    fun `given prior restriction and conclusive eligible result, when checked, then restriction is cleared`() =
        testBlocking {
            stubPriorRestriction(AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON)
            val checker = createChecker()
            client.result = sharedResult(ageLower = 13, ageUpper = 15)

            checker.checkAge(activity)

            assertThat(checker.ageEligibilityState.value.decision).isEqualTo(AgeEligibilityDecision.Allowed)
            verify(prefsWrapper).userAgeRestrictionReason = ""
            verify(accountRepository, never()).logout()
        }

    @Test
    fun `given migrated restriction is cleared, when checker is recreated, then access remains allowed`() =
        testBlocking {
            // GIVEN
            var persistedReason = ""
            var isPersistedEligible = false
            whenever(prefsWrapper.userAgeRestrictionReason).thenAnswer { persistedReason }
            whenever(prefsWrapper.isUserAgeEligibleForAppUse).thenAnswer { isPersistedEligible }
            doAnswer {
                persistedReason = it.getArgument(0)
                null
            }.whenever(prefsWrapper).userAgeRestrictionReason = any()
            doAnswer {
                isPersistedEligible = true
                null
            }.whenever(prefsWrapper).clearLegacyAgeRestriction()
            val checker = createChecker()
            client.result = sharedResult(ageLower = 18, ageUpper = null)

            // WHEN
            checker.checkAge(activity)
            val recreatedChecker = createChecker()

            // THEN
            assertThat(recreatedChecker.ageEligibilityState.value.decision)
                .isEqualTo(AgeEligibilityDecision.Allowed)
        }

    @Test
    fun `given feature is disabled, when checked, then persisted restriction is bypassed without clearing it`() =
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
            // GIVEN
            val checker = createChecker()
            client.gate = CompletableDeferred()

            // WHEN
            val firstCheck = launch { checker.checkAge(activity) }
            yield()
            checker.checkAge(activity, AgeCheckTrigger.MANUAL_RETRY)

            // THEN
            assertThat(client.callCount).isEqualTo(1)

            // WHEN
            client.gate?.complete(Unit)
            firstCheck.join()
            checker.checkAge(activity, AgeCheckTrigger.MANUAL_RETRY)

            // THEN
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
            client.result = sharedResult(ageLower = 0, ageUpper = 12)
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

    @Test
    fun `given Play Store retry overlaps a running check, when resumed again, then retry is preserved`() =
        testBlocking {
            // GIVEN
            val checker = createChecker()
            client.gate = CompletableDeferred()
            val runningCheck = launch { checker.checkAge(activity) }
            yield()
            checker.onPlayStoreOpenedForVerification()

            // WHEN
            checker.retryAfterReturningFromPlayStore(activity)

            // THEN
            assertThat(client.callCount).isEqualTo(1)
            verify(analyticsTracker, never()).trackVerificationAction(AgeCheckTrigger.RETURN_FROM_PLAY_STORE)

            // WHEN
            client.gate?.complete(Unit)
            runningCheck.join()
            checker.retryAfterReturningFromPlayStore(activity)

            // THEN
            assertThat(client.callCount).isEqualTo(2)
            verify(analyticsTracker).trackVerificationAction(AgeCheckTrigger.RETURN_FROM_PLAY_STORE)

            // WHEN
            checker.retryAfterReturningFromPlayStore(activity)

            // THEN
            assertThat(client.callCount).isEqualTo(2)
        }

    @Test
    fun `given a Play Store retry is cancelled, when resumed again, then retry is preserved`() = testBlocking {
        // GIVEN
        val checker = createChecker()
        client.gate = CompletableDeferred()
        checker.onPlayStoreOpenedForVerification()

        // WHEN
        val cancelledRetry = launch { checker.retryAfterReturningFromPlayStore(activity) }
        yield()

        // THEN
        assertThat(client.callCount).isEqualTo(1)

        // WHEN
        cancelledRetry.cancelAndJoin()
        client.gate = null
        checker.retryAfterReturningFromPlayStore(activity)

        // THEN
        assertThat(client.callCount).isEqualTo(2)
    }

    @Test
    fun `given Play Store was not opened, when activity resumes, then age is not checked`() = testBlocking {
        val checker = createChecker()

        checker.retryAfterReturningFromPlayStore(activity)

        assertThat(client.callCount).isZero()
    }

    @Test
    fun `given Play Store was opened, when activity resumes twice, then age is checked once`() = testBlocking {
        val checker = createChecker()
        checker.onPlayStoreOpenedForVerification()

        checker.retryAfterReturningFromPlayStore(activity)
        checker.retryAfterReturningFromPlayStore(activity)

        assertThat(client.callCount).isEqualTo(1)
        verify(analyticsTracker).trackPlayStoreOpened()
        verify(analyticsTracker).trackVerificationAction(AgeCheckTrigger.RETURN_FROM_PLAY_STORE)
    }

    private fun createChecker() = AgeEligibilityChecker(
        client = client,
        prefsWrapper = prefsWrapper,
        accountRepository = accountRepository,
        featureFlagRepository = featureFlagRepository,
        analyticsTracker = analyticsTracker,
        evaluator = evaluator,
        appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher)
    )

    private fun stubPriorRestriction(reason: AgeRestrictionReason) {
        whenever(prefsWrapper.userAgeRestrictionReason).thenReturn(reason.name)
    }

    private class FakeAgeSignalsClient : AgeSignalsClient {
        var result = sharedResult(ageLower = 18, ageUpper = null)
        var exception: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var callCount = 0
        var receivedActivity: Activity? = null

        override suspend fun requestAgeSignals(activity: Activity): AgeSignalsRequestResult {
            callCount++
            receivedActivity = activity
            gate?.await()
            exception?.let { throw it }
            return result
        }
    }

    companion object {
        private fun sharedResult(ageLower: Int?, ageUpper: Int?) = AgeSignalsRequestResult(
            accessStatus = AgeSignalsAccessStatus.SHARED,
            ageSignals = SharedAgeSignals(
                ageLower = ageLower,
                ageUpper = ageUpper,
                ageRangeSource = AppAgeRangeSource.UNSPECIFIED,
                significantChangeStatus = AppSignificantChangeStatus.UNSPECIFIED
            )
        )

        private fun requestFailure() = AgeSignalsRequestException(
            stage = AgeSignalsRequestStage.ACCESS,
            errorCode = AppAgeSignalsErrorCode.NETWORK_ERROR,
            retryCount = 0,
            cause = IllegalStateException()
        )
    }
}
