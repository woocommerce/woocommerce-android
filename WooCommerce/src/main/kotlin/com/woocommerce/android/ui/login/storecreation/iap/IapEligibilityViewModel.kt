package com.woocommerce.android.ui.login.storecreation.iap

import android.content.DialogInterface
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
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.PurchaseStatus
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.iap.IapEligibilityViewModel.IapEligibilityEvent.NavigateToNextStep
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
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
                    is IAPSupportedResult.Error -> onIAPError(result.errorType)
                }
            }
        } else {
            triggerEvent(NavigateToNextStep)
        }
    }

    private fun onUserNotEligibleForIAP(
        @StringRes title: Int = R.string.store_creation_iap_eligibility_check_error_title,
        @StringRes message: Int,
        @StringRes positiveButtonId: Int? = null,
        positiveBtnAction: DialogInterface.OnClickListener? = null,
    ) {
        _isCheckingIapEligibility.value = false
        triggerDialogError(
            title = title,
            message = message,
            positiveButtonId = positiveButtonId,
            positiveBtnAction = positiveBtnAction
        )
    }

    private fun triggerDialogError(
        @StringRes title: Int,
        @StringRes message: Int,
        @StringRes positiveButtonId: Int? = null,
        positiveBtnAction: DialogInterface.OnClickListener? = null,
    ) {
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = title,
                messageId = message,
                positiveButtonId = positiveButtonId,
                positiveBtnAction = positiveBtnAction,
                negativeBtnAction = { dialog, _ ->
                    triggerEvent(MultiLiveEvent.Event.Exit)
                    dialog.dismiss()
                },
                negativeButtonId = R.string.close,
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
                        PurchaseStatus.PURCHASED -> {
                            onUserNotEligibleForIAP(
                                message = R.string.store_creation_iap_eligibility_existing_purchase_not_acknowledged,
                                positiveButtonId = R.string.support_contact,
                                positiveBtnAction = { dialog, _ ->
                                    triggerEvent(NavigateToHelpScreen(STORE_CREATION))
                                    dialog.dismiss()
                                }
                            )
                        }
                        PurchaseStatus.PURCHASED_AND_ACKNOWLEDGED -> onUserNotEligibleForIAP(
                            message = R.string.store_creation_iap_eligibility_check_error_existing_subscription
                        )
                        PurchaseStatus.NOT_PURCHASED -> triggerEvent(NavigateToNextStep)
                    }
                }
                is WPComIsPurchasedResult.Error -> onIAPError(result.errorType)
            }
        }
    }

    private fun onIAPError(error: IAPError) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_IAP_ELIGIBILITY_ERROR,
            mapOf(AnalyticsTracker.KEY_ERROR_TYPE to error.toString())
        )
        val message = when (error) {
            is IAPError.Billing.DeveloperError,
            is IAPError.Billing.ServiceDisconnected,
            is IAPError.Billing.FeatureNotSupported,
            is IAPError.Billing.BillingUnavailable,
            is IAPError.Billing.Unknown,
            is IAPError.Billing.ItemUnavailable,
            is IAPError.Billing.ServiceTimeout,
            is IAPError.Billing.ServiceUnavailable,
            is IAPError.Billing.ItemNotOwned,
            is IAPError.Billing.UserCancelled -> R.string.store_creation_iap_eligibility_check_generic_error
            is IAPError.Billing.ItemAlreadyOwned ->
                R.string.store_creation_iap_eligibility_check_error_existing_subscription
            is IAPError.RemoteCommunication -> R.string.store_creation_iap_eligibility_network_error
        }
        onUserNotEligibleForIAP(message = message)
    }

    sealed class IapEligibilityEvent : MultiLiveEvent.Event() {
        object NavigateToNextStep : IapEligibilityEvent()
    }
}
