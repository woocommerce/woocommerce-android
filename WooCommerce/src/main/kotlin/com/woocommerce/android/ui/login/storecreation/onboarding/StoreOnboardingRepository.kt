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

    enum class OnboardingTaskType(val id: String) {
        ABOUT_YOUR_STORE("store_details"),
        ADD_FIRST_PRODUCT("products"),
        LAUNCH_YOUR_STORE("launch_site"),
        CUSTOMIZE_DOMAIN("add_domain"),
        WC_PAYMENTS("woocommerce-payments"),
        MOBILE_UNSUPPORTED("mobile-unsupported")
    }
}
