package com.woocommerce.android.ui.plans.di

import android.content.Context
import com.woocommerce.android.ui.plans.trial.TrialStatusBarFormatter
import dagger.assisted.AssistedFactory

@AssistedFactory
interface TrialStatusBarFormatterFactory {
    fun create(context: Context): TrialStatusBarFormatter
}
