package com.woocommerce.android.ui.ageeligibility

import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AgeEligibilityChecker @Inject constructor(
    private val client: AgeSignalsClient,
) {

    private val _isUserAgeRangeEligible = MutableStateFlow<Boolean?>(null)
    val isUserAgeRangeEligible: StateFlow<Boolean?> = _isUserAgeRangeEligible.asStateFlow()

    suspend fun checkAge(userStatus: Int? = null, ageUpper: Int? = null) {
        if (userStatus != null) {
            processAgeCheck(userStatus, ageUpper)
        } else {
            try {
                val result = client.checkAge()
                processAgeCheck(result.userStatus, result.ageUpper)
            } catch (e: Exception) {
                WooLog.i(
                    WooLog.T.UTILS,
                    "AgeCheckViewModel exception ${e.javaClass.simpleName} checking age: ${e.message}"
                )
                _isUserAgeRangeEligible.value = true
            }
        }
    }

    private fun processAgeCheck(userStatus: Int?, ageUpper: Int?) {
        val isUserAgeEligible = when (userStatus) {
            AgeSignalsVerificationStatus.VERIFIED -> true
            AgeSignalsVerificationStatus.SUPERVISED,
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING -> {
                // Check if ageUpper is known and below 13
                ageUpper != null && ageUpper < 13 // Woo TOS states our apps are for 13+ years old users
            }

            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED -> false

            AgeSignalsVerificationStatus.UNKNOWN -> true // Safe default: allow access if unknown
            else -> true // Handle other cases or default
        }

        _isUserAgeRangeEligible.value = isUserAgeEligible
    }
}
