package com.woocommerce.android.ui.login

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
class AgeCheckViewModel @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _isUnder13 = MutableStateFlow<Boolean?>(null)
    val isUnder13: StateFlow<Boolean?> = _isUnder13.asStateFlow()

    companion object {
        // https://developer.android.com/google/play/age-signals/use-age-signals-api#age-signals-responses
        const val USER_STATUS_VERIFIED = 1
        const val USER_STATUS_SUPERVISED = 2
        const val USER_STATUS_SUPERVISED_APPROVAL_PENDING = 3
        const val USER_STATUS_SUPERVISED_APPROVAL_DENIED = 4
        const val USER_STATUS_UNKNOWN = 5
        const val USER_STATUS_EMPTY = 0
    }

    fun checkAge(userStatus: Int = USER_STATUS_EMPTY, ageUpper: Int? = null) {
        scope.launch {
            // TODO: Integrate Google Play Age Signals API here.
            // Documentation: https://developer.android.com/google/play/age-signals/use-age-signals-api
            // Since the library might not be fully configured, we are using the passed values or defaults.

            val isUnderage = when (userStatus) {
                USER_STATUS_VERIFIED -> false
                USER_STATUS_SUPERVISED,
                USER_STATUS_SUPERVISED_APPROVAL_PENDING,
                USER_STATUS_SUPERVISED_APPROVAL_DENIED -> {
                    // Check if ageUpper is known and below 13
                    ageUpper != null && ageUpper < 13
                }
                USER_STATUS_UNKNOWN -> false // Safe default: allow access if unknown
                USER_STATUS_EMPTY -> false // Safe default
                else -> false
            }

            _isUnder13.value = isUnderage
        }
    }
}
