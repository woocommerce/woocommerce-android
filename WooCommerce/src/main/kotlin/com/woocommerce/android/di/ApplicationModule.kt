package com.woocommerce.android.di

import android.app.Application
import android.content.Context

import dagger.Binds
import dagger.Module

@Module
abstract class ApplicationModule {
    // Expose Application as an injectable context
    @Binds
    internal abstract fun bindContext(application: Application): Context
}
