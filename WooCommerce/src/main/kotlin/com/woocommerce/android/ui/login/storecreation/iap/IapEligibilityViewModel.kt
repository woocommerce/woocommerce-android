package com.woocommerce.android.ui.login.storecreation.iap

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.PurchaseStatus
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.ui.login.storecreation.iap.IapEligibilityViewModel.IapEligibilityEvent.NavigateToNextStep
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IapEligibilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planSupportChecker: PurchaseWpComPlanSupportChecker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val isIAPEnabled: IsIAPEnabled,
    private val iapManager: PurchaseWPComPlanActions
) : ScopedViewModel(savedStateHandle, planSupportChecker) {
    private val _isCheckingIapEligibility = savedState.getStateFlow(scope = this, initialValue = true)
    val isCheckingIapEligibility: LiveData<Boolean> = _isCheckingIapEligibility.asLiveData()

    fun checkIapEligibility() {
        if (isIAPEnabled()) {
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
            AnalyticsEvent.SITE_CREATION_IAP_ELIGIBILITY_ERROR,
            mapOf(AnalyticsTracker.KEY_ERROR_TYPE to result.errorType.toString())
        )
        onUserNotEligibleForIAP(message = R.string.store_creation_iap_eligibility_check_error_not_available_for_country)
    }

    private fun onUserNotEligibleForIAP(
        @StringRes title: Int = R.string.store_creation_iap_eligibility_check_error_title,
        @StringRes message: Int
    ) {
        _isCheckingIapEligibility.value = false
        triggerDialogError(
            title = title,
            message = message
        )
    }

    private fun triggerDialogError(@StringRes title: Int, @StringRes message: Int) {
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = title,
                messageId = message,
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
            result.isSupported -> checkIfWooPlanAlreadyPurchased()
            else -> onUserNotEligibleForIAP(
                message = R.string.store_creation_iap_eligibility_check_error_not_available_for_country
            )
        }
    }

    private fun checkIfWooPlanAlreadyPurchased() {
        launch {
            when (val result = iapManager.isWPComPlanPurchased()) {
                is WPComIsPurchasedResult.Success -> {
                    when (result.purchaseStatus) {
                        PurchaseStatus.PURCHASED_AND_ACKNOWLEDGED,
                        PurchaseStatus.PURCHASED -> onUserNotEligibleForIAP(
                            message = R.string.store_creation_iap_eligibility_check_error_existing_subscription
                        )
                        PurchaseStatus.NOT_PURCHASED -> triggerEvent(NavigateToNextStep)
                    }
                }
                is WPComIsPurchasedResult.Error -> triggerEvent(NavigateToNextStep)
            }
        }
    }

    sealed class IapEligibilityEvent : MultiLiveEvent.Event() {
        object NavigateToNextStep : IapEligibilityEvent()
        object NavigateToWebStoreCreation : IapEligibilityEvent()
    }
}
