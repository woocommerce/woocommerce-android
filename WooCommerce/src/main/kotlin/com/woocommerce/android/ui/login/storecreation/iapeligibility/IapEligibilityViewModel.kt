package com.woocommerce.android.ui.login.storecreation.iapeligibility

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.ui.login.storecreation.iapeligibility.IapEligibilityViewModel.IapEligibilityEvent.NavigateToNextStep
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IapEligibilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planSupportChecker: PurchaseWpComPlanSupportChecker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle, planSupportChecker) {
    fun checkIapEligibility() {
        if (FeatureFlag.IAP_FOR_STORE_CREATION.isEnabled()) {
            launch {
                when (val result = planSupportChecker.isIAPSupported()) {
                    is IAPSupportedResult.Success -> onSuccess(result)
                    is IAPSupportedResult.Error -> onError(result)
                }
            }
        } else {
            triggerEvent(NavigateToNextStep)
        }
    }

    private fun onError(result: IAPSupportedResult.Error) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_IAP_ERROR,
            mapOf(AnalyticsTracker.KEY_ERROR_TYPE to result.errorType.toString())
        )
        onUserNotEligibleForIAP()
    }

    private fun onUserNotEligibleForIAP() {
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = R.string.store_creation_iap_eligibility_check_error_title,
                messageId = R.string.store_creation_iap_eligibility_check_error_description,
                negativeBtnAction = { dialog, _ ->
                    triggerEvent(MultiLiveEvent.Event.Exit)
                    dialog.dismiss()
                },
                negativeButtonId = R.string.close
            )
        )
    }

    private fun onSuccess(result: IAPSupportedResult.Success) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_IAP_ELIGIBILITY,
            mapOf(AnalyticsTracker.KEY_IAP_ELIGIBLE to result.isSupported)
        )
        when {
            result.isSupported -> triggerEvent(NavigateToNextStep)
            else -> onUserNotEligibleForIAP()
        }
    }

    sealed class IapEligibilityEvent : MultiLiveEvent.Event() {
        object NavigateToNextStep : IapEligibilityEvent()
        object NavigateToWebStoreCreation : IapEligibilityEvent()
    }
}
