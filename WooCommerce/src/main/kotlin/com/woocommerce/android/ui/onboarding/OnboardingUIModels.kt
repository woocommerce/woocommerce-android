package com.woocommerce.android.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.PAYMENTS
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class OnboardingTaskUi(
    val taskUiResources: OnboardingTaskUiResources,
    val isCompleted: Boolean,
)

sealed class OnboardingTaskUiResources(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val labelText: Int = 0,
    @DrawableRes val labelIcon: Int = 0
)

data object NameYourStoreTaskRes : OnboardingTaskUiResources(
    icon = R.drawable.ic_onboarding_name_your_store,
    title = R.string.store_onboarding_task_name_store_title,
    description = R.string.store_onboarding_task_name_store_description
)

data object AboutYourStoreTaskRes : OnboardingTaskUiResources(
    icon = R.drawable.ic_onboarding_about_your_store,
    title = R.string.store_onboarding_task_about_your_store_title,
    description = R.string.store_onboarding_task_about_your_store_description
)

data object AddProductTaskRes : OnboardingTaskUiResources(
    icon = R.drawable.ic_onboarding_add_product,
    title = R.string.store_onboarding_task_add_product_title,
    description = R.string.store_onboarding_task_add_product_description,
    labelText = R.string.store_onboarding_task_product_description_ai_generator_text,
    labelIcon = R.drawable.ic_ai
)

data object LaunchStoreTaskRes : OnboardingTaskUiResources(
    icon = R.drawable.ic_onboarding_launch_store,
    title = R.string.store_onboarding_task_launch_store_title,
    description = R.string.store_onboarding_task_launch_store_description
)

data object CustomizeDomainTaskRes : OnboardingTaskUiResources(
    icon = R.drawable.ic_onboarding_customize_domain,
    title = R.string.store_onboarding_task_change_domain_title,
    description = R.string.store_onboarding_task_change_domain_description
)

data object SetupPaymentsTaskRes : OnboardingTaskUiResources(
    icon = R.drawable.ic_onboarding_payments_setup,
    title = R.string.store_onboarding_task_payments_setup_title,
    description = R.string.store_onboarding_task_payments_setup_description
)

data object SetupWooPaymentsTaskRes : OnboardingTaskUiResources(
    icon = R.drawable.ic_onboarding_payments_setup,
    title = R.string.store_onboarding_task_payments_setup_title,
    description = R.string.store_onboarding_task_woopayments_setup_description
)

object NavigateToOnboardingFullScreen : MultiLiveEvent.Event()
object NavigateToSurvey : MultiLiveEvent.Event()
object NavigateToLaunchStore : MultiLiveEvent.Event()
object NavigateToDomains : MultiLiveEvent.Event()
object NavigateToSetupPayments : MultiLiveEvent.Event() {
    val taskId = PAYMENTS.id
}

object NavigateToSetupWooPayments : MultiLiveEvent.Event()
object NavigateToAboutYourStore : MultiLiveEvent.Event()
object NavigateToAddProduct : MultiLiveEvent.Event()
object ShowNameYourStoreDialog : MultiLiveEvent.Event()
