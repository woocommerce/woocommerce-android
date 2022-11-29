package com.woocommerce.android.ui.login.storecreation.plans

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent.SITE_CREATION_STEP
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STEP_PLAN_PURCHASE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STEP_WEB_CHECKOUT
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo.BillingPeriod.MONTHLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo.BillingPeriod.YEARLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo.Feature
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.CheckoutState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState.PlanPurchaseError
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState.PlanPurchaseError.PLAN_PURCHASE_FAILED
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState.PlanPurchaseError.SITE_CREATION_FAILED
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.PlanState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class PlansViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val repository: StoreCreationRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val NEW_SITE_LANGUAGE_ID = "en"
        private const val NEW_SITE_THEME = "pub/zoologist"
        private const val CART_URL = "https://wordpress.com/checkout"
        private const val WEBVIEW_SUCCESS_TRIGGER_KEYWORD = "https://wordpress.com/checkout/thank-you/"
        private const val WEBVIEW_EXIT_TRIGGER_KEYWORD = "https://woocommerce.com/"
        private const val ECOMMERCE_PLAN_NAME = "eCommerce"
        private const val ECOMMERCE_PLAN_PRICE_MONTHLY = "$70"
    }

    private val _viewState = savedState.getStateFlow<ViewState>(this, LoadingState)
    val viewState = _viewState.asLiveData()

    init {
        loadPlan()
        trackStep(VALUE_STEP_PLAN_PURCHASE)
    }

    private fun trackStep(step: String) {
        analyticsTrackerWrapper.track(
            SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to step
            )
        )
    }

    private fun loadPlan() {
        launch {
            _viewState.update {
                PlanState(
                    plan = Plan(
                        name = ECOMMERCE_PLAN_NAME,
                        billingPeriod = MONTHLY,
                        formattedPrice = ECOMMERCE_PLAN_PRICE_MONTHLY,
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

    private fun showCheckoutWebsite() {
        _viewState.update {
            CheckoutState(
                startUrl = "${CART_URL}/${newStore.data.domain}",
                successTriggerKeyword = WEBVIEW_SUCCESS_TRIGGER_KEYWORD,
                exitTriggerKeyword = WEBVIEW_EXIT_TRIGGER_KEYWORD
            )
        }
        trackStep(VALUE_STEP_WEB_CHECKOUT)
    }

    private fun launchInstallation() {
        suspend fun <T : Any?> handleErrorOrProceed(
            result: WooResult<T>,
            error: PlanPurchaseError,
            successAction: suspend () -> Unit
        ) {
            when {
                result.isError -> {
                    _viewState.update { ErrorState(error, result.error.message) }
                }
                result.model == null -> _viewState.update { ErrorState(error) }
                else -> successAction()
            }
        }

        _viewState.update { LoadingState }

        launch {
            val siteCreationResult = createSite()
            handleErrorOrProceed(siteCreationResult, SITE_CREATION_FAILED) {
                val cartCreationResult = repository.addPlanToCart(newStore.data.siteId!!)
                handleErrorOrProceed(cartCreationResult, PLAN_PURCHASE_FAILED) {
                    showCheckoutWebsite()
                }
            }
        }
    }

    private suspend fun createSite(): WooResult<Long> {
        val result = repository.createNewSite(
            SiteCreationData(
                siteDesign = NEW_SITE_THEME,
                domain = newStore.data.domain,
                title = newStore.data.name,
                segmentId = null
            ),
            NEW_SITE_LANGUAGE_ID,
            TimeZone.getDefault().id
        )

        if (!result.isError) {
            newStore.update(siteId = result.model)
        }

        return result
    }

    fun onExitTriggered() {
        triggerEvent(Exit)
    }

    fun onConfirmClicked() {
        launchInstallation()
    }

    fun onStoreCreated() {
        triggerEvent(NavigateToNextStep)
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object LoadingState : ViewState

        @Parcelize
        data class ErrorState(val error: PlanPurchaseError, val message: String? = null) : ViewState {
            enum class PlanPurchaseError {
                SITE_CREATION_FAILED,
                PLAN_PURCHASE_FAILED
            }
        }

        @Parcelize
        data class CheckoutState(
            val startUrl: String,
            val successTriggerKeyword: String,
            val exitTriggerKeyword: String
        ) : ViewState

        @Parcelize
        data class PlanState(
            val plan: PlanInfo
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

        enum class BillingPeriod(@StringRes val nameId: Int) {
            MONTHLY(string.store_creation_ecommerce_plan_period_month),
            YEARLY((string.store_creation_ecommerce_plan_period_year))
        }
    }

    object NavigateToNextStep : MultiLiveEvent.Event()
}
