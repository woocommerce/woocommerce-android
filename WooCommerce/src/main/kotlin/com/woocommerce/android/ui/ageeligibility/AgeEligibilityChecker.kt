package com.woocommerce.android.ui.ageeligibility

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
        } catch (e: Exception) {
            WooLog.i(
                WooLog.T.UTILS,
                "AgeCheckViewModel exception ${e.javaClass.simpleName} checking age: ${e.message}, " +
                    "setting age eligibility to default value: eligible to use the app"
            )
            _isUserAgeRangeEligible.value = true
        }
        if(isUserAgeRangeEligible.value.not()){
            accountRepository.logout()
        }
    }

    private fun processAgeCheck(userStatus: Int?, ageUpper: Int?) {
        val isUserAgeEligible = when (userStatus) {
            AgeSignalsVerificationStatus.VERIFIED -> true
            AgeSignalsVerificationStatus.SUPERVISED,
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING -> {
                // Check if ageUpper is known and 13 or above
                ageUpper != null && ageUpper >= 13 // Woo TOS states our apps are for 13+ years old users
            }

            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED -> false

            AgeSignalsVerificationStatus.UNKNOWN -> true // Safe default: allow access if unknown
            else -> true // Handle any other cases as default
        }

        prefsWrapper.isUserAgeEligibleForAppUse = isUserAgeEligible
        _isUserAgeRangeEligible.value = isUserAgeEligible
    }
}
