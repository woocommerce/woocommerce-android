package com.woocommerce.android.ui.login

import android.content.Context
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgeCheckViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _isUnder13 = MutableStateFlow<Boolean?>(null)
    val isUnder13: StateFlow<Boolean?> = _isUnder13.asStateFlow()

    fun checkAge(userStatus: Int? = null, ageUpper: Int? = null) {
        scope.launch {
            if (userStatus != null) {
                processAgeCheck(userStatus, ageUpper)
            } else {
                try {
                    val ageSignalsManager = AgeSignalsManagerFactory.create(context)
                    ageSignalsManager
                        .checkAgeSignals(AgeSignalsRequest.builder().build())
                        .addOnSuccessListener { ageSignalsResult ->
                            processAgeCheck(ageSignalsResult.userStatus(), ageSignalsResult.ageUpper())
                        }
                        .addOnFailureListener {
                            // Default to false (allow access) on failure
                            _isUnder13.value = false
                        }
                } catch (e: Exception) {
                    _isUnder13.value = false
                }
            }
        }
    }

    private fun processAgeCheck(userStatus: Int?, ageUpper: Int?) {
        val isUnderage = when (userStatus) {
            AgeSignalsVerificationStatus.VERIFIED -> false
            AgeSignalsVerificationStatus.SUPERVISED,
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING,
            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED -> {
                // Check if ageUpper is known and below 13
                ageUpper != null && ageUpper < 13
            }
            AgeSignalsVerificationStatus.UNKNOWN -> false // Safe default: allow access if unknown
            // Handle other cases or default
            else -> false
        }

        _isUnder13.value = isUnderage
    }
}
