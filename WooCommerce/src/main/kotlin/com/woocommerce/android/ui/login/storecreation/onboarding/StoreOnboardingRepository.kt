package com.woocommerce.android.ui.login.storecreation.onboarding

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.values
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding.TaskDto
import org.wordpress.android.fluxc.store.OnboardingStore
import javax.inject.Inject

class StoreOnboardingRepository @Inject constructor(
    private val onboardingStore: OnboardingStore,
    private val selectedSite: SelectedSite
) {
    suspend fun fetchOnboardingTasks(): List<OnboardingTask> {
        val result = onboardingStore.fetchOnboardingTasks(selectedSite.get())
        return when {
            result.isError -> {
                WooLog.i(WooLog.T.ONBOARDING, "TODO ERROR HANDLING fetchOnboardingTasks")
                emptyList()
            }
            else -> result.model?.map { it.toOnboardingTask() }
                ?.filter { it.type != MOBILE_UNSUPPORTED }
                ?.sortedBy { it.type.order }
                ?: emptyList()
        }
    }

    private fun TaskDto.toOnboardingTask() =
        OnboardingTask(
            type = values().find { it.id == this.id } ?: MOBILE_UNSUPPORTED,
            isComplete = isComplete,
            isVisible = canView,
            isVisited = isVisited
        )

    data class OnboardingTask(
        val type: OnboardingTaskType,
        val isComplete: Boolean,
        val isVisible: Boolean,
        val isVisited: Boolean,
    )

    enum class OnboardingTaskType(val id: String, val order: Int) {
        ABOUT_YOUR_STORE(id = "store_details", order = 1),
        ADD_FIRST_PRODUCT(id = "products", order = 2),
        LAUNCH_YOUR_STORE(id = "launch_site", order = 3),
        CUSTOMIZE_DOMAIN(id = "add_domain", order = 4),
        WC_PAYMENTS(id = "woocommerce-payments", order = 5),
        MOBILE_UNSUPPORTED(id = "mobile-unsupported", order = -1)
    }
}
