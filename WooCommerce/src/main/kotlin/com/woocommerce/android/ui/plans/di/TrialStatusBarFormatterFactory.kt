package com.woocommerce.android.ui.plans.di

import com.woocommerce.android.ui.plans.domain.StartUpgradeFlow
import com.woocommerce.android.ui.plans.trial.TrialStatusBarFormatter
import dagger.assisted.AssistedFactory

@AssistedFactory
interface TrialStatusBarFormatterFactory {
    fun create(startUpgradeFlowFactory: StartUpgradeFlow): TrialStatusBarFormatter
}
