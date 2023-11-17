package com.woocommerce.android.ui.login.storecreation.plans

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.SITE_CREATION_STEP
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STEP_PLAN_PURCHASE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STEP_WEB_CHECKOUT
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Failure
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Success
import com.woocommerce.android.ui.login.storecreation.iap.IsIAPEnabled
import com.woocommerce.android.ui.login.storecreation.plans.BillingPeriod.ECOMMERCE_MONTHLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo.Feature
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.LoadingState
import com.woocommerce.android.util.SiteIndependentCurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.plans.full.Plan
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class PlansViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val repository: StoreCreationRepository,
    private val iapManager: PurchaseWPComPlanActions,
    private val isIAPEnabled: IsIAPEnabled,
    private val siteIndependentCurrencyFormatter: SiteIndependentCurrencyFormatter
) : ScopedViewModel(savedStateHandle, iapManager) {
    companion object {
        const val NEW_SITE_LANGUAGE_ID = "en"
        const val NEW_SITE_THEME = "premium/tsubaki"
        const val CART_URL = "https://wordpress.com/checkout"
        const val WEBVIEW_SUCCESS_TRIGGER_KEYWORD = "https://wordpress.com/checkout/thank-you/"
        const val WEBVIEW_EXIT_TRIGGER_KEYWORD = "https://woo.com/"
        const val ECOMMERCE_PLAN_NAME = "eCommerce"
        const val ECOMMERCE_PLAN_PRICE_MONTHLY = "$70"
    }

    private val _viewState = savedState.getStateFlow<ViewState>(this, LoadingState)
    val viewState = _viewState.asLiveData()

    private var iapPurchaseFlow: Flow<WPComPurchaseResult>? = null

    init {
        loadPlan()
        trackStep(VALUE_STEP_PLAN_PURCHASE)
    }

    fun onExitTriggered() {
        triggerEvent(Exit)
    }

    fun onConfirmClicked(activityWrapper: IAPActivityWrapper) {
        launch {
            val currentState = _viewState.value
            if (currentState is ViewState.PlanState) {
                _viewState.update { currentState.copy(showMainButtonLoading = true) }
            }
            createSite()
                .ifSuccessfulThen { siteId ->
                    newStore.update(siteId = siteId)
                    when {
                        isIAPEnabled() -> purchasePlanUsingIAP(siteId, activityWrapper)
                        else -> proceedToWebviewCheckout()
                    }
                }
        }
    }

    private suspend fun proceedToWebviewCheckout() {
        repository.addPlanToCart(
            newStore.data.planProductId,
            newStore.data.planPathSlug,
            newStore.data.siteId
        ).ifSuccessfulThen {
            showCheckoutWebsite()
        }
    }

    private suspend fun purchasePlanUsingIAP(siteId: Long, activityWrapper: IAPActivityWrapper) {
        observeInAppPurchasesResult(siteId)
        iapManager.purchaseWPComPlan(activityWrapper, siteId)
    }

    fun onPurchaseSuccess() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_IAP_PURCHASE_SUCCESS)
        triggerEvent(NavigateToNextStep)
    }

    private fun trackStep(step: String) {
        analyticsTrackerWrapper.track(SITE_CREATION_STEP, mapOf(AnalyticsTracker.KEY_STEP to step))
    }

    private fun loadPlan() = launch {
        _viewState.update { LoadingState }
        var plan = repository.fetchPlan(ECOMMERCE_MONTHLY)
        if (isIAPEnabled()) {
            plan = updatePlanWithIAPProductInfo(plan)
        }

        if (plan != null) {
            newStore.update(planProductId = plan.productId, planPathSlug = plan.pathSlug)
            _viewState.update {
                ViewState.PlanState(
                    plan = PlanInfo(
                        name = plan.productShortName ?: ECOMMERCE_PLAN_NAME,
                        billingPeriod = BillingPeriod.fromPeriodValue(plan.billPeriod),
                        formattedPrice = plan.formattedPrice ?: ECOMMERCE_PLAN_PRICE_MONTHLY,
                        features = listOf(
                            Feature(
                                iconId = drawable.ic_star,
                                textId = string.store_creation_ecommerce_plan_feature_themes
                            ),
                            Feature(
                                iconId = drawable.ic_box,
                                textId = string.store_creation_ecommerce_plan_feature_products
                            ),
                            Feature(
                                iconId = drawable.ic_present,
                                textId = string.store_creation_ecommerce_plan_feature_subscriptions
                            ),
                            Feature(
                                iconId = drawable.ic_chart,
                                textId = string.store_creation_ecommerce_plan_feature_reports
                            ),
                            Feature(
                                iconId = drawable.ic_dollar,
                                textId = string.store_creation_ecommerce_plan_feature_payments
                            ),
                            Feature(
                                iconId = drawable.ic_truck,
                                textId = string.store_creation_ecommerce_plan_feature_shipping_labels
                            ),
                            Feature(
                                iconId = drawable.ic_megaphone,
                                textId = string.store_creation_ecommerce_plan_feature_sales
                            )
                        )
                    )
                )
            }
        }
    }

    private suspend fun updatePlanWithIAPProductInfo(plan: Plan?): Plan? {
        val iapPlanDataResult = iapManager.fetchWPComPlanProduct()
        return if (iapPlanDataResult is WPComProductResult.Success) {
            plan?.copy(
                productShortName = iapPlanDataResult.productInfo.localizedTitle,
                formattedPrice = siteIndependentCurrencyFormatter.formatAmountWithCurrency(
                    amount = iapPlanDataResult.productInfo.price,
                    currencyCode = iapPlanDataResult.productInfo.currency
                )
            )
        } else plan
    }

    private fun showCheckoutWebsite() {
        _viewState.update {
            ViewState.CheckoutState(
                startUrl = "$CART_URL/${newStore.data.domain ?: newStore.data.siteId}",
                successTriggerKeyword = WEBVIEW_SUCCESS_TRIGGER_KEYWORD,
                exitTriggerKeyword = WEBVIEW_EXIT_TRIGGER_KEYWORD
            )
        }
        trackStep(VALUE_STEP_WEB_CHECKOUT)
    }

    private fun onInAppPurchaseError(error: WPComPurchaseResult.Error) {
        val errorType = error.errorType
        val reason = if (errorType is IAPError.RemoteCommunication.Server) {
            errorType.reason
        } else ""
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_IAP_PURCHASE_ERROR,
            mapOf(
                AnalyticsTracker.KEY_ERROR_TYPE to errorType.toString(),
                AnalyticsTracker.KEY_ERROR_DESC to reason
            )
        )
        _viewState.update { (_viewState.value as ViewState.PlanState).copy(showMainButtonLoading = false) }
    }

    private fun observeInAppPurchasesResult(remoteSiteId: Long) {
        if (iapPurchaseFlow == null) {
            iapPurchaseFlow = iapManager.getPurchaseWpComPlanResult(remoteSiteId)
            viewModelScope.launch {
                iapPurchaseFlow?.collectLatest { result ->
                    when (result) {
                        is WPComPurchaseResult.Success -> onPurchaseSuccess()
                        is WPComPurchaseResult.Error -> onInAppPurchaseError(result)
                    }.exhaustive
                }
            }
        }
    }

    private suspend fun createSite(): StoreCreationResult<Long> {
        suspend fun StoreCreationResult<Long>.recoverIfSiteExists(): StoreCreationResult<Long> {
            return if ((this as? Failure<Long>)?.type == SITE_ADDRESS_ALREADY_EXISTS) {
                repository.getSiteByUrl(newStore.data.domain)?.let { site ->
                    Success(site.siteId)
                } ?: this
            } else {
                this
            }
        }

        return repository.createNewSite(
            siteData = SiteCreationData(
                siteDesign = NEW_SITE_THEME,
                domain = newStore.data.domain,
                title = newStore.data.name,
                segmentId = null
            ),
            languageWordPressId = NEW_SITE_LANGUAGE_ID,
            timeZoneId = TimeZone.getDefault().id,
            siteCreationFlow = newStore.data.planPathSlug
        ).recoverIfSiteExists()
    }

    private suspend fun <T : Any?> StoreCreationResult<T>.ifSuccessfulThen(
        successAction: suspend (T) -> Unit
    ) {
        when (this) {
            is Success -> successAction(this.data)
            is Failure -> _viewState.update { ErrorState(this.type, this.message) }
        }
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object LoadingState : ViewState

        @Parcelize
        data class ErrorState(val errorType: StoreCreationErrorType, val message: String? = null) : ViewState

        @Parcelize
        data class CheckoutState(
            val startUrl: String,
            val successTriggerKeyword: String,
            val exitTriggerKeyword: String
        ) : ViewState

        @Parcelize
        data class PlanState(
            val plan: PlanInfo,
            val showMainButtonLoading: Boolean = false
        ) : ViewState
    }

    @Parcelize
    data class PlanInfo(
        val name: String,
        val billingPeriod: BillingPeriod,
        val formattedPrice: String,
        val features: List<Feature>
    ) : Parcelable {

        @Parcelize
        data class Feature(
            @DrawableRes val iconId: Int,
            @StringRes val textId: Int
        ) : Parcelable
    }

    object NavigateToNextStep : MultiLiveEvent.Event()
}
