package com.woocommerce.android.ui.aiassistant.appfunctions

import com.woocommerce.android.aiassistant.appfunctions.AppFunctionsAccessPolicy
import com.woocommerce.android.aiassistant.appfunctions.AppFunctionsHostNavigation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppFunctionsHostModule {
    @Binds
    internal abstract fun bindAppFunctionsAccessPolicy(impl: WooAppFunctionsAccess): AppFunctionsAccessPolicy

    @Binds
    internal abstract fun bindAppFunctionsHostNavigation(
        impl: WooAppFunctionsHostNavigation,
    ): AppFunctionsHostNavigation
}
