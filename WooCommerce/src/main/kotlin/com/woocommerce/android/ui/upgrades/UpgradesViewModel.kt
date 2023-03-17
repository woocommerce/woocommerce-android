package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.formatStyleFull
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.ZendeskTags
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.domain.CalculateRemainingTrialPeriod
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PERIOD
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_UPGRADE_PLAN
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.plans.trial.isFreeTrial
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesEvent.OpenSubscribeNow
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesEvent.OpenSupportRequestForm
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.Error
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.Loading
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.NonUpgradeable
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.TrialEnded
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.TrialInProgress
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Period
import javax.inject.Inject

@HiltViewModel
class UpgradesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val planRepository: SitePlanRepository,
    private val calculateRemainingTrialPeriod: CalculateRemainingTrialPeriod,
    private val resourceProvider: ResourceProvider,
    private val tracks: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {

    private val _upgradesState = MutableLiveData<UpgradesViewState>()
    val upgradesState: LiveData<UpgradesViewState> = _upgradesState

    init {
        loadSubscriptionState()
    }

    private fun loadSubscriptionState() {
        launch {
            _upgradesState.value = Loading

            val site = selectedSite.get()
            val currentPlan = planRepository.fetchCurrentPlanDetails(site)?.prettifyName()
            val newState = if (currentPlan != null) {

                val remainingTrialPeriod =
                    calculateRemainingTrialPeriod(currentPlan.expirationDate)

                when (currentPlan.type) {
                    SitePlan.Type.FREE_TRIAL -> {
                        if (remainingTrialPeriod.isZero || remainingTrialPeriod.isNegative) {
                            TrialEnded(
                                name = resourceProvider.getString(R.string.free_trial_trial_ended)
                            )
                        } else {
                            TrialInProgress(
                                name = currentPlan.name,
                                freeTrialDuration = FREE_TRIAL_PERIOD,
                                leftInFreeTrialDuration = remainingTrialPeriod,
                            )
                        }
                    }

                    SitePlan.Type.OTHER -> {
                        NonUpgradeable(
                            name = currentPlan.name,
                            currentPlanEndDate = currentPlan.expirationDate.toLocalDate()
                                .formatStyleFull()
                        )
                    }
                }
            } else {
                Error
            }

            _upgradesState.value = newState
        }
    }

    private fun SitePlan.prettifyName(): SitePlan {
        return copy(name = this.name.removePrefix("WordPress.com "))
    }

    fun onSubscribeNowClicked() = triggerEvent(OpenSubscribeNow)

    fun onReportSubscriptionIssueClicked() {
        val tags = selectedSite.getIfExists()
            ?.takeIf { it.isFreeTrial }
            ?.let { listOf(ZendeskTags.freeTrialTag) }
            ?: emptyList()
        triggerEvent(OpenSupportRequestForm(HelpOrigin.UPGRADES, tags))
    }

    fun onPlanUpgraded() {
        launch {
            loadSubscriptionState()
        }
    }

    fun onPlanUpgradeDismissed() {
        tracks.track(
            AnalyticsEvent.PLAN_UPGRADE_ABANDONED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_UPGRADES_SCREEN)
        )
    }

    sealed interface UpgradesViewState {

        sealed interface HasPlan : UpgradesViewState {
            val name: String
        }

        object Loading : UpgradesViewState

        object Error : UpgradesViewState

        data class TrialEnded(
            override val name: String,
            val planToUpgrade: String = FREE_TRIAL_UPGRADE_PLAN
        ) : HasPlan

        data class TrialInProgress(
            override val name: String,
            val freeTrialDuration: Period,
            val leftInFreeTrialDuration: Period,
        ) : HasPlan

        data class NonUpgradeable(
            override val name: String,
            val currentPlanEndDate: String
        ) : HasPlan
    }

    sealed class UpgradesEvent : MultiLiveEvent.Event() {
        object OpenSubscribeNow : UpgradesEvent()
        data class OpenSupportRequestForm(
            val origin: HelpOrigin,
            val extraTags: List<String>
        ) : UpgradesEvent()
    }
}
