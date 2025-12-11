package com.woocommerce.android.ui.login

import androidx.lifecycle.SavedStateHandle
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgeCheckViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val client: AgeSignalsClient
) : ScopedViewModel(savedStateHandle) {

    private val _isUserAgeRangeEligible = MutableStateFlow<Boolean?>(null)
    val isUserAgeRangeEligible: StateFlow<Boolean?> = _isUserAgeRangeEligible.asStateFlow()

    fun checkAge(userStatus: Int? = null, ageUpper: Int? = null) {
        launch {
            if (userStatus != null) {
                processAgeCheck(userStatus, ageUpper)
            } else {
                try {
                    val result = client.checkAge()
                    processAgeCheck(result.userStatus, result.ageUpper)
                } catch (e: Exception) {
                    WooLog.i(
                        T.UTILS,
                        "AgeCheckViewModel exception ${e.javaClass.simpleName} checking age: ${e.message}"
                    )
                    _isUserAgeRangeEligible.value = true
                }
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
