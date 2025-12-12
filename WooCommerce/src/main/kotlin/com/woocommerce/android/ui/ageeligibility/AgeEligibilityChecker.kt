package com.woocommerce.android.ui.ageeligibility

import com.google.android.gms.common.api.ApiException
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.woocommerce.android.AppPrefsWrapper
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
    private val accountRepository: AccountRepository
) {

    private val _isUserAgeRangeEligible = MutableStateFlow(prefsWrapper.isUserAgeEligibleForAppUse)
    val isUserAgeRangeEligible: StateFlow<Boolean> = _isUserAgeRangeEligible.asStateFlow()

    suspend fun checkAge() {
        try {
            val result = client.checkAge()
            processAgeCheck(result.userStatus, result.ageUpper)
        } catch (exception: ApiException) {
            WooLog.i(
                WooLog.T.UTILS,
                "AgeEligibilityChecker ${exception.javaClass.simpleName} while checking user " +
                    "age: ${exception.message}, reverting user eligibility to true"
            )
            prefsWrapper.isUserAgeEligibleForAppUse = true
            _isUserAgeRangeEligible.value = true
        }
        if (isUserAgeRangeEligible.value.not()) {
            accountRepository.logout()
        }
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

        prefsWrapper.isUserAgeEligibleForAppUse = isUserAgeEligible
        _isUserAgeRangeEligible.value = isUserAgeEligible
    }

    companion object {
        private const val WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE = 13
    }
}
