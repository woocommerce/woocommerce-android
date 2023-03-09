package com.woocommerce.android.ui.login.storecreation.onboarding

import com.woocommerce.android.extensions.isCurrentPlanEcommerceTrial
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.values
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding.TaskDto
import org.wordpress.android.fluxc.store.OnboardingStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class StoreOnboardingRepository @Inject constructor(
    private val onboardingStore: OnboardingStore,
    private val selectedSite: SelectedSite,
    private val siteStore: SiteStore
) {
    suspend fun fetchOnboardingTasks(): List<OnboardingTask> {
        val result = onboardingStore.fetchOnboardingTasks(selectedSite.get())
        return when {
            result.isError -> {
                WooLog.i(WooLog.T.ONBOARDING, "TODO ERROR HANDLING fetchOnboardingTasks")
                emptyList()
            }
            else -> {
                val mobileSupportedTasks = result.model?.map { it.toOnboardingTask() }
                    ?.filter { it.type != MOBILE_UNSUPPORTED }
                    ?.toMutableList()
                    ?: emptyList<OnboardingTask>().toMutableList()

                if (
                    selectedSite.get().isCurrentPlanEcommerceTrial &&
                    !mobileSupportedTasks.any { it.type == LAUNCH_YOUR_STORE }
                ) {
                    mobileSupportedTasks.add(
                        OnboardingTask(
                            type = LAUNCH_YOUR_STORE,
                            isComplete = false,
                            isVisible = true,
                            isVisited = false
                        )
                    )
                }

                return mobileSupportedTasks
            }
        }
    }

    suspend fun launchStore(): Result<Unit> {
        WooLog.d(WooLog.T.ONBOARDING, "Launching store")
        val result = siteStore.launchSite(selectedSite.get())
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.ONBOARDING, "Error while launching store. Message: ${result.error.message} ")
                Result.failure(Exception(result.error.message))
            }
            else -> {
                WooLog.d(WooLog.T.ONBOARDING, "Site launched successfully")
                Result.success(Unit)
            }
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
