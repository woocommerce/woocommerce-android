package com.woocommerce.android.ui.ageeligibility

import com.google.android.gms.common.api.ApiException
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgeEligibilityChecker @Inject constructor(
    private val client: AgeSignalsClient,
    private val prefsWrapper: AppPrefsWrapper,
    private val accountRepository: AccountRepository,
    private val trackerWrapper: AnalyticsTrackerWrapper
) {

    private val _isUserAgeRangeEligible = MutableStateFlow(prefsWrapper.isUserAgeEligibleForAppUse)
    val isUserAgeRangeEligible: StateFlow<Boolean> = _isUserAgeRangeEligible.asStateFlow()

    suspend fun checkAge() {
        val trackingProperties = mutableMapOf<String, Any>()
        try {
            val result = client.checkAge()
            processAgeCheck(result.userStatus, result.ageUpper)
            if (isUserAgeRangeEligible.value.not()) {
                accountRepository.logout()
            }
            trackingProperties["retrieved_age"] = result.ageUpper ?: -1
            trackingProperties["user_status"] = getUserStatusAsString(result.userStatus)
        } catch (exception: ApiException) {
            WooLog.i(
                WooLog.T.UTILS,
                "AgeEligibilityChecker ${exception.javaClass.simpleName} while checking user " +
                    "age: ${exception.message}, reverting user eligibility to true"
            )
            _isUserAgeRangeEligible.value = true
        }
        trackingProperties["access_restricted"] = !_isUserAgeRangeEligible.value
        trackerWrapper.track(AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED, properties = trackingProperties)
        prefsWrapper.isUserAgeEligibleForAppUse = _isUserAgeRangeEligible.value
    }

    private fun processAgeCheck(userStatus: Int?, ageUpper: Int?) {
        val isUserAgeEligible = when (userStatus) {
            AgeSignalsVerificationStatus.VERIFIED -> true
            AgeSignalsVerificationStatus.SUPERVISED,
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING -> {
                if (ageUpper == null) {
                    true // If we can't determine the age return true
                } else {
                    ageUpper >= WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE
                }
            }

            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED -> false

            AgeSignalsVerificationStatus.UNKNOWN -> true // Safe default: allow access if unknown
            else -> true // Handle any other cases as default
        }
        _isUserAgeRangeEligible.value = isUserAgeEligible
    }

    private fun getUserStatusAsString(userStatus: Int?): String {
        return when (userStatus) {
            AgeSignalsVerificationStatus.VERIFIED -> "VERIFIED"
            AgeSignalsVerificationStatus.SUPERVISED -> "SUPERVISED"
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING -> "SUPERVISED_APPROVAL_PENDING"
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED -> "SUPERVISED_APPROVAL_DENIED"
            AgeSignalsVerificationStatus.UNKNOWN -> "UNKNOWN"
            else -> "UNKNOWN"
        }
    }

    companion object {
        private const val WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE = 13
    }
}
