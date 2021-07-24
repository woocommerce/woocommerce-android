package com.woocommerce.android.ui.login

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.wordpress.android.login.di.LoginFragmentModule

@Module(includes = [LoginFragmentModule::class])
@InstallIn(ActivityComponent::class)
internal abstract class WooLoginFragmentModule
