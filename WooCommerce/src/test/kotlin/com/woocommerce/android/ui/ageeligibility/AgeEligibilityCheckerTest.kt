package com.woocommerce.android.ui.ageeligibility

import com.google.android.play.agesignals.AgeSignalsException
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AgeEligibilityCheckerTest : BaseUnitTest() {

    private lateinit var ageEligibilityChecker: AgeEligibilityChecker
    private val client = FakeAgeSignalsClient()
    private val prefsWrapper: AppPrefsWrapper = mock()
    private val accountRepository: AccountRepository = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val trackerWrapper: AnalyticsTrackerWrapper = mock()

    @Before
    fun setup() = testBlocking {
        whenever(prefsWrapper.isUserAgeEligibleForAppUse).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AGE_ELIGIBILITY_CHECKS)).thenReturn(true)
        ageEligibilityChecker = AgeEligibilityChecker(
            client,
            prefsWrapper,
            accountRepository,
            featureFlagRepository,
            trackerWrapper
        )
    }

    @Test
    fun `given user is verified, when checkAge called, then user is eligible`() = testBlocking {
        client.setExpectedValues(AgeSignalsVerificationStatus.VERIFIED, DEFAULT_USER_AGE_UPPER)

        ageEligibilityChecker.checkAge()

        assertEquals(true, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
        verify(prefsWrapper).isUserAgeEligibleForAppUse = true

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
    fun `given user is supervised and age is under 13, when checkAge called, then user is NOT eligible`() =
        testBlocking {
            client.setExpectedValues(AgeSignalsVerificationStatus.SUPERVISED, 12)

            ageEligibilityChecker.checkAge()

            assertEquals(false, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
            verify(prefsWrapper).isUserAgeEligibleForAppUse = false
            verify(accountRepository).logout()

            verify(trackerWrapper).track(
                AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
                mapOf(
                    "retrieved_age" to 12,
                    "user_status" to "SUPERVISED",
                    "access_restricted" to true
                )
            )
        }

    @Test
    fun `given user is supervised and age is 13 or over, when checkAge called, then user is eligible`() = testBlocking {
        client.setExpectedValues(AgeSignalsVerificationStatus.SUPERVISED, 13)

        ageEligibilityChecker.checkAge()

        assertEquals(true, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
        verify(prefsWrapper).isUserAgeEligibleForAppUse = true

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                "retrieved_age" to 13,
                "user_status" to "SUPERVISED",
                "access_restricted" to false
            )
        )
    }

    @Test
    fun `given user is supervised approval pending and age is under 13, when checkAge called, then user is NOT eligible`() =
        testBlocking {
            client.setExpectedValues(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING, 12)

            ageEligibilityChecker.checkAge()

            assertEquals(false, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
            verify(prefsWrapper).isUserAgeEligibleForAppUse = false
            verify(accountRepository).logout()

            verify(trackerWrapper).track(
                AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
                mapOf(
                    "retrieved_age" to 12,
                    "user_status" to "SUPERVISED_APPROVAL_PENDING",
                    "access_restricted" to true
                )
            )
        }

    @Test
    fun `given user is supervised approval pending and age is 13 or over, when checkAge called, then user is eligible`() =
        testBlocking {
            client.setExpectedValues(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING, 13)

            ageEligibilityChecker.checkAge()

            assertEquals(true, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
            verify(prefsWrapper).isUserAgeEligibleForAppUse = true

            verify(trackerWrapper).track(
                AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
                mapOf(
                    "retrieved_age" to 13,
                    "user_status" to "SUPERVISED_APPROVAL_PENDING",
                    "access_restricted" to false
                )
            )
        }

    @Test
    fun `given user is supervised approval denied, when checkAge called, then user is NOT eligible`() = testBlocking {
        client.setExpectedValues(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED, DEFAULT_USER_AGE_UPPER)

        ageEligibilityChecker.checkAge()

        assertEquals(false, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
        verify(prefsWrapper).isUserAgeEligibleForAppUse = false
        verify(accountRepository).logout()

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                "retrieved_age" to DEFAULT_USER_AGE_UPPER,
                "user_status" to "SUPERVISED_APPROVAL_DENIED",
                "access_restricted" to true
            )
        )
    }

    @Test
    fun `given user status is unknown, when checkAge called, then user is eligible`() = testBlocking {
        client.setExpectedValues(AgeSignalsVerificationStatus.UNKNOWN, DEFAULT_USER_AGE_UPPER)

        ageEligibilityChecker.checkAge()

        assertEquals(true, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
        verify(prefsWrapper).isUserAgeEligibleForAppUse = true

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                "retrieved_age" to DEFAULT_USER_AGE_UPPER,
                "user_status" to "UNKNOWN",
                "access_restricted" to false
            )
        )
    }

    @Test
    fun `given checkAge throws exception, when checkAge called, then user is eligible`() = testBlocking {
        client.setThrowException(true)

        ageEligibilityChecker.checkAge()

        assertEquals(true, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                "access_restricted" to false
            )
        )
    }

    @Test
    fun `given user is supervised and ageUpper is null, when checkAge called, then user is eligible`() = testBlocking {
        client.setExpectedValues(AgeSignalsVerificationStatus.SUPERVISED, null)

        ageEligibilityChecker.checkAge()

        assertEquals(true, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
        verify(prefsWrapper).isUserAgeEligibleForAppUse = true

        verify(trackerWrapper).track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            mapOf(
                "retrieved_age" to -1,
                "user_status" to "SUPERVISED",
                "access_restricted" to false
            )
        )
    }

    @Test
    fun `given user is supervised approval pending and ageUpper is null, when checkAge called, then user is eligible`() =
        testBlocking {
            client.setExpectedValues(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING, null)

            ageEligibilityChecker.checkAge()

            assertEquals(true, ageEligibilityChecker.ageEligibilityState.value.isUserAgeRangeEligible)
            verify(prefsWrapper).isUserAgeEligibleForAppUse = true

            verify(trackerWrapper).track(
                AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
                mapOf(
                    "retrieved_age" to -1,
                    "user_status" to "SUPERVISED_APPROVAL_PENDING",
                    "access_restricted" to false
                )
            )
        }

    class FakeAgeSignalsClient : AgeSignalsClient {
        private var shouldThrow = false
        private var userStatus: Int = DEFAULT_USER_AGE_STATUS
        private var ageUpper: Int? = DEFAULT_USER_AGE_UPPER

        fun setExpectedValues(userStatus: Int, ageUpper: Int?) {
            this.userStatus = userStatus
            this.ageUpper = ageUpper
        }

        fun setThrowException(shouldThrow: Boolean) {
            this.shouldThrow = shouldThrow
        }

        override suspend fun checkAge(): AgeCheckResult {
            if (shouldThrow) {
                throw AgeSignalsException(-5)
            }
            return AgeCheckResult(userStatus, ageUpper)
        }
    }

    companion object {
        private const val DEFAULT_USER_AGE_STATUS = AgeSignalsVerificationStatus.VERIFIED
        private const val DEFAULT_USER_AGE_UPPER = 19
    }
}
