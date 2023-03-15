package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.plans.trial.isFreeTrial
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesEvent.OpenSubscribeNow
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesEvent.OpenSupportRequestForm
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.CurrentPlanInfo.NonUpgradeable
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.CurrentPlanInfo.Upgradeable
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
        _upgradesState.value = UpgradesViewState()
        viewModelScope.launch {
            selectedSite.observe().collect { site ->
                val currentPlan = if (site.isFreeTrial) {
                    Upgradeable(name = site?.planShortName.orEmpty())
                } else {
                    NonUpgradeable(name = site?.planShortName.orEmpty())
                }

                _upgradesState.value = _upgradesState.value?.copy(currentPlan = currentPlan)
            }
        }
    }

    fun onSubscribeNowClicked() = triggerEvent(OpenSubscribeNow)

    fun onReportSubscriptionIssueClicked() = triggerEvent(
        OpenSupportRequestForm(HelpOrigin.UPGRADES, ArrayList())
    )

    data class UpgradesViewState(
        val currentPlan: CurrentPlanInfo = NonUpgradeable(name = "")
    ) {
        sealed class CurrentPlanInfo {
            abstract val name: String

            data class Upgradeable(override val name: String) : CurrentPlanInfo()
            data class NonUpgradeable(override val name: String) : CurrentPlanInfo()
        }
    }

    sealed class UpgradesEvent : MultiLiveEvent.Event() {
        object OpenSubscribeNow : UpgradesEvent()
        data class OpenSupportRequestForm(
            val origin: HelpOrigin,
            val extraTags: ArrayList<String>
        ): UpgradesEvent()
    }
}
