package com.woocommerce.android.ui.plans.trial

import com.woocommerce.android.SitePlanRepository
import com.woocommerce.android.SitePlanRepository.FreeTrialExpiryDateResult.Error
import com.woocommerce.android.SitePlanRepository.FreeTrialExpiryDateResult.ExpiryAt
import com.woocommerce.android.SitePlanRepository.FreeTrialExpiryDateResult.NotTrial
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivityViewModel.BottomBarState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

class DetermineTrialStatusBarState @Inject constructor(
    private val sitePlanRepository: SitePlanRepository,
    private val selectedSite: SelectedSite
) {

    operator fun invoke(bottomBarState: Flow<BottomBarState>): Flow<TrialStatusBarState> =
        selectedSite.observe().combine(bottomBarState) { selectedSite, state ->
            if (state == BottomBarState.Hidden) {
                TrialStatusBarState.Hidden
            } else {
                if (selectedSite.isFreeTrial) {
                    fetchFreeTrialDetails()
                } else {
                    TrialStatusBarState.Hidden
                }
            }
        }

    private suspend fun fetchFreeTrialDetails(): TrialStatusBarState {
        return when (val result = sitePlanRepository.fetchFreeTrialExpiryDate(selectedSite.get())) {
            is ExpiryAt -> {
                val expireIn = Period.between(LocalDate.now(), result.date)
                val days = expireIn.days
                TrialStatusBarState.Visible(days)
            }
            is Error,
            NotTrial -> TrialStatusBarState.Hidden
        }
    }

    sealed class TrialStatusBarState {
        data class Visible(val daysLeft: Int) : TrialStatusBarState()
        object Hidden : TrialStatusBarState()
    }
}
