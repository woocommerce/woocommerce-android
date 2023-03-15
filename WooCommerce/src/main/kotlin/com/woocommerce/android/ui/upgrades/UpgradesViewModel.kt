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

    sealed interface UpgradesViewState {

        sealed interface HasPlan : UpgradesViewState {
            val name: String
        }

        object Loading : UpgradesViewState {
        }

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
}
