package com.woocommerce.android.ui.plans.trial

import androidx.navigation.NavController
import dagger.assisted.AssistedFactory

@AssistedFactory
interface TrialStatusBarFormatterFactory {
    fun create(navController: NavController): TrialStatusBarFormatter
}
