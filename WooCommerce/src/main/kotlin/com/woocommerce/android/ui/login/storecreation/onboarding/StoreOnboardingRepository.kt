package com.woocommerce.android.ui.login.storecreation.onboarding

import com.woocommerce.android.extensions.isFreeTrial
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.values
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding.TaskDto
import org.wordpress.android.fluxc.store.OnboardingStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.LaunchSiteErrorType.ALREADY_LAUNCHED
import org.wordpress.android.fluxc.store.SiteStore.LaunchSiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.LaunchSiteErrorType.UNAUTHORIZED
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreOnboardingRepository @Inject constructor(
    private val onboardingStore: OnboardingStore,
    private val selectedSite: SelectedSite,
    private val siteStore: SiteStore
) {

    private val onboardingTasksCacheFlow: MutableSharedFlow<List<OnboardingTask>> = MutableSharedFlow()

    fun observeOnboardingTasks(): SharedFlow<List<OnboardingTask>> = onboardingTasksCacheFlow

    suspend fun fetchOnboardingTasks() {
        WooLog.d(WooLog.T.ONBOARDING, "Fetching onboarding tasks")
        val result = onboardingStore.fetchOnboardingTasks(selectedSite.get())
        when {
            result.isError ->
                WooLog.i(WooLog.T.ONBOARDING, "Error fetching onboarding tasks: ${result.error}")

            else -> {
                WooLog.d(WooLog.T.ONBOARDING, "Success fetching onboarding tasks")
                val mobileSupportedTasks = result.model?.map { it.toOnboardingTask() }
                    ?.filter { it.type != MOBILE_UNSUPPORTED }
                    ?.toMutableList()
                    ?.apply {
                        if (
                            selectedSite.get().isFreeTrial &&
                            !this.any { it.type == LAUNCH_YOUR_STORE }
                        ) {
                            add(
                                OnboardingTask(
                                    type = LAUNCH_YOUR_STORE,
                                    isComplete = false,
                                    isVisible = true,
                                    isVisited = false
                                )
                            )
                        }
                    }
                    ?.map {
                        if (shouldMarkLaunchStoreAsCompleted(it)) it.copy(isComplete = true)
                        else it
                    }
                    ?.sortedBy { it.type.order }
                    ?.sortedBy { it.isComplete }
                    ?: emptyList()

                onboardingTasksCacheFlow.emit(mobileSupportedTasks)
            }
        }
    }

    private fun shouldMarkLaunchStoreAsCompleted(task: OnboardingTask) =
        task.type == LAUNCH_YOUR_STORE && selectedSite.get().publishedStatus == PUBLIC.value() && !selectedSite.get().isFreeTrial

    suspend fun launchStore(): LaunchStoreResult {
        WooLog.d(WooLog.T.ONBOARDING, "Launching store")
        val result = siteStore.launchSite(selectedSite.get())
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.ONBOARDING, "Error while launching store. Message: ${result.error.message} ")
                Error(
                    when (result.error.type) {
                        ALREADY_LAUNCHED -> LaunchStoreError.ALREADY_LAUNCHED
                        GENERIC_ERROR,
                        UNAUTHORIZED,
                        null -> LaunchStoreError.GENERIC_ERROR
                    }
                )
            }

            else -> {
                WooLog.d(WooLog.T.ONBOARDING, "Site launched successfully")
                Success
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

    enum class OnboardingTaskType(val id: String, val order: Int) {
        ABOUT_YOUR_STORE(id = "store_details", order = 1),
        ADD_FIRST_PRODUCT(id = "products", order = 2),
        LAUNCH_YOUR_STORE(id = "launch_site", order = 3),
        CUSTOMIZE_DOMAIN(id = "add_domain", order = 4),
        WC_PAYMENTS(id = "woocommerce-payments", order = 5),
        PAYMENTS(id = "payments", order = 5), // WC_PAYMENT and PAYMENTS are considered the same task on mobile
        MOBILE_UNSUPPORTED(id = "mobile-unsupported", order = -1)
    }

    sealed class LaunchStoreResult
    object Success : LaunchStoreResult()
    data class Error(val type: LaunchStoreError) : LaunchStoreResult()

    enum class LaunchStoreError {
        ALREADY_LAUNCHED,
        GENERIC_ERROR
    }
}
