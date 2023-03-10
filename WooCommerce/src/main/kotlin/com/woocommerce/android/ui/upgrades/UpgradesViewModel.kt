package com.woocommerce.android.ui.upgrades

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpgradesViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _upgradesState = MutableLiveData<UpgradesViewState>()
    val upgradesState: LiveData<UpgradesViewState> = _upgradesState

    fun onSubscribeNowClicked() = Unit

    fun onReportSubscriptionIssueClicked() = Unit

    data class UpgradesViewState(
        val currentPlan: CurrentPlanInfo = CurrentPlanInfo.NonUpgradeable(name = "")
    ) {
        sealed class CurrentPlanInfo {
            abstract val name: String

            data class Upgradeable(override val name: String) : CurrentPlanInfo()
            data class NonUpgradeable(override val name: String) : CurrentPlanInfo()
        }
    }
}
