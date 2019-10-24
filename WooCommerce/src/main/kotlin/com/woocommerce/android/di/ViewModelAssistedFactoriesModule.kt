package com.woocommerce.android.di

import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module

@AssistedModule
@Module(includes = [AssistedInject_ViewModelAssistedFactoriesModule::class])
abstract class ViewModelAssistedFactoriesModule
