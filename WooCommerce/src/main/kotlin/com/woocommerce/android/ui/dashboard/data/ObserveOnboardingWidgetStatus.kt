package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.onboarding.ShouldShowOnboarding
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

class ObserveOnboardingWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val storeOnboardingRepository: StoreOnboardingRepository,
    private val shouldShowOnboarding: ShouldShowOnboarding
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .transformLatest {
            // Start with the cached value
            if (shouldShowOnboarding.isOnboardingMarkedAsCompleted()) {
                emit(DashboardWidget.Status.Unavailable(R.string.my_store_widget_onboarding_completed))
            } else {
                emit(DashboardWidget.Status.Available)
            }

            emitAll(
                storeOnboardingRepository.observeOnboardingTasks()
                    .map { tasks ->
                        shouldShowOnboarding.showForTasks(tasks)
                    }
                    .map { showOnboarding ->
                        if (showOnboarding) {
                            DashboardWidget.Status.Available
                        } else {
                            DashboardWidget.Status.Unavailable(R.string.my_store_widget_onboarding_completed)
                        }
                    }
                    .onStart { storeOnboardingRepository.fetchOnboardingTasks() }
            )
        }
        .distinctUntilChanged()
}
