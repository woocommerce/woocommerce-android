package com.woocommerce.android.di

import com.woocommerce.android.ui.aztec.AztecModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * TODO A temporary class to allow injecting fragments relying on dagger.
 * We should remove it after finishing the migration
 */
@InstallIn(ActivityComponent::class)
@Module(
    includes = [
        AztecModule::class
    ]
)
interface MainActivityAggregatorModule
