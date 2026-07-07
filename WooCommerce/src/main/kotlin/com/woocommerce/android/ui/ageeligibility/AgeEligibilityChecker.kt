package com.woocommerce.android.ui.ageeligibility

import android.os.RemoteException
import androidx.annotation.StringRes
import com.google.android.gms.common.api.ApiException
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgeEligibilityChecker @Inject constructor(
    private val client: AgeSignalsClient,
    private val prefsWrapper: AppPrefsWrapper,
    private val accountRepository: AccountRepository,
    private val featureFlagRepository: FeatureFlagRepository,
    private val trackerWrapper: AnalyticsTrackerWrapper
) {

    private val _ageEligibilityState = MutableStateFlow(
        AgeEligibilityState(
            isUserAgeRangeEligible = prefsWrapper.isUserAgeEligibleForAppUse,
            ageRestrictedTitle = R.string.age_restriction_dialog_title,
            ageRestrictedMessage = R.string.age_restriction_supervised_user_account_dialog_message
        )
    )
    val ageEligibilityState: StateFlow<AgeEligibilityState> = _ageEligibilityState.asStateFlow()

    suspend fun checkAge() {
        if (featureFlagRepository.isEnabled(FeatureFlag.AGE_ELIGIBILITY_CHECKS)) {
            val trackingProperties = mutableMapOf<String, Any>()
            try {
                val result = client.checkAge()
                val isUserAgeEligible = isUserAgeEligibleForAppUse(result.userStatus, result.ageUpper)

                _ageEligibilityState.update {
                    ageEligibilityState.value.copy(
                        isUserAgeRangeEligible = isUserAgeEligible,
                        ageRestrictedMessage = if (isAgeBelowWooCommerceTOSMinimum(result.ageUpper)) {
                            R.string.age_restriction_user_below_tos_minimum_age_dialog_message
                        } else {
                            R.string.age_restriction_supervised_user_account_dialog_message
                        }
                    )
                }

                prefsWrapper.isUserAgeEligibleForAppUse = _ageEligibilityState.value.isUserAgeRangeEligible
                trackingProperties["retrieved_age"] = result.ageUpper ?: -1
                trackingProperties["user_status"] = getUserStatusAsString(result.userStatus)
            } catch (exception: ApiException) {
                revertEligibilityToDefault(exception)
            } catch (exception: RemoteException) {
                // The age signals service is backed by a Play Store binder that can die at any
                // time (e.g. Play Store killed or updated); the pending check then fails with a
                // plain RemoteException instead of an ApiException
                revertEligibilityToDefault(exception)
            }

            val isAccessRestricted = _ageEligibilityState.value.isUserAgeRangeEligible.not()
            trackingProperties["access_restricted"] = isAccessRestricted
            trackerWrapper.track(AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED, properties = trackingProperties)

            if (isAccessRestricted) {
                accountRepository.logout()
            }
        } else {
            _ageEligibilityState.update { _ageEligibilityState.value.copy(isUserAgeRangeEligible = true) }
        }
    }

    private fun revertEligibilityToDefault(exception: Exception) {
        WooLog.i(
            WooLog.T.UTILS,
            "AgeEligibilityChecker ${exception.javaClass.simpleName} while checking user " +
                "age: ${exception.message}, reverting user eligibility to default true"
        )
        _ageEligibilityState.update { _ageEligibilityState.value.copy(isUserAgeRangeEligible = true) }
    }

    private fun isAgeBelowWooCommerceTOSMinimum(ageUpper: Int?): Boolean =
        ageUpper != null && ageUpper < WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE

    private fun isUserAgeEligibleForAppUse(userStatus: Int?, ageUpper: Int?) = when (userStatus) {
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

    data class AgeEligibilityState(
        val isUserAgeRangeEligible: Boolean,
        @StringRes val ageRestrictedTitle: Int,
        @StringRes val ageRestrictedMessage: Int
    )

    companion object {
        private const val WOOCOMMERCE_TOS_MINIMUM_AGE_FOR_APP_USE = 13
    }
}
