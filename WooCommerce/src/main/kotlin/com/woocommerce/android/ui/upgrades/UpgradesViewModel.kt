package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.trial.isFreeTrial
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.Loading
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpgradesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    selectedSite: SelectedSite
) : ScopedViewModel(savedState) {

    private val _upgradesState = MutableLiveData<UpgradesViewState>()
    val upgradesState: LiveData<UpgradesViewState> = _upgradesState

    init {
        _upgradesState.value = Loading
        viewModelScope.launch {
            selectedSite.observe().collect { site ->
                val newState = if (site.isFreeTrial) {
                    UpgradesViewState.Upgradeable(name = site?.planShortName.orEmpty(), 2, "todo", "todo")
                } else {
                    UpgradesViewState.NonUpgradeable(name = site?.planShortName.orEmpty(), "todo")
                }

                _upgradesState.value = newState
            }
        }
    }

    fun onSubscribeNowClicked() = triggerEvent(UpgradesEvent.OpenSubscribeNow)

    fun onReportSubscriptionIssueClicked() = Unit

    sealed interface UpgradesViewState {

        sealed interface HasPlan : UpgradesViewState {
            val name: String
        }

        object Loading : UpgradesViewState

        data class TrialEnded(
            override val name: String,
            val planToUpgrade: String = "eCommerce"
        ) : HasPlan

        data class Upgradeable(
            override val name: String,
            val daysLeftInCurrentPlan: Int,
            val currentPlanEndDate: String,
            val nextPlanMonthlyFee: String
        ) : HasPlan

        data class NonUpgradeable(
            override val name: String,
            val currentPlanEndDate: String
        ) : HasPlan
    }

    sealed class UpgradesEvent : MultiLiveEvent.Event() {
        object OpenSubscribeNow : UpgradesEvent()
    }
}
