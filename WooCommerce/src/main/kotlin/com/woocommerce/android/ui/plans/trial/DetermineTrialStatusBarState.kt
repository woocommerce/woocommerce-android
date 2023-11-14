package com.woocommerce.android.ui.plans.trial

import com.woocommerce.android.extensions.isFreeTrial
import com.woocommerce.android.tools.ConnectivityObserver
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivityViewModel.BottomBarState
import com.woocommerce.android.ui.plans.domain.CalculatePlanRemainingPeriod
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult.Error
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult.ExpiryAt
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult.NotTrial
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import java.time.Duration
import javax.inject.Inject

class DetermineTrialStatusBarState @Inject constructor(
    private val sitePlanRepository: SitePlanRepository,
    private val selectedSite: SelectedSite,
    private val calculatePlanRemainingPeriod: CalculatePlanRemainingPeriod,
    private val observeConnectionStatus: ConnectivityObserver
) {

    private companion object {
        val internetConnectionDiscoveryDelay: Duration = Duration.ofSeconds(1)
    }

    operator fun invoke(bottomBarStateFlow: Flow<BottomBarState>): Flow<TrialStatusBarState> =
        combine(
            selectedSite.observe(),
            bottomBarStateFlow,
            observeConnectionStatus.observe().onEach {
                if (it == ConnectivityObserver.Status.CONNECTED) {
                    delay(internetConnectionDiscoveryDelay.toMillis())
                }
            }
        ) { selectedSite, bottomBarState, connectionState ->

            when {
                connectionState == ConnectivityObserver.Status.DISCONNECTED -> TrialStatusBarState.Hidden
                bottomBarState == BottomBarState.Hidden -> TrialStatusBarState.Hidden
                selectedSite.isFreeTrial -> fetchFreeTrialDetails()
                else -> TrialStatusBarState.Hidden
            }
        }

    private suspend fun fetchFreeTrialDetails(): TrialStatusBarState {
        return when (val result = sitePlanRepository.fetchFreeTrialExpiryDate(selectedSite.get())) {
            is ExpiryAt -> {
                val expireIn = calculatePlanRemainingPeriod(result.date)
                TrialStatusBarState.Visible(expireIn.days)
            }

            NotTrial, is Error -> TrialStatusBarState.Hidden
        }
    }

    sealed class TrialStatusBarState {
        data class Visible(val daysLeft: Int) : TrialStatusBarState()
        object Hidden : TrialStatusBarState()
    }
}
