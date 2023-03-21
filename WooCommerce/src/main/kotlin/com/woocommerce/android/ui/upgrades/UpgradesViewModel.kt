package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatStyleFull
import com.woocommerce.android.extensions.pluralizedDays
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.ZendeskTags
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

    fun onSubscribeNowClicked() {
        tracks.track(AnalyticsEvent.FREE_TRIAL_UPGRADE_NOW_TAPPED, tracksProperties)
        triggerEvent(OpenSubscribeNow)
    }

    fun onReportSubscriptionIssueClicked() {
        tracks.track(AnalyticsEvent.UPGRADES_REPORT_SUBSCRIPTION_ISSUE_TAPPED, tracksProperties)

        val tags = selectedSite.getIfExists()
            ?.takeIf { it.isFreeTrial }
            ?.let { listOf(ZendeskTags.freeTrialTag) }
            ?: emptyList()
        triggerEvent(OpenSupportRequestForm(HelpOrigin.UPGRADES, tags))
    }

    fun onPlanUpgraded() {
        tracks.track(AnalyticsEvent.PLAN_UPGRADE_SUCCESS, tracksProperties)
        launch {
            loadSubscriptionState()
        }
    }

    fun onPlanUpgradeDismissed() {
        tracks.track(AnalyticsEvent.PLAN_UPGRADE_ABANDONED, tracksProperties)
    }

    private fun loadSubscriptionState() {
        launch {
            _upgradesState.value = Loading

            _upgradesState.value = planRepository
                .fetchCurrentPlanDetails(selectedSite.get())
                ?.asViewState()
                ?: Error
        }
    }

    private fun SitePlan.asViewState() =
        when (type) {
            SitePlan.Type.FREE_TRIAL -> generateHasPlanViewState()
            SitePlan.Type.OTHER -> NonUpgradeable(
                name = prettifiedName,
                currentPlanEndDate = expirationDate.toLocalDate().formatStyleFull()
            )
        }

    private fun SitePlan.generateHasPlanViewState(): UpgradesViewState.HasPlan {
        val remainingTrialPeriod = calculateRemainingTrialPeriod(expirationDate)
        return if (remainingTrialPeriod.isZero || remainingTrialPeriod.isNegative) {
            TrialEnded(
                name = resourceProvider.getString(R.string.free_trial_trial_ended)
            )
        } else {
            TrialInProgress(
                name = prettifiedName,
                freeTrialDuration = FREE_TRIAL_PERIOD,
                daysLeftInFreeTrial = remainingTrialPeriod.pluralizedDays(resourceProvider)
            )
        }
    }

    private val SitePlan.prettifiedName
        get() = name.removePrefix("WordPress.com ")

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
            val daysLeftInFreeTrial: String
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

    companion object {
        private val tracksProperties = mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_UPGRADES_SCREEN)
    }
}
