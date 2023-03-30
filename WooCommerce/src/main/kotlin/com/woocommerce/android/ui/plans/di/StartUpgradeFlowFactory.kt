package com.woocommerce.android.ui.plans.di

import androidx.navigation.NavController
import com.woocommerce.android.ui.plans.domain.StartUpgradeFlow
import dagger.assisted.AssistedFactory

@AssistedFactory
interface StartUpgradeFlowFactory {
    fun create(navController: NavController): StartUpgradeFlow
}
