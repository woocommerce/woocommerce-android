package com.woocommerce.android.ui.shipping

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class InstallWcShippingFlowViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val installWcShippingFlowState = flow {
        emit(
            InstallWcShippingState(
                InstallWcShippingOnboardingUi(
                    title = R.string.install_wc_shipping_flow_onboarding_screen_title,
                    subtitle = R.string.install_wc_shipping_flow_onboarding_screen_subtitle,
                    bullets = getBulletPointsForInstallingWcShippingFlow()
                )
            )
        )
    }.asLiveData()

    private fun getBulletPointsForInstallingWcShippingFlow() =
        listOf(
            InstallWcShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_postage_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_postage_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_buy
            ),
            InstallWcShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_print_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_print_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_print
            ),
            InstallWcShippingOnboardingBulletUi(
                title = R.string.install_wc_shipping_flow_onboarding_screen_discounts_bullet_title,
                description = R.string.install_wc_shipping_flow_onboarding_screen_discounts_bullet_desc,
                icon = R.drawable.ic_install_wcs_onboarding_bullet_disccounts
            ),
        )

    data class InstallWcShippingState(
        val installWcShippingOnboardingUi: InstallWcShippingOnboardingUi? = null
    )

    data class InstallWcShippingOnboardingUi(
        @StringRes val title: Int,
        @StringRes val subtitle: Int,
        val bullets: List<InstallWcShippingOnboardingBulletUi>
    )

    data class InstallWcShippingOnboardingBulletUi(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @DrawableRes val icon: Int
    )

    object InstallWcShipping : MultiLiveEvent.Event()
}
