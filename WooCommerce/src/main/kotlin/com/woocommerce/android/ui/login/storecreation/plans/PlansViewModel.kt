package com.woocommerce.android.ui.login.storecreation.plans

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Plan.BillingPeriod.MONTHLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Plan.Feature
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
import javax.inject.Inject

@HiltViewModel
class PlansViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val ECOMMERCE_PLAN_NAME = "eCommerce"
        private const val ECOMMERCE_PLAN_PRICE_MONTHLY = "$69.99"
    }

    private val _viewState = savedState.getStateFlow<ViewState>(this, LoadingState)
    val viewState = _viewState.asLiveData()

    init {
        loadPlan()
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

    fun onCloseClicked() {
        triggerEvent(Exit)
    }

    fun onConfirmClicked() {
        triggerEvent(NavigateToNextStep)
    }

    fun onRetryClicked() {
        // TODO
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object LoadingState : ViewState

        @Parcelize
        object ErrorState : ViewState

        @Parcelize
        data class PlanState(
            val plan: Plan
        ) : ViewState
    }

    @Parcelize
    data class Plan(
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
